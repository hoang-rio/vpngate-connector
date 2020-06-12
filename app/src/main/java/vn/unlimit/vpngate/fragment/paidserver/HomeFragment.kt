package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.utils.PaidServerUtil

class HomeFragment : Fragment() {
    private val paidServerUtil = App.getInstance().paidServerUtil
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_paid_server_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        textView.text = paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
        return root
    }
}
