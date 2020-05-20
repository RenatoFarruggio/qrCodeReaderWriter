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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class ScanCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {


    ZXingScannerView scannerView;
    ToneGenerator toneGenerator;
    Dialog qrPopupDialog;
    ImageView popupImageView;
    int qrSize;

    Python py;
    PyObject logSyncMain;
    PyObject server;
    PyObject client;

    int lastNum = 0;

    boolean shouldReceive;
    byte[] lastReceived;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        qrPopupDialog = new Dialog(this);
        qrPopupDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        qrPopupDialog.setContentView(getLayoutInflater().inflate(R.layout.image_layout, null));
        qrPopupDialog.setCanceledOnTouchOutside(false);
        qrPopupDialog.show();

        popupImageView = qrPopupDialog.findViewById(R.id.popupImageView);

        qrSize = getResources().getDisplayMetrics().widthPixels;

        shouldReceive = false;

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
        //  "import main"
        logSyncMain = py.getModule("main");
        Log.d("ScanCodeActivity", "logSyncMain is: " + logSyncMain);
        // logSyncMain KEYSET:
        // [__builtins__, __cached__, __doc__, __file__, __loader__, __name__,
        // __package__, __spec__, check_dir, create_list_of_files, dump_directories_cont, os, pcap,
        // sync, sync_directories, sys, udp_connection]

        PyObject udpConnection = logSyncMain.get("udp_connection");
        Log.d("ScanCodeActivity", "udpConnection is: " + udpConnection);
        // udpConnection KEYSET:
        // [Client, Server, __builtins__, __cached__, __doc__, __file__, __loader__, __name__,
        // __package__, __spec__, buffSize, cbor, main, pcap, socket, sync]

        server = udpConnection.get("Server");
        Log.d("ScanCodeActivity", "server: " + server);
        // server KEYSET:
        // [__class__, __delattr__, __dict__, __dir__, __doc__, __eq__, __format__, __ge__,
        // __getattribute__, __gt__, __hash__, __init__, __init_subclass__, __le__, __lt__,
        // __module__, __ne__, __new__, __reduce__, __reduce_ex__, __repr__, __setattr__,
        // __sizeof__, __str__, __subclasshook__, __weakref__, get_packet_to_send_as_bytes]

        client = udpConnection.get("Client");
        Log.d("ScanCodeActivity", "client: " + client);
        // client KEYSET:
        // [__class__, __delattr__, __dict__, __dir__, __doc__, __eq__, __format__, __ge__,
        // __getattribute__, __gt__, __hash__, __init__, __init_subclass__, __le__, __lt__,
        // __module__, __ne__, __new__, __reduce__, __reduce_ex__, __repr__, __setattr__,
        // __sizeof__, __str__, __subclasshook__, __weakref__,
        // get_compared_files, get_packet_to_receive_as_bytes]

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
