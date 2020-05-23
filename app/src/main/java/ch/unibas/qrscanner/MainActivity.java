package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.TedPermissionResult;
import com.tedpark.tedpermission.rx2.TedRx2Permission;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    Button scanButtonA;
    Button scanButtonB;
    public static ImageView qrImageView;

    private static char device;


    public static char getDevice() {
        return device;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView)findViewById(R.id.result_text);
        scanButtonA = (Button) findViewById(R.id.btn_scan_A);
        scanButtonB = (Button) findViewById(R.id.btn_scan_B);
        qrImageView = (ImageView) findViewById(R.id.result_qr_code);

        scanButtonA.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                device = 'A';
                onPause();
                startActivity(new Intent(getApplicationContext(), ScanCodeActivity.class));
            }
        });

        scanButtonB.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                device = 'B';
                onPause();
                startActivity(new Intent(getApplicationContext(), ScanCodeActivity.class));
            }
        });

        // PERMISSION STUFF //

        TedRx2Permission.with(this)
                .setRationaleTitle("Camera permission")
                .setRationaleMessage("We need permission to use the camera in order for the app to work properly.") // "we need permission for read contact and find your location"
                .setPermissions(Manifest.permission.CAMERA)
                .request()
                .subscribe(new Consumer<TedPermissionResult>() {
                    @Override
                    public void accept(TedPermissionResult tedPermissionResult) throws Exception {
                        if (tedPermissionResult.isGranted()) {
                            Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Camera Permission Denied\n" + tedPermissionResult.getDeniedPermissions().toString(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }
}
