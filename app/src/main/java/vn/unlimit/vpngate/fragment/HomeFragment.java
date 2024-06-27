package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.activities.DetailActivity;
import vn.unlimit.vpngate.activities.MainActivity;
import vn.unlimit.vpngate.adapter.OnItemClickListener;
import vn.unlimit.vpngate.adapter.OnItemLongClickListener;
import vn.unlimit.vpngate.adapter.OnScrollListener;
import vn.unlimit.vpngate.adapter.VPNGateListAdapter;
import vn.unlimit.vpngate.dialog.CopyBottomSheetDialog;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.utils.DataUtil;

/**
 * Created by hoangnd on 1/30/2018.
 */

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, RequestListener, View.OnClickListener, OnItemClickListener, OnItemLongClickListener, OnScrollListener {
    private final String TAG = "HOME_FREE";
    private SwipeRefreshLayout lnSwipeRefresh;
    private Context mContext;
    private RecyclerView recyclerViewVPN;
    private VPNGateListAdapter vpnGateListAdapter;
    private VPNGateTask vpnGateTask;
    private DataUtil dataUtil;
    private View btnToTop;
    private boolean isSearching = false;
    private String mKeyword = "";
    private Handler handler;
    private MainActivity mActivity;
    private InterstitialAd interstitialAd;
    private TextView txtEmpty;
    //Flag ads is showed need request new ad
    private boolean isShowedAd = true;

    public HomeFragment() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vpnGateTask != null) {
            vpnGateTask.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataUtil.hasAds()) {

            if (interstitialAd == null || isShowedAd) {
                AdRequest adRequest = new AdRequest.Builder().build();
                InterstitialAd.load(mContext, getString(R.string.admob_full_screen_detail), adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        HomeFragment.this.interstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
                        HomeFragment.this.interstitialAd = null;
                        Log.e(TAG, loadAdError.toString());
                    }
                });
                isShowedAd = false;
            }

        }
    }

    private void startDetailAct(VPNGateConnection vpnGateConnection) {
        try {
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, vpnGateConnection);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private boolean checkAndShowAd(final VPNGateConnection vpnGateConnection) {
        if (dataUtil.hasAds()) {
            if (interstitialAd != null) {
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        startDetailAct(vpnGateConnection);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
                        // Called when fullscreen content failed to show.
                        startDetailAct(vpnGateConnection);
                    }
                });
                interstitialAd.show(mActivity);
                isShowedAd = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedBundle) {
        super.onCreate(savedBundle);
        try {
            dataUtil = App.getInstance().getDataUtil();
            vpnGateListAdapter = new VPNGateListAdapter(mContext);
            handler = new Handler();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = (MainActivity) getActivity();
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        lnSwipeRefresh = rootView.findViewById(R.id.ln_swipe_refresh);
        lnSwipeRefresh.setColorSchemeResources(R.color.colorAccent);
        lnSwipeRefresh.setOnRefreshListener(this);
        recyclerViewVPN = rootView.findViewById(R.id.rcv_connection);
        recyclerViewVPN.setAdapter(vpnGateListAdapter);
        recyclerViewVPN.setLayoutManager(new LinearLayoutManager(mContext));
        vpnGateListAdapter.setOnItemClickListener(this);
        vpnGateListAdapter.setOnItemLongClickListener(this);
        vpnGateListAdapter.setOnScrollListener(this);
        if (mActivity.getVpnGateConnectionList() != null) {
            vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList().advancedFilter());
        } else {
            vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        }
        btnToTop = rootView.findViewById(R.id.btn_to_top);
        btnToTop.setOnClickListener(this);
        txtEmpty = rootView.findViewById(R.id.txt_empty);
        return rootView;
    }

    public void advanceFilter(VPNGateConnectionList.Filter filter) {
        VPNGateConnectionList vpnGateConnectionList = mActivity.getVpnGateConnectionList().advancedFilter(filter);
        if (isSearching && !"".equals(mKeyword)) {
            vpnGateConnectionList = vpnGateConnectionList.filter(mKeyword);
        } else {
            vpnGateListAdapter.initialize(vpnGateConnectionList);
        }
        if (vpnGateConnectionList.size() == 0) {
            txtEmpty.setText(R.string.empty_filter_result);
            txtEmpty.setVisibility(View.VISIBLE);
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
        vpnGateListAdapter.initialize(vpnGateConnectionList);
    }

    /**
     * Search by keyword
     *
     * @param keyword search keyword
     */
    public void filter(String keyword) {
        stopTask();
        if (mActivity.getVpnGateConnectionList() == null) {
            return;
        }
        if (!"".equals(keyword)) {
            mKeyword = keyword;
            isSearching = true;
            VPNGateConnectionList filterResult = mActivity.getVpnGateConnectionList().filter(keyword);
            if (filterResult.size() == 0) {
                txtEmpty.setText(getString(R.string.empty_search_result, keyword));
                txtEmpty.setVisibility(View.VISIBLE);
                recyclerViewVPN.setVisibility(View.GONE);
            } else {
                txtEmpty.setVisibility(View.GONE);
                recyclerViewVPN.setVisibility(View.VISIBLE);
            }
            vpnGateListAdapter.initialize(filterResult);
        } else {
            recyclerViewVPN.setVisibility(View.VISIBLE);
            txtEmpty.setVisibility(View.GONE);
            vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList().advancedFilter());
        }
    }

    public void sort(String property, int type) {
        try {
            stopTask();
            if (mActivity.getVpnGateConnectionList() != null) {
                mActivity.getVpnGateConnectionList().sort(property, type);
                dataUtil.setConnectionsCache(mActivity.getVpnGateConnectionList());
                if (isSearching) {
                    VPNGateConnectionList filterResult = mActivity.getVpnGateConnectionList().filter(mKeyword);
                    vpnGateListAdapter.initialize(filterResult);
                } else {
                    closeSearch();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void stopTask() {
        lnSwipeRefresh.setEnabled(false);
        lnSwipeRefresh.setRefreshing(false);
        if (vpnGateTask != null && !vpnGateTask.isCancelled()) {
            vpnGateTask.stop();
        }
    }

    /**
     * Close search
     */
    public void closeSearch() {
        isSearching = false;
        txtEmpty.setVisibility(View.GONE);
        recyclerViewVPN.setVisibility(View.VISIBLE);
        if (mActivity.getVpnGateConnectionList() != null) {
            vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList().advancedFilter());
        } else {
            vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        }
        handler.postDelayed(() -> {
            lnSwipeRefresh.setEnabled(true);
            lnSwipeRefresh.setRefreshing(false);
        }, 300);

    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnToTop)) {
            recyclerViewVPN.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onItemClick(Object o, int position) {
        Bundle params = new Bundle();
        params.putString("ip", ((VPNGateConnection) o).getIp());
        params.putString("hostname", ((VPNGateConnection) o).getCalculateHostName());
        params.putString("country", ((VPNGateConnection) o).getCountryLong());
        FirebaseAnalytics.getInstance(mContext).logEvent("Select_Server", params);
        if (!checkAndShowAd((VPNGateConnection) o)) {
            startDetailAct((VPNGateConnection) o);
        }
    }

    @Override
    public void onItemLongClick(Object o, int position) {
        try {
            Bundle params = new Bundle();
            params.putString("ip", ((VPNGateConnection) o).getIp());
            params.putString("hostname", ((VPNGateConnection) o).getCalculateHostName());
            params.putString("country", ((VPNGateConnection) o).getCountryLong());
            FirebaseAnalytics.getInstance(mContext).logEvent("Long_Click_Server", params);
            CopyBottomSheetDialog dialog = CopyBottomSheetDialog.newInstance((VPNGateConnection) o);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !mActivity.isFinishing() && !mActivity.isDestroyed()) {
                assert getFragmentManager() != null;
                dialog.show(getFragmentManager(), CopyBottomSheetDialog.class.getName());
            } else if (!mActivity.isFinishing()) {
                assert getFragmentManager() != null;
                dialog.show(getFragmentManager(), CopyBottomSheetDialog.class.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onScrollUp() {
        btnToTop.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScrollDown() {
        btnToTop.setVisibility(View.GONE);
    }

    @Override
    public void onRefresh() {
        if (vpnGateTask != null && !vpnGateTask.isCancelled()) {
            vpnGateTask.stop();
        }
        vpnGateTask = new VPNGateTask();
        vpnGateTask.setRequestListener(this);
        vpnGateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onSuccess(Object o) {
        mActivity.setVpnGateConnectionList((VPNGateConnectionList) o);
        if (!"".equals(mActivity.getSortProperty())) {
            mActivity.getVpnGateConnectionList().sort(mActivity.getSortProperty(), mActivity.getSortType());
        }
        txtEmpty.setVisibility(View.GONE);
        recyclerViewVPN.setVisibility(View.VISIBLE);
        vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        dataUtil.setConnectionsCache(mActivity.getVpnGateConnectionList());
        lnSwipeRefresh.setRefreshing(false);
    }

    @Override
    public void onError(String error) {
        try {
            lnSwipeRefresh.setRefreshing(false);
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.onError(error);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
