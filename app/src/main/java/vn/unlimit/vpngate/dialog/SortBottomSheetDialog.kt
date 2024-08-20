package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.LayoutSortBottomDialogBinding
import vn.unlimit.vpngate.models.VPNGateConnectionList

/**
 * Created by hoangnd on 1/31/2018.
 */
class SortBottomSheetDialog : BottomSheetDialogFragment(), RadioGroup.OnCheckedChangeListener,
    View.OnClickListener {
    private var mSortProperty: String? = null
    private var mSortType = VPNGateConnectionList.ORDER.ASC
    private var onApplyClickListener: OnApplyClickListener? = null
    private lateinit var binding: LayoutSortBottomDialogBinding

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutSortBottomDialogBinding.inflate(layoutInflater)
        binding.btnApply.setOnClickListener(this)
        fillRadioGroup()
        binding.rdgSortProperty.setOnCheckedChangeListener(this)
        binding.rdgSortProperty2.setOnCheckedChangeListener(this)
        binding.rdgSortType.setOnCheckedChangeListener(this)
        return binding.root
    }

    private fun fillRadioGroup() {
        //Fill property radio group
        when (mSortProperty) {
            VPNGateConnectionList.SortProperty.SESSION -> binding.rdgSortProperty.check(R.id.radio_session)
            VPNGateConnectionList.SortProperty.COUNTRY -> binding.rdgSortProperty.check(R.id.radio_country)
            VPNGateConnectionList.SortProperty.SPEED -> binding.rdgSortProperty.check(R.id.radio_speed)
            VPNGateConnectionList.SortProperty.PING -> binding.rdgSortProperty2.check(R.id.radio_ping)
            VPNGateConnectionList.SortProperty.SCORE -> binding.rdgSortProperty2.check(R.id.radio_score)
            VPNGateConnectionList.SortProperty.UPTIME -> binding.rdgSortProperty2.check(R.id.radio_uptime)
            else -> {}
        }
        //Fill type radio group
        if (mSortType == VPNGateConnectionList.ORDER.DESC) {
            binding.rdgSortType.check(R.id.radio_sort_type_desc)
        } else {
            binding.rdgSortType.check(R.id.radio_sort_type_asc)
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        if (radioGroup == binding.rdgSortProperty || radioGroup == binding.rdgSortProperty2) {
            when (checkedId) {
                R.id.radio_session -> mSortProperty = VPNGateConnectionList.SortProperty.SESSION
                R.id.radio_country -> mSortProperty = VPNGateConnectionList.SortProperty.COUNTRY
                R.id.radio_speed -> mSortProperty = VPNGateConnectionList.SortProperty.SPEED
                R.id.radio_ping -> mSortProperty = VPNGateConnectionList.SortProperty.PING
                R.id.radio_uptime -> mSortProperty = VPNGateConnectionList.SortProperty.UPTIME
                R.id.radio_score -> mSortProperty = VPNGateConnectionList.SortProperty.SCORE
                else -> {}
            }
            binding.rdgSortProperty.setOnCheckedChangeListener(null)
            binding.rdgSortProperty2.setOnCheckedChangeListener(null)
            if (radioGroup == binding.rdgSortProperty) {
                binding.rdgSortProperty2.clearCheck()
            } else {
                binding.rdgSortProperty.clearCheck()
            }
            binding.rdgSortProperty.setOnCheckedChangeListener(this)
            binding.rdgSortProperty2.setOnCheckedChangeListener(this)
        } else if (radioGroup == binding.rdgSortType) {
            mSortType = if (checkedId == R.id.radio_sort_type_asc) {
                VPNGateConnectionList.ORDER.ASC
            } else {
                VPNGateConnectionList.ORDER.DESC
            }
        }
    }

    fun setOnApplyClickListener(inOnApplyClickListener: OnApplyClickListener?) {
        if ("" != mSortProperty) {
            onApplyClickListener = inOnApplyClickListener
        }
    }

    override fun onClick(view: View) {
        if (view == binding.btnApply && onApplyClickListener != null) {
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
