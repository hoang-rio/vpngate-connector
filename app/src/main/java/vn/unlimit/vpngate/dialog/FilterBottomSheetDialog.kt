package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.LayoutFilterDialogBinding
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
    private lateinit var binding: LayoutFilterDialogBinding

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

    private val applyButtonClickListener = View.OnClickListener {
        if (!binding.chbFilterTcp.isChecked && !binding.chbFilterUdp.isChecked && !binding.chbFilterL2tp.isChecked && !binding.chbFilterSstp.isChecked) {
            Toast.makeText(
                context,
                resources.getString(R.string.must_check_at_least_1_protocol),
                Toast.LENGTH_SHORT
            ).show()
            return@OnClickListener
        }
        mFilter.isShowTCP = binding.chbFilterTcp.isChecked
        mFilter.isShowUDP = binding.chbFilterUdp.isChecked
        mFilter.isShowL2TP = binding.chbFilterL2tp.isChecked
        mFilter.isShowSSTP = binding.chbFilterSstp.isChecked
        mFilter.ping = binding.txtPing.text.toString().toIntOrNull()
        mFilter.speed = binding.txtSpeed.text.toString().toIntOrNull()
        mFilter.sessionCount = binding.txtSession.text.toString().toIntOrNull()
        onButtonClickListener!!.onButtonClick(mFilter)
        this.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutFilterDialogBinding.inflate(layoutInflater)
        binding.btnReset.setOnClickListener {
            onButtonClickListener?.onButtonClick(null)
            this.dismiss()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.chbFilterL2tp.visibility = View.GONE
        }
        binding.btnApply.setOnClickListener(applyButtonClickListener)
        bindData()
        return binding.root
    }

    private fun bindData() {
        val spinnerInitPing = SpinnerInit(context, binding.spinnerPingOperator)
        val listOperator = resources.getStringArray(R.array.number_filter_operator)
        val spinnerInitSpeed = SpinnerInit(context, binding.spinnerSpeedOperator)
        val spinnerInitSession = SpinnerInit(context, binding.spinnerSessionOperator)
        val listOperatorEnum = VPNGateConnectionList.NumberFilterOperator.entries.toTypedArray()
        mFilter.isShowTCP.let { binding.chbFilterTcp.isChecked = mFilter.isShowTCP }
        mFilter.isShowUDP.let { binding.chbFilterUdp.isChecked = mFilter.isShowUDP }
        mFilter.isShowL2TP.let { binding.chbFilterL2tp.isChecked = mFilter.isShowL2TP && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU }
        mFilter.isShowSSTP.let { binding.chbFilterSstp.isChecked = mFilter.isShowSSTP }
        mFilter.ping?.let { binding.txtPing.setText(it.toString()) }
        mFilter.speed?.let { binding.txtSpeed.setText(it.toString()) }
        mFilter.sessionCount?.let { binding.txtSession.setText(it.toString()) }
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