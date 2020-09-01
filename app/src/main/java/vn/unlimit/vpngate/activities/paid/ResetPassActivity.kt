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
    var resetPassToken: String? = null
    var userViewModel: UserViewModel? = null
    var loadingDialog: LoadingDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_pass)
        txtNewPassword = findViewById(R.id.txt_new_password)
        txtRenewPassword = findViewById(R.id.txt_re_new_password);
        btnResetPass = findViewById(R.id.btn_reset_pass)
        btnBackToFree = findViewById(R.id.btn_back_to_free)
        btnBackToFree!!.setOnClickListener(this)
        btnResetPass!!.setOnClickListener(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, Observer {
            if (it) {
                loadingDialog = if (loadingDialog == null) loadingDialog else LoadingDialog.newInstance()
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.simpleName)
            } else {
                loadingDialog!!.dismiss()
            }
        })
    }

    override fun onResume() {
        resetPassToken = intent.getStringExtra(PaidServerProvider.RESET_PASS_TOKEN)
        if (resetPassToken == null) {
            Toast.makeText(this, getString(R.string.reset_pass_token_can_not_empty), Toast.LENGTH_LONG).show()
            onBackPressed()
        }
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
            btnResetPass -> doResetPass()
        }
    }
}