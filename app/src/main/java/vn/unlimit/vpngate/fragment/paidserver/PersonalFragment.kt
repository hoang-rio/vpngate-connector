package vn.unlimit.vpngate.fragment.paidserver

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity

class PersonalFragment : Fragment(), View.OnClickListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var rootView = inflater.inflate(R.layout.fragment_personal, container, false)
        rootView.findViewById<View>(R.id.ln_notification_setting)?.setOnClickListener(this);
        rootView.findViewById<View>(R.id.ln_profile)?.setOnClickListener(this)
        rootView.findViewById<View>(R.id.ln_change_password)?.setOnClickListener(this)
        rootView.findViewById<View>(R.id.ln_about)?.setOnClickListener(this)
        return rootView
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.ln_about -> {
                var intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra(MainActivity.TARGET_FRAGMENT, "about");
                startActivity(intent);
            }
        }
    }
}