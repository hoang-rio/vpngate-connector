package vn.unlimit.vpngate.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;

/**
 * Created by hoangnd on 2/1/2018.
 */

public class CopyBottomSheetDialog extends BottomSheetDialogFragment implements View.OnClickListener {
    private VPNGateConnection mVpnGateConnection;
    private View btnCopyIp;
    private View btnCopyHostName;

    public static CopyBottomSheetDialog newInstance(VPNGateConnection vpnGateConnection) {
        CopyBottomSheetDialog copyBottomSheetDialog = new CopyBottomSheetDialog();
        copyBottomSheetDialog.mVpnGateConnection = vpnGateConnection;
        return copyBottomSheetDialog;
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
            } catch (Throwable th) {
                th.printStackTrace();
            }
        });
        return dialog;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(@NotNull Dialog dialog, int style) {
        try {
            View contentView = View.inflate(getContext(), R.layout.layout_copy_bottom_dialog, null);
            btnCopyIp = contentView.findViewById(R.id.btn_copy_ip);
            TextView txtTitle = contentView.findViewById(R.id.txt_title);
            txtTitle.setText(mVpnGateConnection.getIp());
            ImageView imgFlag = contentView.findViewById(R.id.img_flag);
            Glide.with(this)
                    .load(App.getInstance().getDataUtil().getBaseUrl() + "/images/flags/" + mVpnGateConnection.getCountryShort() + ".png")
                    .placeholder(R.color.colorOverlay)
                    .error(R.color.colorOverlay)
                    .into(imgFlag);
            btnCopyIp.setOnClickListener(this);
            btnCopyHostName = contentView.findViewById(R.id.btn_copy_hostname);
            btnCopyHostName.setOnClickListener(this);
            dialog.setContentView(contentView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        try {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = null;
            if (view.equals(btnCopyIp)) {
                Bundle params = new Bundle();
                params.putString("type", "ip");
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(requireActivity().getApplicationContext()).logEvent("Copy", params);
                clip = ClipData.newPlainText("text", mVpnGateConnection.getIp());
            } else if (view.equals(btnCopyHostName)) {
                Bundle params = new Bundle();
                params.putString("type", "hostname");
                params.putString("ip", mVpnGateConnection.getIp());
                params.putString("hostname", mVpnGateConnection.getCalculateHostName());
                params.putString("country", mVpnGateConnection.getCountryLong());
                FirebaseAnalytics.getInstance(requireActivity().getApplicationContext()).logEvent("Copy", params);
                clip = ClipData.newPlainText("text", mVpnGateConnection.getCalculateHostName());
            }
            if (clip != null) {
                assert clipboard != null;
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), getResources().getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
