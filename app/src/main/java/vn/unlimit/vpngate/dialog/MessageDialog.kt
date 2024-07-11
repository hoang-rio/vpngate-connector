package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.DialogFragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.utils.DataUtil
import java.util.Objects

/**
 * Created by hoangnd on 2/6/2018.
 */
class MessageDialog : DialogFragment(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private var operatorMessage: String? = null
    private var txtOpMessage: TextView? = null
    private var chbHideAllMessage: AppCompatCheckBox? = null
    private var btnClose: Button? = null
    private var dataUtil: DataUtil? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.layout_message_dialog, container, false)
        txtOpMessage = rootView.findViewById(R.id.txt_message)
        txtOpMessage!!.text = operatorMessage
        chbHideAllMessage = rootView.findViewById(R.id.cbh_hide_5time)
        chbHideAllMessage!!.setOnCheckedChangeListener(this)
        btnClose = rootView.findViewById(R.id.btn_close)
        btnClose!!.setOnClickListener(this)
        return rootView
    }

    override fun onClick(view: View) {
        if (view == btnClose) {
            dismiss()
        }
    }

    override fun onCheckedChanged(checkBox: CompoundButton, isChecked: Boolean) {
        try {
            if (checkBox == chbHideAllMessage && isChecked) {
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
