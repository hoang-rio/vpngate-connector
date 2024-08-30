package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.LayoutConnectUseProtocolDialogBinding
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.models.VPNGateConnection

class ConnectionUseProtocol : BottomSheetDialogFragment(), View.OnClickListener {
    private var mVpnGateConnection: VPNGateConnection? = null
    private var paidServer: PaidServer? = null
    private var clickResult: ClickResult? = null
    private lateinit var binding: LayoutConnectUseProtocolDialogBinding

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

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutConnectUseProtocolDialogBinding.inflate(layoutInflater)
        binding.btnUseTcp.setOnClickListener(this)
        binding.btnUseUdp.setOnClickListener(this)
        if (mVpnGateConnection != null) {
            binding.btnUseTcp.text = "TCP " + mVpnGateConnection!!.tcpPort
            binding.btnUseUdp.text = "UDP " + mVpnGateConnection!!.udpPort
        } else if (paidServer != null) {
            binding.btnUseTcp.background = ResourcesCompat.getDrawable(requireContext().resources, R.drawable.selector_paid_button, requireActivity().theme)
            binding.btnUseTcp.text = "TCP " + paidServer!!.tcpPort
            binding.btnUseUdp.text = "UDP " + paidServer!!.udpPort
        }
        return binding.root
    }

    override fun onClick(view: View) {
        if (clickResult != null) {
            clickResult!!.onResult(binding.btnUseTcp != view)
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
