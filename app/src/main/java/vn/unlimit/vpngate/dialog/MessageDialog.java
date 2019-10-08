package vn.unlimit.vpngate.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.DialogFragment;

import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.utils.DataUtil;

/**
 * Created by hoangnd on 2/6/2018.
 */

public class MessageDialog extends DialogFragment implements View.OnClickListener, CheckBox.OnCheckedChangeListener {
    private String operatorMessage;
    private TextView txtOpMessage;
    private AppCompatCheckBox chbHideAllMessage;
    private Button btnClose;
    private DataUtil dataUtil;

    public static MessageDialog newInstance(String message, @NonNull DataUtil dataUtil) {
        MessageDialog f = new MessageDialog();
        f.operatorMessage = message;
        f.dataUtil = dataUtil;
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_message, container, false);
        txtOpMessage = rootView.findViewById(R.id.txt_message);
        txtOpMessage.setText(operatorMessage);
        chbHideAllMessage = rootView.findViewById(R.id.cbh_hide_5time);
        chbHideAllMessage.setOnCheckedChangeListener(this);
        btnClose = rootView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnClose)) {
            dismiss();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
        try {
            if (checkBox.equals(chbHideAllMessage) && isChecked) {
                dataUtil.setIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 5);
            } else {
                dataUtil.setIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        try {
            // request a window without the title
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }catch (Exception e){
            e.printStackTrace();
        }
        return dialog;
    }
}
