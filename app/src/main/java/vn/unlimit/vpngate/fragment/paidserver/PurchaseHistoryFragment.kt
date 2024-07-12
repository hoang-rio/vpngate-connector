package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import vn.unlimit.vpngate.adapter.OnScrollListener
import vn.unlimit.vpngate.adapter.PurchaseHistoryAdapter
import vn.unlimit.vpngate.databinding.FragmentPurchaseHistoryBinding
import vn.unlimit.vpngate.viewmodels.PurchaseViewModel

class PurchaseHistoryFragment : Fragment(), View.OnClickListener,
    SwipeRefreshLayout.OnRefreshListener, OnScrollListener {
    private lateinit var binding: FragmentPurchaseHistoryBinding
    private var purchaseViewModal: PurchaseViewModel? = null
    private var isInitListingPurchase = false
    private var purchaseHistoryAdapter: PurchaseHistoryAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPurchaseHistoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener(this)
        binding.rcvPurchaseHistory.layoutManager = LinearLayoutManager(requireContext())
        purchaseHistoryAdapter = PurchaseHistoryAdapter(requireContext())
        purchaseHistoryAdapter?.onScrollListener = this
        binding.rcvPurchaseHistory.adapter = purchaseHistoryAdapter
        binding.lnSwipeRefresh.setOnRefreshListener(this)
        bindViewModel()
    }

    private fun bindViewModel() {
        purchaseViewModal = ViewModelProvider(this)[PurchaseViewModel::class.java]
        purchaseViewModal?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            if (!isInitListingPurchase) {
                return@observe
            }
            if (!isLoading) {
                binding.incLoading.lnLoading.visibility = View.GONE
                isInitListingPurchase = false
            } else {
                binding.lnSwipeRefresh.isRefreshing = false
            }
        }
        purchaseViewModal?.purchaseList?.observe(viewLifecycleOwner) { purchaseList ->
            binding.progressLoadMore.visibility = View.GONE
            if (purchaseList.size > 0) {
                binding.lnNoPurchase.visibility = View.GONE
                binding.lnSwipeRefresh.visibility = View.VISIBLE
                purchaseHistoryAdapter?.initialize(purchaseList)
            } else {
                binding.lnSwipeRefresh.visibility = View.GONE
                binding.lnNoPurchase.visibility = View.VISIBLE
            }
        }
        isInitListingPurchase = true
        purchaseViewModal?.listPurchase(true)
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.btnBack -> findNavController().popBackStack()
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
            binding.progressLoadMore.visibility = View.VISIBLE
            purchaseViewModal?.listPurchase()
        }
    }
}