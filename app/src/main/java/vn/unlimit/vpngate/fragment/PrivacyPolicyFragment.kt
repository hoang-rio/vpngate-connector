package vn.unlimit.vpngate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.FragmentPrivacyPolicyBinding
import java.io.ByteArrayOutputStream
import java.io.IOException

class PrivacyPolicyFragment : Fragment(), View.OnClickListener {
    private var mainActivity: MainActivity? = null
    private lateinit var binding: FragmentPrivacyPolicyBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            mainActivity = activity as MainActivity?
            binding = FragmentPrivacyPolicyBinding.inflate(layoutInflater)
            binding.btnAccept.setOnClickListener(this)
            binding.btnDecide.setOnClickListener(this)
            binding.root
        } catch (ex: Exception) {
            if (ex.message != null && ex.message!!.contains("webview")) {
                Toast.makeText(
                    mainActivity,
                    R.string.no_webview_installed_you_must_install_system_webview_from_playstore_to_continue,
                    Toast.LENGTH_LONG
                ).show()
                mainActivity!!.finish()
            }
            null
        }
    }

    override fun onViewCreated(view: View, savedInstance: Bundle?) {
        //Load content to webview
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                binding.progressBar.visibility = View.GONE
            }
        }
        binding.webView.loadData(readTextFromResource(), "text/html", "utf-8")
    }

    private fun readTextFromResource(): String {
        val raw = resources.openRawResource(R.raw.privacy_policy)
        val stream = ByteArrayOutputStream()
        var i: Int
        try {
            i = raw.read()
            while (i != -1) {
                stream.write(i)
                i = raw.read()
            }
            raw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stream.toString()
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