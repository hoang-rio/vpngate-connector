package vn.unlimit.vpngate.activities;

import static de.blinkt.openvpn.core.OpenVPNService.humanReadableByteCount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.utils.TotalTraffic;
import kittoku.osc.preference.OscPrefKey;
import kittoku.osc.service.SstpVpnService;
import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.dialog.ConnectionUseProtocol;
import vn.unlimit.vpngate.dialog.MessageDialog;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.NotificationUtil;

/**
 * Created by hoangnd on 2/5/2018.
 */

public class DetailActivity extends AppCompatActivity implements View.OnClickListener, VpnStatus.StateListener, VpnStatus.ByteCountListener {
    public static final int TYPE_FROM_NOTIFY = 1001;
    public static final int TYPE_NORMAL = 1000;
    public static final String TYPE_START = "vn.ulimit.vpngate.TYPE_START";
    public static final int START_VPN_PROFILE = 70;
    public static final int START_VPN_SSTP = 80;
    public static final String ACTION_VPN_CONNECT = "kittoku.osc.connect";
    public static final String ACTION_VPN_DISCONNECT = "kittoku.osc.disconnect";
    private static final String TAG = "DetailActivity";
    private static IOpenVPNServiceInternal mVPNService;
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
    View linkCheckIp;
    LinearLayout lnContentDetail;
    private View lnTCP;
    private TextView txtTCP;
    private View lnUDP;
    private TextView txtUDP;
    private View lnL2TP;
    private View getLnL2TPBtn;
    private Button btnConnectL2TP;
    private View lnSSTP;
    private View lnSTTPBtn;
    private Button btnConnectSSTP;
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private Button btnConnect;
    private View btnBack;
    private VpnProfile vpnProfile;
    private InterstitialAd mInterstitialAd;
    private AdView adView;
    private AdView adViewBellow;
    private View btnInstallOpenVpn;
    private View btnSaveConfigFile;
    private TextView txtNetStats;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private boolean isConnecting = false;
    private boolean isAuthFailed = false;
    private boolean isShowAds = false;
    private boolean isSSTPConnectOrDisconnecting = false;
    private boolean isSSTPConnected = false;
    private boolean isFullScreenAdLoaded = false;

