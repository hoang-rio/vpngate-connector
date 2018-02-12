package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.DetailActivity;
import vn.unlimit.vpngate.MainActivity;
import vn.unlimit.vpngate.R;
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
import vn.unlimit.vpngate.ultils.DataUtil;

/**
 * Created by hoangnd on 1/30/2018.
 */

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, RequestListener, View.OnClickListener, OnItemClickListener, OnItemLongClickListener, OnScrollListener {
    final String TAG = "HOME";
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
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedBundle) {
        super.onCreate(savedBundle);
        try {
            dataUtil = ((App) getActivity().getApplication()).getDataUtil();
            vpnGateListAdapter = new VPNGateListAdapter(mContext, dataUtil);
            handler = new Handler();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
        vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        btnToTop = rootView.findViewById(R.id.btn_to_top);
        btnToTop.setOnClickListener(this);
        return rootView;
    }

    /**
     * Search by keyword
     *
     * @param keyword
     */
    public void filter(String keyword) {
        stopTask();
        if (!"".equals(keyword)) {
            mKeyword = keyword;
            isSearching = true;
            VPNGateConnectionList filterResult = mActivity.getVpnGateConnectionList().filter(keyword);
            vpnGateListAdapter.initialize(filterResult);
        }
    }

    public void sort(String property, int type) {
        stopTask();
        mActivity.getVpnGateConnectionList().sort(property, type);
        dataUtil.setConnectionsCache(mActivity.getVpnGateConnectionList());
        if (isSearching) {
            VPNGateConnectionList filterResult = mActivity.getVpnGateConnectionList().filter(mKeyword);
            vpnGateListAdapter.initialize(filterResult);
        } else {
            closeSearch();
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
        vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lnSwipeRefresh.setEnabled(true);
                lnSwipeRefresh.setRefreshing(false);
            }
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
        Answers.getInstance().logCustom(new CustomEvent("Select server")
                .putCustomAttribute("ip", ((VPNGateConnection) o).getIp())
                .putCustomAttribute("country", ((VPNGateConnection) o).getCountryLong()));
        Intent intent = new Intent(getContext(), DetailActivity.class);
        intent.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, (VPNGateConnection) o);
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(Object o, int position) {
        try {
            Answers.getInstance().logCustom(new CustomEvent("Long click server")
                    .putCustomAttribute("ip", ((VPNGateConnection) o).getIp())
                    .putCustomAttribute("country", ((VPNGateConnection) o).getCountryLong()));
            CopyBottomSheetDialog dialog = CopyBottomSheetDialog.newInstance((VPNGateConnection) o);
            dialog.show(getFragmentManager(), CopyBottomSheetDialog.class.getName());
        } catch (Exception e) {
            e.printStackTrace();
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
        vpnGateListAdapter.initialize(mActivity.getVpnGateConnectionList());
        dataUtil.setConnectionsCache(mActivity.getVpnGateConnectionList());
        lnSwipeRefresh.setRefreshing(false);
    }

    @Override
    public void onError(String error) {
        try {
            lnSwipeRefresh.setRefreshing(false);
            ((MainActivity) getActivity()).onError(error);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
