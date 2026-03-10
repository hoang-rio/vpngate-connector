package vn.unlimit.vpngate.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.DialogVpnProtocolSelectionBinding
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.models.VPNGateConnection

/**
 * Bottom sheet dialog for selecting VPN protocol (OpenVPN or SoftEther)
 */
class VpnProtocolSelectionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogVpnProtocolSelectionBinding? = null
    private val binding get() = _binding!!

    private var listener: ProtocolSelectionListener? = null
    private var isSoftEtherAvailable: Boolean = true

    // Generic server config fields — populated by newInstance() factory methods
    private var serverName: String = ""
    private var serverCountry: String = ""
    private var tcpPort: Int = 0
    private var udpPort: Int = 0
    private var seTcpPort: Int = 0
    private var seUdpPort: Int = 0
    private var sstpSupport: Boolean = false

    interface ProtocolSelectionListener {
        fun onProtocolSelected(protocol: VpnProtocol)
    }

    enum class VpnProtocol {
        OPENVPN_TCP,
        OPENVPN_UDP,
        SOFTEther_TCP,
        SOFTEther_UDP,
        MS_SSTP
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
        binding.txtServerName.text = serverName
        binding.txtServerLocation.text = serverCountry

        // Configure SoftEther TCP option
        val hasSoftEtherTcp = isSoftEtherAvailable && seTcpPort > 0
        if (hasSoftEtherTcp) {
            binding.cardSoftEtherTcp.visibility = View.VISIBLE
            binding.txtSoftEtherTcpStatus.text = getString(R.string.protocol_available_port, seTcpPort)
        } else {
            binding.cardSoftEtherTcp.visibility = View.GONE
        }

        // TODO: SoftEther UDP (RUDP) is not yet implemented. RUDP requires a full reliable
        //       UDP transport layer (~5000+ lines in reference implementation) including
        //       NAT traversal, sequence numbers, ACKs, retransmission, and HMAC signatures.
        //       Hide this option until RUDP support is added to the native layer.
        binding.cardSoftEtherUdp.visibility = View.GONE

        // Configure OpenVPN TCP option
        if (tcpPort > 0) {
            binding.cardOpenVpnTcp.visibility = View.VISIBLE
            binding.txtOpenVpnTcpStatus.text = getString(R.string.protocol_available_port, tcpPort)
        } else {
            binding.cardOpenVpnTcp.visibility = View.GONE
        }

        // Configure OpenVPN UDP option
        if (udpPort > 0) {
            binding.cardOpenVpnUdp.visibility = View.VISIBLE
            binding.txtOpenVpnUdpStatus.text = getString(R.string.protocol_available_port, udpPort)
        } else {
            binding.cardOpenVpnUdp.visibility = View.GONE
        }

        // Configure MS-SSTP option
        val hasSstp = sstpSupport && tcpPort > 0
        if (hasSstp) {
            binding.cardMsSstp.visibility = View.VISIBLE
            binding.txtMsSstpStatus.text = getString(R.string.protocol_available_port, tcpPort)
        } else {
            binding.cardMsSstp.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.cardOpenVpnTcp.setOnClickListener {
            if (tcpPort > 0) {
                listener?.onProtocolSelected(VpnProtocol.OPENVPN_TCP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.openvpn_tcp_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardOpenVpnUdp.setOnClickListener {
            if (udpPort > 0) {
                listener?.onProtocolSelected(VpnProtocol.OPENVPN_UDP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.openvpn_udp_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardSoftEtherTcp.setOnClickListener {
            if (isSoftEtherAvailable && seTcpPort > 0) {
                listener?.onProtocolSelected(VpnProtocol.SOFTEther_TCP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.softether_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        // TODO: Enable SoftEther UDP when RUDP is implemented in native layer
        binding.cardSoftEtherUdp.setOnClickListener {
            if (isSoftEtherAvailable && seUdpPort > 0) {
                listener?.onProtocolSelected(VpnProtocol.SOFTEther_UDP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.softether_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardMsSstp.setOnClickListener {
            if (sstpSupport) {
                listener?.onProtocolSelected(VpnProtocol.MS_SSTP)
                dismiss()
            } else {
                Toast.makeText(context, R.string.sstp_support, Toast.LENGTH_SHORT).show()
            }
        }
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
                serverName = connection?.calculateHostName ?: ""
                serverCountry = connection?.countryLong ?: ""
                tcpPort = connection?.tcpPort ?: 0
                udpPort = connection?.udpPort ?: 0
                seTcpPort = connection?.seTcpPort ?: 0
                seUdpPort = connection?.seUdpPort ?: 0
                sstpSupport = connection?.isSSTPSupport() == true
                setSoftEtherAvailable(isSoftEtherAvailable)
            }
        }

        fun newInstance(
            server: PaidServer?,
            isSoftEtherAvailable: Boolean = true
        ): VpnProtocolSelectionDialog {
            return VpnProtocolSelectionDialog().apply {
                serverName = server?.serverDomain ?: ""
                serverCountry = server?.serverLocation ?: ""
                tcpPort = server?.tcpPort ?: 0
                udpPort = server?.udpPort ?: 0
                seTcpPort = server?.tcpPort ?: 0
                seUdpPort = server?.udpPort ?: 0
                sstpSupport = server?.isSSTPSupport() == true
                setSoftEtherAvailable(isSoftEtherAvailable)
            }
        }
    }
}
