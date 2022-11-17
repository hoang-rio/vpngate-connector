package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.PaidServerProvider
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.regex.Pattern

class ResetPassActivity : AppCompatActivity(), View.OnClickListener {
    private var txtNewPassword: EditText? = null
    private var txtRenewPassword: EditText? = null
    private var btnResetPass: Button? = null
    private var btnBackToFree: Button? = null
    private var btnBackToFreeError: Button? = null
    private var resetPassToken: String? = null
    private var userViewModel: UserViewModel? = null
    private var loadingDialog: LoadingDialog? = null
    private var lnCheckingToken: View? = null
    private var lnInvalidToken: View? = null
    private var lnForm: View? = null
    private var isCheckingToken = false
    private var isPressedResetPass = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_pass)
        txtNewPassword = findViewById(R.id.txt_new_password)
        txtRenewPassword = findViewById(R.id.txt_re_new_password)
        btnResetPass = findViewById(R.id.btn_reset_pass)
        btnBackToFree = findViewById(R.id.btn_back_to_free)
        btnBackToFreeError = findViewById(R.id.btn_back_to_free_error)
        btnBackToFreeError!!.setOnClickListener(this)
        btnBackToFree!!.setOnClickListener(this)
        btnResetPass!!.setOnClickListener(this)
        lnCheckingToken = findViewById(R.id.ln_checking_token)
        lnForm = findViewById(R.id.ln_form)
        lnInvalidToken = findViewById(R.id.ln_invalid_token)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, Observer {
            if (it) {
                loadingDialog =
                    if (loadingDialog != null) loadingDialog else LoadingDialog.newInstance()
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.simpleName)
            } else {
                if (loadingDialog != null) {
                    loadingDialog!!.dismiss()
                }
                if (!isPressedResetPass) {
                    return@Observer
                }
                isPressedResetPass = false
                if (userViewModel!!.isPasswordReset.value!!) {
                    //Reset pass success => Redirect to login
                    Toast.makeText(
                        this,
                        getString(R.string.password_updated_you_can_login_now),
                        Toast.LENGTH_LONG
                    ).show()
                    val loginIntent = Intent(this, LoginActivity::class.java)
                    startActivity(loginIntent)
                    finish()
                } else {
                    // Reset pass failure
                    var toastMessage = getString(R.string.change_password_failed_with_unknown_error)
                    if (userViewModel!!.errorList.value!!.has("password")) {
                        // Password like old password
                        toastMessage =
                            getString(R.string.this_password_is_used_before_please_choose_another)
                    }
                    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                }
            }
        })
        userViewModel!!.isValidResetPassToken.observe(this, Observer {
            if (!isCheckingToken) {
                return@Observer
            }
            isCheckingToken = false
            if (it) {
                // Valid token => hide checking and show form
                lnCheckingToken!!.visibility = View.GONE
                lnInvalidToken!!.visibility = View.GONE
                lnForm!!.visibility = View.VISIBLE
            } else {
                // Invalid token => show error layout
                lnCheckingToken!!.visibility = View.GONE
                lnForm!!.visibility = View.GONE
                lnInvalidToken!!.visibility = View.VISIBLE
            }
        })
    }

    override fun onResume() {
        resetPassToken = intent.getStringExtra(PaidServerProvider.RESET_PASS_TOKEN)
        if (resetPassToken == null) {
            Toast.makeText(
                this,
                getString(R.string.reset_pass_token_can_not_empty),
                Toast.LENGTH_LONG
            ).show()
            onBackPressedDispatcher.onBackPressed()
        }
        isCheckingToken = true
        userViewModel!!.checkResetPassToken(resetPassToken!!)
        super.onResume()
    }

    private fun doResetPass() {
        isPressedResetPass = true
        val newPassword = txtNewPassword!!.text.toString()
        val reRenewPassword = txtRenewPassword!!.text.toString()
        val matcher = Pattern.compile(SignUpActivity.passWordRegex).matcher(newPassword)
        if (!matcher.matches()) {
            return Toast.makeText(this, getString(R.string.password_is_invalid), Toast.LENGTH_LONG)
                .show()
        }
        if (newPassword != reRenewPassword) {
            return Toast.makeText(
                this,
                getString(R.string.re_type_password_does_not_match),
                Toast.LENGTH_LONG
            ).show()
        }
        userViewModel!!.resetPassword(resetPassToken!!, newPassword, reRenewPassword)
    }

    private fun backToFree() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBackToFree -> backToFree()
            btnBackToFreeError -> backToFree()
            btnResetPass -> doResetPass()
        }
    }
}