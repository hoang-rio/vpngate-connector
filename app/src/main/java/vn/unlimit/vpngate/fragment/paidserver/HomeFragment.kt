package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.viewmodels.UserViewModel

class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var txtWelcome: TextView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var userViewModel: UserViewModel? = null
    private var paidServerActivity: PaidServerActivity? = null
    private var txtDataSize: TextView? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_paid_server_home, container, false)
        txtWelcome = root.findViewById(R.id.text_home)
        txtDataSize = root.findViewById(R.id.txt_data_size)
        if(paidServerUtil.getUserInfo() != null) {
            txtWelcome!!.text = getString(R.string.home_paid_welcome, paidServerUtil.getUserInfo()!!.getString("fullname"))
            txtDataSize!!.text = paidServerUtil.getUserInfo()!!.getInt("dataSize").toString()
        }
        swipeRefreshLayout = root.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        bindViewModel()
        return root
    }

    private fun bindViewModel() {
        paidServerActivity = (activity as PaidServerActivity)
        this.userViewModel = paidServerActivity?.userViewModel
        userViewModel?.userInfo?.observe(paidServerActivity!!, { userInfo ->
            run {
                txtWelcome!!.text = getString(R.string.home_paid_welcome, userInfo?.getString("fullname"))
                txtDataSize!!.text = OpenVPNService.humanReadableByteCount(userInfo!!.getLong("dataSize"), false, resources)
            }
        })
        userViewModel?.isLoading?.observe(paidServerActivity!!, {
            swipeRefreshLayout?.isRefreshing = it
        })
    }

    override fun onRefresh() {
        userViewModel?.fetchUser(true, paidServerActivity)
    }
}
