package info.deconinck.inclinometer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ModuleTypeSelectionActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_MODULE_TYPE = "sensor_type";
    TextView title;

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mContentView = LayoutInflater.from(this).inflate(R.layout.module_type_selection_activity, null);
        setContentView(mContentView);

        title = findViewById(R.id.title_text);
        title.setText(getString(R.string.select_module_type));

        findViewById(R.id.bt_three).setOnClickListener(this);
        findViewById(R.id.bt_six).setOnClickListener(this);
        findViewById(R.id.bt_nine).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        int id = view.getId();
        if (id == R.id.bt_three) {
            intent.putExtra(EXTRA_MODULE_TYPE, 3);
        }
        else if (id == R.id.bt_six) {
            intent.putExtra(EXTRA_MODULE_TYPE, 6);
        }
        else if (id == R.id.bt_nine) {
            intent.putExtra(EXTRA_MODULE_TYPE, 9);
        }
        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();  // The program automatically returns to the previous activity
    }

}
