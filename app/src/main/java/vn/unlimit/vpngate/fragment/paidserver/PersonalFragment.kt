package vn.unlimit.vpngate.fragment.paidserver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.common.base.Strings
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.viewmodels.DeviceViewModel

class PersonalFragment : Fragment(), View.OnClickListener {

    private var lnNotificationSetting: View? = null
    var deviceViewModel: DeviceViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_personal, container, false)
        lnNotificationSetting = rootView.findViewById(R.id.ln_notification_setting)
        lnNotificationSetting?.setOnClickListener(this)
        rootView.findViewById<View>(R.id.ln_profile)?.setOnClickListener(this)
        rootView.findViewById<View>(R.id.ln_change_password)?.setOnClickListener(this)
        rootView.findViewById<View>(R.id.ln_about)?.setOnClickListener(this)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)
        if (deviceViewModel!!.deviceInfo.value == null || Strings.isNullOrEmpty(deviceViewModel!!.deviceInfo.value?._id)) {
            lnNotificationSetting?.visibility = View.GONE
            view.findViewById<View>(R.id.line_notification_setting).visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ln_about -> {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra(MainActivity.TARGET_FRAGMENT, "about")
                startActivity(intent)
            }
            R.id.ln_notification_setting -> findNavController().navigate(R.id.navigation_notification_setting)
            R.id.ln_change_password -> findNavController().navigate(R.id.navigation_change_pass)
        }
    }
}