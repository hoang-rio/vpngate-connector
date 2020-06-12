package vn.unlimit.vpngate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.task.ApiRequest
import vn.unlimit.vpngate.utils.PaidServerUtil

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null
    private var btnLogin: Button? = null
    private var progressBar: ProgressBar? = null
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
        progressBar = findViewById(R.id.loading)
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
                progressBar!!.visibility = View.VISIBLE

                apiRequest.post(ApiRequest.USER_LOGIN_URL, loginData, object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        Log.e(TAG, response.toString())
                        progressBar!!.visibility = View.INVISIBLE
                        paidServerUtil.setStringSetting(PaidServerUtil.SESSION_ID_KEY, response!!.get("sessionId").toString())
                        paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response.get("user").toString())
                        paidServerUtil.setIsLoggedIn(true)
                        val paidServerIntent = Intent(this@LoginActivity, PaidServerActivity::class.java)
                        startActivity(paidServerIntent)
                        finish()
                    }

                    override fun onError(anError: ANError?) {
                        Log.e(TAG, anError.toString())
                        progressBar!!.visibility = View.INVISIBLE
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