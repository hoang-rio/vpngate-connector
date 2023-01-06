package vn.unlimit.vpngate.fragment

import android.webkit.WebView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import vn.unlimit.vpngate.R
import android.widget.Toast
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.activities.MainActivity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception

class PrivacyPolicyFragment : Fragment(), View.OnClickListener {
    private var btnAccept: View? = null
    private var btnDecide: View? = null
    private var mainActivity: MainActivity? = null
    private var webView: WebView? = null
    private var progressBar: View? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            mainActivity = activity as MainActivity?
            val rootView = inflater.inflate(R.layout.fragment_privacy_policy, container, false)
            btnAccept = rootView.findViewById(R.id.btn_accept)
            btnAccept?.setOnClickListener(this)
            btnDecide = rootView.findViewById(R.id.btn_decide)
            btnDecide?.setOnClickListener(this)
            webView = rootView.findViewById(R.id.web_view)
            progressBar = rootView.findViewById(R.id.progress_bar)
            rootView
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
        if (webView != null) {
            //Load content to webview
            webView!!.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    progressBar!!.visibility = View.GONE
                }
            }
            webView!!.loadData(readTextFromResource(), "text/html", "utf-8")
        }
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
        if (view == btnDecide) {
            //Exit app when user decide
            mainActivity!!.finish()
        } else if (view == btnAccept) {
            //Start home fragment
            App.getInstance().dataUtil.isAcceptedPrivacyPolicy = true
            mainActivity!!.restartApp()
        }
    }
}