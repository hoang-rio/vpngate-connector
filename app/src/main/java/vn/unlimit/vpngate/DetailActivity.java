package vn.unlimit.vpngate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

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
import vn.unlimit.vpngate.dialog.MessageDialog;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.ultils.DataUtil;
import vn.unlimit.vpngate.ultils.TotalTraffic;

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
    View linkCheckIp;
    LinearLayout lnContentDetail;
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private Button btnConnect;
    private View btnBack;
    private VpnProfile vpnProfile;
    private BroadcastReceiver brStatus;
    private InterstitialAd mInterstitialAd;
    private com.facebook.ads.InterstitialAd fInterstitialAd;
    private AdView adView;
    private com.facebook.ads.AdView fAdView;
    private AdView adViewBellow;
    private com.facebook.ads.AdView fAdViewBellow;
    private boolean mDestroyCalled = false;
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
            loadVpnProfile();
            try {
                Answers.getInstance().logCustom(new CustomEvent("Open detail")
                        .putCustomAttribute("from", "Notification")
                        .putCustomAttribute("ip", mVpnGateConnection.getIp())
                        .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }

        } else {
            mVpnGateConnection = getIntent().getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION);
        }
        checkConnectionData();
        setContentView(R.layout.activity_detail);
        btnConnect = findViewById(R.id.btn_connect);
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
        bindData();
        registerBroadCast();
        if (App.isAdMobPrimary()) {
            initAdMob();
        } else {
            initFan();
        }
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
                        fAdView = new com.facebook.ads.AdView(getApplicationContext(), getString(R.string.fan_banner_bottom_detail), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                        fAdView.setAdListener(new com.facebook.ads.AdListener() {
                            @Override
                            public void onError(Ad ad, AdError adError) {
                                hideAdContainer();
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {

                            }

                            @Override
                            public void onAdClicked(Ad ad) {

                            }

                            @Override
                            public void onLoggingImpression(Ad ad) {

                            }
                        });
                        ((RelativeLayout) findViewById(R.id.ad_container_detail)).addView(fAdView);
                        fAdView.loadAd();
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
                        fAdViewBellow = new com.facebook.ads.AdView(getApplicationContext(), getString(R.string.fan_banner_bellow_detail), com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250);
                        fAdViewBellow.setAdListener(new com.facebook.ads.AdListener() {
                            @Override
                            public void onError(Ad ad, AdError adError) {
                                fAdViewBellow.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {

                            }

                            @Override
                            public void onAdClicked(Ad ad) {

                            }

                            @Override
                            public void onLoggingImpression(Ad ad) {

                            }
                        });
                        lnContentDetail.addView(fAdViewBellow);
                        fAdViewBellow.loadAd();
                    }
                });
                lnContentDetail.addView(adViewBellow);
                adViewBellow.loadAd(new AdRequest.Builder().build());
            } else {
                hideAdContainer();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initFan() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this, dataUtil.getAdMobId());
                fAdView = new com.facebook.ads.AdView(getApplicationContext(), getString(R.string.fan_banner_bottom_detail), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                fAdView.setAdListener(new com.facebook.ads.AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {
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
                        ((RelativeLayout) findViewById(R.id.ad_container_detail)).addView(adView);
                        adView.loadAd(new AdRequest.Builder().build());
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {

                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                });
                ((RelativeLayout) findViewById(R.id.ad_container_detail)).addView(fAdView);
                fAdView.loadAd();
                //Banner bellow
                fAdViewBellow = new com.facebook.ads.AdView(getApplicationContext(), getString(R.string.fan_banner_bellow_detail), com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250);
                fAdViewBellow.setAdListener(new com.facebook.ads.AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {
                        fAdViewBellow.setVisibility(View.GONE);
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
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {

                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                });
                lnContentDetail.addView(fAdViewBellow);
                fAdViewBellow.loadAd();
            } else {
                hideAdContainer();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void hideAdContainer() {
        try {
            ScrollView scrollView = findViewById(R.id.scrollView);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) scrollView.getLayoutParams();
            findViewById(R.id.ad_container_detail).setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            params.setMargins(marginLayoutParams.leftMargin, marginLayoutParams.topMargin, marginLayoutParams.rightMargin, 0);
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
            if (fAdView != null) {
                fAdView.setVisibility(View.GONE);
            }
            scrollView.setLayoutParams(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mDestroyCalled = true;
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

            if (intent.getStringExtra("detailstatus").equals("NOPROCESS")) {
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
                    Answers.getInstance().logCustom(new CustomEvent("Connect Error")
                            .putCustomAttribute("ip", mVpnGateConnection.getIp())
                            .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
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
                        .load("http://www.vpngate.net/images/flags/" + mVpnGateConnection.getCountryShort() + ".png")
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
                        Answers.getInstance().logCustom(new CustomEvent("Disconnect VPN")
                                .putCustomAttribute("type", "disconnect current")
                                .putCustomAttribute("ip", mVpnGateConnection.getIp())
                                .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                        stopVpn();
                        if (Build.VERSION.SDK_INT >= 16) {
                            btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                        }
                        btnConnect.setText(R.string.connect_to_this_server);
                        txtStatus.setText(R.string.disconnecting);
                    } else {
                        loadAds();
                        if (checkStatus()) {
                            stopVpn();
                            Answers.getInstance().logCustom(new CustomEvent("Connect VPN")
                                    .putCustomAttribute("type", "replace current")
                                    .putCustomAttribute("ip", mVpnGateConnection.getIp())
                                    .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                            linkCheckIp.setVisibility(View.GONE);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    prepareVpn();
                                }
                            }, 500);
                        } else {
                            Answers.getInstance().logCustom(new CustomEvent("Connect VPN")
                                    .putCustomAttribute("type", "connect new")
                                    .putCustomAttribute("ip", mVpnGateConnection.getIp())
                                    .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                            prepareVpn();
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
                } else {
                    Answers.getInstance().logCustom(new CustomEvent("Cancel VPN")
                            .putCustomAttribute("type", "cancel connect to vpn")
                            .putCustomAttribute("ip", mVpnGateConnection.getIp())
                            .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
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
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://myiponline.com"));
                startActivity(browserIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAds() {
        if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)) {
            isShowAds = true;
            if (((App) getApplication()).isAdMobPrimary()) {
                mInterstitialAd = new InterstitialAd(getApplicationContext());
                if (BuildConfig.DEBUG) {
                    mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
                } else {
                    mInterstitialAd.setAdUnitId(getString(R.string.admob_full_screen));
                }
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        mInterstitialAd.show();
                    }

                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        if (!mDestroyCalled) {
                            fInterstitialAd = new com.facebook.ads.InterstitialAd(getApplicationContext(), getString(R.string.fan_full_screen));
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
            } else {
                fInterstitialAd = new com.facebook.ads.InterstitialAd(getApplicationContext(), getString(R.string.fan_full_screen));
                fInterstitialAd.setAdListener(new InterstitialAdListener() {
                    @Override
                    public void onInterstitialDisplayed(Ad ad) {

                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {

                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        if (!mDestroyCalled) {
                            mInterstitialAd = new InterstitialAd(getApplicationContext());
                            if (BuildConfig.DEBUG) {
                                mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
                            } else {
                                mInterstitialAd.setAdUnitId(getString(R.string.admob_full_screen));
                            }
                            mInterstitialAd.setAdListener(new AdListener() {
                                @Override
                                public void onAdLoaded() {
                                    mInterstitialAd.show();
                                }
                            });
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
    }

    private void sendConnectVPN() {
        Intent intent = new Intent(BaseProvider.ACTION.ACTION_CONNECT_VPN);
        sendBroadcast(intent);
    }

    private void prepareVpn() {
        if (loadVpnProfile()) {
            startVpn();
        } else {
            Toast.makeText(this, getString(R.string.error_load_profile), Toast.LENGTH_SHORT).show();
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
        if (mVPNService != null && mVPNService.getManagement() != null)
            mVPNService.getManagement().stopVPN(false);

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
                switch (requestCode) {
                    case START_VPN_PROFILE:
                        VPNLaunchHelper.startOpenVpn(vpnProfile, getBaseContext());
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
