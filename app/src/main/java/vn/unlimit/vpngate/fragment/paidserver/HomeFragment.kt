package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity

class HomeFragment : Fragment() {
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var txtWelcome: TextView? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_paid_server_home, container, false)
        txtWelcome = root.findViewById(R.id.text_home)
        if(paidServerUtil.getUserInfo() != null) {
            txtWelcome!!.text = getString(R.string.home_paid_welcome, paidServerUtil.getUserInfo()!!.getString("fullname"))
        }
        bindViewModel()
        return root
    }

    private fun bindViewModel() {
        val paidServerActivity = (activity as PaidServerActivity)
        val userViewModel = paidServerActivity.userViewModel
        userViewModel?.userInfo?.observe(paidServerActivity, Observer { userInfo ->
            run {
                txtWelcome!!.text = getString(R.string.home_paid_welcome, userInfo?.getString("fullname"))
            }
        })
    }
}
