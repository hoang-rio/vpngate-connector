package vn.unlimit.vpngate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.activities.MainActivity;

public class PrivacyPolicyFragment extends Fragment implements View.OnClickListener {
    private View btnAccept;
    private View btnDecide;
    private MainActivity mainActivity;
    private WebView webView;
    private View progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            mainActivity = (MainActivity) getActivity();
            View rootView = inflater.inflate(R.layout.fragment_privacy_policy, container, false);
            btnAccept = rootView.findViewById(R.id.btn_accept);
            btnAccept.setOnClickListener(this);
            btnDecide = rootView.findViewById(R.id.btn_decide);
            btnDecide.setOnClickListener(this);
            webView = rootView.findViewById(R.id.web_view);
            progressBar = rootView.findViewById(R.id.progress_bar);
            return rootView;
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("webview")) {
                Toast.makeText(mainActivity, R.string.no_webview_installed_you_must_install_system_webview_from_playstore_to_continue, Toast.LENGTH_LONG).show();
                mainActivity.finish();
            }
            return null;
        }
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstance) {
        if (webView != null) {
            //Load content to webview
            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    progressBar.setVisibility(View.GONE);
                }
            });
            webView.loadData(readTextFromResource(), "text/html", "utf-8");
        }
    }

    private String readTextFromResource() {
        InputStream raw = getResources().openRawResource(R.raw.privacy_policy);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int i;
        try {
            i = raw.read();
            while (i != -1) {
                stream.write(i);
                i = raw.read();
            }
            raw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toString();
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnDecide)) {
            //Exit app when user decide
            mainActivity.finish();
        } else if (view.equals(btnAccept)) {
            //Start home fragment
            App.getInstance().getDataUtil().setAcceptedPrivacyPolicy(true);
            mainActivity.restartApp();
        }
    }
}
