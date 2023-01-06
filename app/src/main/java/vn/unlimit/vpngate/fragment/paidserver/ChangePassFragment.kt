package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.SignUpActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.regex.Pattern


class ChangePassFragment : Fragment(), View.OnClickListener {
    private var btnBack: View? = null
    private var btnSave: View? = null
    private var userViewModel: UserViewModel? = null
    private var txtPassword: EditText? = null
    private var txtNewPassword: EditText? = null
    private var txtReNewPassword: EditText? = null
    private val loadingDialog = LoadingDialog.newInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_change_pass, container, false)
        btnBack = rootView.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        txtPassword = rootView.findViewById(R.id.txt_password)
        txtNewPassword = rootView.findViewById(R.id.txt_new_password)
        txtReNewPassword = rootView.findViewById(R.id.txt_re_new_password)
        btnSave = rootView.findViewById(R.id.btn_save)
        btnSave?.setOnClickListener(this)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txtPassword?.requestFocus()
        bindViewModel()
    }

    private fun checkEmptyField(editText: EditText?, fieldPromptResId: Int) {
        if (editText!!.text.isEmpty()) {
            editText.requestFocus()
            throw Exception(
                getString(
                    R.string.validate_field_cannot_empty,
                    getString(fieldPromptResId)
                )
            )
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBack -> findNavController().popBackStack()
            btnSave -> {
                try {
                    checkEmptyField(txtPassword, R.string.prompt_password)
                    checkEmptyField(txtNewPassword, R.string.prompt_new_password)
                    checkEmptyField(txtReNewPassword, R.string.prompt_re_new_password)
                    // Check password regex
                    val matcher = Pattern.compile(SignUpActivity.passWordRegex).matcher(txtNewPassword!!.text)
                    if (!matcher.matches()) {
                        throw Exception(getString(R.string.new_password_is_invalid))
                    }
                    if (txtPassword!!.text.toString() == txtNewPassword!!.text.toString()) {
                        throw Exception(getString(R.string.new_password_must_not_same_as_current_password))
                    }
                    // Check retype password
                    if (txtNewPassword!!.text.toString() != txtReNewPassword!!.text.toString()) {
                        throw Exception(getString(R.string.re_type_password_does_not_match))
                    }
                    userViewModel?.changePass(txtPassword!!.text.toString(), txtNewPassword!!.text.toString(), requireActivity())
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(viewLifecycleOwner) {
            if (it && !loadingDialog.isVisible) {
               return@observe loadingDialog.show(parentFragmentManager, LoadingDialog::class.java.name)
            }
            if (loadingDialog.isVisible) {
                loadingDialog.dismiss()
            }
        }
    }
}