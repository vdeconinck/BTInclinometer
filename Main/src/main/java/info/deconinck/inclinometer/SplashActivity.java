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

import java.util.LinkedHashMap;
import java.util.Map;

import info.deconinck.inclinometer.util.AppPermission;

public class SplashActivity extends AppCompatActivity {

    private static final int BLUETOOTH_SCAN_REQUEST_CODE = 0;
    private static final int BLUETOOTH_CONNECT_REQUEST_CODE = 1;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 2;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 3;
    private static final int POST_NOTIFICATIONS_REQUEST_CODE = 4;

    private static final Map<Integer,AppPermission> appPermissions = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        new Handler().postDelayed(() -> {
            // 1. Check app permissions (bluetooth, location & notifications)
            appPermissions.put(BLUETOOTH_SCAN_REQUEST_CODE, new AppPermission(Build.VERSION_CODES.S, Manifest.permission.BLUETOOTH_SCAN , getString(R.string.bluetooth_scan_permission_text), true));
            appPermissions.put(BLUETOOTH_CONNECT_REQUEST_CODE, new AppPermission(Build.VERSION_CODES.S, Manifest.permission.BLUETOOTH_CONNECT , getString(R.string.bluetooth_connect_permission_text), true));
            appPermissions.put(ACCESS_COARSE_LOCATION_REQUEST_CODE, new AppPermission(Build.VERSION_CODES.M, Manifest.permission.ACCESS_COARSE_LOCATION , getString(R.string.network_location_permission_text), false));
            appPermissions.put(ACCESS_FINE_LOCATION_REQUEST_CODE, new AppPermission(Build.VERSION_CODES.M, Manifest.permission.ACCESS_FINE_LOCATION , getString(R.string.gps_location_permission_text), false));
            appPermissions.put(POST_NOTIFICATIONS_REQUEST_CODE, new AppPermission(Build.VERSION_CODES.TIRAMISU, Manifest.permission.POST_NOTIFICATIONS , getString(R.string.post_notification_permission_text), false));

            for (Integer requestCode : appPermissions.keySet()) {
                AppPermission appPermission = appPermissions.get(requestCode);
                if (Build.VERSION.SDK_INT >= appPermission.getMinBuildVersion()) {
                    checkPermission(requestCode, appPermission);
                }
            }

            // 2. Start the Data Monitor activity
            Intent intent;
            intent = new Intent(getApplicationContext(), DataMonitorActivity.class);
            startActivity(intent);
            finish();
        }, 500);
    }

    private void checkPermission(Integer requestCode, AppPermission appPermission) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), appPermission.getPermission()) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(SplashActivity.this, appPermission.getPermission())) {
                // Show a dialog explaining why the permission is necessary and prompting the user to grant it
               new AlertDialog.Builder(getApplicationContext())
                        .setTitle(getPermissionTitle(appPermission))
                        .setMessage(R.string.bluetooth_permission_explanation)
                        .setPositiveButton(getString(R.string.grant_permission), (dialog, which) -> {
                            // Request the permission again
                            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{appPermission.getPermission()}, requestCode);
                        })
                        .setNegativeButton(R.string.deny, null)
                        .show();
            }
            else {
                // Request the permission again
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{appPermission.getPermission()}, requestCode);
            }
        }
    }

    private String getPermissionTitle(AppPermission appPermission) {
        if (appPermission.isMandatory()) {
            return getString(R.string.permission_required, appPermission.getText());
        }
        else {
            return getString(R.string.permission_requested, appPermission.getText());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppPermission appPermission = appPermissions.get(requestCode);
        if (appPermission == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Unknown Permission result !")
                    .setMessage("Handler received result for an unknown permission code. App will close")
                    .setPositiveButton("OK", (dialog, which) -> {
                        finishAffinity(); // This will close all activities and exit the app
                    });
        }
        else {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, appPermission.getPermission())) {
                    // User has denied permission and selected "Don't ask again"
                    new AlertDialog.Builder(this)
                            .setTitle(getPermissionTitle(appPermission))
                            .setMessage(getString(R.string.please_grant_permission_retry, appPermission.getText()))
                            .setPositiveButton(R.string.android_settings_label, (dialog, which) -> {
                                // Open app settings
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                startActivity(intent);
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                                if (appPermission.isMandatory()) {
                                    // User cancelled the dialog for a mandatory permission, so exit the app
                                    finishAffinity(); // This will close all activities and exit the app
                                }
                            })
                            .show();
                }
                else {
                    if (appPermission.isMandatory()) {
                        // User denied a mandatory permission, so exit the app
                        finishAffinity(); // This will close all activities and exit the app
                    }
                }
            }
        }
    }
}
