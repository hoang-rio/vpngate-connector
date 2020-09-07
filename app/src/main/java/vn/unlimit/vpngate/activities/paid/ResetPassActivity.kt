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

class ResetPassActivity : AppCompatActivity(), View.OnClickListener {
    var txtNewPassword: EditText? = null
    var txtRenewPassword: EditText? = null
    var btnResetPass: Button? = null
    var btnBackToFree: Button? = null
    var btnBackToFreeError: Button? = null
    var resetPassToken: String? = null
    var userViewModel: UserViewModel? = null
    var loadingDialog: LoadingDialog? = null
    var lnCheckingToken: View? = null
    var lnInvalidToken: View? = null
    var lnForm: View? = null
    private var isCheckingToken = false
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
                loadingDialog = if (loadingDialog == null) loadingDialog else LoadingDialog.newInstance()
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.simpleName)
            } else if (loadingDialog != null) {
                loadingDialog!!.dismiss()
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
            Toast.makeText(this, getString(R.string.reset_pass_token_can_not_empty), Toast.LENGTH_LONG).show()
            onBackPressed()
        }
        isCheckingToken = true
        userViewModel!!.checkResetPassToken(resetPassToken!!)
        super.onResume()
    }

    private fun doResetPass() {

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