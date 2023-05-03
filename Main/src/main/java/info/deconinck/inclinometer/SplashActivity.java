package info.deconinck.inclinometer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    public static final int BLUETOOTH_SCAN_REQUEST_CODE = 0;
    public static final int BLUETOOTH_CONNECT_REQUEST_CODE = 1;
    public static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 2;
    public static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                // 1. Check required permissions (bluetooth & GPS)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkPermission(Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_REQUEST_CODE, getString(R.string.bluetooth_scan_permission_text));
                    checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_REQUEST_CODE, getString(R.string.bluetooth_connect_permission_text));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION_REQUEST_CODE, getString(R.string.network_location_permission_text));
                    checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION_REQUEST_CODE, getString(R.string.gps_location_permission_text));
                }


                // 2. Start the Data Monitor activity

                Intent intent;
                intent = new Intent(getApplicationContext(), DataMonitorActivity.class);
                startActivity(intent);
                finish();
            }
        }, 500);
    }

    private void checkPermission(String permission, int reqestCode, String permissionText) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(SplashActivity.this, permission)) {
                // Show a dialog explaining why the permission is necessary and prompting the user to grant it
                new AlertDialog.Builder(getApplicationContext())
                        .setTitle(getString(R.string.permission_required, permissionText))
                        .setMessage(R.string.bluetooth_permission_explanation)
                        .setPositiveButton(getString(R.string.grant_permission), (dialog, which) -> {
                            // Request the permission again
                            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{permission}, reqestCode);
                        })
                        .setNegativeButton(R.string.deny, null)
                        .show();
            }
            else {
                // Request the permission again
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{permission}, reqestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        String permission = null;
        String permissionText = null;
        switch (requestCode) {
            case BLUETOOTH_SCAN_REQUEST_CODE:
                permission = Manifest.permission.BLUETOOTH_SCAN;
                permissionText = getString(R.string.bluetooth_scan_permission_text);
                break;
            case BLUETOOTH_CONNECT_REQUEST_CODE:
                permission = Manifest.permission.BLUETOOTH_CONNECT;
                permissionText = getString(R.string.bluetooth_connect_permission_text);
                break;
            case ACCESS_FINE_LOCATION_REQUEST_CODE:
                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                permissionText = getString(R.string.gps_location_permission_text);
                break;
            case ACCESS_COARSE_LOCATION_REQUEST_CODE:
                permission = Manifest.permission.ACCESS_COARSE_LOCATION;
                permissionText = getString(R.string.network_location_permission_text);
                break;
            default:
                new AlertDialog.Builder(this)
                        .setTitle("Unknown Permission result !")
                        .setMessage("Handler received result for an unknown permission code. App will close")
                        .setPositiveButton("OK", (dialog, which) -> {
                            finishAffinity(); // This will close all activities and exit the app
                        });
        }

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // User has denied permission and selected "Don't ask again"
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_required, permissionText))
                        .setMessage(getString(R.string.please_grant_permission_retry, permissionText))
                        .setPositiveButton(R.string.android_settings_label, (dialog, which) -> {
                            // Open app settings
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            // User cancelled the dialog, so exit the app
                            finishAffinity(); // This will close all activities and exit the app
                        })
                        .show();
            }
            else {
                // User denied the permission, so exit the app
                finishAffinity(); // This will close all activities and exit the app
            }
        }
    }

}
