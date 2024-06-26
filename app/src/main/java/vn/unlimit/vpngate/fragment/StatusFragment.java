package vn.unlimit.vpngate.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.utils.PropertiesService;
import de.blinkt.openvpn.utils.TotalTraffic;
import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.activities.DetailActivity;
import vn.unlimit.vpngate.activities.MainActivity;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.utils.DataUtil;

/**
 * Created by hoangnd on 2/9/2018.
 */

public class StatusFragment extends Fragment implements View.OnClickListener, VpnStatus.StateListener, VpnStatus.ByteCountListener {
    private final String TAG = "StatusFragment";
    private ImageView btnOnOff;
    private TextView txtStatus;
    private TextView txtUploadSession;
    private TextView txtDownloadSession;
    private TextView txtUploadTotal;
    private TextView txtDownloadTotal;
    private TextView txtUploadSpeed;
    private TextView txtDownloadSpeed;
    private Button btnClearStatistics;
    private IOpenVPNServiceInternal mVPNService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mVPNService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mVPNService = null;
        }

    };
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private boolean isConnecting = false;
    private boolean isAuthFailed = false;
    private boolean isDetached = false;
    private InterstitialAd mInterstitialAd;
    private VpnProfile vpnProfile;
    private Context mContext;
    private boolean isFullScreenAdsLoaded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstancesState) {
        View rootView = inflater.inflate(R.layout.fragment_status, container, false);
        btnOnOff = rootView.findViewById(R.id.btn_on_off);
        btnOnOff.setOnClickListener(this);
        txtStatus = rootView.findViewById(R.id.txt_status);
        txtUploadSession = rootView.findViewById(R.id.txt_upload_session);
        txtDownloadSession = rootView.findViewById(R.id.txt_download_session);
        txtUploadTotal = rootView.findViewById(R.id.txt_total_upload);
        txtDownloadTotal = rootView.findViewById(R.id.txt_total_download);
        txtUploadSpeed = rootView.findViewById(R.id.txt_upload_speed);
        txtDownloadSpeed = rootView.findViewById(R.id.txt_download_speed);
        btnClearStatistics = rootView.findViewById(R.id.btn_clear_statistics);
        btnClearStatistics.setOnClickListener(this);
        mVpnGateConnection = dataUtil.getLastVPNConnection();
        loadAdMob();
        bindData();
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dataUtil = App.getInstance().getDataUtil();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        isDetached = false;
        super.onAttach(context);
    }

    private void bindData() {
        try {
            txtUploadTotal.setText(OpenVPNService.humanReadableByteCount(PropertiesService.getUploaded(mContext), false, getResources()));
            txtDownloadTotal.setText(OpenVPNService.humanReadableByteCount(PropertiesService.getDownloaded(mContext), false, getResources()));
            if (checkStatus()) {
                btnOnOff.setActivated(true);
                txtStatus.setText(String.format(getResources().getString(R.string.tap_to_disconnect), getConnectionName()));
            } else if (mVpnGateConnection != null) {
                txtStatus.setText(String.format(getResources().getString(R.string.tap_to_connect_last), getConnectionName()));
            } else {
                btnOnOff.setActivated(false);
                btnOnOff.setEnabled(false);
                txtStatus.setText(R.string.no_last_vpn_server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(getContext());
        if (mVPNService != null) {
            try {
                mVPNService.stopVPN(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void loadAdMob() {
        if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(mContext, getResources().getString(R.string.admob_full_screen_status), adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull @NotNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    isFullScreenAdsLoaded = true;
                }

                @Override
                public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
        }
    }

    private void showAds() {
        if (dataUtil.hasAds() && isFullScreenAdsLoaded) {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(requireActivity());
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnClearStatistics)) {
            TotalTraffic.clearTotal(mContext);
            Toast.makeText(getContext(), "Statistics clear completed", Toast.LENGTH_SHORT).show();
            txtUploadTotal.setText(OpenVPNService.humanReadableByteCount(0, false, getResources()));
            txtDownloadTotal.setText(OpenVPNService.humanReadableByteCount(0, false, getResources()));
        }
        if (view.equals(btnOnOff)) {
            if (mVpnGateConnection == null) {
                return;
            }
            if (!isConnecting) {
                if (checkStatus()) {
                    Bundle params = new Bundle();
                    params.putString("type", "disconnect current");
                    params.putString("ip", mVpnGateConnection.getIp());
                    params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                    params.putString("country", mVpnGateConnection.getCountryLong());
                    FirebaseAnalytics.getInstance(mContext).logEvent("Disconnect_VPN", params);
                    stopVpn();
                    isConnecting = false;
                    btnOnOff.setActivated(false);
                    txtStatus.setText(R.string.disconnecting);
                } else {
                    showAds();
                    Bundle params = new Bundle();
                    params.putString("type", "connect from status");
                    params.putString("ip", mVpnGateConnection.getIp());
                    params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                    params.putString("country", mVpnGateConnection.getCountryLong());
                    FirebaseAnalytics.getInstance(mContext).logEvent("Connect_VPN", params);
                    prepareVpn();
                    txtStatus.setText(getString(R.string.connecting));
                    btnOnOff.setActivated(true);
                    isConnecting = true;
                    dataUtil.setLastVPNConnection(mVpnGateConnection);
                }
            } else {
                Bundle params = new Bundle();
                params.putString("type", "cancel connect to vpn");
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(mContext).logEvent("Cancel_VPN", params);
                stopVpn();
                btnOnOff.setActivated(false);
                txtStatus.setText(getString(R.string.canceled));
                isConnecting = false;
            }
        }
    }

    private void prepareVpn() {
        if (loadVpnProfile()) {
            startVpn();
        } else {
            Toast.makeText(getContext(), getString(R.string.error_load_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean loadVpnProfile() {
        try {
            boolean useUdp = dataUtil.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false);
            byte[] data;
            if (useUdp) {
                data = mVpnGateConnection.getOpenVpnConfigDataUdp().getBytes();
            } else {
                data = mVpnGateConnection.getOpenVpnConfigData().getBytes();
            }
            ConfigParser cp = new ConfigParser();
            InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
            cp.parseConfig(isr);
            vpnProfile = cp.convertProfile();
            vpnProfile.mName = getConnectionName();
            if (dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)) {
                vpnProfile.mOverrideDNS = true;
                vpnProfile.mDNS1 = FirebaseRemoteConfig.getInstance().getString(getString(R.string.dns_block_ads_primary_cfg_key));
                vpnProfile.mDNS2 = FirebaseRemoteConfig.getInstance().getString(getString(R.string.dns_block_ads_alternative_cfg_key));
            } else if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
                vpnProfile.mOverrideDNS = true;
                vpnProfile.mDNS1 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8");
                String dns2 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, null);
                if (dns2 != null) {
                    vpnProfile.mDNS2 = dns2;
                }
            }
            ProfileManager.setTemporaryProfile(getActivity(), vpnProfile);
        } catch (IOException | ConfigParser.ConfigParseError e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void startVpn() {
        Intent intent = VpnService.prepare(getContext());

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, DetailActivity.START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
            }
        } else {
            onActivityResult(DetailActivity.START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            Intent intent = new Intent(getContext(), OpenVPNService.class);
            OpenVPNService.mDisplaySpeed = dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true);
            intent.setAction(OpenVPNService.START_SERVICE);
            requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            TotalTraffic.saveTotal(mContext);
            requireActivity().unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            VpnStatus.removeStateListener(this);
            VpnStatus.removeByteCountListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setConnectedVPN(String uuid) {
        // Do nothing
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (isDetached()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (checkStatus() && !isDetached) {
                txtDownloadSession.setText(OpenVPNService.humanReadableByteCount(in, false, getResources()));
                txtDownloadSpeed.setText(OpenVPNService.humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true, getResources()));
                txtUploadSession.setText(OpenVPNService.humanReadableByteCount(out, false, getResources()));
                txtUploadSpeed.setText(OpenVPNService.humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true, getResources()));
                txtDownloadTotal.setText(OpenVPNService.humanReadableByteCount(TotalTraffic.inTotal, false, getResources()));
                txtUploadTotal.setText(OpenVPNService.humanReadableByteCount(TotalTraffic.outTotal, false, getResources()));
            }
        });
    }

    @Override
    public void onDetach() {
        isDetached = true;
        super.onDetach();
    }

    private boolean checkStatus() {
        try {
            return VpnStatus.isVPNActive();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String getConnectionName() {
        boolean useUdp = dataUtil.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false);
        return mVpnGateConnection.getName(useUdp);
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus status, Intent Intent) {
        requireActivity().runOnUiThread(() -> {
            try {
                txtStatus.setText(VpnStatus.getLastCleanLogMessage(mContext));
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true);
                switch (status) {
                    case LEVEL_CONNECTED:
                        btnOnOff.setActivated(true);
                        isConnecting = false;
                        isAuthFailed = false;
                        boolean isStartUpDetail = dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0;
                        OpenVPNService.setNotificationActivityClass(isStartUpDetail ? DetailActivity.class : MainActivity.class);
                        dataUtil.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false);
                        break;
                    case LEVEL_WAITING_FOR_USER_INPUT:
                        dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false);
                        break;
                    case LEVEL_NOTCONNECTED:
                        if (!isConnecting && !isAuthFailed) {
                            btnOnOff.setActivated(false);
                            txtStatus.setText(String.format(getString(R.string.tap_to_connect_last), getConnectionName()));
                        }
                        break;
                    case LEVEL_AUTH_FAILED:
                        isAuthFailed = true;
                        btnOnOff.setActivated(false);
                        Bundle params = new Bundle();
                        params.putString("ip", mVpnGateConnection.getIp());
                        params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                        params.putString("country", mVpnGateConnection.getCountryLong());
                        FirebaseAnalytics.getInstance(mContext).logEvent("Connect_Error", params);
                        txtStatus.setText(getResources().getString(R.string.vpn_auth_failure));
                        isConnecting = false;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Status update error", e);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == DetailActivity.START_VPN_PROFILE) {
                    VPNLaunchHelper.startOpenVpn(vpnProfile, mContext);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
