package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.text.DateFormat;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.MainActivity;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.SpinnerInit;

/**
 * Created by dongh on 31/01/2018.
 */

public class SettingFragment extends Fragment implements View.OnClickListener, AppCompatSpinner.OnItemSelectedListener, SwitchCompat.OnCheckedChangeListener {
    private Button btnClearCache;
    private View lnClearCache;
    private DataUtil dataUtil;
    private SwitchCompat swBlockAds;
    private View lnBlockAds;
    private Context mContext;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        dataUtil = App.getInstance().getDataUtil();
        AppCompatSpinner spinnerCacheTime = rootView.findViewById(R.id.spin_cache_time);
        spinnerCacheTime.setOnItemSelectedListener(this);
        btnClearCache = rootView.findViewById(R.id.btn_clear_cache);
        btnClearCache.setOnClickListener(this);
        lnBlockAds = rootView.findViewById(R.id.ln_block_ads);
        lnBlockAds.setOnClickListener(this);
        swBlockAds = rootView.findViewById(R.id.sw_block_ads);
        swBlockAds.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false));
        swBlockAds.setOnCheckedChangeListener(this);
        lnClearCache = rootView.findViewById(R.id.ln_clear_cache);
        TextView txtCacheExpires = rootView.findViewById(R.id.txt_cache_expire);
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

        return rootView;
    }
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mContext = context;
    }
    @Override
    public void onClick(View view) {
        if (view.equals(btnClearCache)) {
            MainActivity activity = (MainActivity) getActivity();
            if (dataUtil.clearConnectionCache()) {
                Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_success), Toast.LENGTH_SHORT).show();
                lnClearCache.setVisibility(View.GONE);
                sendClearCache();
            } else {
                Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_error), Toast.LENGTH_SHORT).show();
            }
        } else if (view.equals(lnBlockAds)) {
            swBlockAds.setChecked(!swBlockAds.isChecked());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton switchCompat, boolean isChecked) {
        if (dataUtil.hasAds() && isChecked) {
            switchCompat.setChecked(false);
            Toast.makeText(getContext(), getString(R.string.feature_available_in_pro), Toast.LENGTH_LONG).show();
            return;
        }
        //Only save setting in pro version
        if (!dataUtil.hasAds() && switchCompat.equals(swBlockAds)) {
            Toast.makeText(getContext(), getText(R.string.setting_apply_on_next_connection_time), Toast.LENGTH_SHORT).show();
            dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, isChecked);
            Answers.getInstance().logCustom(new CustomEvent("Change Block Ads Setting").putCustomAttribute("enabled", isChecked + ""));
        }
    }

    private void sendClearCache() {
        try {
            Intent intent = new Intent(BaseProvider.ACTION.ACTION_CLEAR_CACHE);
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
