package vn.unlimit.vpngate.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.LayoutLoadingDialogBinding

class LoadingDialog : DialogFragment() {
    private var loadingText: String? = null
    private lateinit var binding: LayoutLoadingDialogBinding

    companion object {
        fun newInstance(loadingText: String): LoadingDialog {
            val loadingDialog = LoadingDialog()
            loadingDialog.loadingText = loadingText
            return loadingDialog
        }

        fun newInstance(): LoadingDialog {
            val loadingDialog = LoadingDialog()
            loadingDialog.loadingText = App.instance!!.getString(R.string.default_loading_text)
            return loadingDialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutLoadingDialogBinding.inflate(layoutInflater)
        binding.txtLoadingText.text = loadingText
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        try {
            dialog.setCanceledOnTouchOutside(false)
            // request a window without the title
            dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dialog
    }
}