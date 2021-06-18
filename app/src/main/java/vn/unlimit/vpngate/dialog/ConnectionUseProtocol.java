package vn.unlimit.vpngate.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.PaidServer;
import vn.unlimit.vpngate.models.VPNGateConnection;

public class ConnectionUseProtocol extends BottomSheetDialogFragment implements View.OnClickListener {
    public interface ClickResult {
        void onResult(boolean useUdp);
    }

    private VPNGateConnection mVpnGateConnection;
    private PaidServer paidServer;
    private Button btnUseTCP;
    private ClickResult clickResult;

    public static ConnectionUseProtocol newInstance(VPNGateConnection vpnGateConnection, ClickResult clickResult) {
        ConnectionUseProtocol connectionUseProtocol = new ConnectionUseProtocol();
        connectionUseProtocol.mVpnGateConnection = vpnGateConnection;
        connectionUseProtocol.clickResult = clickResult;
        return connectionUseProtocol;
    }

    public static ConnectionUseProtocol newInstance(PaidServer paidServer, ClickResult clickResult) {
        ConnectionUseProtocol connectionUseProtocol = new ConnectionUseProtocol();
        connectionUseProtocol.paidServer = paidServer;
        connectionUseProtocol.clickResult = clickResult;
        return connectionUseProtocol;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireActivity());

        dialog.setOnShowListener(dialog1 -> {
            try {
                BottomSheetDialog d = (BottomSheetDialog) dialog1;

                FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                assert bottomSheet != null;
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return dialog;
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        try {
            View contentView = View.inflate(getContext(), R.layout.layout_connect_use_protocol_dialog, null);
            btnUseTCP = contentView.findViewById(R.id.btn_use_tcp);
            btnUseTCP.setOnClickListener(this);
            Button btnUseUDP = contentView.findViewById(R.id.btn_use_udp);
            btnUseUDP.setOnClickListener(this);
            if (mVpnGateConnection != null) {
                btnUseTCP.setText("TCP " + mVpnGateConnection.getTcpPort());
                btnUseUDP.setText("UDP " + mVpnGateConnection.getUdpPort());
            } else if (paidServer != null) {
                btnUseTCP.setBackground(requireContext().getResources().getDrawable(R.drawable.selector_paid_button));
                btnUseTCP.setText("TCP " + paidServer.getTcpPort());
                btnUseUDP.setText("UDP " + paidServer.getUdpPort());
            }
            dialog.setContentView(contentView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        if (clickResult != null) {
            clickResult.onResult(!btnUseTCP.equals(view));
        }
        this.dismiss();
    }
}
