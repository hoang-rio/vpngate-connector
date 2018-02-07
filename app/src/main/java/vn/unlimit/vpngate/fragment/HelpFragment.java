package vn.unlimit.vpngate.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import vn.unlimit.vpngate.R;

/**
 * Created by hoangnd on 2/6/2018.
 */

public class HelpFragment extends Fragment implements View.OnClickListener {
    private EditText edtName;
    private EditText edtContent;
    private Button btnSend;
    private TextView txtErrorName;
    private TextView txtErrorContent;
    private int minContentLength = 50;
    private int minNameLength = 10;
    private boolean isValidName = false;
    private boolean isValideContent = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_help, container, false);
        txtErrorName = rootView.findViewById(R.id.txt_error_name);
        txtErrorContent = rootView.findViewById(R.id.txt_error_content);
        edtName = rootView.findViewById(R.id.edt_name);
        edtName.setHint(String.format(getResources().getString(R.string.name_hint), minNameLength));
        edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < minNameLength) {
                    txtErrorName.setText(String.format(getResources().getString(R.string.name_error_message), minNameLength));
                    isValidName = false;
                } else {
                    txtErrorName.setText("");
                    isValidName = true;
                }
            }
        });
        edtContent = rootView.findViewById(R.id.edt_content);
        edtContent.setHint(String.format(getResources().getString(R.string.content_hint), minContentLength));
        edtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() < minContentLength) {
                    txtErrorContent.setText(String.format(getResources().getString(R.string.content_error_message), minContentLength));
                    isValideContent = false;
                } else {
                    txtErrorContent.setText("");
                    isValideContent = true;
                }
            }
        });
        btnSend = rootView.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnSend)) {
            try {
                if (isValidName && isValideContent) {
                    Intent mailIntent = new Intent(Intent.ACTION_SEND);
                    mailIntent.setType("message/rfc822");
                    mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@chiasenhac.us"});
                    mailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.help_request_from) + " " + edtName.getText());
                    mailIntent.putExtra(Intent.EXTRA_TEXT, edtContent.getText().toString());
                    startActivity(Intent.createChooser(mailIntent, getResources().getString(R.string.send_email)));
                } else {
                    Toast.makeText(getContext(), getResources().getString(R.string.email_error_fix), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
