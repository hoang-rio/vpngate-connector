package vn.unlimit.vpngate.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.DetailActivity;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.ultils.DataUtil;
import vn.unlimit.vpngate.ultils.PropertiesService;
import vn.unlimit.vpngate.ultils.TotalTraffic;

/**
 * Created by hoangnd on 2/9/2018.
 */

public class StatusFragment extends Fragment implements View.OnClickListener {
    private ImageView btnOnOff;
    private TextView txtStatus;
    private TextView txtUploadSession;
    private TextView txtDownloadSession;
    private TextView txtUploadTotal;
    private TextView txtDownloadTotal;
    private Button btnClearStatistics;
    private OpenVPNService mVPNService;
    private BroadcastReceiver brStatus;
    private BroadcastReceiver trafficReceiver;
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
            mVPNService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mVPNService = null;
        }

    };
    private boolean isConnecting = false;
    private boolean isAuthFailed = false;
    private InterstitialAd mInterstitialAd;
    private VpnProfile vpnProfile;
    private com.facebook.ads.InterstitialAd fInterstitialAd;
    private boolean mDestroyCalled = false;

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
        btnClearStatistics = rootView.findViewById(R.id.btn_clear_statistics);
        btnClearStatistics.setOnClickListener(this);
        mVpnGateConnection = dataUtil.getLastVPNConnection();
        bindData();
        registerBroadCast();
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dataUtil = ((App) getActivity().getApplication()).getDataUtil();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindData() {
        try {
            txtUploadTotal.setText(OpenVPNService.humanReadableByteCount(PropertiesService.getUploaded(), false));
            txtDownloadTotal.setText(OpenVPNService.humanReadableByteCount(PropertiesService.getDownloaded(), false));
            if (checkStatus()) {
                btnOnOff.setActivated(true);
                txtStatus.setText(String.format(getResources().getString(R.string.tap_to_disconnect), mVpnGateConnection.getName()));
            } else if (mVpnGateConnection != null) {
                txtStatus.setText(String.format(getResources().getString(R.string.tap_to_connect_last), mVpnGateConnection.getName()));
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
        if (mVPNService != null && mVPNService.getManagement() != null)
            mVPNService.getManagement().stopVPN(false);

    }

    private void loadAdMob() {
        if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)) {
            mInterstitialAd = new InterstitialAd(getContext());
            if (BuildConfig.DEBUG) {
                //Test
                mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
            } else {
                //Real
                mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_full_screen_status));
            }
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }

                @Override
                public void onAdFailedToLoad(int errCode) {
                    if (!mDestroyCalled) {
                        fInterstitialAd = new com.facebook.ads.InterstitialAd(getContext(), getString(R.string.fan_full_screen_status));
                        fInterstitialAd.setAdListener(new InterstitialAdListener() {
                            @Override
                            public void onInterstitialDisplayed(Ad ad) {

                            }

                            @Override
                            public void onInterstitialDismissed(Ad ad) {

                            }

                            @Override
                            public void onError(Ad ad, AdError adError) {
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                                fInterstitialAd.show();
                            }

                            @Override
                            public void onAdClicked(Ad ad) {

                            }

                            @Override
                            public void onLoggingImpression(Ad ad) {

                            }
                        });
                        fInterstitialAd.loadAd();
                    }
                }
            });
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    private void loadFan() {
        if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)) {
            fInterstitialAd = new com.facebook.ads.InterstitialAd(getContext(), getString(R.string.fan_full_screen_status));
            fInterstitialAd.setAdListener(new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {

                }

                @Override
                public void onInterstitialDismissed(Ad ad) {

                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    try {
                        if (!mDestroyCalled) {
                            mInterstitialAd = new InterstitialAd(getContext());
                            if (BuildConfig.DEBUG) {
                                //Test
                                mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
                            } else {
                                //Real
                                mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_full_screen_status));
                            }
                            mInterstitialAd.setAdListener(new AdListener() {
                                @Override
                                public void onAdLoaded() {
                                    mInterstitialAd.show();
                                }

                                @Override
                                public void onAdFailedToLoad(int errCode) {

                                }
                            });
                        }
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    fInterstitialAd.show();
                }

                @Override
                public void onAdClicked(Ad ad) {

                }

                @Override
                public void onLoggingImpression(Ad ad) {

                }
            });
            fInterstitialAd.loadAd();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnClearStatistics)) {
            TotalTraffic.clearTotal();
            Toast.makeText(getContext(), "Statistics clear completed", Toast.LENGTH_SHORT).show();
            txtUploadTotal.setText(OpenVPNService.humanReadableByteCount(0, false));
            txtDownloadTotal.setText(OpenVPNService.humanReadableByteCount(0, false));
        }
        if (view.equals(btnOnOff)) {
            if (mVpnGateConnection == null) {
                return;
            }
            if (!isConnecting) {
                if (checkStatus()) {
                    Answers.getInstance().logCustom(new CustomEvent("Disconnect VPN")
                            .putCustomAttribute("type", "disconnect current")
                            .putCustomAttribute("ip", mVpnGateConnection.getIp())
                            .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                    stopVpn();
                    isConnecting = false;
                    btnOnOff.setActivated(false);
                    txtStatus.setText(R.string.disconnecting);
                } else {
                    if (App.isAdMobPrimary()) {
                        loadAdMob();
                    } else {
                        loadFan();
                    }
                    Answers.getInstance().logCustom(new CustomEvent("Connect VPN")
                            .putCustomAttribute("type", "connect from status")
                            .putCustomAttribute("ip", mVpnGateConnection.getIp())
                            .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                    prepareVpn();
                    txtStatus.setText(getString(R.string.connecting));
                    btnOnOff.setActivated(true);
                    isConnecting = true;
                    dataUtil.setLastVPNConnection(mVpnGateConnection);
                }
            } else {
                Answers.getInstance().logCustom(new CustomEvent("Cancel VPN")
                        .putCustomAttribute("type", "cancel connect to vpn")
                        .putCustomAttribute("ip", mVpnGateConnection.getIp())
                        .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
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
        byte[] data = mVpnGateConnection.getOpenVpnConfigData().getBytes();
        ConfigParser cp = new ConfigParser();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
        try {
            cp.parseConfig(isr);
            vpnProfile = cp.convertProfile();
            vpnProfile.mName = mVpnGateConnection.getName();
            ProfileManager.setTemporaryProfile(vpnProfile);
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
                    VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
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
            intent.setAction(OpenVPNService.START_SERVICE);
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            TotalTraffic.saveTotal();
            getActivity().unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyCalled = true;
        try {
            getActivity().unregisterReceiver(brStatus);
            getActivity().unregisterReceiver(trafficReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerBroadCast() {
        try {
            brStatus = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    receiveStatus(intent);
                }
            };

            getActivity().registerReceiver(brStatus, new IntentFilter(OpenVPNService.VPN_STATUS));
            trafficReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    receiveTraffic(intent);
                }
            };

            getActivity().registerReceiver(trafficReceiver, new IntentFilter(TotalTraffic.TRAFFIC_ACTION));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void receiveTraffic(Intent intent) {
        if (checkStatus()) {
            txtDownloadSession.setText(intent.getStringExtra(TotalTraffic.DOWNLOAD_SESSION));
            txtUploadSession.setText(intent.getStringExtra(TotalTraffic.UPLOAD_SESSION));
            txtDownloadTotal.setText(intent.getStringExtra(TotalTraffic.DOWNLOAD_ALL));
            txtUploadTotal.setText(intent.getStringExtra(TotalTraffic.UPLOAD_ALL));
        }
    }

    private boolean checkStatus() {
        try {
            return VpnStatus.isVPNActive();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void receiveStatus(Intent intent) {
        try {
            txtStatus.setText(VpnStatus.getLastCleanLogMessage(getActivity().getApplicationContext()));
            changeServerStatus(VpnStatus.ConnectionStatus.valueOf(intent.getStringExtra("status")));

            if (intent.getStringExtra("detailstatus").equals("NOPROCESS")) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeServerStatus(VpnStatus.ConnectionStatus status) {
        try {
            dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true);
            switch (status) {
                case LEVEL_CONNECTED:
                    btnOnOff.setActivated(true);
                    isConnecting = false;
                    isAuthFailed = false;
                    break;
                case LEVEL_WAITING_FOR_USER_INPUT:
                    dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false);
                    break;
                case LEVEL_NOTCONNECTED:
                    if (!isConnecting && !isAuthFailed) {
                        btnOnOff.setActivated(false);
                        txtStatus.setText(String.format(getString(R.string.tap_to_connect_last), mVpnGateConnection.getName()));
                    }
                    break;
                case LEVEL_AUTH_FAILED:
                    isAuthFailed = true;
                    btnOnOff.setActivated(false);
                    Answers.getInstance().logCustom(new CustomEvent("Connect Error")
                            .putCustomAttribute("ip", mVpnGateConnection.getIp())
                            .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                    txtStatus.setText(getResources().getString(R.string.vpn_auth_failure));
                    isConnecting = false;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case DetailActivity.START_VPN_PROFILE:
                        VPNLaunchHelper.startOpenVpn(vpnProfile, getActivity().getBaseContext());
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
