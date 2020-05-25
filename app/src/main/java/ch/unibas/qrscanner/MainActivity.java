package ch.unibas.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gun0912.tedpermission.TedPermissionResult;
import com.tedpark.tedpermission.rx2.TedRx2Permission;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    Button scanButtonA;
    Button scanButtonB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButtonA = (Button) findViewById(R.id.btn_scan_A);
        scanButtonB = (Button) findViewById(R.id.btn_scan_B);

        scanButtonA.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                Intent startScannerIntent = new Intent(getApplicationContext(), ScanCodeActivity.class);

                startScannerIntent.putExtra("path", getApplicationContext().getFilesDir().getPath());
                startScannerIntent.putExtra("device", 'A');
                startScannerIntent.putExtra("packetsize", 12);

                startActivity(startScannerIntent);
            }
        });

        scanButtonB.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {

                Intent startScannerIntent = new Intent(getApplicationContext(), ScanCodeActivity.class);

                startScannerIntent.putExtra("path", getApplicationContext().getFilesDir().getPath());
                startScannerIntent.putExtra("device", 'B');
                startScannerIntent.putExtra("packetsize", 12);

                startActivity(startScannerIntent);
            }
        });

        // PERMISSION STUFF //

        TedRx2Permission.with(this)
                .setRationaleTitle("Camera permission")
                .setRationaleMessage("We need permission to use the camera in order to sync over QR.") // "we need permission for read contact and find your location"
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
