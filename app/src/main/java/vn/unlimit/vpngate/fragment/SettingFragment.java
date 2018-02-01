package vn.unlimit.vpngate.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import vn.unlimit.vpngate.ultils.SpinnerInit;

/**
 * Created by dongh on 31/01/2018.
 */

public class SettingFragment extends Fragment implements View.OnClickListener, AppCompatSpinner.OnItemSelectedListener {
    private AppCompatSpinner spinnerCacheTime;
    private Button btnClearCache;
    private View lnClearCache;
    private DataUtil dataUtil;
    private TextView txtCacheExpires;
    private View lnLoading;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        dataUtil = ((MainActivity) getActivity()).getDataUtil();
        spinnerCacheTime = rootView.findViewById(R.id.spin_cache_time);
        spinnerCacheTime.setOnItemSelectedListener(this);
        btnClearCache = rootView.findViewById(R.id.btn_clear_cache);
        btnClearCache.setOnClickListener(this);
        lnClearCache = rootView.findViewById(R.id.ln_clear_cache);
        txtCacheExpires = rootView.findViewById(R.id.txt_cache_expire);
        lnLoading = rootView.findViewById(R.id.ln_loading);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpinnerInit spinnerInit = new SpinnerInit(getContext(), spinnerCacheTime);
                String[] listCacheTime = getResources().getStringArray(R.array.setting_cache_time);
                spinnerInit.setStringArray(listCacheTime,
                        listCacheTime[dataUtil.getIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, 0)]
                );
                spinnerInit.setOnItemSelectedIndexListener(new SpinnerInit.OnItemSelectedIndexListener() {
                    @Override
                    public void onItemSelected(String name, int index) {
                        dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, index);
                    }
                });
                if (dataUtil.getConnectionCacheExpires() == null) {
                    lnClearCache.setVisibility(View.GONE);
                } else {
                    lnClearCache.setVisibility(View.VISIBLE);
                    txtCacheExpires.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(dataUtil.getConnectionCacheExpires()));
                }
                lnLoading.setVisibility(View.GONE);
            }
        }, 300);

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
        Intent intent = new Intent(BaseProvider.ACTION.ACTION_CLEAR_CACHE);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
