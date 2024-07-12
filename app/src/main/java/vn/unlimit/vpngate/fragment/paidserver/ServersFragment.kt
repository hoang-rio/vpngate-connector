package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.activities.paid.ServerActivity
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.OnScrollListener
import vn.unlimit.vpngate.adapter.PaidServerAdapter
import vn.unlimit.vpngate.databinding.FragmentServerListBinding
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.viewmodels.ServerViewModel

class ServersFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, OnItemClickListener,
    OnScrollListener {

    private lateinit var binding: FragmentServerListBinding
    private var mContext: Context? = null
    private var serverViewModel: ServerViewModel? = null
    private var paidServerAdapter: PaidServerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentServerListBinding.inflate(layoutInflater)
        binding.rcvListServer.layoutManager = LinearLayoutManager(mContext)
        paidServerAdapter = PaidServerAdapter((mContext))
        paidServerAdapter!!.setOnItemClickListener(this)
        paidServerAdapter!!.setOnScrollListener(this)
        binding.rcvListServer.adapter = paidServerAdapter
        binding.lnSwipeRefresh.setOnRefreshListener(this)
        return binding.root
    }

    private fun bindViewModel() {
        serverViewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        serverViewModel?.isLoggedIn?.observe(viewLifecycleOwner) { isLoggedIn ->
            if (!isLoggedIn!!) {
                // Go to login screen if user login status is changed
                val intentLogin = Intent(activity, LoginActivity::class.java)
                startActivity(intentLogin)
                activity?.finish()
            }
        }
        serverViewModel?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            if (serverViewModel?.serverList?.value.isNullOrEmpty()) {
                binding.lnLoadingWrap.visibility = View.VISIBLE
                binding.lnSwipeRefresh.visibility = View.GONE
                return@observe
            }
            binding.lnSwipeRefresh.visibility = View.VISIBLE
            binding.lnLoadingWrap.visibility = View.GONE
            binding.lnSwipeRefresh.isRefreshing = isLoading
        }
        serverViewModel?.serverList?.observe(viewLifecycleOwner) { listServer ->
            paidServerAdapter?.initialize(listServer)
        }
        serverViewModel?.loadServer(activity as PaidServerActivity, true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    override fun onRefresh() {
        serverViewModel?.loadServer(activity as PaidServerActivity, true)
    }

    override fun onItemClick(o: Any?, position: Int) {
        val selectedServer: PaidServer = (o as PaidServer)
        val intentServer = Intent(context, ServerActivity::class.java)
        intentServer.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, selectedServer)
        startActivity(intentServer)
    }

    override fun onScrollDown() {
        serverViewModel?.loadServer(activity as PaidServerActivity)
    }

    override fun onScrollUp() {

    }
}
