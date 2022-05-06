package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pixplicity.sharp.Sharp
import org.json.JSONObject
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel

class ForgotPassActivity : AppCompatActivity(), View.OnClickListener {
    private var loadingDialog: LoadingDialog? = null
    private var ivCaptcha: ImageView? = null
    private var captchaSecret: String? = null
    private val userApiRequest = UserApiRequest()
    private var userViewModel: UserViewModel? = null
    private var txtCaptchaAnswer: EditText? = null
    private var txtEmail: EditText? = null
    private var btnLogin: Button? = null
    private var btnResetPass: Button? = null
    private var isResetPasClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        loadingDialog = LoadingDialog.newInstance()
        ivCaptcha = findViewById(R.id.iv_captcha)
        ivCaptcha!!.setOnClickListener(this)
        txtCaptchaAnswer = findViewById(R.id.txt_captcha_answer)
        btnLogin = findViewById(R.id.btn_login)
        btnResetPass = findViewById(R.id.btn_reset_pass)
        btnLogin!!.setOnClickListener(this)
        btnResetPass!!.setOnClickListener(this)
        txtEmail = findViewById(R.id.txt_email)
        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadCaptcha()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
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
                // Show toast try again
                Toast.makeText(
                    this,
                    getString(R.string.request_forgot_pass_failure),
                    Toast.LENGTH_LONG
                ).show()
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
        userApiRequest.getCaptcha(object : RequestListener {
            override fun onSuccess(result: Any?) {
                val svgImage: String = (result as JSONObject).getString("image")
                captchaSecret = result.getString("secret")
                Sharp.loadString(svgImage).into(ivCaptcha!!)
                txtCaptchaAnswer!!.setText("")
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
            ivCaptcha -> loadCaptcha(true)
            btnLogin -> backToLogin()
            btnResetPass -> {
                if (!Patterns.EMAIL_ADDRESS.matcher(txtEmail!!.text.toString()).matches()) {
                    Toast.makeText(this, getString(R.string.email_is_invalid), Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                if (txtCaptchaAnswer!!.text.isBlank()) {
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
                    txtEmail!!.text.toString(),
                    captchaSecret.toString(),
                    txtCaptchaAnswer!!.text.toString().toInt()
                )
            }
        }
    }

}