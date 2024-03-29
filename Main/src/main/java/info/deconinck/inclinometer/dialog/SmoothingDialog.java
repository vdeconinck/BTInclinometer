package info.deconinck.inclinometer.dialog;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import info.deconinck.inclinometer.R;


/**
 * ${GWB}
 * 平滑系数
 * 2017/5/9.
 */
public class SmoothingDialog extends BDialog implements View.OnClickListener {

    EditText startName;


    private String value;

    private SmoothingDialogCallBack smoothingDialogCallBack;

    public SmoothingDialog() {
    }

    public static SmoothingDialog newInstance() {
        SmoothingDialog dialog = new SmoothingDialog();
        return dialog;
    }

    public void showKeybard() {
        startName.setFocusable(true);
        startName.setFocusableInTouchMode(true);
        startName.requestFocus();
        InputMethodManager imm = (InputMethodManager) startName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(startName, 0);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_smoothing_dialog, container, false);
        startName = view.findViewById(R.id.value_field);
        Button okBtn = view.findViewById(R.id.bt_save);
        Button cancelBtn = view.findViewById(R.id.bt_cancel);
        okBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public SmoothingDialogCallBack getDevDialogCallBack() {
        return smoothingDialogCallBack;
    }

    public void setDevDialogCallBack(SmoothingDialogCallBack smoothingDialogCallBack) {
        this.smoothingDialogCallBack = smoothingDialogCallBack;
    }


    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_save) {
            value = startName.getText().toString();
            if (value == null || value.equals("")) {
                Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (smoothingDialogCallBack != null) {
                smoothingDialogCallBack.save(value);
            }
            dismiss();
        } else if (i == R.id.bt_cancel) {
            if (smoothingDialogCallBack != null) {
                smoothingDialogCallBack.back();
            }
            dismiss();
        }
    }


    public interface SmoothingDialogCallBack {

        void save(String value);

        void back();

    }


}
