package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnection

/**
 * Created by hoangnd on 2/1/2018.
 */
class CopyBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var mVpnGateConnection: VPNGateConnection? = null
    private var btnCopyIp: View? = null
    private var btnCopyHostName: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            BottomSheetDialog(requireActivity())

        dialog.setOnShowListener { dialog1: DialogInterface ->
            try {
                val d = dialog1 as BottomSheetDialog

                val bottomSheet =
                    checkNotNull(d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet))
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
        return dialog
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        try {
            val contentView = View.inflate(context, R.layout.layout_copy_bottom_dialog, null)
            btnCopyIp = contentView.findViewById(R.id.btn_copy_ip)
            val txtTitle = contentView.findViewById<TextView>(R.id.txt_title)
            txtTitle.text = mVpnGateConnection!!.ip
            val imgFlag = contentView.findViewById<ImageView>(R.id.img_flag)
            Glide.with(this)
                .load(instance!!.dataUtil!!.baseUrl + "/images/flags/" + mVpnGateConnection!!.countryShort + ".png")
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(imgFlag)
            btnCopyIp!!.setOnClickListener(this)
            btnCopyHostName = contentView.findViewById(R.id.btn_copy_hostname)
            btnCopyHostName!!.setOnClickListener(this)
            dialog.setContentView(contentView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View) {
        try {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var clip: ClipData? = null
            if (view == btnCopyIp) {
                val params = Bundle()
                params.putString("type", "ip")
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(requireActivity().applicationContext)
                    .logEvent("Copy", params)
                clip = ClipData.newPlainText("text", mVpnGateConnection!!.ip)
            } else if (view == btnCopyHostName) {
                val params = Bundle()
                params.putString("type", "hostname")
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(requireActivity().applicationContext)
                    .logEvent("Copy", params)
                clip = ClipData.newPlainText("text", mVpnGateConnection!!.calculateHostName)
            }
            if (clip != null) {
                checkNotNull(clipboard)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, resources.getString(R.string.copied), Toast.LENGTH_SHORT)
                    .show()
            }
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(vpnGateConnection: VPNGateConnection?): CopyBottomSheetDialog {
            val copyBottomSheetDialog = CopyBottomSheetDialog()
            copyBottomSheetDialog.mVpnGateConnection = vpnGateConnection
            return copyBottomSheetDialog
        }
    }
}
