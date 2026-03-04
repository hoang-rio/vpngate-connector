package vn.unlimit.vpngate.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.DialogVpnProtocolSelectionBinding
import vn.unlimit.vpngate.models.VPNGateConnection

/**
 * Bottom sheet dialog for selecting VPN protocol (OpenVPN or SoftEther)
 */
class VpnProtocolSelectionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogVpnProtocolSelectionBinding? = null
    private val binding get() = _binding!!

    private var vpnGateConnection: VPNGateConnection? = null
    private var listener: ProtocolSelectionListener? = null
    private var isSoftEtherAvailable: Boolean = true

    interface ProtocolSelectionListener {
        fun onProtocolSelected(protocol: VpnProtocol)
    }

    enum class VpnProtocol {
        OPENVPN_TCP,
        OPENVPN_UDP,
        SOFTEther_TCP,
        SOFTEther_UDP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use day-night bottom sheet style that adapts to system theme (light/dark mode)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVpnProtocolSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        vpnGateConnection?.let { connection ->
            binding.txtServerName.text = connection.calculateHostName
            binding.txtServerLocation.text = connection.countryLong

            // Check OpenVPN availability
            val hasOpenVpnTcp = connection.tcpPort > 0
            val hasOpenVpnUdp = connection.udpPort > 0

            // Configure OpenVPN TCP option - hide if not available
            if (hasOpenVpnTcp) {
                binding.cardOpenVpnTcp.visibility = View.VISIBLE
                binding.txtOpenVpnTcpStatus.text = getString(R.string.protocol_available_port, connection.tcpPort)
            } else {
                binding.cardOpenVpnTcp.visibility = View.GONE
            }

            // Configure OpenVPN UDP option - hide if not available
            if (hasOpenVpnUdp) {
                binding.cardOpenVpnUdp.visibility = View.VISIBLE
                binding.txtOpenVpnUdpStatus.text = getString(R.string.protocol_available_port, connection.udpPort)
            } else {
                binding.cardOpenVpnUdp.visibility = View.GONE
            }

            // Configure SoftEther TCP option - hide if not available
            val hasSoftEtherTcp = isSoftEtherAvailable && connection.seTcpPort > 0
            if (hasSoftEtherTcp) {
                binding.cardSoftEtherTcp.visibility = View.VISIBLE
                binding.txtSoftEtherTcpStatus.text = getString(R.string.protocol_available_port, connection.seTcpPort)
            } else {
                binding.cardSoftEtherTcp.visibility = View.GONE
            }

            // Configure SoftEther UDP option - hide if not available
            val hasSoftEtherUdp = isSoftEtherAvailable && connection.seUdpPort > 0
            if (hasSoftEtherUdp) {
                binding.cardSoftEtherUdp.visibility = View.VISIBLE
                binding.txtSoftEtherUdpStatus.text = getString(R.string.protocol_available_port, connection.seUdpPort)
            } else {
                binding.cardSoftEtherUdp.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardOpenVpnTcp.setOnClickListener {
            if (vpnGateConnection?.tcpPort ?: 0 > 0) {
                listener?.onProtocolSelected(VpnProtocol.OPENVPN_TCP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.openvpn_tcp_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardOpenVpnUdp.setOnClickListener {
            if (vpnGateConnection?.udpPort ?: 0 > 0) {
                listener?.onProtocolSelected(VpnProtocol.OPENVPN_UDP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.openvpn_udp_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardSoftEtherTcp.setOnClickListener {
            if (isSoftEtherAvailable && vpnGateConnection?.seTcpPort ?: 0 > 0) {
                listener?.onProtocolSelected(VpnProtocol.SOFTEther_TCP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.softether_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardSoftEtherUdp.setOnClickListener {
            if (isSoftEtherAvailable && vpnGateConnection?.seUdpPort ?: 0 > 0) {
                listener?.onProtocolSelected(VpnProtocol.SOFTEther_UDP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.softether_not_available, Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun setVPNGateConnection(connection: VPNGateConnection?) {
        this.vpnGateConnection = connection
    }

    fun setSoftEtherAvailable(available: Boolean) {
        this.isSoftEtherAvailable = available
    }

    fun setProtocolSelectionListener(listener: ProtocolSelectionListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VpnProtocolSelectionDialog"

        fun newInstance(
            connection: VPNGateConnection?,
            isSoftEtherAvailable: Boolean = true
        ): VpnProtocolSelectionDialog {
            return VpnProtocolSelectionDialog().apply {
                setVPNGateConnection(connection)
                setSoftEtherAvailable(isSoftEtherAvailable)
            }
        }
    }
}
