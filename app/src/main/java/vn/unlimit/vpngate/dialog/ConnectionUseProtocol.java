package vn.unlimit.vpngate.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;

public class ConnectionUseProtocol extends BottomSheetDialogFragment implements View.OnClickListener {
    public interface ClickResult {
        void onResult(boolean useUdp);
    }

    private VPNGateConnection mVpnGateConnection;
    private Button btnUseTCP;
    private Button btnUseUDP;
    private ClickResult clickResult;

    public static ConnectionUseProtocol newInstance(VPNGateConnection vpnGateConnection, ClickResult clickResult) {
        ConnectionUseProtocol connectionUseProtocol = new ConnectionUseProtocol();
        connectionUseProtocol.mVpnGateConnection = vpnGateConnection;
        connectionUseProtocol.clickResult = clickResult;
        return connectionUseProtocol;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(getActivity());

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;

                FrameLayout bottomSheet = d.findViewById(android.support.design.R.id.design_bottom_sheet);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        //

        return dialog;
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        try {
            View contentView = View.inflate(getContext(), R.layout.layout_connect_use_protocol, null);
            btnUseTCP = contentView.findViewById(R.id.btn_use_tcp);
            btnUseTCP.setOnClickListener(this);
            btnUseUDP = contentView.findViewById(R.id.btn_use_udp);
            btnUseUDP.setOnClickListener(this);
            btnUseTCP.setText("TCP " + mVpnGateConnection.getTcpPort());
            btnUseUDP.setText("UDP " + mVpnGateConnection.getUdpPort());
            dialog.setContentView(contentView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnUseTCP)) {
            clickResult.onResult(false);
        } else {
            clickResult.onResult(true);
        }
    }
}