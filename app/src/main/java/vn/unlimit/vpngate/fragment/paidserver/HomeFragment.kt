package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.viewmodels.UserViewModel

class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var txtWelcome: TextView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var userViewModel: UserViewModel? = null
    private var paidServerActivity: PaidServerActivity? = null
    private var txtDataSize: TextView? = null
    private var lnBuyData: LinearLayout? = null
    private var lnPurchaseHistory: LinearLayout? = null
    private var isObsveredRefresh = false
    private var isAttached = false

    companion object {
        const val TAG = "HomeFragment"
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_paid_server_home, container, false)
        txtWelcome = root.findViewById(R.id.text_home)
        txtDataSize = root.findViewById(R.id.txt_data_size)
        if (paidServerUtil.getUserInfo() != null) {
            txtWelcome!!.text = getString(R.string.home_paid_welcome, paidServerUtil.getUserInfo()!!.getString("fullname"))
            txtDataSize!!.text = paidServerUtil.getUserInfo()!!.getInt("dataSize").toString()
        }
        swipeRefreshLayout = root.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        lnBuyData = root.findViewById(R.id.ln_buy_data)
        lnBuyData?.setOnClickListener(this)
        lnPurchaseHistory = root.findViewById(R.id.ln_purchase_history)
        lnPurchaseHistory?.setOnClickListener(this)
        bindViewModel()
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    private fun bindViewModel() {
        paidServerActivity = (activity as PaidServerActivity)
        this.userViewModel = paidServerActivity?.userViewModel
        userViewModel?.userInfo?.observe(paidServerActivity!!, { userInfo ->
            run {
                if (isAttached) {
                    txtWelcome!!.text = getString(R.string.home_paid_welcome, userInfo?.getString("fullname"))
                    txtDataSize!!.text = OpenVPNService.humanReadableByteCount(userInfo!!.getLong("dataSize"), false, resources)
                }
            }
        })
    }

    override fun onRefresh() {
        try {
            if (isAttached) {
                userViewModel?.fetchUser(true, paidServerActivity, true)
            } else {
                swipeRefreshLayout?.isRefreshing = false
            }
            if (!isObsveredRefresh) {
                userViewModel?.isLoading?.observe(paidServerActivity!!, {
                    swipeRefreshLayout?.isRefreshing = it
                })
                isObsveredRefresh = true
            }
        } catch (th: Throwable) {
            Log.e(TAG, "OnRefresh error", th)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this.isDetached) {
            userViewModel?.fetchUser(true, paidServerActivity)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            lnBuyData -> {
                Toast.makeText(context, "Buy data is developing feature", Toast.LENGTH_SHORT).show()
            }
            lnPurchaseHistory -> {
                Toast.makeText(context, "Purchase history is developing feature", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
