package info.deconinck.inclinometer.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import info.deconinck.inclinometer.R;

/**
 * ${GWB}
 * 地址
 * 2017/5/9.
 */
public class AngleDialog extends BDialog implements View.OnClickListener {

    private static float[] angle;
    private static int angleIndex;
    EditText startName;

    private final String angleType;

    private AngleDialogCallBack angleDialogCallBack;

    public AngleDialog(String angleType) {
        this.angleType = angleType;
    }

    public static AngleDialog newInstance(String angleType, float[] angle, int angleIndex) {
        AngleDialog.angle = angle;
        AngleDialog.angleIndex = angleIndex;
        AngleDialog dialog = new AngleDialog(angleType);
        return dialog;
    }

    public void showKeyboard() {
        startName.setFocusable(true);
        startName.setFocusableInTouchMode(true);
        startName.requestFocus();
        InputMethodManager imm = (InputMethodManager) startName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(startName, 0);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_angle_dialog, container, false);
        TextView prompt = view.findViewById(R.id.angle_prompt);
        prompt.setText(getString(R.string.select_angle, angleType));
        startName = view.findViewById(R.id.value_field);
        Button okBtn = view.findViewById(R.id.bt_save);
        Button currentBtn = view.findViewById(R.id.bt_current_angle);
        Button cancelBtn = view.findViewById(R.id.bt_cancel);
        okBtn.setOnClickListener(this);
        currentBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public AngleDialogCallBack getAngleDialogCallBack() {
        return angleDialogCallBack;
    }

    public void setAngleDialogCallBack(AngleDialogCallBack angleDialogCallBack) {
        this.angleDialogCallBack = angleDialogCallBack;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.bt_save:
                String value = startName.getText().toString();
                if (value == null || value.equals("")) {
                    Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (angleDialogCallBack != null) {
                    angleDialogCallBack.save(value);
                }
                dismiss();
                break;
            case R.id.bt_cancel:
                if (angleDialogCallBack != null) {
                    angleDialogCallBack.back();
                }
                dismiss();
            case R.id.bt_current_angle:
                startName.setText(""+angle[angleIndex]);
        }
    }


    public interface AngleDialogCallBack {

        void save(String value);

        void back();

    }


}
