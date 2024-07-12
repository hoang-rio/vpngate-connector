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
import vn.unlimit.vpngate.databinding.FragmentChangePassBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.regex.Pattern


class ChangePassFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentChangePassBinding
    private var userViewModel: UserViewModel? = null
    private val loadingDialog = LoadingDialog.newInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePassBinding.inflate(layoutInflater)
        binding.btnBack.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtPassword.requestFocus()
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
            binding.btnBack -> findNavController().popBackStack()
            binding.btnSave -> {
                try {
                    checkEmptyField(binding.txtPassword, R.string.prompt_password)
                    checkEmptyField(binding.txtNewPassword, R.string.prompt_new_password)
                    checkEmptyField(binding.txtReNewPassword, R.string.prompt_re_new_password)
                    // Check password regex
                    val matcher =
                        Pattern.compile(SignUpActivity.PASSWORD_REGEX).matcher(binding.txtNewPassword.text)
                    if (!matcher.matches()) {
                        throw Exception(getString(R.string.new_password_is_invalid))
                    }
                    if (binding.txtPassword.text.toString() == binding.txtNewPassword.text.toString()) {
                        throw Exception(getString(R.string.new_password_must_not_same_as_current_password))
                    }
                    // Check retype password
                    if (binding.txtNewPassword.text.toString() != binding.txtReNewPassword.text.toString()) {
                        throw Exception(getString(R.string.re_type_password_does_not_match))
                    }
                    userViewModel?.changePass(
                        binding.txtPassword.text.toString(),
                        binding.txtNewPassword.text.toString(),
                        requireActivity()
                    )
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel!!.isLoading.observe(viewLifecycleOwner) {
            if (it && !loadingDialog.isVisible) {
                return@observe loadingDialog.show(
                    parentFragmentManager,
                    LoadingDialog::class.java.name
                )
            }
            if (loadingDialog.isVisible) {
                loadingDialog.dismiss()
            }
        }
    }
}