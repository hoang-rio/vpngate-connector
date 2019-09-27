package vn.unlimit.vpngate.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.R;

/**
 * Created by hoangnd on 2/6/2018.
 */

public class AboutFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtVersion = rootView.findViewById(R.id.txt_version_name);
        txtVersion.setText(BuildConfig.VERSION_NAME);
        TextView txtAboutHtml = rootView.findViewById(R.id.txt_about_html);
        txtAboutHtml.setText(String.format(getString(R.string.about_html), getString(R.string.app_name)));
        TextView txtVPNGateLink = rootView.findViewById(R.id.txt_vpn_gate_link);
        txtVPNGateLink.setText(getString(R.string.vpn_gate_link));
        txtVPNGateLink.setOnClickListener(this);
        TextView txtLicense = rootView.findViewById(R.id.txt_license_html);
        txtLicense.setText(String.format(getString(R.string.license_html), getString(R.string.app_name)));
        TextView txtLicenseLink = rootView.findViewById(R.id.txt_github_link);
        txtLicenseLink.setText(getString(R.string.license_link));
        txtLicenseLink.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view instanceof TextView) {
            openUrl(((TextView) view).getText().toString());
        }
    }

    private void openUrl(String url) {
        Intent browserItent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserItent);
    }
}
