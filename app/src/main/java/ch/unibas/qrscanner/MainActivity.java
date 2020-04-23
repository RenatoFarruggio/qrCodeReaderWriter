package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

                startActivity(new Intent(getApplicationContext(), ScanCodeActivity.class));
            }
        });
    }
}
