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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

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
import io.fabric.sdk.android.Fabric;
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
    private static final int START_VPN_PROFILE = 70;
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
    private DataUtil dataUtil;
    private VPNGateConnection mVpnGateConnection;
    private Button btnConnect;
    private View btnBack;
    private VpnProfile vpnProfile;
    private BroadcastReceiver brStatus;
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
        Fabric.with(this, new Crashlytics());
        Fabric.with(this, new Answers());
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
        bindData();
        registerBroadCast();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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

    private void changeServerStatus(VpnStatus.ConnectionStatus status) {
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
            case LEVEL_NOTCONNECTED:
                if (!isConnecting && !isAuthFailed) {
                    linkCheckIp.setVisibility(View.GONE);
                    btnConnect.setText(R.string.connect_to_this_server);
                    if (Build.VERSION.SDK_INT >= 16) {
                        btnConnect.setBackground(getResources().getDrawable(R.drawable.selector_primary_button));
                    }
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

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case START_VPN_PROFILE:
                    VPNLaunchHelper.startOpenVpn(vpnProfile, getBaseContext());
                    break;
            }
        }
    }

}
