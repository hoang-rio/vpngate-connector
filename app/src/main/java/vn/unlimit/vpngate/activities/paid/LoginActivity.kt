package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.viewmodels.UserViewModel

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnSignUp: Button? = null
    private var btnForgotPass: Button? = null
    private var ivHidePassword: ImageView? = null
    private var userViewModel: UserViewModel? = null
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var loadingDialog: LoadingDialog? = null
    private var isFirstTimeHidePass = true;

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        btnBackToFree = findViewById(R.id.btn_back_to_free)
        txtUsername = findViewById(R.id.txt_username)
        txtPassword = findViewById(R.id.txt_password)
        btnBackToFree!!.setOnClickListener(this)
        ivHidePassword = findViewById(R.id.iv_hide_password)
        ivHidePassword!!.setOnClickListener(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        btnLogin = findViewById(R.id.btn_login)
        btnLogin!!.setOnClickListener(this)
        btnSignUp = findViewById(R.id.btn_sign_up)
        btnSignUp!!.setOnClickListener(this)
        btnForgotPass = findViewById(R.id.btn_forgot_pass)
        btnForgotPass!!.setOnClickListener(this)
        loadingDialog = LoadingDialog.newInstance(getString(R.string.login_loading_text))
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, Observer<Boolean> { isLoggingIn ->
            if (isLoggingIn!!) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                loadingDialog!!.dismiss()
                if (!userViewModel!!.isLoggedIn.value!!) {
                    if (userViewModel!!.errorList.value!!.get("code") == 101) {
                        Toast.makeText(this, getString(R.string.please_activate_account_first), Toast.LENGTH_SHORT).show()
                    } else if (userViewModel!!.errorList.value!!.get("code") == 102) {
                        Toast.makeText(this, getString(R.string.account_is_banned, userViewModel!!.errorList.value!!.get("banedReason")), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Go to paid home screen
                    val paidIntent = Intent(this, PaidServerActivity::class.java)
                    paidIntent.putExtra(BaseProvider.FROM_LOGIN, true)
                    startActivity(paidIntent)
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!txtPassword!!.isFocused) {
            txtUsername!!.requestFocus()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBackToFree -> backToFree()
            btnLogin -> {
                if (txtUsername!!.text.isEmpty() || txtPassword!!.text.isEmpty()) {
                    Toast.makeText(this, R.string.username_and_password_is_required, Toast.LENGTH_SHORT).show()
                    return
                }
                userViewModel!!.login(txtUsername!!.text.toString(), txtPassword!!.text.toString())
            }
            ivHidePassword -> {
                if (txtPassword!!.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD || isFirstTimeHidePass) {
                    txtPassword!!.inputType = InputType.TYPE_CLASS_TEXT
                    txtPassword!!.transformationMethod = null
                    GlideApp.with(this).load(R.drawable.ic_eye_hide).into(ivHidePassword!!)
                    isFirstTimeHidePass = false
                } else {
                    txtPassword!!.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                    txtPassword!!.transformationMethod = PasswordTransformationMethod.getInstance()
                    GlideApp.with(this).load(R.drawable.ic_eye_show).into(ivHidePassword!!)
                }
                txtPassword!!.setSelection(txtPassword!!.text.length)
            }
            btnSignUp -> {
                val intentSignUp = Intent(this, SignUpActivity::class.java)
                startActivity(intentSignUp)
            }
            btnForgotPass -> {
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