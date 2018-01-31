package vn.unlimit.vpngate.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;

import vn.unlimit.vpngate.MainActivity;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.ultils.DataUtil;

/**
 * Created by dongh on 31/01/2018.
 */

public class SettingFragment extends Fragment implements View.OnClickListener, AppCompatSpinner.OnItemSelectedListener {
    private AppCompatSpinner spinnerCacheTime;
    private Button btnClearCache;
    private View lnClearCache;
    private DataUtil dataUtil;
    private TextView txtCacheExpires;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        dataUtil = ((MainActivity) getActivity()).getDataUtil();
        spinnerCacheTime = rootView.findViewById(R.id.spin_cache_time);
        spinnerCacheTime.setOnItemSelectedListener(this);
        btnClearCache = rootView.findViewById(R.id.btn_clear_cache);
        btnClearCache.setOnClickListener(this);
        lnClearCache = rootView.findViewById(R.id.ln_clear_cache);
        if (dataUtil.getConnectionCacheExpires() == null) {
            lnClearCache.setVisibility(View.GONE);
        } else {
            lnClearCache.setVisibility(View.VISIBLE);
            txtCacheExpires = rootView.findViewById(R.id.txt_cache_expire);
            txtCacheExpires.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(dataUtil.getConnectionCacheExpires()));
        }
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnClearCache)) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity.getDataUtil().clearConnectionCache()) {
                Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_success), Toast.LENGTH_SHORT).show();
                lnClearCache.setVisibility(View.GONE);
                sendClearCache();
            } else {
                Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendClearCache() {
        Intent intent = new Intent(BaseProvider.Action.ACTION_CLEAR_CACHE);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
