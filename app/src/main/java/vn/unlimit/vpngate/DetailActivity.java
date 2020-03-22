package vn.unlimit.vpngate;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import vn.unlimit.vpngate.dialog.ConnectionUseProtocol;
import vn.unlimit.vpngate.dialog.MessageDialog;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.TotalTraffic;

/**
 * Created by hoangnd on 2/5/2018.
 */

public class DetailActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int TYPE_FROM_NOTIFY = 1001;
    public static final int TYPE_NORMAL = 1000;
    public static final String TYPE_START = "vn.ulimit.vpngate.TYPE_START";
    public static final int START_VPN_PROFILE = 70;
    private static OpenVPNService mVPNService;
    ImageView imgFlag;
    TextView txtCountry;
    TextView txtIp;
    TextView txtHostname;
    TextView txtScore;
    TextView txtUptime;
    TextView txtSpeed;
    TextView txtPing;
    TextView txtSession;
    TextView txtOwner;
    TextView txtTotalUser;
    TextView txtTotalTraffic;
    TextView txtLogType;
    TextView txtStatus;
    private View lnTCP;
    private TextView txtTCP;
    private View lnUDP;
    private TextView txtUDP;
    View linkCheckIp;
    LinearLayout lnContentDetail;
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private Button btnConnect;
    private View btnBack;
    private VpnProfile vpnProfile;
    private BroadcastReceiver brStatus;
    private InterstitialAd mInterstitialAd;
    private AdView adView;
    private AdView adViewBellow;
    private View btnInstallOpenVpn;
    private View btnSaveConfigFile;
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
    private boolean isShowAds = false;

    private void checkConnectionData() {
        if (mVpnGateConnection == null) {
            //Start main
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUtil = ((App) getApplication()).getDataUtil();
        if (getIntent().getIntExtra(TYPE_START, TYPE_NORMAL) == TYPE_FROM_NOTIFY) {
            mVpnGateConnection = dataUtil.getLastVPNConnection();
            loadVpnProfile(dataUtil.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false));
            try {
                Bundle params = new Bundle();
                params.putString("from", "Notification");
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Open_Detail", params);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }

        } else {
            mVpnGateConnection = getIntent().getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION);
        }
        checkConnectionData();
        setContentView(R.layout.activity_detail);
        btnConnect = findViewById(R.id.btn_connect);
        btnSaveConfigFile = findViewById(R.id.btn_save_config_file);
        btnSaveConfigFile.setOnClickListener(this);
        btnInstallOpenVpn = findViewById(R.id.btn_install_openvpn);
        btnInstallOpenVpn.setOnClickListener(this);
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        imgFlag = findViewById(R.id.img_flag);
        txtCountry = findViewById(R.id.txt_country);
        txtIp = findViewById(R.id.txt_ip);
        txtHostname = findViewById(R.id.txt_hostname);
        txtScore = findViewById(R.id.txt_score);
        txtUptime = findViewById(R.id.txt_uptime);
        txtSpeed = findViewById(R.id.txt_speed);
        txtPing = findViewById(R.id.txt_ping);
        txtSession = findViewById(R.id.txt_session);
        txtOwner = findViewById(R.id.txt_owner);
        txtTotalUser = findViewById(R.id.txt_total_user);
        txtTotalTraffic = findViewById(R.id.txt_total_traffic);
        txtLogType = findViewById(R.id.txt_log_type);
        txtStatus = findViewById(R.id.txt_status);
        linkCheckIp = findViewById(R.id.txt_check_ip);
        linkCheckIp.setOnClickListener(this);
        lnContentDetail = findViewById(R.id.ln_content_detail);
        lnTCP = findViewById(R.id.ln_tcp);
        txtTCP = findViewById(R.id.txt_tcp_port);
        lnUDP = findViewById(R.id.ln_udp);
        txtUDP = findViewById(R.id.txt_udp_port);
        bindData();
        registerBroadCast();
        initAdMob();
        initInterstitialAd();
    }

    private void initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this, dataUtil.getAdMobId());
                adView = new AdView(getApplicationContext());
                adView.setAdSize(AdSize.BANNER);
                if (BuildConfig.DEBUG) {
                    //Test
                    adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                } else {
                    //Real
                    adView.setAdUnitId(getResources().getString(R.string.admob_banner_bottom_detail));
                }
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        hideAdContainer();
                    }
                });
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                adView.setLayoutParams(params);
                ((RelativeLayout) findViewById(R.id.ad_container_detail)).addView(adView);
                adView.loadAd(new AdRequest.Builder().build());
                //Banner bellow
                adViewBellow = new AdView(getApplicationContext());
                if (BuildConfig.DEBUG) {
                    adViewBellow.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                } else {
                    adViewBellow.setAdUnitId(getString(R.string.admob_banner_bellow_detail));
                }
                adViewBellow.setAdSize(AdSize.MEDIUM_RECTANGLE);
                adViewBellow.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        adViewBellow.setVisibility(View.GONE);
                    }
                });
                lnContentDetail.addView(adViewBellow);
                adViewBellow.loadAd(new AdRequest.Builder().build());
            } else {
                hideAdContainer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideAdContainer() {
        try {
            findViewById(R.id.ad_container_detail).setVisibility(View.GONE);
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(brStatus);
    }

    private void registerBroadCast() {
        brStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receiveStatus(intent);
            }
        };

        registerReceiver(brStatus, new IntentFilter(OpenVPNService.VPN_STATUS));
    }

    private void receiveStatus(Intent intent) {
        if (!isFinishing()) {
            txtStatus.setText(VpnStatus.getLastCleanLogMessage(getApplicationContext()));
            changeServerStatus(VpnStatus.ConnectionStatus.valueOf(intent.getStringExtra("status")));

            if ("NOPROCESS".equals(intent.getStringExtra("detailstatus"))) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void changeServerStatus(VpnStatus.ConnectionStatus status) {
        try {
            dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true);
            switch (status) {
                case LEVEL_CONNECTED:
                    btnConnect.setText(getString(R.string.disconnect));
                    isConnecting = false;
                    isAuthFailed = false;
                    linkCheckIp.setVisibility(View.VISIBLE);
                    if (!mVpnGateConnection.getMessage().equals("") && dataUtil.getIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 0) == 0) {
                        MessageDialog messageDialog = MessageDialog.newInstance(mVpnGateConnection.getMessage(), dataUtil);
                        messageDialog.show(getSupportFragmentManager(), MessageDialog.class.getName());
                    }
                    break;
                case LEVEL_WAITING_FOR_USER_INPUT:
                    dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false);
                    break;
                case LEVEL_NOTCONNECTED:
                    if (!isConnecting && !isAuthFailed) {
                        linkCheckIp.setVisibility(View.GONE);
                        btnConnect.setText(R.string.connect_to_this_server);
                        if (Build.VERSION.SDK_INT >= 16) {
                            btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                        }
                        txtStatus.setText(R.string.disconnected);
                    }
                    break;
                case LEVEL_AUTH_FAILED:
                    isAuthFailed = true;
                    btnConnect.setText(getString(R.string.retry_connect));
                    Bundle params = new Bundle();
                    params.putString("ip", mVpnGateConnection.getIp());
                    params.putString("country", mVpnGateConnection.getCountryLong());
                    FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_Error", params);
                    if (Build.VERSION.SDK_INT >= 16) {
                        btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                    }
                    txtStatus.setText(getResources().getString(R.string.vpn_auth_failure));
                    linkCheckIp.setVisibility(View.GONE);
                    isConnecting = false;
                    break;
                default:
                    linkCheckIp.setVisibility(View.GONE);
            }
            if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false) && !isShowAds) {
                loadAds();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindData() {
        if (mVpnGateConnection != null) {
            try {
                GlideApp.with(this)
                        .load(App.getInstance().getDataUtil().getBaseUrl() + "/images/flags/" + mVpnGateConnection.getCountryShort() + ".png")
                        .placeholder(R.color.colorOverlay)
                        .error(R.color.colorOverlay)
                        .into(imgFlag);
                txtCountry.setText(mVpnGateConnection.getCountryLong());
                txtIp.setText(mVpnGateConnection.getIp());
                txtHostname.setText(mVpnGateConnection.getCalculateHostName());
                txtScore.setText(mVpnGateConnection.getScoreAsString());
                txtUptime.setText(mVpnGateConnection.getCalculateUpTime(getApplicationContext()));
                txtSpeed.setText(mVpnGateConnection.getCalculateSpeed());
                txtPing.setText(mVpnGateConnection.getPingAsString());
                txtSession.setText(mVpnGateConnection.getNumVpnSessionAsString());
                txtOwner.setText(mVpnGateConnection.getOperator());
                txtTotalUser.setText(String.valueOf(mVpnGateConnection.getTotalUser()));
                txtTotalTraffic.setText(mVpnGateConnection.getCalculateTotalTraffic());
                txtLogType.setText(mVpnGateConnection.getLogType());
                boolean isIncludeUDP = App.getInstance().getDataUtil().getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true);
                if (!isIncludeUDP || mVpnGateConnection.getTcpPort() == 0) {
                    lnTCP.setVisibility(View.GONE);
                } else {
                    txtTCP.setText(String.valueOf(mVpnGateConnection.getTcpPort()));
                }
                if (!isIncludeUDP || mVpnGateConnection.getUdpPort() == 0) {
                    lnUDP.setVisibility(View.GONE);
                } else {
                    txtUDP.setText(String.valueOf(mVpnGateConnection.getUdpPort()));
                }
                if (isCurrent() && checkStatus()) {
                    btnConnect.setText(getResources().getString(R.string.disconnect));
                    if (Build.VERSION.SDK_INT >= 16) {
                        btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_apply_button));
                    }
                    txtStatus.setText(VpnStatus.getLastCleanLogMessage(this));
                }
                if (checkStatus()) {
                    linkCheckIp.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isCurrent() {
        VPNGateConnection vpnGateConnection = dataUtil.getLastVPNConnection();
        return vpnGateConnection != null && vpnGateConnection.getName().equals(mVpnGateConnection.getName());
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            Intent intent = new Intent(this, OpenVPNService.class);
            intent.setAction(OpenVPNService.START_SERVICE);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (!App.isIsImportToOpenVPN()) {
                btnInstallOpenVpn.setVisibility(View.GONE);
                btnInstallOpenVpn.setVisibility(View.GONE);
                btnConnect.setVisibility(View.VISIBLE);
            } else {
                btnConnect.setVisibility(View.GONE);
                if (dataUtil.hasOpenVPNInstalled()) {
                    btnSaveConfigFile.setVisibility(View.VISIBLE);
                    btnInstallOpenVpn.setVisibility(View.GONE);
                } else {
                    btnSaveConfigFile.setVisibility(View.GONE);
                    btnInstallOpenVpn.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            TotalTraffic.saveTotal();
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleImport(final boolean useUdp) {
        loadAds();
        String data;
        if (useUdp) {
            data = mVpnGateConnection.getOpenVpnConfigDataUdp();
        } else {
            data = mVpnGateConnection.getOpenVpnConfigData();
        }
        try {
            while (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
            String fileName = mVpnGateConnection.getName(useUdp) + ".ovpn";
            File writeFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(writeFile);
            fileOutputStream.write(data.getBytes());
            Toast.makeText(getApplicationContext(), getString(R.string.saved_ovpn_file_in, "Download/" + fileName), Toast.LENGTH_LONG).show();
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                PackageManager packageManager = getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage("net.openvpn.openvpn");
                if (intent != null) {
                    intent.setAction(Intent.ACTION_VIEW);
                    startActivity(intent);
                }
            }, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(final boolean useUdp) {
        loadAds();
        if (checkStatus()) {
            stopVpn();
            Bundle params = new Bundle();
            params.putString("type", "replace current");
            params.putString("ip", mVpnGateConnection.getIp());
            params.putString("country", mVpnGateConnection.getCountryLong());
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_VPN", params);
            linkCheckIp.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    prepareVpn(useUdp);
                }
            }, 500);
        } else {
            Bundle params = new Bundle();
            params.putString("type", "connect new");
            params.putString("ip", mVpnGateConnection.getIp());
            params.putString("country", mVpnGateConnection.getCountryLong());
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_VPN", params);
            prepareVpn(useUdp);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_apply_button));
            txtStatus.setText(getString(R.string.connecting));
        }
        isConnecting = true;
        btnConnect.setText(R.string.cancel);
        dataUtil.setLastVPNConnection(mVpnGateConnection);
        sendConnectVPN();
    }

    @Override
    public void onClick(View view) {
        try {
            if (view.equals(btnBack)) {
                onBackPressed();
                return;
            }
            if (view.equals(btnConnect)) {
                if (!isConnecting) {
                    if (checkStatus() && isCurrent()) {
                        Bundle params = new Bundle();
                        params.putString("type", "disconnect current");
                        params.putString("ip", mVpnGateConnection.getIp());
                        params.putString("country", mVpnGateConnection.getCountryLong());
                        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Disconnect_VPN", params);
                        stopVpn();
                        if (Build.VERSION.SDK_INT >= 16) {
                            btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                        }
                        btnConnect.setText(R.string.connect_to_this_server);
                        txtStatus.setText(R.string.disconnecting);
                    } else if (mVpnGateConnection.getTcpPort() > 0 && mVpnGateConnection.getUdpPort() > 0) {
                        ConnectionUseProtocol connectionUseProtocol = ConnectionUseProtocol.newInstance(mVpnGateConnection, useUdp -> handleConnection(useUdp));
                        connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                    } else {
                        handleConnection(false);
                    }
                } else {
                    Bundle params = new Bundle();
                    params.putString("type", "cancel connect to vpn");
                    params.putString("ip", mVpnGateConnection.getIp());
                    params.putString("country", mVpnGateConnection.getCountryLong());
                    FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Cancel_VPN", params);
                    stopVpn();
                    if (Build.VERSION.SDK_INT >= 16) {
                        btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                    }
                    btnConnect.setText(R.string.connect_to_this_server);
                    txtStatus.setText(getString(R.string.canceled));
                    isConnecting = false;
                }
            }
            if (view.equals(linkCheckIp)) {
                Bundle params = new Bundle();
                params.putString("type", "check ip click");
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Click_Check_IP", params);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url")));
                startActivity(browserIntent);
            }
            if (view.equals(btnInstallOpenVpn)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=net.openvpn.openvpn")));
                } catch (android.content.ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=net.openvpn.openvpn")));
                }
            }
            if (view.equals(btnSaveConfigFile)) {
                if (mVpnGateConnection.getTcpPort() > 0 && mVpnGateConnection.getUdpPort() > 0) {
                    ConnectionUseProtocol connectionUseProtocol = ConnectionUseProtocol.newInstance(mVpnGateConnection, useUdp -> handleImport(useUdp));
                    connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                } else {
                    handleImport(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isFullScreenAdLoaded = false;

    private void initInterstitialAd() {
        if (dataUtil.hasAds()) {
            try {
                mInterstitialAd = new InterstitialAd(getApplicationContext());
                if (BuildConfig.DEBUG) {
                    mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
                } else {
                    mInterstitialAd.setAdUnitId(getString(R.string.admob_full_screen));
                }
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        isFullScreenAdLoaded = true;
                    }

                });
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadAds() {
        try {
            if (dataUtil.hasAds() && dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false) && isFullScreenAdLoaded) {
                isShowAds = true;
                if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendConnectVPN() {
        Intent intent = new Intent(BaseProvider.ACTION.ACTION_CONNECT_VPN);
        sendBroadcast(intent);
    }

    private void prepareVpn(boolean useUdp) {
        if (loadVpnProfile(useUdp)) {
            startVpn();
        } else {
            Toast.makeText(this, getString(R.string.error_load_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean loadVpnProfile(boolean useUDP) {
        byte[] data;
        if (useUDP) {
            data = mVpnGateConnection.getOpenVpnConfigDataUdp().getBytes();
        } else {
            data = mVpnGateConnection.getOpenVpnConfigData().getBytes();
        }
        dataUtil.setBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, useUDP);
        ConfigParser cp = new ConfigParser();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
        try {
            cp.parseConfig(isr);
            vpnProfile = cp.convertProfile();
            vpnProfile.mName = mVpnGateConnection.getName(useUDP);
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
            ProfileManager.setTemporaryProfile(vpnProfile);
        } catch (IOException | ConfigParser.ConfigParseError e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean checkStatus() {
        try {
            return VpnStatus.isVPNActive();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        if (mVPNService != null && mVPNService.getManagement() != null) {
            mVPNService.getManagement().stopVPN(false);
        }

    }

    private void startVpn() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == START_VPN_PROFILE) {
                    VPNLaunchHelper.startOpenVpn(vpnProfile, getBaseContext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
