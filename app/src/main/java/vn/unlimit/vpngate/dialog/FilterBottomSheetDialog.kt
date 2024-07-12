package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.utils.SpinnerInit
import vn.unlimit.vpngate.utils.SpinnerInit.OnItemSelectedIndexListener


class FilterBottomSheetDialog(filter: VPNGateConnectionList.Filter?) : BottomSheetDialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(filter: VPNGateConnectionList.Filter?): FilterBottomSheetDialog {
            return FilterBottomSheetDialog(filter)
        }
    }

    private var mFilter = filter ?: VPNGateConnectionList.Filter()

    var onButtonClickListener: OnButtonClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity())
        dialog.setOnShowListener { localDialog ->
            val d = localDialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
            BottomSheetBehavior.from<FrameLayout?>(bottomSheet).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

        //
        return dialog
    }

    private var checkBoxTCP: CheckBox? = null
    private var checkBoxUDP: CheckBox? = null
    private var checkBoxL2TP: CheckBox? = null
    private var checkBoxSSTP: CheckBox? = null
    private var spinnerPingOperator: AppCompatSpinner? = null
    private var spinnerSpeedOperator: AppCompatSpinner? = null
    private var spinnerSessionOperator: AppCompatSpinner? = null
    private var txtPing: EditText? = null
    private var txtSpeed: EditText? = null
    private var txtSession: EditText? = null
    private val applyButtonClickListener = View.OnClickListener {
        if (!checkBoxTCP!!.isChecked && !checkBoxUDP!!.isChecked && !checkBoxL2TP!!.isChecked && !checkBoxSSTP!!.isChecked) {
            Toast.makeText(
                context,
                resources.getString(R.string.must_check_at_least_1_protocol),
                Toast.LENGTH_SHORT
            ).show()
            return@OnClickListener
        }
        mFilter.isShowTCP = checkBoxTCP!!.isChecked
        mFilter.isShowUDP = checkBoxUDP!!.isChecked
        mFilter.isShowL2TP = checkBoxL2TP!!.isChecked
        mFilter.isShowSSTP = checkBoxSSTP!!.isChecked
        mFilter.ping = txtPing!!.text.toString().toIntOrNull()
        mFilter.speed = txtSpeed!!.text.toString().toIntOrNull()
        mFilter.sessionCount = txtSession!!.text.toString().toIntOrNull()
        onButtonClickListener!!.onButtonClick(mFilter)
        this.dismiss()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        val rootView = View.inflate(context, R.layout.layout_filter_dialog, null)
        rootView.findViewById<Button>(R.id.btn_reset).setOnClickListener {
            onButtonClickListener?.onButtonClick(null)
            this.dismiss()
        }
        checkBoxTCP = rootView.findViewById(R.id.chb_filter_tcp)
        checkBoxUDP = rootView.findViewById(R.id.chb_filter_udp)
        checkBoxL2TP = rootView.findViewById(R.id.chb_filter_l2tp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkBoxL2TP?.visibility = View.GONE
        }
        checkBoxSSTP = rootView.findViewById(R.id.chb_filter_sstp)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            checkBoxSSTP?.visibility = View.GONE
        }
        spinnerPingOperator = rootView.findViewById(R.id.spinner_ping_operator)
        spinnerSpeedOperator = rootView.findViewById(R.id.spinner_speed_operator)
        spinnerSessionOperator = rootView.findViewById(R.id.spinner_session_operator)
        txtPing = rootView.findViewById(R.id.txt_ping)
        txtSpeed = rootView.findViewById(R.id.txt_speed)
        txtSession = rootView.findViewById(R.id.txt_session)
        rootView.findViewById<Button>(R.id.btn_apply).setOnClickListener(applyButtonClickListener)
        bindData()
        dialog.setContentView(rootView)
    }

    private fun bindData() {
        val spinnerInitPing = SpinnerInit(context, spinnerPingOperator!!)
        val listOperator = resources.getStringArray(R.array.number_filter_operator)
        val spinnerInitSpeed = SpinnerInit(context, spinnerSpeedOperator!!)
        val spinnerInitSession = SpinnerInit(context, spinnerSessionOperator!!)
        val listOperatorEnum = VPNGateConnectionList.NumberFilterOperator.entries.toTypedArray()
        mFilter.isShowTCP.let { checkBoxTCP!!.isChecked = mFilter.isShowTCP }
        mFilter.isShowUDP.let { checkBoxUDP!!.isChecked = mFilter.isShowUDP }
        mFilter.isShowL2TP.let { checkBoxL2TP!!.isChecked = mFilter.isShowL2TP }
        mFilter.isShowSSTP.let { checkBoxSSTP!!.isChecked = mFilter.isShowSSTP }
        mFilter.ping?.let { txtPing!!.setText(it.toString()) }
        mFilter.speed?.let { txtSpeed!!.setText(it.toString()) }
        mFilter.sessionCount?.let { txtSession!!.setText(it.toString()) }
        spinnerInitPing.setStringArray(
            listOperator,
            listOperator[listOperatorEnum.indexOf(mFilter.pingFilterOperator)]
        )
        spinnerInitPing.onItemSelectedIndexListener = object : OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                mFilter.pingFilterOperator = listOperatorEnum[index]
            }

        }
        spinnerInitSpeed.setStringArray(
            listOperator,
            listOperator[listOperatorEnum.indexOf(mFilter.speedFilterOperator)]
        )
        spinnerInitSpeed.onItemSelectedIndexListener = object: OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                mFilter.speedFilterOperator = listOperatorEnum[index]
            }

        }
        spinnerInitSession.setStringArray(
            listOperator,
            listOperator[listOperatorEnum.indexOf(mFilter.sessionCountFilterOperator)]
        )
        spinnerInitSession.onItemSelectedIndexListener = object: OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                mFilter.sessionCountFilterOperator = listOperatorEnum[index]
            }
        }
    }

    interface OnButtonClickListener {
        fun onButtonClick(filter: VPNGateConnectionList.Filter?)
    }
}