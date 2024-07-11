package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnectionList

/**
 * Created by hoangnd on 1/31/2018.
 */
class SortBottomSheetDialog : BottomSheetDialogFragment(), RadioGroup.OnCheckedChangeListener,
    View.OnClickListener {
    var btnApply: View? = null
    private var mSortProperty: String? = null
    private var mSortType = VPNGateConnectionList.ORDER.ASC
    private var rdgSortProperty: RadioGroup? = null
    private var rdgSortProperty2: RadioGroup? = null
    private var rdgSortType: RadioGroup? = null
    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        }
    private var onApplyClickListener: OnApplyClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSortProperty = requireArguments().getString(PROPERTY_KEY)
        mSortType = requireArguments().getInt(TYPE_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            BottomSheetDialog(requireActivity())

        dialog.setOnShowListener { dia ->
            val d = dia as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(
                bottomSheet!!
            ).state = BottomSheetBehavior.STATE_EXPANDED
        }

        //
        return dialog
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        val contentView = View.inflate(context, R.layout.layout_sort_bottom_dialog, null)
        rdgSortProperty = contentView.findViewById(R.id.rdg_sort_property)
        rdgSortProperty2 = contentView.findViewById(R.id.rdg_sort_property2)
        rdgSortType = contentView.findViewById(R.id.rdg_sort_type)
        btnApply = contentView.findViewById(R.id.btn_apply)
        btnApply!!.setOnClickListener(this)
        fillRadioGroup()
        rdgSortProperty!!.setOnCheckedChangeListener(this)
        rdgSortProperty2!!.setOnCheckedChangeListener(this)
        rdgSortType!!.setOnCheckedChangeListener(this)
        dialog.setContentView(contentView)
        val layoutParams =
            (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
    }

    private fun fillRadioGroup() {
        //Fill property radio group
        when (mSortProperty) {
            VPNGateConnectionList.SortProperty.SESSION -> rdgSortProperty!!.check(R.id.radio_session)
            VPNGateConnectionList.SortProperty.COUNTRY -> rdgSortProperty!!.check(R.id.radio_country)
            VPNGateConnectionList.SortProperty.SPEED -> rdgSortProperty!!.check(R.id.radio_speed)
            VPNGateConnectionList.SortProperty.PING -> rdgSortProperty2!!.check(R.id.radio_ping)
            VPNGateConnectionList.SortProperty.SCORE -> rdgSortProperty2!!.check(R.id.radio_score)
            VPNGateConnectionList.SortProperty.UPTIME -> rdgSortProperty2!!.check(R.id.radio_uptime)
            else -> {}
        }
        //Fill type radio group
        if (mSortType == VPNGateConnectionList.ORDER.DESC) {
            rdgSortType!!.check(R.id.radio_sort_type_desc)
        } else {
            rdgSortType!!.check(R.id.radio_sort_type_asc)
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        if (radioGroup == rdgSortProperty || radioGroup == rdgSortProperty2) {
            when (checkedId) {
                R.id.radio_session -> mSortProperty = VPNGateConnectionList.SortProperty.SESSION
                R.id.radio_country -> mSortProperty = VPNGateConnectionList.SortProperty.COUNTRY
                R.id.radio_speed -> mSortProperty = VPNGateConnectionList.SortProperty.SPEED
                R.id.radio_ping -> mSortProperty = VPNGateConnectionList.SortProperty.PING
                R.id.radio_uptime -> mSortProperty = VPNGateConnectionList.SortProperty.UPTIME
                R.id.radio_score -> mSortProperty = VPNGateConnectionList.SortProperty.SCORE
                else -> {}
            }
            rdgSortProperty!!.setOnCheckedChangeListener(null)
            rdgSortProperty2!!.setOnCheckedChangeListener(null)
            if (radioGroup == rdgSortProperty) {
                rdgSortProperty2!!.clearCheck()
            } else {
                rdgSortProperty!!.clearCheck()
            }
            rdgSortProperty!!.setOnCheckedChangeListener(this)
            rdgSortProperty2!!.setOnCheckedChangeListener(this)
        } else if (radioGroup == rdgSortType) {
            mSortType = if (checkedId == R.id.radio_sort_type_asc) {
                VPNGateConnectionList.ORDER.ASC
            } else {
                VPNGateConnectionList.ORDER.DESC
            }
        }
    }

    fun setOnApplyClickListener(_onApplyClickListener: OnApplyClickListener?) {
        if ("" != mSortProperty) {
            onApplyClickListener = _onApplyClickListener
        }
    }

    override fun onClick(view: View) {
        if (view == btnApply && onApplyClickListener != null) {
            onApplyClickListener!!.onApplyClick(mSortProperty, mSortType)
        }
        dismiss()
    }

    interface OnApplyClickListener {
        fun onApplyClick(sortProperty: String?, sortType: Int)
    }

    companion object {
        private const val PROPERTY_KEY = "PROPERTY_KEY"
        private const val TYPE_KEY = "TYPE_KEY"
        fun newInstance(sortProperty: String?, sortType: Int): SortBottomSheetDialog {
            val sortBottomSheetDialog = SortBottomSheetDialog()
            val args = Bundle()
            args.putString(PROPERTY_KEY, sortProperty)
            args.putInt(TYPE_KEY, sortType)
            sortBottomSheetDialog.arguments = args
            return sortBottomSheetDialog
        }
    }
}
