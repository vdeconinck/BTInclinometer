package com.bt901.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bt901.R;


/**
 * ${GWB}
 * 地址
 * 2017/5/9.
 */
public class PwmDialog extends BDialog implements View.OnClickListener {

    private EditText pwm;

    private String value;

    private PwmDialogCallBack pwmDialogCallBack;

    public PwmDialog() {
    }

    public static PwmDialog newInstance() {
        PwmDialog dialog = new PwmDialog();
        return dialog;
    }

//    public void showKeybard() {
//        startName.setFocusable(true);
//        startName.setFocusableInTouchMode(true);
//        startName.requestFocus();
//        InputMethodManager imm = (InputMethodManager) startName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.showSoftInput(startName, 0);
//    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lay_pwm_dialog, container, false);
        pwm = (EditText) view.findViewById(R.id.et_pwm);
        Button sure = (Button) view.findViewById(R.id.bt_save);
        Button abli = (Button) view.findViewById(R.id.bt_abolish);
        sure.setOnClickListener(this);
        abli.setOnClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public PwmDialogCallBack getPwmDialogCallBack() {
        return pwmDialogCallBack;
    }

    public void setPwmDialogCallBack(PwmDialogCallBack pwmDialogCallBack) {
        this.pwmDialogCallBack = pwmDialogCallBack;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bt_save) {
            value = pwm.getText().toString();
            if (value == null || value.equals("")) {
                Toast.makeText(getContext(), R.string.data_null, Toast.LENGTH_SHORT).show();
                return;
            }
            if (pwmDialogCallBack != null) {
                pwmDialogCallBack.save(value);
            }
            dismiss();
        } else if (i == R.id.bt_abolish) {
            if (pwmDialogCallBack != null) {
                pwmDialogCallBack.back();
            }
            dismiss();
        }
    }


    public interface PwmDialogCallBack {

        void save(String value);

        void back();

    }


}
