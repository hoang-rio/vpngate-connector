package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.ActivityLoginBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.UserViewModel

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var userViewModel: UserViewModel? = null
    private val paidServerUtil = App.instance!!.paidServerUtil!!
    private var loadingDialog: LoadingDialog? = null
    private var isFirstTimeHidePass = true
    private var isClickedLogin = false
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBackToFree.setOnClickListener(this)
        binding.ivHidePassword.setOnClickListener(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.btnLogin.setOnClickListener(this)
        binding.btnSignUp.setOnClickListener(this)
        binding.btnForgotPass.setOnClickListener(this)
        loadingDialog = LoadingDialog.newInstance(getString(R.string.login_loading_text))
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel!!.isLoading.observe(this) { isLoggingIn ->
            if (!isClickedLogin) {
                return@observe
            }
            if (isLoggingIn!!) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else {
                isClickedLogin = false
                loadingDialog?.dismiss()
                if (!userViewModel!!.isLoggedIn.value!!) {
                    val errorMsg: String =
                        if (!userViewModel!!.errorList.value!!.has("code")) {
                            getString(R.string.login_failed)
                        } else if (userViewModel!!.errorList.value!!.get("code") == 101) {
                            getString(R.string.please_activate_account_first)
                        } else if (userViewModel!!.errorList.value!!.get("code") == 102) {
                            if (userViewModel!!.errorList.value!!.has("bannedReason")) {
                                getString(
                                    R.string.account_is_banned,
                                    userViewModel!!.errorList.value!!.get("bannedReason")
                                )
                            } else {
                                getString(R.string.account_is_banned_no_reason)
                            }
                        } else if (userViewModel!!.errorList.value!!.get("code") == 103) {
                            getString(R.string.account_did_not_exist)
                        } else {
                            getString(R.string.login_failed)
                        }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    val params = Bundle()
                    params.putString("username", binding.txtUsername.text.toString())
                    params.putString("errorMsg", errorMsg)
                    FirebaseAnalytics.getInstance(this).logEvent("Paid_Server_Login_Failed", params)
                } else {
                    paidServerUtil.setStringSetting(
                        PaidServerUtil.SAVED_VPN_PW,
                        binding.txtPassword.text.toString()
                    )
                    val params = Bundle()
                    params.putString("username", binding.txtUsername.text.toString())
                    FirebaseAnalytics.getInstance(this)
                        .logEvent("Paid_Server_Login_Success", params)
                    // Go to paid home screen
                    val paidIntent = Intent(this, PaidServerActivity::class.java)
                    paidIntent.putExtra(BaseProvider.FROM_LOGIN, true)
                    startActivity(paidIntent)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.txtPassword.isFocused) {
            binding.txtUsername.requestFocus()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnBackToFree -> backToFree()
            binding.btnLogin -> {
                if (binding.txtUsername.text.isEmpty() || binding.txtPassword.text.isEmpty()) {
                    Toast.makeText(
                        this,
                        R.string.username_and_password_is_required,
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                isClickedLogin = true
                userViewModel!!.login(binding.txtUsername.text.toString(), binding.txtPassword.text.toString())
            }

            binding.ivHidePassword -> {
                if (binding.txtPassword.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD || isFirstTimeHidePass) {
                    binding.txtPassword.inputType = InputType.TYPE_CLASS_TEXT
                    binding.txtPassword.transformationMethod = null
                    Glide.with(this).load(R.drawable.ic_eye_hide).into(binding.ivHidePassword)
                    isFirstTimeHidePass = false
                } else {
                    binding.txtPassword.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                    binding.txtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                    Glide.with(this).load(R.drawable.ic_eye_show).into(binding.ivHidePassword)
                }
                binding.txtPassword.setSelection(binding.txtPassword.text.length)
            }

            binding.btnSignUp -> {
                val intentSignUp = Intent(this, SignUpActivity::class.java)
                startActivity(intentSignUp)
            }

            binding.btnForgotPass -> {
                val intentForgot = Intent(this, ForgotPassActivity::class.java)
                startActivity(intentForgot)
            }
        }
    }

    private fun backToFree() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        backToFree()
        return super.onSupportNavigateUp()
    }
}