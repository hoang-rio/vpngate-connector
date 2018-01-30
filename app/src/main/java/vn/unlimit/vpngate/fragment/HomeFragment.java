package vn.unlimit.vpngate.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import vn.unlimit.vpngate.MainActivity;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.adapter.OnItemClickListener;
import vn.unlimit.vpngate.adapter.OnItemLongClickListener;
import vn.unlimit.vpngate.adapter.OnScrollListener;
import vn.unlimit.vpngate.adapter.VPNGateListAdapter;
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
    private VPNGateConnectionList vpnGateConnectionList;
    private VPNGateListAdapter vpnGateListAdapter;
    private VPNGateTask vpnGateTask;
    private DataUtil dataUtil;
    private View btnToTop;

    public HomeFragment() {

    }

    public static HomeFragment newInstance(VPNGateConnectionList _vpnGateConnectionList) {
        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putParcelable(BaseProvider.PASS_VPN_CONNECTION_LIST, _vpnGateConnectionList);
        homeFragment.setArguments(args);
        return homeFragment;
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
            vpnGateConnectionList = getArguments().getParcelable(BaseProvider.PASS_VPN_CONNECTION_LIST);
            vpnGateListAdapter = new VPNGateListAdapter(mContext);
            dataUtil = ((MainActivity) getActivity()).getDataUtil();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        vpnGateListAdapter.initialize(vpnGateConnectionList);
        btnToTop = rootView.findViewById(R.id.btn_to_top);
        btnToTop.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnToTop)) {
            recyclerViewVPN.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onItemClick(Object o, int position) {
        Toast.makeText(mContext, "Selected item at position: " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(Object o, int position) {
        Toast.makeText(mContext, "Long click item at position: " + position, Toast.LENGTH_SHORT).show();
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
        vpnGateConnectionList = (VPNGateConnectionList) o;
        vpnGateListAdapter.initialize(vpnGateConnectionList);
        dataUtil.setConnectionsCache(vpnGateConnectionList);
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
