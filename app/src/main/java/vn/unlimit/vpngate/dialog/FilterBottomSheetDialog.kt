package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.utils.SpinnerInit


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
    private var spinnerPingOperator: AppCompatSpinner? = null
    private var spinnerSpeedOperator: AppCompatSpinner? = null
    private var spinnerSessionOperator: AppCompatSpinner? = null
    private var txtPing: EditText? = null
    private var txtSpeed: EditText? = null
    private var txtSession: EditText? = null
    private val applyButtonClickListener = View.OnClickListener {
        if (!checkBoxTCP!!.isChecked && !checkBoxUDP!!.isChecked && !checkBoxL2TP!!.isChecked) {
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
        mFilter.ping = txtPing!!.text.toString().toIntOrNull()
        mFilter.speed = txtSpeed!!.text.toString().toIntOrNull()
        mFilter.sessionCount = txtSession!!.text.toString().toIntOrNull()
        onButtonClickListener!!.onButtonClick(mFilter)
        this.dismiss()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        val rootView = View.inflate(context, R.layout.layout_filter_dialog, null)
        rootView.findViewById<Button>(R.id.btn_reset).setOnClickListener {
            onButtonClickListener?.onButtonClick(null)
            this.dismiss()
        }
        checkBoxTCP = rootView.findViewById(R.id.chb_filter_tcp)
        checkBoxUDP = rootView.findViewById(R.id.chb_filter_udp)
        checkBoxL2TP = rootView.findViewById(R.id.chb_filter_l2tp)
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
        val spinnerInitPing = SpinnerInit(context, spinnerPingOperator)
        val listOperator = resources.getStringArray(R.array.number_filter_operator)
        val spinnerInitSpeed = SpinnerInit(context, spinnerSpeedOperator)
        val spinnerInitSession = SpinnerInit(context, spinnerSessionOperator)
        val listOperatorEnum = VPNGateConnectionList.NumberFilterOperator.values()
        mFilter.isShowTCP?.let { checkBoxTCP!!.isChecked = mFilter.isShowTCP }
        mFilter.isShowUDP?.let { checkBoxUDP!!.isChecked = mFilter.isShowUDP }
        mFilter.isShowL2TP?.let { checkBoxL2TP!!.isChecked = mFilter.isShowL2TP }
        mFilter.ping?.let { txtPing!!.setText(it.toString()) }
        mFilter.speed?.let { txtSpeed!!.setText(it.toString()) }
        mFilter.sessionCount?.let { txtSession!!.setText(it.toString()) }
        if (mFilter.pingFilterOperator != null) {
            spinnerInitPing.setStringArray(
                listOperator,
                listOperator[listOperatorEnum.indexOf(mFilter.pingFilterOperator)]
            )
        } else {
            spinnerInitPing.setStringArray(listOperator, listOperator[0])
        }
        spinnerInitPing.setOnItemSelectedIndexListener { _, index ->
            mFilter.pingFilterOperator = listOperatorEnum[index]
        }
        if (mFilter.speedFilterOperator != null) {
            spinnerInitSpeed.setStringArray(
                listOperator,
                listOperator[listOperatorEnum.indexOf(mFilter.speedFilterOperator)]
            )
        } else {
            spinnerInitSpeed.setStringArray(listOperator, listOperator[0])
        }
        spinnerInitSpeed.setOnItemSelectedIndexListener { _, index ->
            mFilter.speedFilterOperator = listOperatorEnum[index]
        }
        if (mFilter.sessionCountFilterOperator != null) {
            spinnerInitSession.setStringArray(
                listOperator,
                listOperator[listOperatorEnum.indexOf(mFilter.sessionCountFilterOperator)]
            )
        } else {
            spinnerInitSession.setStringArray(listOperator, listOperator[0])
        }
        spinnerInitSession.setOnItemSelectedIndexListener { _, index ->
            mFilter.sessionCountFilterOperator = listOperatorEnum[index]
        }
    }

    interface OnButtonClickListener {
        fun onButtonClick(filter: VPNGateConnectionList.Filter?)
    }
}