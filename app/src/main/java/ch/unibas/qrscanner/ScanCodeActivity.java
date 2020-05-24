package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
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
    String dirName;

    int cameraID;

    int lastNum = 0;

    byte[] output;
    byte[] input;

    final Object shouldUpdateQRMonitor = new Object();
    volatile private boolean shouldUpdateQR;
    volatile private byte[] setToQR;

    final Object shouldReceiveMonitor = new Object();
    volatile private boolean shouldReceive;
    volatile private byte[] lastReceived;
    volatile private byte[] wholeInput;


    private String path;

    private static final int PACKETSIZE = 96;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraID = 1;
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);

        Log.d("ScanCodeActivity", "Started as device " + MainActivity.getDevice());

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 40);

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
        wholeInput = new byte[0];

        // Initialize QR code
        //int initialCode = 0;
        //setTextToPopupImageView(getStringOfByteSize(200, initialCode));
        //setTextToPopupImageView("0");

        init();
    }

    private void init() {
        // Initialize Python
        initializePython();
        Log.d("ScanCodeActivity", "Python initialized.");

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
     * Synchronizes Database from device A to device B.
     *  Steps:
     *      1: A sending i_have_list to B
     *      2: B sending i_want_list to A
     *      3: A sending event_list to B
     */
    class SynchronizerThread extends Thread {

        @Override
        public void run() {
            output = new byte[PACKETSIZE];
            input = new byte[PACKETSIZE];
            if (MainActivity.getDevice() == 'A') {
                // Get i_have_list
                PyObject i_have_list_py = transport.callAttr("get_i_have_list", path);
                Log.d("ScanCodeActivity", "i_have_list: " + i_have_list_py);

                // Convert PyObject to byte[]
                byte[] i_have_list = PyObject2ByteArray(i_have_list_py);
                Log.d("ScanCodeActivity", "i_have_list: " + Arrays.toString(i_have_list));
                Log.d("ScanCodeActivity", "i_have_list length: " + i_have_list.length);

                //// Step 1: Send i_have_list to B ////

                // Send packet as subpackets
                int numSubPackets = (i_have_list.length / (PACKETSIZE-1)) + 1;
                Log.d("ScanCodeActivity", "numSubPackets: " + numSubPackets);
                for (int i = 0; i < numSubPackets; i++) {

                    boolean last = false;
                    if (i == numSubPackets-1) {
                        last = true;
                    }

                    // Create subPacket of i_have_list
                    output = new byte[PACKETSIZE];
                    if (!last) {
                        System.arraycopy(i_have_list, i * (PACKETSIZE-1), output=new byte[PACKETSIZE], 1, PACKETSIZE - 1);
                        output[0] = (byte)0;
                    } else {
                        System.arraycopy(i_have_list, i * (PACKETSIZE-1), output=new byte[i_have_list.length % (PACKETSIZE - 1)+1], 1, i_have_list.length % (PACKETSIZE - 1));
                        output[0] = (byte)1;
                    }
                    Log.d("ScanCodeActivity", "i_have_list (" + (i+1) + "/" + numSubPackets + "): " + Arrays.toString(output));

                    // Set output
                    //Log.d("ScanCodeActivity", "output: " + Arrays.toString(output));
                    setToQR = output;
                    synchronized (shouldUpdateQRMonitor) {
                        shouldUpdateQR = true;
                    }

                    Log.d("ScanCodeActivity", "Wait for receiving...");
                    synchronized (shouldReceiveMonitor) {
                        shouldReceive = true;
                        try {
                            shouldReceiveMonitor.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d("ScanCodeActivity", "Received a packet.");

                }


                //// Step 2: Receive i_want_list from B ////
                synchronized (shouldReceiveMonitor) {

                }

                // Handle i_want_list
                PyObject i_want_list_py;
                synchronized (shouldReceiveMonitor) {
                    byte[] i_want_list = lastReceived;
                    i_want_list_py = byteArray2PyObject(i_want_list);
                }

                //// Step 3: Send event_list to B ////
                synchronized (shouldReceiveMonitor) {

                }
                // Set new QR code. Need to read a qr code to make this change effective.
                synchronized (shouldUpdateQRMonitor) {
                    byte[] toQRView = PyObject2ByteArray(transport.callAttr("get_event_list", i_want_list_py, path));
                    setToQR = toQRView;
                    Log.d("ScanCodeActivity", "Set QR code to byte[] of length: " + toQRView.length);
                    shouldUpdateQR = true;
                }

                Log.i("ScanCodeActivity", "Synchronization complete. Please wait for Device B to finish!");



            } else if (MainActivity.getDevice() == 'B') {

                //// Step 1: Receive i_have_list from A ////
                synchronized (shouldReceiveMonitor) {
                    while (true) {
                        // Start accepting QR codes
                        shouldReceive = true;

                        // Wait for scanner to get a packet (i_have_list).
                        try {
                            shouldReceiveMonitor.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Concatenate lastReceived into wholeInput
                        Log.d("ScanCodeActivity", "new lastReceived: " + Arrays.toString(lastReceived));
                        Log.d("ScanCodeActivity", "wholeInput before concat: " + Arrays.toString(wholeInput));
                        wholeInput = Arrays.copyOf(wholeInput, wholeInput.length + lastReceived.length-1);
                        System.arraycopy(lastReceived, 1, wholeInput, wholeInput.length - (lastReceived.length-1), lastReceived.length-1);
                        Log.d("ScanCodeActivity", "wholeInput after concat: " + Arrays.toString(wholeInput));

                        // if 'last' flag is 1
                        if (lastReceived[0] != 0) {
                            Log.d("ScanCodeActivity", "Whole i_want_list received.");
                            break;
                        }



                    }
                }

                // Process i_have_list
                byte[] i_want_list;
                PyObject extension_list_py;
                synchronized (shouldReceiveMonitor) {
                    if (lastReceived==null) throw new AssertionError("lastReceived must not be null!");
                    byte[] i_have_list = wholeInput;
                    PyObject i_have_list_py = byteArray2PyObject(i_have_list);
                    PyObject i_want_list_and_extension_list = transport.callAttr("get_i_want_list", i_have_list_py, path);
                    PyObject i_want_list_py = i_want_list_and_extension_list.asList().get(0);
                    i_want_list = PyObject2ByteArray(i_want_list_py);
                    extension_list_py = i_want_list_and_extension_list.asList().get(1);
                }

                //// Step 2: Send i_want_list to A ////
                // Set new QR code. Need to read a qr code to make this change effective.
                synchronized (shouldUpdateQRMonitor) {
                    setToQR = i_want_list;
                    Log.d("ScanCodeActivity", "Set QR code to byte[] of length: " + i_want_list.length);
                    shouldUpdateQR = true;
                }

                // Start accepting QR codes
                synchronized (shouldReceiveMonitor) {
                    shouldReceive = true;
                    Log.d("ScanCodeActivity", "shouldReceive set to " + shouldReceive);
                }

                //// Step 3: Receive event_list from A ////
                // Wait for scanner to get a packet (event_list).
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
                    transport.callAttr("sync_extensions", extension_list_py, event_list_py, path);
                }

                toneGenerator.startTone(AudioManager.STREAM_ALARM, 1500);

                Log.i("ScanCodeActivity", "Synchronization complete. You may exit both Apps now.");

                //onBackPressed();
            } else {
                throw new IllegalArgumentException("Device should be 'A' or 'B'.");
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
        Log.d("ScanCodeActivity", "py is: " + py);

        // Python equivalent to
        //  "import transport"
        transport = py.getModule("transport");
        Log.d("ScanCodeActivity", "transport is: " + transport);
        //Log.d("ScanCodeActivity", "transport KEYSET: " + transport.keySet());
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
            Log.d("ScanCodeActivity", "File in path: " + inFile);
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

            // Extract subPacket
            boolean last = (i_have_list.length < PACKETSIZE);
            byte[] output = new byte[PACKETSIZE];
            System.arraycopy(i_have_list, 0, output, 1, Math.min(PACKETSIZE-1, i_have_list.length-1));
            output[0] = (byte)(last ? 1 : 0);
            Log.d("ScanCodeActivity", "Set initial QR code for device A.");
            Log.d("ScanCodeActivity", "Initial QR output: " + Arrays.toString(output));
            Log.d("ScanCodeActivity", "Initial QR output length: " + output.length);

            // Set initial code
            setByteArrayToPopupImageView(output);
        }
    }

    @Override
    public void handleResult(Result result) {
        Log.d("ScanCodeActivity", "QR code detected.");

        //MainActivity.resultTextView.setText(result.getText());


        // Update QR code if needed
        synchronized (shouldUpdateQRMonitor) {
            if (shouldUpdateQR) {
                shouldUpdateQR = false;
                setByteArrayToPopupImageView(setToQR);
                setToQR = null;
            }
        }

        // Handle QR code result
        synchronized (shouldReceiveMonitor) {
            Log.d("ScanCodeActivity", "shouldReceive: " + shouldReceive);
            if (shouldReceive) {
                byte[] nowReceived = Base64.decode(result.getText(), Base64.DEFAULT);

                if (nowReceived.length == 1)
                    Log.d("ScanCodeActivity", "Received empty message.");

                Log.d("ScanCodeActivity", "nowReceived: " + Arrays.toString(nowReceived));

                // Check if scanned code has already been scanned
                if (!Arrays.equals(nowReceived, lastReceived)) {
                    shouldReceive = false;
                    lastReceived = nowReceived;

                    setByteArrayToPopupImageView(lastReceived);
                    playBeep(100);
                    shouldReceiveMonitor.notifyAll();
                } else {
                    Log.d("ScanCodeActivity", "Read same qr code as before.");
                }
            }
        }

        onPause();
        onResume();
    }


    private static byte[] PyObject2ByteArray(PyObject o) {
        return o.toJava(byte[].class);
    }

    private PyObject byteArray2PyObject(byte[] array) {
        return transport.callAttr("get_bytes_from_tojava_pyobject", PyObject.fromJava(array));
    }

    private int setByteArrayToPopupImageView(byte[] binaryData) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            String base64Text = Base64.encodeToString(binaryData, Base64.DEFAULT);
            Log.d("ScanCodeActivity", "Writing following base64Text to QR code view: " + base64Text);
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
        try {
            Thread.sleep(pauseLengthInMilliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void playBeep(int playLengthInMilliseconds) {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
        try {
            Thread.sleep(playLengthInMilliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    ///////////////////////////////////////////////////////
}
