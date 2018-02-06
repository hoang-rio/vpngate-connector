package vn.unlimit.vpngate.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;

/**
 * Created by hoangnd on 2/1/2018.
 */

public class CopyBottomSheetDialog extends BottomSheetDialogFragment implements View.OnClickListener {
    private VPNGateConnection mVpnGateConnection;
    private View btnCopyIp;
    private View btnCopyHostName;
    private TextView txtTitle;
    private ImageView imgFlag;
    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public static CopyBottomSheetDialog newInstance(VPNGateConnection vpnGateConnection) {
        CopyBottomSheetDialog copyBottomSheetDialog = new CopyBottomSheetDialog();
        copyBottomSheetDialog.mVpnGateConnection = vpnGateConnection;
        return copyBottomSheetDialog;
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
        View contentView = View.inflate(getContext(), R.layout.layout_copy_bottom, null);
        btnCopyIp = contentView.findViewById(R.id.btn_copy_ip);
        txtTitle = contentView.findViewById(R.id.txt_title);
        txtTitle.setText(mVpnGateConnection.getIp());
        imgFlag = contentView.findViewById(R.id.img_flag);
        GlideApp.with(this)
                .load("http://www.vpngate.net/images/flags/" + mVpnGateConnection.getCountryShort() + ".png")
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(imgFlag);
        btnCopyIp.setOnClickListener(this);
        btnCopyHostName = contentView.findViewById(R.id.btn_copy_hostname);
        btnCopyHostName.setOnClickListener(this);
        dialog.setContentView(contentView);
    }

    @Override
    public void onClick(View view) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = null;
            if (view.equals(btnCopyIp)) {
                Answers.getInstance().logCustom(new CustomEvent("Copy")
                        .putCustomAttribute("type", "ip")
                        .putCustomAttribute("ip", mVpnGateConnection.getIp())
                        .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                clip = ClipData.newPlainText("text", mVpnGateConnection.getIp());
            } else if (view.equals(btnCopyHostName)) {
                Answers.getInstance().logCustom(new CustomEvent("Copy")
                        .putCustomAttribute("type", "hostname")
                        .putCustomAttribute("ip", mVpnGateConnection.getIp())
                        .putCustomAttribute("country", mVpnGateConnection.getCountryLong()));
                clip = ClipData.newPlainText("text", mVpnGateConnection.getCalculateHostName());
            }
            if (clip != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), getResources().getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
