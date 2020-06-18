package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnSignUp: Button? = null
    private var btnHidePassword: Button? = null
    private val userApiRequest = UserApiRequest()
    private val paidServerUtil = App.getInstance().paidServerUtil

    companion object {
        private val TAG = "LoginActivity"
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
        btnSignUp = findViewById(R.id.btn_signup)
        btnSignUp!!.setOnClickListener(this)
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
                    Toast.makeText(applicationContext, R.string.username_and_password_is_required, Toast.LENGTH_SHORT).show()
                    return
                }
                val loadingDialog = LoadingDialog.newInstance(getString(R.string.login_loading_text))
                loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)

                userApiRequest.login(txtUsername!!.text.toString(), txtPassword!!.text.toString(), object : RequestListener {
                    override fun onSuccess(result: Any?) {
                        Log.e(TAG, result.toString())
                        loadingDialog.dismiss()
                        val paidServerIntent = Intent(this@LoginActivity, PaidServerActivity::class.java)
                        paidServerIntent.putExtra(BaseProvider.FROM_LOGIN, true)
                        startActivity(paidServerIntent)
                        finish()
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, error)
                        loadingDialog.dismiss()
                        Toast.makeText(applicationContext, R.string.login_failed, Toast.LENGTH_SHORT).show()
                    }
                })
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