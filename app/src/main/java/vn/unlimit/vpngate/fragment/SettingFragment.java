package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.DateFormat;

import de.blinkt.openvpn.core.OpenVPNService;
import kittoku.osc.preference.OscPrefKey;
import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.activities.DetailActivity;
import vn.unlimit.vpngate.activities.MainActivity;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.SpinnerInit;

/**
 * Created by dongh on 31/01/2018.
 */

public class SettingFragment extends Fragment implements View.OnClickListener, AppCompatSpinner.OnItemSelectedListener, SwitchCompat.OnCheckedChangeListener, EditText.OnFocusChangeListener {
    private Button btnClearCache;
    private View lnClearCache;
    private DataUtil dataUtil;
    private SwitchCompat swBlockAds;
    private View lnBlockAds;
    private View lnBlockAdsWrap;
    private SwitchCompat swUdp;
    private View lnUdp;
    private Context mContext;
    private View lnDns;
    private View lnDnsWrap;
    private SwitchCompat swDns;
    private View lnDnsIP;
    private EditText txtDns1;
    private EditText txtDns2;
    private View lnDomain;
    private SwitchCompat swDomain;
    private View lnProtocol;
    private View lnNotifySpeed;
    private SwitchCompat swNotifySpeed;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        dataUtil = App.getInstance().getDataUtil();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        AppCompatSpinner spinnerCacheTime = rootView.findViewById(R.id.spin_cache_time);
        spinnerCacheTime.setOnItemSelectedListener(this);
        btnClearCache = rootView.findViewById(R.id.btn_clear_cache);
        btnClearCache.setOnClickListener(this);
        lnBlockAds = rootView.findViewById(R.id.ln_block_ads);
        lnBlockAdsWrap = rootView.findViewById(R.id.ln_block_ads_wrap);
        lnDnsWrap = rootView.findViewById(R.id.ln_dns_wrap);
        lnBlockAds.setOnClickListener(this);
        swBlockAds = rootView.findViewById(R.id.sw_block_ads);
        swBlockAds.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false));
        swBlockAds.setOnCheckedChangeListener(this);
        lnUdp = rootView.findViewById(R.id.ln_udp);
        lnUdp.setOnClickListener(this);
        swUdp = rootView.findViewById(R.id.sw_udp);
        swUdp.setChecked(dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true));
        swUdp.setOnCheckedChangeListener(this);
        lnClearCache = rootView.findViewById(R.id.ln_clear_cache);
        TextView txtCacheExpires = rootView.findViewById(R.id.txt_cache_expire);
        SpinnerInit spinnerInit = new SpinnerInit(getContext(), spinnerCacheTime);
        String[] listCacheTime = getResources().getStringArray(R.array.setting_cache_time);
        spinnerInit.setStringArray(listCacheTime,
                listCacheTime[dataUtil.getIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, 0)]
        );
        spinnerInit.setOnItemSelectedIndexListener((name, index) -> {
            Bundle params = new Bundle();
            params.putString("selected_cache_value", listCacheTime[index]);
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Cache_Time_Setting", params);
            dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, index);
        });
        if (dataUtil.getConnectionCacheExpires() == null) {
            lnClearCache.setVisibility(View.GONE);
        } else {
            lnClearCache.setVisibility(View.VISIBLE);
            txtCacheExpires.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(dataUtil.getConnectionCacheExpires()));
        }
        lnDns = rootView.findViewById(R.id.ln_dns);
        lnDns.setOnClickListener(this);
        lnDnsIP = rootView.findViewById(R.id.ln_dns_ip);
        swDns = rootView.findViewById(R.id.sw_dns);
        if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
            swDns.setChecked(true);
            lnDnsIP.setVisibility(View.VISIBLE);
        } else {
            swDns.setChecked(false);
            lnDnsIP.setVisibility(View.GONE);
        }
        swDns.setOnCheckedChangeListener(this);
        InputFilter[] inputFilters = this.getIpInputFilters();
        txtDns1 = rootView.findViewById(R.id.txt_dns_1);
        txtDns1.setFilters(inputFilters);
        txtDns1.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8"));
        txtDns1.setOnFocusChangeListener(this);
        txtDns2 = rootView.findViewById(R.id.txt_dns_2);
        txtDns2.setFilters(inputFilters);
        txtDns2.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, ""));
        txtDns2.setOnFocusChangeListener(this);
        lnDomain = rootView.findViewById(R.id.ln_domain);
        lnDomain.setOnClickListener(this);
        swDomain = rootView.findViewById(R.id.sw_domain);
        swDomain.setChecked(dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false));
        swDomain.setOnCheckedChangeListener(this);
        lnProtocol = rootView.findViewById(R.id.ln_default_protocol);
        AppCompatSpinner spinnerProto = rootView.findViewById(R.id.spin_proto);
        spinnerProto.setOnItemSelectedListener(this);
        String[] listProtocol = getResources().getStringArray(R.array.list_protocol);
        SpinnerInit spinnerInitProto = new SpinnerInit(getContext(), spinnerProto);
        spinnerInitProto.setStringArray(listProtocol,
                listProtocol[dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0)]
        );
        spinnerInitProto.setOnItemSelectedIndexListener((name, index) -> {
            Bundle params = new Bundle();
            params.putString("selected_protocol", listProtocol[index]);
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Default_Protocol_Setting", params);
            dataUtil.setIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, index);
        });
        lnNotifySpeed = rootView.findViewById(R.id.ln_notify_speed);
        lnNotifySpeed.setOnClickListener(this);
        swNotifySpeed = rootView.findViewById(R.id.sw_notify_speed);
        swNotifySpeed.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true));
        swNotifySpeed.setOnCheckedChangeListener(this);
        View lnStartUpScreen = rootView.findViewById(R.id.ln_startup_screen);
        if (dataUtil.getLastVPNConnection() != null) {
            lnStartUpScreen.setVisibility(View.VISIBLE);
            AppCompatSpinner spinnerStartupScreen = rootView.findViewById(R.id.spin_screen);
            String[] listScreen = getResources().getStringArray(R.array.startup_screen);
            SpinnerInit spinnerInitScreen = new SpinnerInit(getContext(), spinnerStartupScreen);
            spinnerInitScreen.setStringArray(listScreen, listScreen[dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0)]);
            spinnerInitScreen.setOnItemSelectedIndexListener(((name, index) -> {
                Bundle params = new Bundle();
                params.putString("selected_screen", listScreen[index]);
                FirebaseAnalytics.getInstance(mContext).logEvent("Change_StartUp_Screen_Setting", params);
                dataUtil.setIntSetting(DataUtil.SETTING_STARTUP_SCREEN, index);
                OpenVPNService.setNotificationActivityClass(index == 0 ? DetailActivity.class : MainActivity.class);
            }));
        } else {
            lnStartUpScreen.setVisibility(View.GONE);
        }

        return rootView;
    }

    private InputFilter[] getIpInputFilters() {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = (source, start, end, dest, dstart, dend) -> {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                    return "";
                } else {
                    String[] splits = resultingTxt.split("\\.");
                    for (String val : splits) {
                        if (Integer.parseInt(val) > 255) {
                            return "";
                        }
                    }
                }
            }
            return null;
        };
        return filters;

    }

    @Override
    public void onFocusChange(View view, boolean isFocus) {
        if (!isFocus) {
            String dnsIP;
            String settingKey;
            if (view.equals(txtDns1)) {
                dnsIP = txtDns1.getText().toString();
                settingKey = DataUtil.CUSTOM_DNS_IP_1;
            } else {
                dnsIP = txtDns2.getText().toString();
                settingKey = DataUtil.CUSTOM_DNS_IP_2;
            }
            if (Patterns.IP_ADDRESS.matcher(dnsIP).matches()) {
                dataUtil.setStringSetting(settingKey, dnsIP);
                if (settingKey.equals(DataUtil.CUSTOM_DNS_IP_1)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(String.valueOf(OscPrefKey.DNS_CUSTOM_ADDRESS), dnsIP);
                    editor.apply();
                }
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (App.isIsImportToOpenVPN()) {
            lnBlockAdsWrap.setVisibility(View.GONE);
            lnDnsWrap.setVisibility(View.GONE);
        } else {
            lnBlockAdsWrap.setVisibility(View.VISIBLE);
            lnDnsWrap.setVisibility(View.VISIBLE);
        }
    }

    private void clearListServerCache(boolean showToast) {
        MainActivity activity = (MainActivity) getActivity();
        if (dataUtil.clearConnectionCache()) {
            if (showToast) {
                Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_success), Toast.LENGTH_SHORT).show();
            }
            lnClearCache.setVisibility(View.GONE);
            sendClearCache();
        } else if (showToast) {
            Toast.makeText(activity, getResources().getString(R.string.setting_clear_cache_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnClearCache)) {
            clearListServerCache(true);
        } else if (view.equals(lnBlockAds)) {
            swBlockAds.setChecked(!swBlockAds.isChecked());
        } else if (view.equals(lnUdp)) {
            swUdp.setChecked(!swUdp.isChecked());
        } else if (view.equals(lnDns)) {
            swDns.setChecked(!swDns.isChecked());
        } else if (view.equals(lnDomain)) {
            swDomain.setChecked(!swDomain.isChecked());
        } else if (view.equals(lnNotifySpeed)) {
            swNotifySpeed.setChecked(!swNotifySpeed.isChecked());
        }
    }

    private void hideKeyBroad() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(txtDns1.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton switchCompat, boolean isChecked) {
        Bundle params = new Bundle();
        params.putString("enabled", isChecked + "");
        if (switchCompat.equals(swUdp)) {
            dataUtil.setBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, isChecked);
            lnProtocol.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            clearListServerCache(false);
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Include_UDP_Setting", params);
            return;
        }
        if (switchCompat.equals(swDns)) {
            dataUtil.setBooleanSetting(DataUtil.USE_CUSTOM_DNS, isChecked);
            if (isChecked) {
                if (swBlockAds.isChecked()) {
                    // Turn off Block Ads if custom DNS is enabled
                    swBlockAds.setChecked(false);
                    dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false);
                }
                lnDnsIP.setVisibility(View.VISIBLE);
                txtDns1.requestFocus();
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(txtDns1, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                hideKeyBroad();
                lnDnsIP.setVisibility(View.GONE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(String.valueOf(OscPrefKey.DNS_CUSTOM_ADDRESS));
                editor.apply();
            }
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Custom_DNS_Setting", params);
            return;
        }
        if (switchCompat.equals(swDomain)) {
            dataUtil.setBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, isChecked);
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Use_Domain_To_Connect_Setting", params);
            return;
        }
        if (switchCompat.equals(swNotifySpeed)) {
            Toast.makeText(getContext(), getText(R.string.setting_apply_on_next_connection_time), Toast.LENGTH_SHORT).show();
            dataUtil.setBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, isChecked);
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Notify_Speed_Setting", params);
            return;
        }
        if (dataUtil.hasAds() && isChecked) {
            switchCompat.setChecked(false);
            Toast.makeText(getContext(), getString(R.string.feature_available_in_pro), Toast.LENGTH_LONG).show();
            return;
        }
        //Only save setting in pro version
        if (switchCompat.equals(swBlockAds)) {
            Toast.makeText(getContext(), getText(R.string.setting_apply_on_next_connection_time), Toast.LENGTH_SHORT).show();
            dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, isChecked);
            if (isChecked && swDns.isChecked()) {
                swDns.setChecked(false);
            }
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Block_Ads_Setting", params);
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
