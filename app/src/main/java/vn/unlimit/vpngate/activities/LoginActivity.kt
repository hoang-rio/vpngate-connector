package vn.unlimit.vpngate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.task.ApiRequest
import vn.unlimit.vpngate.utils.PaidServerUtil

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null
    private var btnLogin: Button? = null
    private val apiRequest = ApiRequest()
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
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        btnLogin = findViewById(R.id.btn_login)
        btnLogin!!.setOnClickListener(this)
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
                val loginData = HashMap<String, String>()
                loginData["username"] = txtUsername!!.text.toString()
                loginData["password"] = txtPassword!!.text.toString()
                val loadingDialog = LoadingDialog.newInstance(getString(R.string.login_loading_text))
                loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)

                apiRequest.post(ApiRequest.USER_LOGIN_URL, loginData, object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        Log.e(TAG, response.toString())
                        paidServerUtil.setStringSetting(PaidServerUtil.SESSION_ID_KEY, response!!.get("sessionId").toString())
                        paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response.get("user").toString())
                        paidServerUtil.setIsLoggedIn(true)
                        loadingDialog.dismiss()
                        val paidServerIntent = Intent(this@LoginActivity, PaidServerActivity::class.java)
                        paidServerIntent.putExtra(BaseProvider.FROM_LOGIN, true)
                        startActivity(paidServerIntent)
                        finish()
                    }

                    override fun onError(anError: ANError?) {
                        Log.e(TAG, anError.toString())
                        loadingDialog.dismiss()
                        Toast.makeText(applicationContext, R.string.login_failed, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    fun backToFree() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        backToFree()
        return super.onSupportNavigateUp()
    }
}