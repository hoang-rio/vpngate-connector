package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.analytics.FirebaseAnalytics
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.activities.paid.ServerActivity
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.SessionAdapter
import vn.unlimit.vpngate.databinding.FragmentPaidServerHomeBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.models.ConnectedSession
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.SpinnerInit
import vn.unlimit.vpngate.viewmodels.ChartViewModel
import vn.unlimit.vpngate.viewmodels.SessionViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel


class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private lateinit var binding: FragmentPaidServerHomeBinding
    private val paidServerUtil = App.instance!!.paidServerUtil!!
    private var userViewModel: UserViewModel? = null
    private var paidServerActivity: PaidServerActivity? = null
    private var isObservedRefresh = false
    private var isAttached = false
    private var chartViewModel: ChartViewModel? = null
    private var sessionViewModel: SessionViewModel? = null
    private var sessionAdapter: SessionAdapter? = null

    companion object {
        const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaidServerHomeBinding.inflate(layoutInflater)
        if (paidServerUtil.getUserInfo() != null) {
            binding.textHome.text = getString(
                R.string.home_paid_welcome,
                paidServerUtil.getUserInfo()!!.username
            )
            binding.txtDataSize.text = OpenVPNService.humanReadableByteCount(
                paidServerUtil.getUserInfo()!!.dataSize!!,
                false,
                resources
            )
        }
        binding.lnSwipeRefresh.setOnRefreshListener(this)
        binding.lnBuyData.setOnClickListener(this)
        binding.lnPurchaseHistory.setOnClickListener(this)
        val chartTypes = resources.getStringArray(R.array.chart_type)
        val spinnerInit = SpinnerInit(requireContext(), binding.incDataChart.spinChartType)
        spinnerInit.setStringArray(chartTypes, chartTypes[0])
        spinnerInit.onItemSelectedIndexListener = object : SpinnerInit.OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                when (index) {
                    0 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.HOURLY
                    1 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.DAILY
                    2 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.MONTHLY
                }
                val params = Bundle()
                params.putString("username", userViewModel?.userInfo?.value?.username)
                params.putString("chart_type", chartViewModel?.chartType?.value.toString())
                FirebaseAnalytics.getInstance(requireContext())
                    .logEvent("user_change_chart_type", params)
            }

        }
        binding.incDataChart.lnChartError.setOnClickListener { chartViewModel?.getChartData() }
        binding.incSessionList.lnSessionError.setOnClickListener { sessionViewModel?.getListSession() }
        binding.incSessionList.rcvSession.layoutManager = LinearLayoutManager(requireContext())
        sessionAdapter = SessionAdapter(requireContext())
        sessionAdapter?.onDisconnectListener =
            object : OnItemClickListener {
                override fun onItemClick(o: Any?, position: Int) {
                    disConnectSession(o as ConnectedSession)
                }
            }
        sessionAdapter?.onOpenDetailServer =
            object : OnItemClickListener {
                override fun onItemClick(o: Any?, position: Int) {
                    openDetailServer()
                }
            }
        binding.incSessionList.rcvSession.adapter = sessionAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
        if (BuildConfig.DEBUG) {
            val btnLogout: View = view.findViewById(R.id.iv_logout)
            btnLogout.visibility = View.VISIBLE
            btnLogout.setOnClickListener {
                userViewModel?.logout(activity)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onStart() {
        super.onStart()
        if (userViewModel?.isProfileUpdate == true && paidServerUtil.getUserInfo() != null) {
            binding.textHome.text = getString(
                R.string.home_paid_welcome,
                paidServerUtil.getUserInfo()!!.fullname
            )
            binding.txtDataSize.text = OpenVPNService.humanReadableByteCount(
                paidServerUtil.getUserInfo()!!.dataSize!!,
                false,
                resources
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    private fun bindViewModel() {
        paidServerActivity = (activity as PaidServerActivity)
        this.userViewModel = paidServerActivity?.userViewModel
        userViewModel?.userInfo?.observe(viewLifecycleOwner) { userInfo ->
            run {
                if (userInfo != null && isAttached) {
                    binding.textHome.text =
                        getString(R.string.home_paid_welcome, userInfo.fullname)
                    binding.txtDataSize.text = OpenVPNService.humanReadableByteCount(
                        userInfo.dataSize!!,
                        false,
                        resources
                    )
                }
            }
        }
        chartViewModel = ViewModelProvider(this)[ChartViewModel::class.java]
        chartViewModel?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) binding.incDataChart.incLoadingChart.lnLoading.visibility = View.VISIBLE
        }
        chartViewModel?.chartData?.observe(viewLifecycleOwner) { chartData ->
            if (chartData.size > 0) {
                this.drawChart(chartData)
            }
        }
        chartViewModel?.chartType?.observe(viewLifecycleOwner) {
            chartViewModel?.getChartData()
        }
        chartViewModel?.isError?.observe(viewLifecycleOwner) { isError ->
            binding.incDataChart.lnChartError.visibility = if (isError) View.VISIBLE else View.GONE
        }
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        sessionViewModel?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            run {
                binding.incSessionList.lnLoadingSession.lnLoading.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
            }
        }
        sessionViewModel?.isError?.observe(viewLifecycleOwner) { isError ->
            binding.incSessionList.lnSessionError.visibility =
                if (isError) View.VISIBLE else View.GONE
        }
        sessionViewModel?.sessionList?.observe(viewLifecycleOwner) { sessionList ->
            sessionAdapter?.initialize(sessionList)
            binding.incSessionList.lnSessionEmpty.visibility =
                if (sessionList.size == 0) View.VISIBLE else View.GONE
        }
        chartViewModel?.getChartData()
        sessionViewModel?.getListSession()
    }

    private fun openDetailServer() {
        val intentServer = Intent(context, ServerActivity::class.java)
        val paidServer = paidServerUtil.getLastConnectServer()
        intentServer.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, paidServer)
        startActivity(intentServer)
    }

    private fun disConnectSession(connectedSession: ConnectedSession) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle(R.string.disconnect_session)
        alertDialogBuilder.setMessage(
            getString(
                R.string.alert_disconnect_confirm,
                connectedSession.sessionId,
                connectedSession.clientInfo?.ip,
                connectedSession.serverId?.serverName
            )
        )
        alertDialogBuilder.setPositiveButton(R.string.sure_btn) { dialog, _ ->
            dialog.dismiss()
            val loadingDialog = LoadingDialog.newInstance(getString(R.string.disconnecting_session))
            loadingDialog.show(parentFragmentManager, TAG)
            val params = Bundle()
            params.putString("username", userViewModel?.userInfo?.value?.username)
            params.putString("server_name", connectedSession.serverId?.serverName)
            params.putString("session_id", connectedSession.sessionId)
            sessionViewModel?.deleteSession(connectedSession._id, object : RequestListener {
                override fun onSuccess(result: Any?) {
                    // Disconnect success -> reload list
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.disconnect_success, connectedSession.sessionId),
                        Toast.LENGTH_LONG
                    ).show()
                    sessionViewModel?.getListSession()
                    FirebaseAnalytics.getInstance(requireContext())
                        .logEvent("user_disconnect_session_success", params)
                }

                override fun onError(error: String?) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.disconnect_failed, connectedSession.sessionId),
                        Toast.LENGTH_LONG
                    ).show()
                    FirebaseAnalytics.getInstance(requireContext())
                        .logEvent("user_disconnect_session_failure", params)
                }
            })
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel_btn) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialogBuilder.show()
    }

    class ChartValueFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return "%.1f MB".format(value)
        }

        override fun getFormattedValue(value: Float): String {
            return if (value > 0) "%.1f MB".format(value) else ""
        }
    }

    private fun drawChart(entries: ArrayList<Entry>) {
        val dataSet =
            LineDataSet(entries, getString(R.string.chart_transferred)) // add entries to dataset
        dataSet.setDrawFilled(true)
        dataSet.lineWidth = 2.5F
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.colorProgressPaid)
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.colorLink)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
        dataSet.fillAlpha = 150
        dataSet.valueFormatter = ChartValueFormatter()
        dataSet.setDrawCircles(false)
        val lineData = LineData(dataSet)
        binding.incDataChart.lineChart.data = lineData
        binding.incDataChart.lineChart.description!!.isEnabled = false
        binding.incDataChart.lineChart.legend!!.textColor =
            ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
        binding.incDataChart.lineChart.xAxis?.position = XAxis.XAxisPosition.BOTTOM
        binding.incDataChart.lineChart.axisLeft?.axisMinimum = 0F
        binding.incDataChart.lineChart.axisRight?.isEnabled = false
        binding.incDataChart.lineChart.xAxis?.valueFormatter = IndexAxisValueFormatter(chartViewModel!!.xLabels)
        binding.incDataChart.lineChart.xAxis?.textColor =
            ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
        binding.incDataChart.lineChart.axisLeft?.valueFormatter = ChartValueFormatter()
        binding.incDataChart.lineChart.axisLeft?.textColor =
            ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
        binding.incDataChart.lineChart.invalidate()
        binding.incDataChart.incLoadingChart.lnLoading.visibility = View.GONE
    }

    override fun onRefresh() {
        try {
            if (isAttached) {
                userViewModel?.fetchUser(true, paidServerActivity, true)
                chartViewModel?.getChartData()
                sessionViewModel?.getListSession()
            } else {
                binding.lnSwipeRefresh.isRefreshing = false
            }
            if (!isObservedRefresh) {
                userViewModel?.isLoading?.observe(paidServerActivity!!) {
                    binding.lnSwipeRefresh.isRefreshing = it
                }
                isObservedRefresh = true
            }
        } catch (th: Throwable) {
            Log.e(TAG, "OnRefresh error", th)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAttached) {
            userViewModel?.fetchUser(true, paidServerActivity)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.lnBuyData -> findNavController().navigate(R.id.navigation_buy_data)
            binding.lnPurchaseHistory -> findNavController().navigate(R.id.navigation_purchase_history)
        }
    }
}
