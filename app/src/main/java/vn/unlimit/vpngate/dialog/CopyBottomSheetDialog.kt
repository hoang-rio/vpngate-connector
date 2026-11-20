package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.LayoutCopyBottomDialogBinding
import vn.unlimit.vpngate.models.VPNGateConnection

/**
 * Created by hoangnd on 2/1/2018.
 */
class CopyBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var mVpnGateConnection: VPNGateConnection? = null
    private lateinit var binding: LayoutCopyBottomDialogBinding

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutCopyBottomDialogBinding.inflate(layoutInflater)
        val connection = mVpnGateConnection ?: run {
            mVpnGateConnection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelable(ARG_CONNECTION, VPNGateConnection::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelable(ARG_CONNECTION)
            }
            mVpnGateConnection
        }
        if (connection == null) {
            // Fragment restored after process death without arguments: nothing to show.
            dismissAllowingStateLoss()
            return binding.root
        }
        binding.txtTitle.text = connection.ip
        binding.btnCopyIp.setOnClickListener(this)
        binding.btnCopyHostname.setOnClickListener(this)
        val baseUrl = instance?.dataUtil?.baseUrl.orEmpty()
        Glide.with(this)
            .load(baseUrl + "/images/flags/" + connection.countryShort + ".png")
            .placeholder(R.color.colorOverlay)
            .error(R.color.colorOverlay)
            .into(binding.imgFlag)
        return binding.root
    }

    override fun onClick(view: View) {
        try {
            val connection = mVpnGateConnection ?: return
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var clip: ClipData? = null
            if (view == binding.btnCopyIp) {
                val params = Bundle()
                params.putString("type", "ip")
                params.putString("ip", connection.ip)
                params.putString("hostname", connection.calculateHostName)
                params.putString("country", connection.countryLong)
                FirebaseAnalytics.getInstance(requireActivity().applicationContext)
                    .logEvent("Copy", params)
                clip = ClipData.newPlainText("text", connection.ip)
            } else if (view == binding.btnCopyHostname) {
                val params = Bundle()
                params.putString("type", "hostname")
                params.putString("ip", connection.ip)
                params.putString("hostname", connection.calculateHostName)
                params.putString("country", connection.countryLong)
                FirebaseAnalytics.getInstance(requireActivity().applicationContext)
                    .logEvent("Copy", params)
                clip = ClipData.newPlainText("text", connection.calculateHostName)
            }
            if (clip != null) {
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
        private const val ARG_CONNECTION = "vpn_gate_connection"

        @JvmStatic
        fun newInstance(vpnGateConnection: VPNGateConnection?): CopyBottomSheetDialog {
            val copyBottomSheetDialog = CopyBottomSheetDialog()
            copyBottomSheetDialog.mVpnGateConnection = vpnGateConnection
            copyBottomSheetDialog.arguments = Bundle().apply {
                putParcelable(ARG_CONNECTION, vpnGateConnection)
            }
            return copyBottomSheetDialog
        }
    }
}
