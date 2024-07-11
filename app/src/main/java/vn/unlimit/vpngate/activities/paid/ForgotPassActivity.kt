package vn.unlimit.vpngate.activities.paid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pixplicity.sharp.Sharp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.ActivityForgotPassBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.models.response.CaptchaResponse
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel

class ForgotPassActivity : AppCompatActivity(), View.OnClickListener {
    private var loadingDialog: LoadingDialog? = null
    private var captchaSecret: String? = null
    private var userViewModel: UserViewModel? = null
    private var isResetPasClicked = false
    private lateinit var binding: ActivityForgotPassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loadingDialog = LoadingDialog.newInstance()
        binding.ivCaptcha.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
        binding.btnResetPass.setOnClickListener(this)
        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadCaptcha()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun buildErrorList(): String {
        var errorMessage = ""
        val value = userViewModel?.errorList?.value
        if (value?.has("captcha") == true) {
            when (value.get("captcha")) {
                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_captcha_answer)
                ) + "\n"
                109 -> errorMessage = errorMessage + getString(R.string.captcha_answer_is_not_correct) + "\n"
            }
        }
        if (value?.has("email") == true) {
            when(value.get("email")) {
                103 -> errorMessage = errorMessage + getString(R.string.account_did_not_exist) + "\n"
            }
        }
        return errorMessage
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel!!.isLoading.observe(this) { isLoading ->
            if (isLoading && !loadingDialog!!.isVisible) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                loadingDialog!!.dismiss()
            }
        }
        userViewModel!!.isForgotPassSuccess.observe(this, Observer {
            if (userViewModel!!.isLoading.value!! || !isResetPasClicked) {
                return@Observer
            }
            isResetPasClicked = false
            if (it) {
                // For got password success => show toast + back to login
                Toast.makeText(
                    this,
                    getString(R.string.request_forgot_pass_success),
                    Toast.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    backToLogin()
                }, 1000)
            } else {
                val alertDialog: AlertDialog = AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface?.dismiss() }
                    .create()
                alertDialog.setTitle(getString(R.string.request_forgot_pass_failure))
                alertDialog.setMessage(buildErrorList())
                alertDialog.show()
                loadCaptcha(false)
            }
        })
    }

    private fun loadCaptcha(isReload: Boolean = false) {
        lateinit var loadingDialog: LoadingDialog
        if (isReload) {
            loadingDialog = LoadingDialog.newInstance(getString(R.string.reloading_captcha))
            loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)
        }
        userViewModel?.getCaptcha(object : RequestListener {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: Any?) {
                val svgImage: String = (result as CaptchaResponse).image
                captchaSecret = result.secret
                Sharp.loadString(svgImage).into(binding.ivCaptcha)
                binding.txtCaptchaAnswer.setText("")
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }

            override fun onError(error: String?) {
                Toast.makeText(
                    this@ForgotPassActivity,
                    getString(R.string.error_get_captcha),
                    Toast.LENGTH_SHORT
                ).show()
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }
        })
    }

    private fun backToLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish()
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.ivCaptcha -> loadCaptcha(true)
            binding.btnLogin -> backToLogin()
            binding.btnResetPass -> {
                if (!Patterns.EMAIL_ADDRESS.matcher(binding.txtEmail.text.toString()).matches()) {
                    Toast.makeText(this, getString(R.string.email_is_invalid), Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                if (binding.txtCaptchaAnswer.text.isBlank()) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.validate_field_cannot_empty,
                            getString(R.string.prompt_captcha_answer)
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                isResetPasClicked = true
                userViewModel!!.forgotPassword(
                    binding.txtEmail.text.toString(),
                    captchaSecret.toString(),
                    binding.txtCaptchaAnswer.text.toString().toInt()
                )
            }
        }
    }

}