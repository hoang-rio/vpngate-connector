package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.adapter.OnScrollListener
import vn.unlimit.vpngate.adapter.PurchaseHistoryAdapter
import vn.unlimit.vpngate.viewmodels.PurchaseViewModel

class PurchaseHistoryFragment : Fragment(), View.OnClickListener, SwipeRefreshLayout.OnRefreshListener, OnScrollListener {
    private var ivBack: ImageView? = null
    private var lnLoadingWrap: View? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var rcvPurchaseHistory: RecyclerView? = null
    private var purchaseViewModal: PurchaseViewModel? = null
    private var isInitListingPurchase = false
    private var purchaseHistoryAdapter: PurchaseHistoryAdapter? = null
    private var progressLoadMore: ProgressBar? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_purchase_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivBack = view.findViewById(R.id.btn_back)
        ivBack?.setOnClickListener(this)
        lnLoadingWrap = view.findViewById(R.id.ln_loading_wrap)
        rcvPurchaseHistory = view.findViewById(R.id.rcv_purchase_history)
        rcvPurchaseHistory?.layoutManager = LinearLayoutManager(requireContext())
        purchaseHistoryAdapter = PurchaseHistoryAdapter(requireContext())
        purchaseHistoryAdapter?.onScrollListener = this
        rcvPurchaseHistory?.adapter = purchaseHistoryAdapter
        swipeRefreshLayout = view.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        progressLoadMore = view.findViewById(R.id.progress_load_more)
        bindViewModel()
    }

    private fun bindViewModel() {
        purchaseViewModal = ViewModelProvider(this).get(PurchaseViewModel::class.java)
        purchaseViewModal?.isLoading?.observe(viewLifecycleOwner, { isLoading ->
            if (!isInitListingPurchase) {
                return@observe
            }
            if (!isLoading) {
                swipeRefreshLayout?.visibility = View.VISIBLE
                lnLoadingWrap?.visibility = View.GONE
                isInitListingPurchase = false
            } else {
                swipeRefreshLayout?.isRefreshing = false
            }
        })
        purchaseViewModal?.purchaseList?.observe(viewLifecycleOwner, { purchaseList ->
            progressLoadMore?.visibility = View.GONE
            purchaseHistoryAdapter?.initialize(purchaseList)
        })
        isInitListingPurchase = true
        purchaseViewModal?.listPurchase(true)
    }

    override fun onClick(view: View?) {
        when (view) {
            ivBack -> findNavController().popBackStack()
        }
    }

    override fun onRefresh() {
        isInitListingPurchase = true
        purchaseViewModal?.listPurchase(true)
    }

    override fun onScrollUp() {
        // Do nothing  here
    }

    override fun onScrollDown() {
        // Scroll to load more data
        if (!purchaseViewModal!!.isOutOfData) {
            progressLoadMore?.visibility = View.VISIBLE
            purchaseViewModal?.listPurchase()
        }
    }
}