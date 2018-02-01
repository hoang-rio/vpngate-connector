package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;

/**
 * Created by hoangnd on 2/1/2018.
 */

public class DetailFragment extends Fragment implements View.OnClickListener {
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
    public void onClick(View view) {
        try {
            if (view.equals(btnViaIp)) {
                Toast.makeText(getContext(), "Connecting via Ip: " + mVpnGateConnection.getIp(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Connecting via hostname: " + mVpnGateConnection.getCalculateHostName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