    private void checkConnectionData() {
        if (mVpnGateConnection == null) {
            //Start main
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startVpnSSTPService(String action) {
        Intent intent = new Intent(getApplicationContext(), SstpVpnService.class).setAction(action);

        if (Objects.equals(action, ACTION_VPN_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(intent);
        } else {
            getApplicationContext().startService(intent);
        }
    }

    private void initSSTP() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        listener = (sharedPreferences, key) -> {
            if (String.valueOf(OscPrefKey.ROOT_STATE).equals(key)) {
                boolean newState = prefs.getBoolean(String.valueOf(OscPrefKey.ROOT_STATE), false);
                if (!newState) {
                    btnConnectSSTP.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_paid_button, null));
                    btnConnectSSTP.setText(R.string.connect_via_sstp);
                    if (isSSTPConnectOrDisconnecting) {
                        txtStatus.setText(R.string.sstp_disconnected);
                    } else {
                        txtStatus.setText(R.string.sstp_disconnected_by_error);
                    }
                    isSSTPConnected = false;
                    linkCheckIp.setVisibility(View.GONE);
                }
                isSSTPConnectOrDisconnecting = false;
            }
            if (String.valueOf(OscPrefKey.HOME_CONNECTED_IP).equals(key)) {
                String connectedIp = prefs.getString(String.valueOf(OscPrefKey.HOME_CONNECTED_IP), "");
                if (!connectedIp.isEmpty()) {
                    btnConnectSSTP.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_red_button, null));
                    btnConnectSSTP.setText(R.string.disconnect_sstp);
                    txtStatus.setText(getString(R.string.sstp_connected, connectedIp));
                    isSSTPConnected = true;
                    linkCheckIp.setVisibility(View.VISIBLE);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
        isSSTPConnected = prefs.getBoolean(String.valueOf(OscPrefKey.ROOT_STATE), false);
        String sstpHostName = prefs.getString(String.valueOf(OscPrefKey.HOME_HOSTNAME), "");
        if (isSSTPConnected) {
            linkCheckIp.setVisibility(View.VISIBLE);
            if (sstpHostName.equals(mVpnGateConnection.getCalculateHostName())) {
                btnConnectSSTP.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_red_button, null));
                btnConnectSSTP.setText(R.string.disconnect_sstp);
            }
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
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Open_Detail", params);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCreate error", ex);
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
        lnL2TP = findViewById(R.id.ln_l2tp);
        getLnL2TPBtn = findViewById(R.id.ln_l2tp_btn);
        btnConnectL2TP = findViewById(R.id.btn_l2tp_connect);
        btnConnectL2TP.setOnClickListener(this);
        lnSSTP = findViewById(R.id.ln_sstp);
        lnSTTPBtn = findViewById(R.id.ln_sstp_btn);
        btnConnectSSTP = findViewById(R.id.btn_sstp_connect);
        btnConnectSSTP.setOnClickListener(this);
        txtNetStats = findViewById(R.id.txt_net_stats);
        bindData();
        initAdMob();
        initInterstitialAd();
        initSSTP();
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        txtStatus.setText("");
    }

    private void initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this);
                adView = new AdView(getApplicationContext());
                adView.setAdSize(AdSize.LARGE_BANNER);
                adView.setAdUnitId(getResources().getString(R.string.admob_banner_bottom_detail));
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        hideAdContainer();
                        Log.e(TAG, error.toString());
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
                adViewBellow.setAdUnitId(getString(R.string.admob_banner_bellow_detail));
                adViewBellow.setAdSize(AdSize.MEDIUM_RECTANGLE);
                adViewBellow.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        adViewBellow.setVisibility(View.GONE);
                    }
                });
                lnContentDetail.addView(adViewBellow);
                adViewBellow.loadAd(new AdRequest.Builder().build());
            } else {
                hideAdContainer();
            }
        } catch (Exception e) {
            Log.e(TAG, "initAdMob error", e);
        }
    }

    private void hideAdContainer() {
        try {
            findViewById(R.id.ad_container_detail).setVisibility(View.GONE);
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "hideAdContainer error", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void setConnectedVPN(String uuid) {
        // Do nothing
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus status, Intent Intent) {
        runOnUiThread(() -> {
            try {
                txtStatus.setText(VpnStatus.getLastCleanLogMessage(this));
                switch (status) {
                    case LEVEL_CONNECTED:
                        if (isCurrent()) {
                            btnConnect.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_red_button, null));
                            btnConnect.setText(getString(R.string.disconnect));
                            txtNetStats.setVisibility(View.VISIBLE);
                            if (isConnecting && !mVpnGateConnection.getMessage().isEmpty() && dataUtil.getIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 0) == 0) {
                                MessageDialog messageDialog = MessageDialog.newInstance(mVpnGateConnection.getMessage(), dataUtil);
                                if (!isFinishing() && !isDestroyed()) {
                                    messageDialog.show(getSupportFragmentManager(), MessageDialog.class.getName());
                                } else if (!isFinishing()) {
                                    messageDialog.show(getSupportFragmentManager(), MessageDialog.class.getName());
                                }
                            }
                            boolean isStartUpDetail = dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0;
                            OpenVPNService.setNotificationActivityClass(isStartUpDetail ? DetailActivity.class : MainActivity.class);
                            dataUtil.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false);
                        }
                        isConnecting = false;
                        isAuthFailed = false;
                        linkCheckIp.setVisibility(View.VISIBLE);
                        break;
                    case LEVEL_NOTCONNECTED:
                        if (!isConnecting && !isAuthFailed) {
                            if (!isSSTPConnected) {
                                linkCheckIp.setVisibility(View.GONE);
                            }
                            btnConnect.setText(R.string.connect_to_this_server);
                            btnConnect.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_primary_button, null));
                            txtStatus.setText(R.string.disconnected);
                            txtNetStats.setVisibility(View.GONE);
                        }
                        break;
                    case LEVEL_AUTH_FAILED:
                        isAuthFailed = true;
                        btnConnect.setText(getString(R.string.retry_connect));
                        Bundle params = new Bundle();
                        params.putString("ip", mVpnGateConnection.getIp());
                        params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                        params.putString("country", mVpnGateConnection.getCountryLong());
                        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_Error", params);
                        btnConnect.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_primary_button, null));
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
                Log.e(TAG, "UpdateState error", e);
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void bindData() {
        if (mVpnGateConnection != null) {
            try {
                Glide.with(this)
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
                if (mVpnGateConnection.isL2TPSupport()) {
                    lnL2TP.setVisibility(View.VISIBLE);
                    getLnL2TPBtn.setVisibility(View.VISIBLE);
                } else {
                    lnL2TP.setVisibility(View.GONE);
                    getLnL2TPBtn.setVisibility(View.GONE);
                }

                if (mVpnGateConnection.isSSTPSupport()) {
                    lnSSTP.setVisibility(View.VISIBLE);
                    lnSTTPBtn.setVisibility(View.VISIBLE);
                } else {
                    lnSSTP.setVisibility(View.GONE);
                    lnSTTPBtn.setVisibility(View.GONE);
                }

                if (isCurrent() && checkStatus()) {
                    btnConnect.setText(getResources().getString(R.string.disconnect));
                    btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_apply_button));
                    txtStatus.setText(VpnStatus.getLastCleanLogMessage(this));
                    txtNetStats.setVisibility(View.VISIBLE);
                } else {
                    txtNetStats.setVisibility(View.GONE);
                }
                if (checkStatus()) {
                    linkCheckIp.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "bindData error", e);
            }
        }
    }

    private boolean isCurrent() {
        VPNGateConnection vpnGateConnection = dataUtil.getLastVPNConnection();
        return vpnGateConnection != null && mVpnGateConnection != null && vpnGateConnection.getName().equals(mVpnGateConnection.getName());
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, OpenVPNService.class);
                OpenVPNService.mDisplaySpeed = dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true);
                intent.setAction(OpenVPNService.START_SERVICE);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }, 300);
            if (!App.isIsImportToOpenVPN()) {
                btnInstallOpenVpn.setVisibility(View.GONE);
                btnSaveConfigFile.setVisibility(View.GONE);
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
            Log.e(TAG, "onResume error", e);
        }
    }

    @Override
    public void onPause() {
        try {
            super.onPause();
            TotalTraffic.saveTotal(this);
            unbindService(mConnection);
        } catch (Exception e) {
            Log.e(TAG, "onPause error", e);
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
            Log.e(TAG, "handleImport error", e);
        }
    }

    private void handleConnection(final boolean useUdp) {
        loadAds();
        if (isSSTPConnected) {
            startVpnSSTPService(ACTION_VPN_DISCONNECT);
        }
        if (checkStatus()) {
            stopVpn();
            Bundle params = new Bundle();
            params.putString("type", "replace current");
            params.putString("hostname", mVpnGateConnection.getCalculateHostName());
            params.putString("ip", mVpnGateConnection.getIp());
            params.putString("country", mVpnGateConnection.getCountryLong());
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_VPN", params);
            linkCheckIp.setVisibility(View.GONE);
            new Handler().postDelayed(() -> prepareVpn(useUdp), 500);
        } else {
            Bundle params = new Bundle();
            params.putString("type", "connect new");
            params.putString("hostname", mVpnGateConnection.getCalculateHostName());
            params.putString("ip", mVpnGateConnection.getIp());
            params.putString("country", mVpnGateConnection.getCountryLong());
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_VPN", params);
            prepareVpn(useUdp);
        }
        btnConnect.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_apply_button, null));
        txtStatus.setText(getString(R.string.connecting));
        isConnecting = true;
        btnConnect.setText(R.string.cancel);
        dataUtil.setLastVPNConnection(mVpnGateConnection);
        sendConnectVPN();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onClick(View view) {
        try {
            if (view.equals(btnBack)) {
                finish();
                return;
            }
            if (view.equals(btnConnect)) {
                if (!isConnecting) {
                    if (checkStatus() && isCurrent()) {
                        Bundle params = new Bundle();
                        params.putString("type", "disconnect current");
                        params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                        params.putString("ip", mVpnGateConnection.getIp());
                        params.putString("country", mVpnGateConnection.getCountryLong());
                        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Disconnect_VPN", params);
                        stopVpn();
                        btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                        btnConnect.setText(R.string.connect_to_this_server);
                        txtStatus.setText(R.string.disconnecting);
                    } else if (mVpnGateConnection.getTcpPort() > 0 && mVpnGateConnection.getUdpPort() > 0) {
                        if (dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0) != 0) {
                            // Apply default protocol in setting
                            String protocol = "TCP";
                            if (dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0) == 1) {
                                handleConnection(false);
                            } else {
                                protocol = "UDP";
                                handleConnection(true);
                            }
                            Toast.makeText(this, getString(R.string.connecting_use_protocol, protocol), Toast.LENGTH_SHORT).show();
                        } else {
                            ConnectionUseProtocol connectionUseProtocol = ConnectionUseProtocol.newInstance(mVpnGateConnection, this::handleConnection);
                            if (!isFinishing() && !isDestroyed()) {
                                connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                            } else if (!isFinishing()) {
                                connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                            }
                        }
                    } else {
                        handleConnection(false);
                    }
                } else {
                    Bundle params = new Bundle();
                    params.putString("type", "cancel connect to vpn");
                    params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                    params.putString("ip", mVpnGateConnection.getIp());
                    params.putString("country", mVpnGateConnection.getCountryLong());
                    FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Cancel_VPN", params);
                    stopVpn();
                    btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                    btnConnect.setText(R.string.connect_to_this_server);
                    txtStatus.setText(getString(R.string.canceled));
                    isConnecting = false;
                }
            } else if (view.equals(linkCheckIp)) {
                Bundle params = new Bundle();
                params.putString("type", "check ip click");
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Click_Check_IP", params);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url")));
                startActivity(browserIntent);
            } else if (view.equals(btnConnectL2TP)) {
                Bundle params = new Bundle();
                params.putString("type", "connect via L2TP");
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_Via_L2TP", params);
                loadAds();
                Intent l2tpIntent = new Intent(this, L2TPConnectActivity.class);
                l2tpIntent.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, mVpnGateConnection);
                startActivity(l2tpIntent);
            }
            if (view.equals(btnConnectSSTP)) {
                handleSSTPBtn();
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
                    ConnectionUseProtocol connectionUseProtocol = ConnectionUseProtocol.newInstance(mVpnGateConnection, this::handleImport);
                    if (!isFinishing() && !isDestroyed()) {
                        connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                    } else if (!isFinishing()) {
                        connectionUseProtocol.show(getSupportFragmentManager(), ConnectionUseProtocol.class.getName());
                    }
                } else {
                    handleImport(false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onClick error", e);
        }

    }

    private void connectSSTPVPN() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(String.valueOf(OscPrefKey.HOME_HOSTNAME), mVpnGateConnection.getCalculateHostName());
        editor.putString(String.valueOf(OscPrefKey.HOME_COUNTRY), mVpnGateConnection.getCountryShort().toUpperCase(Locale.ROOT));
        editor.putString(String.valueOf(OscPrefKey.HOME_USERNAME), "vpn");
        editor.putString(String.valueOf(OscPrefKey.HOME_PASSWORD), "vpn");
        editor.putString(String.valueOf(OscPrefKey.SSL_PORT), String.valueOf(mVpnGateConnection.getTcpPort()));
        editor.apply();
        btnConnectSSTP.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_apply_button, null));
        btnConnectSSTP.setText(R.string.cancel_sstp);
        txtStatus.setText(R.string.sstp_connecting);
        loadAds();
        startVpnSSTPService(ACTION_VPN_CONNECT);
    }

    private void startSSTPVPN() {
        if (checkStatus()) {
            stopVpn();
        }
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            try {
                startActivityForResult(intent, START_VPN_SSTP);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "OS does not support VPN");
            }
        } else {
            onActivityResult(START_VPN_SSTP, Activity.RESULT_OK, null);
        }
    }

    private void handleSSTPBtn() {
        isSSTPConnectOrDisconnecting = true;
        Bundle params = new Bundle();
        params.putString("hostname", mVpnGateConnection.getCalculateHostName());
        params.putString("ip", mVpnGateConnection.getIp());
        params.putString("country", mVpnGateConnection.getCountryLong());
        String sstpHostName = prefs.getString(String.valueOf(OscPrefKey.HOME_HOSTNAME), "");
        if (isSSTPConnected && !sstpHostName.equals(mVpnGateConnection.getCalculateHostName())) {
            // Connected but not must disconnect old first
            startVpnSSTPService(ACTION_VPN_DISCONNECT);
            params.putString("type", "replace connect via MS-SSTP");
            linkCheckIp.setVisibility(View.GONE);
            new Handler(getMainLooper()).postDelayed(this::connectSSTPVPN, 100);
        } else if (!isSSTPConnected) {
            params.putString("type", "connect via MS-SSTP");
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Connect_Via_SSTP", params);
            startSSTPVPN();
        } else {
            params.putString("type", "cancel MS-SSTP");
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Cancel_Via_SSTP", params);
            startVpnSSTPService(ACTION_VPN_DISCONNECT);
            btnConnectSSTP.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.selector_paid_button, null));
            btnConnectSSTP.setText(R.string.connect_via_sstp);
            txtStatus.setText(R.string.sstp_disconnecting);
        }
    }

    private void initInterstitialAd() {
        if (dataUtil.hasAds()) {
            try {
                AdRequest adRequest = new AdRequest.Builder().build();
                InterstitialAd.load(getApplicationContext(), getString(R.string.admob_full_screen), adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        isFullScreenAdLoaded = true;
                        mInterstitialAd = interstitialAd;
                        Log.e(TAG, "Full screen ads loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError var1) {
                        isFullScreenAdLoaded = false;
                        mInterstitialAd = null;
                        Log.e(TAG, String.format("Full screen ads failed to load %s", var1));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "initInterstitialAd error", e);
            }
        }
    }

    private void loadAds() {
        try {
            if (dataUtil.hasAds() && dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false) && isFullScreenAdLoaded) {
                isShowAds = true;
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAds error", e);
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
            ProfileManager.setTemporaryProfile(getApplicationContext(), vpnProfile);
        } catch (IOException | ConfigParser.ConfigParseError e) {
            Log.e(TAG, "loadVpnProfile error", e);
            return false;
        }

        return true;
    }

    private boolean checkStatus() {
        try {
            return VpnStatus.isVPNActive();
        } catch (Exception e) {
            Log.e(TAG, "checkStatus error", e);
        }

        return false;
    }

    private void stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        if (mVPNService != null) {
            try {
                mVPNService.stopVPN(false);
            } catch (RemoteException e) {
                VpnStatus.logException(e);
            }
        }

    }

    private void startVpn() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
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
                if (requestCode == START_VPN_SSTP) {
                    connectSSTPVPN();
                }
                new NotificationUtil(this).requestPermission();
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true);
            } else {
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "onActivityResult error", e);
        }
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (!isCurrent()) {
            return;
        }
        runOnUiThread(() -> {
            String netstat = String.format(getString(de.blinkt.openvpn.R.string.statusline_bytecount),
                    humanReadableByteCount(in, false, getResources()),
                    humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true, getResources()),
                    humanReadableByteCount(out, false, getResources()),
                    humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true, getResources()));
            txtNetStats.setText(netstat);
        });
    }
}
