package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.util.Arrays;


public class ScanCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    ZXingScannerView scannerView;
    ToneGenerator toneGenerator;
    Dialog qrPopupDialog;
    ImageView popupImageView;
    int qrSize;

    Python py;
    PyObject transport;
    PyObject sync;
    String dirName;

    int cameraID;

    int lastNum = 0;

    volatile private boolean shouldUpdateQR;
    volatile private byte[] setToQR;

    final Object shouldUpdateQRMonitor = new Object();

    volatile private boolean shouldReceive;
    volatile private byte[] lastReceived;

    final Object shouldReceiveMonitor = new Object();

    String path;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraID = 1;
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);

        Log.d("ScanCodeActivity", "Started as device " + MainActivity.getDevice());

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        qrPopupDialog = new Dialog(this);
        qrPopupDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        qrPopupDialog.setContentView(getLayoutInflater().inflate(R.layout.image_layout, null));
        qrPopupDialog.setCanceledOnTouchOutside(false);
        qrPopupDialog.show();

        popupImageView = qrPopupDialog.findViewById(R.id.popupImageView);

        qrSize = getResources().getDisplayMetrics().widthPixels;

        synchronized (shouldReceiveMonitor) {
            shouldReceive = false;
        }

        synchronized (shouldUpdateQRMonitor) {
            shouldUpdateQR = false;
        }

        dirName = "/databases/udpDir/";

        // Initialize QR code
        //int initialCode = 0;
        //setTextToPopupImageView(getStringOfByteSize(200, initialCode));
        //setTextToPopupImageView("0");

        // Initialize Python
        initializePython();

        // Setup Path
        initializePath();
        Log.d("ScanCodeActivity", "Path initialized.");

        // initialize QR code:
        initializeQRCode();

        // Start synchronizer thread
        SynchronizerThread synchronizer = new SynchronizerThread();
        synchronizer.start();
    }

    /**
     * Synchronizes Database from Device A to device B.
     */
    class SynchronizerThread extends Thread {


        public void run1() {
           while (true) {
               try {
                   Log.d("ScanCodeActivity", "THIS IS A MESSAGE");
                   Thread.sleep(1000);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
        }

        public void run() {
            // Synchronize
            if (MainActivity.getDevice() == 'A') {
                // Get i_have_list
                PyObject i_have_list_py = transport.callAttr("get_i_have_list", path);
                Log.d("ScanCodeActivity", "i_have_list: " + i_have_list_py);

                // Convert PyObject to byte[]
                byte[] i_have_list = PyObject2ByteArray(i_have_list_py);
                Log.d("ScanCodeActivity", "i_have_list1: " + Arrays.toString(i_have_list));
                Log.d("ScanCodeActivity", "length of array: " + i_have_list.length);

                Log.d("ScanCodeActivity", "length of str array: " + Arrays.toString(i_have_list).length());


                Log.d("ScanCodeActivity", "Show QR code.");

                // Start accepting qr codes
                synchronized (shouldReceiveMonitor) {
                    shouldReceive = true;
                }

                // WAIT for receiving //

                Log.d("ScanCodeActivity", "Wait for receiving...");

                synchronized (shouldReceiveMonitor) {
                    try {
                        Log.d("ScanCodeActivity", "Before");
                        shouldReceiveMonitor.wait();
                        Log.d("ScanCodeActivity", "After");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d("ScanCodeActivity", "I've waited.");


                // Handle input from Device B
                PyObject i_want_list_py;
                synchronized (shouldReceiveMonitor) {
                    byte[] i_want_list = lastReceived;
                    i_want_list_py = byteArray2PyObject(i_want_list);
                }

                synchronized (shouldUpdateQRMonitor) {
                    shouldUpdateQR = true;
                    setToQR = PyObject2ByteArray(transport.callAttr("get_event_list", i_want_list_py, path));
                }

                Log.i("ScanCodeActivity", "Synchronization complete. Please wait for Device B!");

            } else if (MainActivity.getDevice() == 'B') {

                // Start accepting QR codes
                synchronized (shouldReceiveMonitor) {
                    shouldReceive = true;
                }

                // Wait for scanner to get a packet.
                synchronized (shouldReceiveMonitor) {
                    try {
                        shouldReceiveMonitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                byte[] i_want_list;
                PyObject extension_list_py;
                synchronized (shouldReceiveMonitor) {
                    if (lastReceived==null) throw new AssertionError("lastReceived must not be null!");
                    byte[] i_have_list = lastReceived;
                    PyObject i_have_list_py = byteArray2PyObject(i_have_list);
                    PyObject i_want_list_and_extension_list = transport.callAttr("get_i_want_list", i_have_list_py);
                    PyObject i_want_list_py = i_want_list_and_extension_list.asList().get(0);
                    i_want_list = PyObject2ByteArray(i_want_list_py);
                    extension_list_py = i_want_list_and_extension_list.asList().get(1);
                }

                synchronized (shouldUpdateQRMonitor) {
                    setToQR = i_want_list;
                    shouldUpdateQR = true;
                }

                // Wait for scanner to get a packet.
                synchronized (shouldReceiveMonitor) {
                    try {
                        shouldReceiveMonitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Read event_list
                synchronized (shouldReceiveMonitor) {
                    byte[] event_list = lastReceived;
                    PyObject event_list_py = byteArray2PyObject(event_list);
                    transport.callAttr("sync_extensions", extension_list_py, event_list_py);
                }


                Log.i("ScanCodeActivity", "Synchronization complete.");

            }

        }

    }


    private void initializePython() {

        // Start Python
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Get python instance
        py = Python.getInstance();
        Log.d("ScanCodeActivity", "Python is: " + py);


        // Python equivalent to
        //  "import transport"
        transport = py.getModule("transport");
        Log.d("ScanCodeActivity", "transport is: " + transport);
        Log.d("ScanCodeActivity", "transport KEYSET: " + transport.keySet());
        // transport KEYSET:
        // [__builtins__, __cached__, __doc__, __file__, __loader__, __name__, __package__,
        // __spec__, cbor, get_event_list, get_i_have_list, get_i_want_list, pcap, sync]
    }


    private int initializePath() {
        // TODO: implement absolutePathname String parameter

        // Create '/databases' directory in ch.unibas.qrscanner.files
        path = getApplicationContext().getFilesDir().getPath();
        if (!path.substring(path.lastIndexOf("/")+1).equals("files")) {
            path += "/files";
        }
        path += dirName;

        File f = new File(path);
        Log.d("ScanCodeActivity", "Path: " + path);
        if (!f.exists())
            Log.d("ScanCodeActivity", "Created Path: " + path);
        f.mkdirs();

        File[] files = f.listFiles();
        for (File inFile : files) {
            Log.d("ScanCodeActivity", "inFile: " + inFile);
        }


        if (false) // if error
            return -1;
        return 0;
    }


    private void initializeQRCode() {
        if (MainActivity.getDevice() == 'A') {
            // Get i_have_list
            PyObject i_have_list_py = transport.callAttr("get_i_have_list", path);
            //Log.d("ScanCodeActivity", "i_have_list: " + i_have_list_py);

            // Convert PyObject to byte[]
            byte[] i_have_list = PyObject2ByteArray(i_have_list_py);
            setBase64ToPopupImageView(i_have_list);
        }
    }

    @Override
    public void handleResult(Result result) {
        //onPause();


        Log.d("ScanCodeActivity", "FOUND RESULT!");

        playBeep(100);
        //MainActivity.resultTextView.setText(result.getText());



        // Actually handle the result //
        //String outText = handleResultByCounting(result.getText());
        //String outText = handleResultByCountingInLargePackets(result.getText());
        //String outText = handleResultByCountingInDatabase(result.getText());
        //String outText = handleResultBySyncingLog(result.getText());


        synchronized (shouldUpdateQRMonitor) {
            if (shouldUpdateQR) {
                shouldUpdateQR = false;
                setBase64ToPopupImageView(setToQR);
                setToQR = null;
            }
        }

        synchronized (shouldReceiveMonitor) {
            if (shouldReceive) {
                shouldReceive = false;
                byte[] nowReceived = Base64.decode(result.getText(), Base64.DEFAULT);
                if (!Arrays.equals(nowReceived, lastReceived)) {
                    lastReceived = nowReceived;
                    playBeep(100);
                    shouldReceiveMonitor.notifyAll();
                }
            }
        }

        /*try {
            semaphore.acquire();
            try {
                Log.d("ScanCodeActivity", "shouldReceive: " + shouldReceive);
                if (shouldReceive) {
                    shouldReceive = false;
                    lastReceived = Base64.decode(result.getText(), Base64.DEFAULT);
                    playBeep(100);
                }
                synchronized (shouldUpdateQRMonitor) {
                    if (shouldUpdateQR) {
                        shouldUpdateQR = false;
                        setBase64ToPopupImageView(setToQR);
                        setToQR = null;
                    }
                }
            } finally {
                synchronized (shouldReceiveMonitor) {
                    shouldReceiveMonitor.notifyAll();
                    semaphore.release();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/



        /*synchronized (shouldReceiveMonitor) {
            if (shouldReceive) {
                shouldReceive = false;
                lastReceived = Base64.decode(result.getText(), Base64.DEFAULT);
                //lastReceived = Base64.decode(result.getRawBytes(), Base64.DEFAULT);
                playBeep(100);
                shouldReceiveMonitor.notify();
            }
        }*/




        // Write text into QR code
        //setTextToPopupImageView(outText);
        //setBase64ToPopupImageView(outData);

        onPause();
        onResume();
    }


    private static byte[] PyObject2ByteArray(PyObject o) {
        return o.toJava(byte[].class);
    }

    private PyObject byteArray2PyObject(byte[] array) {
        return transport.callAttr("get_bytes_from_tojava_pyobject", PyObject.fromJava(array));
    }


    private int setBase64ToPopupImageView(byte[] binaryData) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            String base64Text = Base64.encodeToString(binaryData, Base64.DEFAULT);
            Log.d("ScanCodeActivity", "base64Text: " + base64Text);
            Log.d("ScanCodeActivity", "base64Text length: " + base64Text.length());
            BitMatrix bitMatrix = multiFormatWriter.encode(base64Text, BarcodeFormat.QR_CODE, qrSize, qrSize);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            popupImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    private void playBeep(int playLengthInMilliseconds, int pauseLengthInMilliseconds) {
        playBeep(playLengthInMilliseconds);
        SystemClock.sleep(pauseLengthInMilliseconds);
    }

    private void playBeep(int playLengthInMilliseconds) {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
        SystemClock.sleep(playLengthInMilliseconds);
        toneGenerator.stopTone();
    }

    @Override
    protected void onPause() {
        super.onPause();

        scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

        scannerView.setResultHandler(this);
        scannerView.startCamera(cameraID);
    }

    protected void switchCamera() {
        onPause();
        cameraID = (cameraID+1)%2;
        onResume();
    }

    protected void switchCameraToFrontcam() {
        onPause();
        cameraID = 1;
        onResume();
    }

    protected void switchCameraToBackcam() {
        onPause();
        cameraID = 0;
        onResume();
    }

    /*
    // CALLBACKS
    public byte[] rd_callback() { // called when logSync wants to receive
        lastReceived = null;
        shouldReceive = true;
        while (true) {
            if (lastReceived != null) {
                return lastReceived;
            }
        }
        //return "TestString".getBytes(StandardCharsets.UTF_8);
    }

    // Returns 0 if successful.
    // Returns -1 if error occured.
    public int wr_callback(byte[] binData) {  // called when logSync wants to send

        int errCode = setBase64ToPopupImageView(binData);
        return errCode;
    }*/


    //// THIS SECTION IS FOR DEV TESTING PURPOSES ONLY ////

    // Return a string of a certain bytesize.
    // 1 char (utf-16) needs 2 bytes.
    private String getStringOfByteSize(int size, int code) {
        // Must have: size >= 1


        char[] chars = new char[size-1];
        Arrays.fill(chars, 'a');
        //chars[chars.length-1] = (char) code;
        String text = new String(chars) + code;

        //Log.d("ScanCodeActivity", "whole object: " + text);
        //Log.d("ScanCodeActivity", "utf-8: " + text.getBytes(StandardCharsets.UTF_8));
        //Log.d("ScanCodeActivity", "utf-16: " + text.getBytes(StandardCharsets.UTF_16));
        //Log.d("ScanCodeActivity", "byteSize: " + text.getBytes(StandardCharsets.UTF_8).length);
        //Log.d("ScanCodeActivity", "code: " + text.charAt(size-1));
        return text;
    }


    private String handleResultByCounting(String text) {
        int num = Integer.parseInt(text);
        String outText = (num+1)+"";

        for (int i = 0; i < num; i++) {
            playBeep(100, 100);
        }

        if (num >= 10) {
            playBeep(1000, 0);
            onBackPressed();
        }
        return outText;
    }

    private String handleResultByCountingInLargePackets(String text) {
        int num = text.charAt(text.length()-1);
        String outTextLargePacket = text;
        if (num>lastNum) {
            lastNum = num;
            //String outText = (num+1)+"";
            outTextLargePacket = getStringOfByteSize(text.length(), num + 1);

            for (int i = 0; i < 2; i++) {
                playBeep(900, 100);
            }

            if (num >= 8) {
                playBeep(5000, 0);
                MainActivity.resultTextView.setText("Done!");
                onBackPressed();
            }
        } else {
            playBeep(2900,100);
        }
        return outTextLargePacket;
    }

    private String handleResultByCountingInDatabase(String text) {
        int num = text.charAt(text.length()-1);
        String outTextLargePacket = text;
        if (num>lastNum) {
            lastNum = num;
            //String outText = (num+1)+"";
            outTextLargePacket = getStringOfByteSize(text.length(), num + 1);

            for (int i = 0; i < 2; i++) {
                playBeep(900, 100);
            }

            if (num >= 8) {
                playBeep(5000, 0);
                MainActivity.resultTextView.setText("Done!");
                onBackPressed();
            }
        } else {
            playBeep(2900,100);
        }
        return outTextLargePacket;
    }


    private void setTextToPopupImageView(String text) {
        // Initially copied from:
        // https://medium.com/@aanandshekharroy/generate-barcode-in-android-app-using-zxing-64c076a5d83a
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize);
            //bitMatrix.rotate180();
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            //MainActivity.qrImageView.setImageBitmap(bitmap);
            popupImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
    ///////////////////////////////////////////////////////

}
