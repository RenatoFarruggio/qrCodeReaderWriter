package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ch.unibas.qrscanner.intenthelper.IntentIntegrator;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    Button scanButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView)findViewById(R.id.result_text);
        scanButton = (Button) findViewById(R.id.btn_scan);

        scanButton.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {

                // Using back camera
                startActivity(new Intent(getApplicationContext(), ScanCodeActivity.class));

                // Using front camera   // to be continued....
                // if we do not use this, remove the intenthelper package!!
                //IntentIntegrator integrator = new IntentIntegrator();
                //integrator.setCameraId(1);
                //integrator.initiateScan();
                //startActivity(new Intent(getApplicationContext(), ScanCodeActivity.class).putExtra("SCAN_CAMERA_ID", 1));
            }
        });
    }
}
