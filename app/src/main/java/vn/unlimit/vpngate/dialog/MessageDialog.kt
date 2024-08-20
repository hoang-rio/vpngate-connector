package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import vn.unlimit.vpngate.databinding.LayoutMessageDialogBinding
import vn.unlimit.vpngate.utils.DataUtil

/**
 * Created by hoangnd on 2/6/2018.
 */
class MessageDialog : DialogFragment(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private var operatorMessage: String? = null
    private var dataUtil: DataUtil? = null
    private lateinit var binding: LayoutMessageDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutMessageDialogBinding.inflate(layoutInflater)
        binding.txtMessage.text = operatorMessage
        binding.cbhHide5time.setOnCheckedChangeListener(this)
        binding.btnClose.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(view: View) {
        if (view == binding.btnClose) {
            dismiss()
        }
    }

    override fun onCheckedChanged(checkBox: CompoundButton, isChecked: Boolean) {
        try {
            if (checkBox == binding.cbhHide5time && isChecked) {
                dataUtil!!.setIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 5)
            } else {
                dataUtil!!.setIntSetting(DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        try {
            // request a window without the title
            dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dialog
    }

    companion object {
        fun newInstance(message: String?, dataUtil: DataUtil): MessageDialog {
            val f = MessageDialog()
            f.operatorMessage = message
            f.dataUtil = dataUtil
            return f
        }
    }
}
