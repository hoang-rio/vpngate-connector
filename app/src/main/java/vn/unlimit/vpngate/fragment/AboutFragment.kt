package vn.unlimit.vpngate.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import vn.unlimit.vpngate.R
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.BuildConfig

/**
 * Created by hoangnd on 2/6/2018.
 */
class AboutFragment : Fragment(), View.OnClickListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_about, container, false)
        val txtVersion = rootView.findViewById<TextView>(R.id.txt_version_name)
        txtVersion.text = BuildConfig.VERSION_NAME
        val txtAboutHtml = rootView.findViewById<TextView>(R.id.txt_about_html)
        txtAboutHtml.text =
            String.format(getString(R.string.about_html), getString(R.string.app_name))
        val txtVPNGateLink = rootView.findViewById<TextView>(R.id.txt_vpn_gate_link)
        txtVPNGateLink.text = getString(R.string.vpn_gate_link)
        txtVPNGateLink.setOnClickListener(this)
        val txtLicense = rootView.findViewById<TextView>(R.id.txt_license_html)
        txtLicense.text =
            String.format(getString(R.string.license_html), getString(R.string.app_name))
        val txtLicenseLink = rootView.findViewById<TextView>(R.id.txt_github_link)
        txtLicenseLink.text = getString(R.string.license_link)
        txtLicenseLink.setOnClickListener(this)
        val txtLicenseLinkSStp = rootView.findViewById<TextView>(R.id.txt_github_link_sstp)
        txtLicenseLinkSStp.setOnClickListener(this)
        return rootView
    }

    override fun onClick(view: View) {
        if (view is TextView) {
            openUrl(view.text.toString())
        }
    }

    private fun openUrl(url: String) {
        val browserItent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserItent)
    }
}