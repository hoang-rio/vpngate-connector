package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
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
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.SessionAdapter
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.models.ConnectedSession
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.SpinnerInit
import vn.unlimit.vpngate.viewmodels.ChartViewModel
import vn.unlimit.vpngate.viewmodels.SessionViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel


class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var txtWelcome: TextView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var userViewModel: UserViewModel? = null
    private var paidServerActivity: PaidServerActivity? = null
    private var txtDataSize: TextView? = null
    private var lnBuyData: View? = null
    private var lnPurchaseHistory: View? = null
    private var isObservedRefresh = false
    private var isAttached = false
    private var chartViewModel: ChartViewModel? = null
    private var lnLoadingChart: View? = null
    private var lnLoadingSession: View? = null
    private var lineChart: LineChart? = null
    private var spinnerChartType: AppCompatSpinner? = null
    private var lnChartError: View? = null
    private var sessionViewModel: SessionViewModel? = null
    private var lnSessionError: View? = null
    private var rcvSession: RecyclerView? = null
    private var lnSessionEmtpy: View? = null
    private var sessionAdapter: SessionAdapter? = null

    companion object {
        const val TAG = "HomeFragment"
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_paid_server_home, container, false)
        txtWelcome = root.findViewById(R.id.text_home)
        txtDataSize = root.findViewById(R.id.txt_data_size)
        if (paidServerUtil.getUserInfo() != null) {
            txtWelcome!!.text = getString(R.string.home_paid_welcome, paidServerUtil.getUserInfo()!!.getString("fullname"))
            txtDataSize!!.text = paidServerUtil.getUserInfo()!!.getInt("dataSize").toString()
        }
        swipeRefreshLayout = root.findViewById(R.id.ln_swipe_refresh)
        swipeRefreshLayout?.setOnRefreshListener(this)
        lnBuyData = root.findViewById(R.id.ln_buy_data)
        lnBuyData?.setOnClickListener(this)
        lnPurchaseHistory = root.findViewById(R.id.ln_purchase_history)
        lnPurchaseHistory?.setOnClickListener(this)
        lnLoadingChart = root.findViewById(R.id.ln_loading_chart)
        lnLoadingSession = root.findViewById(R.id.ln_loading_session)
        lineChart = root.findViewById(R.id.line_chart)
        spinnerChartType = root.findViewById(R.id.spin_chart_type)
        val chartTypes = resources.getStringArray(R.array.chart_type)
        val spinnerInit = SpinnerInit(requireContext(), spinnerChartType)
        spinnerInit.setStringArray(chartTypes, chartTypes[0])
        spinnerInit.setOnItemSelectedIndexListener { _, index ->
            when (index) {
                0 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.HOURLY
                1 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.DAILY
                2 -> chartViewModel?.chartType?.value = ChartViewModel.ChartType.MONTHLY
            }
            val params = Bundle()
            params.putString("username", userViewModel?.userInfo?.value?.getString("username"))
            params.putString("chart_type", chartViewModel?.chartType?.value.toString())
            FirebaseAnalytics.getInstance(requireContext())
                .logEvent("user_change_chart_type", params)
        }
        lnChartError = root.findViewById(R.id.ln_chart_error)
        lnChartError?.setOnClickListener { chartViewModel?.getChartData() }
        lnSessionError = root.findViewById(R.id.ln_session_error)
        lnSessionError?.setOnClickListener { sessionViewModel?.getListSession() }
        rcvSession = root.findViewById(R.id.rcv_session)
        rcvSession?.layoutManager = LinearLayoutManager(requireContext())
        sessionAdapter = SessionAdapter(requireContext())
        sessionAdapter?.onDisconnectListener = OnItemClickListener { o, _ -> disConnectSession(o as ConnectedSession) }
        rcvSession?.adapter = sessionAdapter
        lnSessionEmtpy = root.findViewById(R.id.ln_session_empty)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    private fun bindViewModel() {
        paidServerActivity = (activity as PaidServerActivity)
        this.userViewModel = paidServerActivity?.userViewModel
        userViewModel?.userInfo?.observe(viewLifecycleOwner, { userInfo ->
            run {
                if (isAttached) {
                    txtWelcome!!.text = getString(R.string.home_paid_welcome, userInfo?.getString("fullname"))
                    txtDataSize!!.text = OpenVPNService.humanReadableByteCount(userInfo!!.getLong("dataSize"), false, resources)
                }
            }
        })
        chartViewModel = ViewModelProvider(this).get(ChartViewModel::class.java)
        chartViewModel?.isLoading?.observe(viewLifecycleOwner, { isLoading -> if (isLoading) lnLoadingChart!!.visibility = View.VISIBLE })
        chartViewModel?.chartData?.observe(viewLifecycleOwner, { chartData ->
            if (chartData.size > 0) {
                this.drawChart(chartData)
            }
        })
        chartViewModel?.chartType?.observe(viewLifecycleOwner, {
            chartViewModel?.getChartData()
        })
        chartViewModel?.isError?.observe(viewLifecycleOwner, { isError ->
            lnChartError?.visibility = if (isError) View.VISIBLE else View.GONE
        })
        sessionViewModel = ViewModelProvider(this).get(SessionViewModel::class.java)
        sessionViewModel?.isLoading?.observe(viewLifecycleOwner, { isLoading ->
            run {
                lnLoadingSession?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        })
        sessionViewModel?.isError?.observe(viewLifecycleOwner, { isError -> lnSessionError?.visibility = if (isError) View.VISIBLE else View.GONE })
        sessionViewModel?.sessionList?.observe(viewLifecycleOwner, { sessionList ->
            sessionAdapter?.initialize(sessionList)
            lnSessionEmtpy?.visibility = if (sessionList.size == 0) View.VISIBLE else View.GONE
        })
        chartViewModel?.getChartData()
        sessionViewModel?.getListSession()
    }

    private fun disConnectSession(connectedSession: ConnectedSession) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle(R.string.disconnect_session)
        alertDialogBuilder.setMessage(getString(R.string.alert_disconnect_confirm, connectedSession.sessionId, connectedSession.clientInfo?.ip, connectedSession.serverId?.serverName))
        alertDialogBuilder.setPositiveButton(R.string.sure_btn) { dialog, _ ->
            dialog.dismiss()
            val loadingDialog = LoadingDialog.newInstance(getString(R.string.disconnecting_session))
            loadingDialog.show(parentFragmentManager, TAG)
            val params = Bundle()
            params.putString("username", userViewModel?.userInfo?.value?.getString("username"))
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
        val dataSet = LineDataSet(entries, getString(R.string.chart_transferred)) // add entries to dataset
        dataSet.setDrawFilled(true)
        dataSet.lineWidth = 2.5F
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.colorProgressPaid)
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.colorLink)
        dataSet.fillAlpha = 150
        dataSet.valueFormatter = ChartValueFormatter()
        dataSet.setDrawCircles(false)
        val lineData = LineData(dataSet)
        lineChart?.data = lineData
        lineChart?.description!!.isEnabled = false
        lineChart?.xAxis?.position = XAxis.XAxisPosition.BOTTOM
        lineChart?.axisLeft?.axisMinimum = 0F
        lineChart?.axisRight?.isEnabled = false
        lineChart?.xAxis?.valueFormatter = IndexAxisValueFormatter(chartViewModel!!.xLabels)
        lineChart?.axisLeft?.valueFormatter = ChartValueFormatter()
        lineChart?.invalidate()
        lnLoadingChart?.visibility = View.GONE
    }

    override fun onRefresh() {
        try {
            if (isAttached) {
                userViewModel?.fetchUser(true, paidServerActivity, true)
                chartViewModel?.getChartData()
                sessionViewModel?.getListSession()
            } else {
                swipeRefreshLayout?.isRefreshing = false
            }
            if (!isObservedRefresh) {
                userViewModel?.isLoading?.observe(paidServerActivity!!, {
                    swipeRefreshLayout?.isRefreshing = it
                })
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
            lnBuyData -> findNavController().navigate(R.id.navigation_buy_data)
            lnPurchaseHistory -> findNavController().navigate(R.id.navigation_purchase_history)
        }
    }
}
