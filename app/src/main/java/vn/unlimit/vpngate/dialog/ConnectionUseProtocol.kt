package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.models.VPNGateConnection

class ConnectionUseProtocol : BottomSheetDialogFragment(), View.OnClickListener {
    private var mVpnGateConnection: VPNGateConnection? = null
    private var paidServer: PaidServer? = null
    private var btnUseTCP: Button? = null
    private var clickResult: ClickResult? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            BottomSheetDialog(requireActivity())

        dialog.setOnShowListener { dialog1: DialogInterface ->
            try {
                val d = dialog1 as BottomSheetDialog

                val bottomSheet =
                    checkNotNull(d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet))
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return dialog
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables", "RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        try {
            val contentView =
                View.inflate(context, R.layout.layout_connect_use_protocol_dialog, null)
            btnUseTCP = contentView.findViewById(R.id.btn_use_tcp)
            btnUseTCP!!.setOnClickListener(this)
            val btnUseUDP = contentView.findViewById<Button>(R.id.btn_use_udp)
            btnUseUDP.setOnClickListener(this)
            if (mVpnGateConnection != null) {
                btnUseTCP!!.text = "TCP " + mVpnGateConnection!!.tcpPort
                btnUseUDP.text = "UDP " + mVpnGateConnection!!.udpPort
            } else if (paidServer != null) {
                btnUseTCP!!.background = requireContext().resources.getDrawable(R.drawable.selector_paid_button, requireActivity().theme)
                btnUseTCP!!.text = "TCP " + paidServer!!.tcpPort
                btnUseUDP.text = "UDP " + paidServer!!.udpPort
            }
            dialog.setContentView(contentView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View) {
        if (clickResult != null) {
            clickResult!!.onResult(btnUseTCP != view)
        }
        try {
            this.dismiss()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Got exception when handle on click ", e)
        }
    }

    interface ClickResult {
        fun onResult(useUdp: Boolean)
    }

    companion object {
        const val TAG = "ConnectionUseProtocol"
        fun newInstance(
            vpnGateConnection: VPNGateConnection?,
            clickResult: ClickResult?
        ): ConnectionUseProtocol {
            val connectionUseProtocol = ConnectionUseProtocol()
            connectionUseProtocol.mVpnGateConnection = vpnGateConnection
            connectionUseProtocol.clickResult = clickResult
            return connectionUseProtocol
        }

        fun newInstance(paidServer: PaidServer?, clickResult: ClickResult?): ConnectionUseProtocol {
            val connectionUseProtocol = ConnectionUseProtocol()
            connectionUseProtocol.paidServer = paidServer
            connectionUseProtocol.clickResult = clickResult
            return connectionUseProtocol
        }
    }
}
