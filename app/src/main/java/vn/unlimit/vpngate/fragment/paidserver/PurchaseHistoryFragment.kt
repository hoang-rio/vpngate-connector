package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.viewmodels.PurchaseViewModel

class PurchaseHistoryFragment : Fragment(), View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    private var ivBack: ImageView? = null
    private var lnLoadingWrap: View? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var rcvPurchaseHistory: RecyclerView? = null
    private var purchaseViewModal: PurchaseViewModel? = null
    private var isListingPurchase = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_purchase_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivBack = view.findViewById(R.id.btn_back)
        ivBack?.setOnClickListener(this)
        lnLoadingWrap = view.findViewById(R.id.ln_loading_wrap)
        rcvPurchaseHistory = view.findViewById(R.id.rcv_purchase_history)
        swipeRefreshLayout = view.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        bindViewModel()
    }

    private fun bindViewModel() {
        purchaseViewModal = ViewModelProvider(this).get(PurchaseViewModel::class.java)
        purchaseViewModal?.isLoading?.observe(viewLifecycleOwner, { isLoading ->
            if (!isListingPurchase) {
                return@observe
            }
            if (!isLoading) {
                swipeRefreshLayout?.visibility = View.VISIBLE
                lnLoadingWrap?.visibility = View.GONE
                isListingPurchase = false
            } else {
                swipeRefreshLayout?.isRefreshing = false
            }
        })
        isListingPurchase = true
        purchaseViewModal?.listPurchase(true)
    }

    override fun onClick(view: View?) {
        when (view) {
            ivBack -> findNavController().popBackStack()
        }
    }

    override fun onRefresh() {
        purchaseViewModal?.listPurchase(false)
    }
}