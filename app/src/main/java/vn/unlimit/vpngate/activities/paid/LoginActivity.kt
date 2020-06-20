package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.UserViewModel

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnSignUp: Button? = null
    private var btnHidePassword: Button? = null
    private var userViewModel: UserViewModel? = null
    private val paidServerUtil = App.getInstance().paidServerUtil
    private var loadingDialog: LoadingDialog? = null

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
        btnHidePassword = findViewById(R.id.btn_hide_password)
        btnHidePassword!!.setOnClickListener(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        btnLogin = findViewById(R.id.btn_login)
        btnLogin!!.setOnClickListener(this)
        btnSignUp = findViewById(R.id.btn_sign_up)
        btnSignUp!!.setOnClickListener(this)
        loadingDialog = LoadingDialog.newInstance(getString(R.string.login_loading_text))
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, Observer<Boolean> { isLoggingIn ->
            if (isLoggingIn!!) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                if (!userViewModel!!.isLoggedIn.value!!) {
                    Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                }
                loadingDialog!!.dismiss()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        paidServerUtil.setStartupScreen(PaidServerUtil.StartUpScreen.PAID_SERVER)
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
            btnHidePassword -> {
                if (txtPassword!!.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    txtPassword!!.inputType = InputType.TYPE_CLASS_TEXT
                    txtPassword!!.transformationMethod = null
                    btnHidePassword!!.text = getText(R.string.action_hide_password)
                } else {
                    txtPassword!!.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                    txtPassword!!.transformationMethod = PasswordTransformationMethod.getInstance()
                    btnHidePassword!!.text = getText(R.string.action_show_password)
                }
                txtPassword!!.setSelection(txtPassword!!.text.length)
            }
            btnSignUp -> {
                val intentSignUp = Intent(this, SignUpActivity::class.java)
                startActivity(intentSignUp)
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