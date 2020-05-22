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

import com.chaquo.python.Kwarg;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;


public class ScanCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    // TODO: make a daemon thread for camera activity and close it in onBackPressed().
    //  (In case camera pauses when running python code.)
    ZXingScannerView scannerView;
    ToneGenerator toneGenerator;
    Dialog qrPopupDialog;
    ImageView popupImageView;
    int qrSize;
    char device; // 'A' or 'B'

    Python py;
    PyObject transport;
    String dirName;

    int lastNum = 0;

    boolean shouldReceive;
    byte[] lastReceived;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        shouldReceive = false;

        dirName = "databases/udpDir";

        // Initialize QR code
        //int initialCode = 0;
        //setTextToPopupImageView(getStringOfByteSize(200, initialCode));
        //setTextToPopupImageView("0");

        // Initialize Python
        initializePython();

    }

    @Override
    public void handleResult(Result result) {
        //playBeep(500, 500);
        //MainActivity.resultTextView.setText(result.getText());



        // Actually handle the result //
        //String outText = handleResultByCounting(result.getText());
        //String outText = handleResultByCountingInLargePackets(result.getText());
        //String outText = handleResultByCountingInDatabase(result.getText());
        //String outText = handleResultBySyncingLog(result.getText());
        if (shouldReceive) {
            shouldReceive = false;
            lastReceived = Base64.decode(result.getText(), Base64.DEFAULT);
            //lastReceived = Base64.decode(result.getRawBytes(), Base64.DEFAULT);
            playBeep(100);
            // TODO: optimize this (maybe remove it?)
            // These commands are needed to prevent the camera from freezing
            onPause();
            onResume();
        }




        // Write text into QR code
        //setTextToPopupImageView(outText);
        //setBase64ToPopupImageView(outData);
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


        //PyObject transportObject = py.getModule("transport");
        //Log.d("ScanCodeActivity", "pythonModule is: " + transportObject);

        // Python equivalent to
        //  "import sys"
        PyObject sys = py.getModule("sys");
        Log.d("ScanCodeActivity", "sys is: " + sys);
        Log.d("ScanCodeActivity", "SYS KEYSET: " + sys.keySet());
        // sys KEYSET:
        // [__breakpointhook__, __displayhook__, __doc__, __excepthook__, __interactivehook__,
        // __loader__, __name__, __package__, __spec__, __stderr__, __stdin__, __stdout__,
        // __unraisablehook__, __warningregistry__, _base_executable, _clear_type_cache,
        // _current_frames, _debugmallocstats, _framework, _getframe, _git, _home, _xoptions,
        // abiflags, addaudithook, api_version, argv, audit, base_exec_prefix, base_prefix,
        // breakpointhook, builtin_module_names, byteorder, call_tracing, callstats, copyright,
        // displayhook, dont_write_bytecode, exc_info, excepthook, exec_prefix, executable, exit,
        // flags, float_info, float_repr_style, get_asyncgen_hooks,
        // get_coroutine_origin_tracking_depth, getallocatedblocks, getandroidapilevel,
        // getcheckinterval, getdefaultencoding, getdlopenflags, getfilesystemencodeerrors,
        // getfilesystemencoding, getprofile, getrecursionlimit, getrefcount, getsizeof,
        // getswitchinterval, gettrace, hash_info, hexversion, implementation, int_info, intern,
        // is_finalizing, maxsize, maxunicode, meta_path, modules, path, path_hooks,
        // path_importer_cache, platform, prefix, pycache_prefix, set_asyncgen_hooks,
        // set_coroutine_origin_tracking_depth, setcheckinterval, setdlopenflags, setprofile,
        // setrecursionlimit, setswitchinterval, settrace, stderr, stdin, stdout, thread_info,
        // unraisablehook, version, version_info, warnoptions]



        Log.d("ScanCodeActivity", "222");
        // Create Directory for Databases
        // Python equivalent to
        //  "main.check_dir(dir1)"
        String path = getApplicationContext().getFilesDir().getPath() + "/" + dirName + "/";
        File f = new File(path);
        if (!f.exists())
            f.mkdir();
        File[] files = f.listFiles();
        for (File inFile : files) {
            Log.d("ScanCodeActivity", "inFile: " + inFile);
        }


        Log.d("ScanCodeActivity", "333");
        // Synchronize
        if (MainActivity.getDevice() == 'A') {
            // FIXME: array0 should be equal to array2.
            //PyObject i_have_list = transport.callAttr("get_i_have_list", path);
            PyObject array0 = transport.callAttr("get_i_have_list", path);
            Log.d("ScanCodeActivity", "array0: " + array0);

            //byte[] array1 = i_have_list.toJava(byte[].class);
            byte[] array1 = array0.toJava(byte[].class);
            Log.d("ScanCodeActivity", "array1: " + array1);


            PyObject array2 = PyObject.fromJava(array1);
            Log.d("ScanCodeActivity", "array2: " + array2);

            /*
            // FIXMEE: i_have_list, a PyObject, should be the same as itself transformed into a byte[], transformed back into a PyObject.

            Log.d("ScanCodeActivity", "i_have_list: " + i_have_list);
            Log.d("ScanCodeActivity", "Size of i_have_list: " + sys.callAttr("getsizeof", "i_have_list"));

            // PyObject: i_have_list -> byte[]: array1
            byte[] array1 = PyObject2ByteArray(i_have_list);
            Log.d("ScanCodeActivity", "length of i_have_list as java byte[]: " + array1.length);

            // byte[]: array1 -> PyObject: array2
            PyObject array2 = byteArray2PyObject(array1);
            Log.d("ScanCodeActivity", "array2: " + array2);
            Log.d("ScanCodeActivity", "size of array2: " + sys.callAttr("getsizeof", "array2"));
            //setBase64ToPopupImageView(array);

            // THEN i_have_list SHOULD BE EQUIVALENT TO array2.
            // If not, we will pass wrong bytes.

             */


        } else if (MainActivity.getDevice() == 'B') {


        }


    }

    private static byte[] PyObject2ByteArray(PyObject o) {
        return o.toJava(byte[].class);
    }

    private static PyObject byteArray2PyObject(byte[] array) {
        Log.d("ScanCodeActivity", "array size: " + array.length);
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte)((int)(array[i]+256) % 256);
        }
        //for (byte b : array) {
        //    Log.d("ScanCodeActivity", "array1: " + b);
        //}
        PyObject out = PyObject.fromJava(array);
        return out;
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

    private int setBase64ToPopupImageView(byte[] binaryData) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            String base64Text = Base64.encodeToString(binaryData, Base64.DEFAULT);
            BitMatrix bitMatrix = multiFormatWriter.encode(base64Text, BarcodeFormat.QR_CODE, qrSize, qrSize);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            //MainActivity.qrImageView.setImageBitmap(bitmap);
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
        scannerView.startCamera(1);
    }

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
    }


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

    ///////////////////////////////////////////////////////

}
