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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;

/**
 * Created by hoangnd on 2/1/2018.
 */

public class DetailFragment extends Fragment implements View.OnClickListener {
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
    Context mContext;
    private VPNGateConnection mVpnGateConnection;
    private Button btnViaIp;
    private Button btnViaHostName;
    private VpnProfile vpnProfile;
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

    public static DetailFragment newInstance(VPNGateConnection vpnGateConnection) {
        DetailFragment detailFragment = new DetailFragment();
        detailFragment.mVpnGateConnection = vpnGateConnection;
        return detailFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        btnViaIp = rootView.findViewById(R.id.btn_via_ip);
        btnViaIp.setOnClickListener(this);
        btnViaHostName = rootView.findViewById(R.id.btn_via_hostname);
        btnViaHostName.setOnClickListener(this);
        imgFlag = rootView.findViewById(R.id.img_flag);
        txtCountry = rootView.findViewById(R.id.txt_country);
        txtIp = rootView.findViewById(R.id.txt_ip);
        txtHostname = rootView.findViewById(R.id.txt_hostname);
        txtScore = rootView.findViewById(R.id.txt_score);
        txtUptime = rootView.findViewById(R.id.txt_uptime);
        txtSpeed = rootView.findViewById(R.id.txt_speed);
        txtPing = rootView.findViewById(R.id.txt_ping);
        txtSession = rootView.findViewById(R.id.txt_session);
        txtOwner = rootView.findViewById(R.id.txt_owner);
        txtTotalUser = rootView.findViewById(R.id.txt_total_user);
        txtTotalTraffic = rootView.findViewById(R.id.txt_total_traffic);
        txtLogType = rootView.findViewById(R.id.txt_log_type);
        bindData();
        return rootView;
    }

    private void bindData() {
        if (mVpnGateConnection != null) {
            try {
                GlideApp.with(mContext)
                        .load("http://www.vpngate.net/images/flags/" + mVpnGateConnection.getCountryShort() + ".png")
                        .placeholder(R.color.colorOverlay)
                        .error(R.color.colorOverlay)
                        .into(imgFlag);
                txtCountry.setText(mVpnGateConnection.getCountryLong());
                txtIp.setText(mVpnGateConnection.getIp());
                txtHostname.setText(mVpnGateConnection.getCalculateHostName());
                txtScore.setText(mVpnGateConnection.getScoreAsString());
                txtUptime.setText(mVpnGateConnection.getCalculateUpTime(mContext));
                txtSpeed.setText(mVpnGateConnection.getCalculateSpeed());
                txtPing.setText(mVpnGateConnection.getPingAsString());
                txtSession.setText(mVpnGateConnection.getNumVpnSessionAsString());
                txtOwner.setText(mVpnGateConnection.getOperator());
                txtTotalUser.setText(String.valueOf(mVpnGateConnection.getTotalUser()));
                txtTotalTraffic.setText(mVpnGateConnection.getCalculateTotalTraffic());
                txtLogType.setText(mVpnGateConnection.getLogType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            Intent intent = new Intent(getContext(), OpenVPNService.class);
            intent.setAction(OpenVPNService.START_SERVICE);
            getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            getContext().unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        try {
            if (view.equals(btnViaHostName)) {
                mVpnGateConnection.setConnectViaHostName(true);
            } else {
                mVpnGateConnection.setConnectViaHostName(false);
            }
            if (checkStatus()) {
                stopVpn();
            } else {
                prepareVpn();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareVpn() {
        if (loadVpnProfile()) {
            startVpn();
        } else {
            Toast.makeText(getContext(), getString(R.string.error_importing_file), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean loadVpnProfile() {
        byte[] data = mVpnGateConnection.getOpenVpnConfigData().getBytes();
        ConfigParser cp = new ConfigParser();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
        try {
            cp.parseConfig(isr);
            vpnProfile = cp.convertProfile();
            if (mVpnGateConnection.isConnectViaHostName()) {
                vpnProfile.mName = mVpnGateConnection.getCalculateHostName() + "[" + mVpnGateConnection.getCountryLong() + "]";
                vpnProfile.mServerName = mVpnGateConnection.getCalculateHostName();
            } else {
                vpnProfile.mName = mVpnGateConnection.getIp() + "[" + mVpnGateConnection.getCountryLong() + "]";
                vpnProfile.mIPv4Address = mVpnGateConnection.getIp();
            }

            ProfileManager.getInstance(getContext()).addProfile(vpnProfile);
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
        ProfileManager.setConntectedVpnProfileDisconnected(getContext());
        if (mVPNService != null && mVPNService.getManagement() != null)
            mVPNService.getManagement().stopVPN(false);

    }

    private void startVpn() {
        Intent intent = VpnService.prepare(getContext());

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
                    VPNLaunchHelper.startOpenVpn(vpnProfile, getActivity().getBaseContext());
                    break;
            }
        }
    }

}
