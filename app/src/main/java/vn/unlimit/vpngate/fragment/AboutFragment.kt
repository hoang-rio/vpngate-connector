package vn.unlimit.vpngate.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.FragmentAboutBinding

/**
 * Created by hoangnd on 2/6/2018.
 */
class AboutFragment : Fragment(), View.OnClickListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAboutBinding.inflate(layoutInflater)
        binding.txtVersionName.text = BuildConfig.VERSION_NAME
        binding.txtAboutHtml.text =
            String.format(getString(R.string.about_html), getString(R.string.app_name))
        binding.txtVpnGateLink.text = getString(R.string.vpn_gate_link)
        binding.txtVpnGateLink.setOnClickListener(this)
        binding.txtLicenseHtml.text =
            String.format(getString(R.string.license_html), getString(R.string.app_name))
        binding.txtGithubLink.text = getString(R.string.license_link)
        binding.txtGithubLink.setOnClickListener(this)
        binding.txtGithubLinkSstp.setOnClickListener(this)
        return binding.root
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