package vn.unlimit.vpngate.fragment

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.FragmentPrivacyPolicyBinding
import vn.unlimit.vpngate.utils.InsetUtils
import java.io.IOException

class PrivacyPolicyFragment : Fragment(), View.OnClickListener {
    private var mainActivity: MainActivity? = null
    private lateinit var binding: FragmentPrivacyPolicyBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as MainActivity?
        binding = FragmentPrivacyPolicyBinding.inflate(layoutInflater)
        binding.btnAccept.setOnClickListener(this)
        binding.btnDecide.setOnClickListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstance: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { v, windowInsets ->
            val insets = InsetUtils.getSystemBarsInsets(windowInsets)
            if (insets.bottom > 0) {
                v.updatePadding(bottom = insets.bottom)
            } else {
                v.updatePadding(bottom = 0)
            }
            windowInsets
        }
        binding.tvPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPolicy.text = Html.fromHtml(readTextFromResource(), Html.FROM_HTML_MODE_LEGACY)
    }

    private fun readTextFromResource(): String {
        return try {
            resources.openRawResource(R.raw.privacy_policy).use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    override fun onClick(view: View) {
        if (view == binding.btnDecide) {
            //Exit app when user decide
            mainActivity!!.finish()
        } else if (view == binding.btnAccept) {
            //Start home fragment
            App.instance!!.dataUtil!!.isAcceptedPrivacyPolicy = true
            mainActivity!!.restartApp()
        }
    }
}