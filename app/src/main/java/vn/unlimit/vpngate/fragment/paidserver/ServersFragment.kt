package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.adapter.PaidServerAdapter
import vn.unlimit.vpngate.viewmodels.ServerViewModel

class ServersFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var recyclerView: RecyclerView? = null
    private var mContext: Context? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var serverViewModel: ServerViewModel? = null
    private var paidServerAdapter: PaidServerAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_server_list, container, false)
        recyclerView = root.findViewById(R.id.rcv_list_server)
        recyclerView!!.layoutManager = LinearLayoutManager(mContext)
        paidServerAdapter = PaidServerAdapter((mContext))
        recyclerView!!.adapter = paidServerAdapter
        swipeRefreshLayout = root.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        return root
    }

    private fun bindViewModel() {
        serverViewModel?.isLoading?.observe(this, {isLoading ->
            swipeRefreshLayout?.isRefreshing = isLoading
        })
        serverViewModel?.serverList?.observe(this, {listServer ->
            paidServerAdapter?.initialize(listServer)
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        this.serverViewModel = (activity as PaidServerActivity).serverViewModel
        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
        serverViewModel?.loadServer(activity as PaidServerActivity, true)
    }

    override fun onRefresh() {
        serverViewModel?.loadServer(activity as PaidServerActivity, true)
    }
}
