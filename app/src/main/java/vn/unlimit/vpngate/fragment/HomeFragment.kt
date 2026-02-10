package vn.unlimit.vpngate.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.OnItemLongClickListener
import vn.unlimit.vpngate.adapter.OnScrollListener
import vn.unlimit.vpngate.adapter.VPNGateListAdapter
import vn.unlimit.vpngate.databinding.FragmentHomeBinding
import vn.unlimit.vpngate.dialog.CopyBottomSheetDialog
import vn.unlimit.vpngate.dialog.CopyBottomSheetDialog.Companion.newInstance
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.viewmodels.ConnectionListViewModel

/**
 * Created by hoangnd on 1/30/2018.
 */
class HomeFragment : Fragment(), OnRefreshListener, View.OnClickListener, OnItemClickListener,
    OnItemLongClickListener, OnScrollListener {
    companion object {
        private const val TAG = "HOME_FREE"
    }

    private var mContext: Context? = null
    private var vpnGateListAdapter: VPNGateListAdapter? = null
    private var connectionListViewModel: ConnectionListViewModel? = null
    private var dataUtil: DataUtil? = null
    private var isSearching = false
    private var mKeyword = ""
    private var handler: Handler? = null
    private var mActivity: MainActivity? = null
    private var interstitialAd: InterstitialAd? = null

    //Flag ads is showed need request new ad
    private var isShowedAd = true
    private lateinit var binding: FragmentHomeBinding

    override fun onResume() {
        super.onResume()
        if (dataUtil!!.hasAds()) {
            if (interstitialAd == null || isShowedAd) {
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    mContext!!,
                    getString(R.string.admob_full_screen_detail),
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            this@HomeFragment.interstitialAd = interstitialAd
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            this@HomeFragment.interstitialAd = null
                            Log.e(TAG, loadAdError.toString())
                        }
                    })
                isShowedAd = false
            }
        }
    }

    private fun startDetailAct(vpnGateConnection: VPNGateConnection?) {
        try {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, vpnGateConnection)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message, e)
        }
    }

    private fun checkAndShowAd(vpnGateConnection: VPNGateConnection?): Boolean {
        if (dataUtil!!.hasAds()) {
            if (interstitialAd != null) {
                interstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        startDetailAct(vpnGateConnection)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        // Called when fullscreen content failed to show.
                        startDetailAct(vpnGateConnection)
                    }
                }
                interstitialAd!!.show(mActivity!!)
                isShowedAd = true
                return true
            }
        }
        return false
    }

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreate(savedBundle: Bundle?) {
        super.onCreate(savedBundle)
        try {
            dataUtil = instance!!.dataUtil
            vpnGateListAdapter = VPNGateListAdapter(mContext!!)
            val showNativeAd = dataUtil!!.hasAds() && 
                FirebaseRemoteConfig.getInstance().getBoolean(getString(R.string.cfg_show_native_ad))
            vpnGateListAdapter!!.setHasAds(showNativeAd)
            vpnGateListAdapter!!.setAdUnitId(getString(R.string.admob_native_unit_id))
            handler = Handler(Looper.getMainLooper())
            connectionListViewModel = ViewModelProvider(this)[ConnectionListViewModel::class.java]
            connectionListViewModel!!.isLoading.observe(this) { isLoading: Boolean? ->
                if (!isLoading!! && connectionListViewModel!!.vpnGateConnectionList.value != null) {
                    onAPISuccess(connectionListViewModel!!.vpnGateConnectionList.value)
                }
            }
            connectionListViewModel!!.isError.observe(this) { isError: Boolean ->
                if (isError) {
                    onError("")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message, e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mActivity = activity as MainActivity?
        binding = FragmentHomeBinding.inflate(layoutInflater)
        binding.lnSwipeRefresh.setColorSchemeResources(R.color.colorAccent)
        binding.lnSwipeRefresh.setOnRefreshListener(this)
        binding.rcvConnection.setAdapter(vpnGateListAdapter)
        binding.rcvConnection.setLayoutManager(LinearLayoutManager(mContext))
        vpnGateListAdapter!!.setOnItemClickListener(this)
        vpnGateListAdapter!!.setOnItemLongClickListener(this)
        vpnGateListAdapter!!.setOnScrollListener(this)
        binding.btnToTop.setOnClickListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch(Dispatchers.IO) {
            if ("" != mActivity!!.sortProperty) {
                mActivity!!.vpnGateConnectionList?.sort(
                    mActivity!!.sortProperty,
                    mActivity!!.sortType
                )
            } else {
                mActivity!!.vpnGateConnectionList?.advancedFilter()
            }
            withContext(Dispatchers.Main) {
                vpnGateListAdapter!!.initialize(mActivity!!.vpnGateConnectionList)
            }
        }
    }

    fun advanceFilter(filter: VPNGateConnectionList.Filter?) {
        lifecycleScope.launch(Dispatchers.IO) {
            var vpnGateConnectionList = mActivity!!.vpnGateConnectionList!!.advancedFilter(filter)
            if (isSearching && "" != mKeyword) {
                vpnGateConnectionList = vpnGateConnectionList.filter(mKeyword)
            }
            withContext(Dispatchers.Main) {
                if (vpnGateConnectionList.size() == 0) {
                    binding.txtEmpty.setText(R.string.empty_filter_result)
                    binding.txtEmpty.visibility = View.VISIBLE
                } else {
                    binding.txtEmpty.visibility = View.GONE
                }
                vpnGateListAdapter!!.initialize(vpnGateConnectionList)
            }
        }
    }

    /**
     * Search by keyword
     *
     * @param keyword search keyword
     */
    fun filter(keyword: String) {
        stopTask()
        if (mActivity!!.vpnGateConnectionList == null) {
            return
        }
        if ("" != keyword) {
            mKeyword = keyword
            isSearching = true
            lifecycleScope.launch(Dispatchers.IO) {
                val filterResult = mActivity!!.vpnGateConnectionList!!.filter(keyword)
                withContext(Dispatchers.Main) {
                    if (filterResult.size() == 0) {
                        binding.txtEmpty.text = getString(R.string.empty_search_result, keyword)
                        binding.txtEmpty.visibility = View.VISIBLE
                        binding.rcvConnection.visibility = View.GONE
                    } else {
                        binding.txtEmpty.visibility = View.GONE
                        binding.rcvConnection.visibility = View.VISIBLE
                    }
                    vpnGateListAdapter!!.initialize(filterResult)
                }
            }
        } else {
            binding.rcvConnection.visibility = View.VISIBLE
            binding.txtEmpty.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.IO) {
                val vpnGateConnectionList = mActivity!!.vpnGateConnectionList!!.advancedFilter()
                withContext(Dispatchers.Main) {
                    vpnGateListAdapter!!.initialize(vpnGateConnectionList)
                }
            }
        }
    }

    fun sort(property: String?, type: Int) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                if (mActivity!!.vpnGateConnectionList != null) {
                    mActivity!!.vpnGateConnectionList!!.sort(property, type)
                    if (isSearching) {
                        val filterResult = mActivity!!.vpnGateConnectionList!!.filter(mKeyword)
                        withContext(Dispatchers.Main) {
                            vpnGateListAdapter!!.initialize(filterResult)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            vpnGateListAdapter!!.initialize(mActivity!!.vpnGateConnectionList)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message, e)
        }
    }

    private fun stopTask() {
        binding.lnSwipeRefresh.isEnabled = false
        binding.lnSwipeRefresh.isRefreshing = false
    }

    /**
     * Close search
     */
    fun closeSearch() {
        isSearching = false
        binding.txtEmpty.visibility = View.GONE
        binding.rcvConnection.visibility = View.VISIBLE
        if (mActivity!!.vpnGateConnectionList != null) {
            mActivity!!.vpnGateConnectionList!!.mKeyword = null
            lifecycleScope.launch(Dispatchers.IO) {
                val vpnGateConnectionList = mActivity!!.vpnGateConnectionList!!.advancedFilter()
                withContext(Dispatchers.Main) {
                    vpnGateListAdapter!!.initialize(vpnGateConnectionList)
                }
            }
        } else {
            vpnGateListAdapter!!.initialize(mActivity!!.vpnGateConnectionList)
        }
        handler!!.postDelayed({
            binding.lnSwipeRefresh.isEnabled = true
            binding.lnSwipeRefresh.isRefreshing = false
        }, 300)
    }

    override fun onClick(view: View) {
        if (view == binding.btnToTop) {
            binding.rcvConnection.smoothScrollToPosition(0)
        }
    }

    override fun onItemClick(o: Any?, position: Int) {
        val params = Bundle()
        params.putString("ip", (o as VPNGateConnection?)!!.ip)
        params.putString("hostname", o!!.calculateHostName)
        params.putString("country", o.countryLong)
        FirebaseAnalytics.getInstance(mContext!!).logEvent("Select_Server", params)
        if (!checkAndShowAd(o)) {
            startDetailAct(o)
        }
    }

    override fun onItemLongClick(o: Any?, position: Int) {
        try {
            val params = Bundle()
            params.putString("ip", (o as VPNGateConnection?)!!.ip)
            params.putString("hostname", o!!.calculateHostName)
            params.putString("country", o.countryLong)
            FirebaseAnalytics.getInstance(mContext!!).logEvent("Long_Click_Server", params)
            val dialog = newInstance(o)
            if (!mActivity!!.isFinishing && !mActivity!!.isDestroyed) {
                dialog.show(parentFragmentManager, CopyBottomSheetDialog::class.java.name)
            } else if (!mActivity!!.isFinishing) {
                dialog.show(parentFragmentManager, CopyBottomSheetDialog::class.java.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message, e)
        }
    }

    override fun onScrollUp() {
        binding.btnToTop.visibility = View.VISIBLE
    }

    override fun onScrollDown() {
        binding.btnToTop.visibility = View.GONE
    }

    override fun onRefresh() {
        connectionListViewModel!!.getAPIData()
    }

    private fun onAPISuccess(o: Any?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val vpnGateConnectionList = o as VPNGateConnectionList?
            if ("" != mActivity!!.sortProperty) {
                vpnGateConnectionList?.sort(
                    mActivity!!.sortProperty,
                    mActivity!!.sortType
                )
            }
            withContext(Dispatchers.Main) {
                mActivity!!.vpnGateConnectionList = vpnGateConnectionList
                binding.txtEmpty.visibility = View.GONE
                binding.rcvConnection.visibility = View.VISIBLE
                vpnGateListAdapter!!.initialize(vpnGateConnectionList)
                binding.lnSwipeRefresh.isRefreshing = false
            }
        }
    }

    fun onError(error: String?) {
        try {
            binding.lnSwipeRefresh.isRefreshing = false
            val mainActivity = activity as MainActivity?
            mainActivity?.onError(error)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message, e)
        }
    }
}
