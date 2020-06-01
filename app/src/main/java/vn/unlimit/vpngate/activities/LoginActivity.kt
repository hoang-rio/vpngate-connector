package vn.unlimit.vpngate.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import vn.unlimit.vpngate.R

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var btnBackToFree: Button? = null
    private var txtUsername: EditText? = null
    private var txtPassword: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        btnBackToFree = findViewById(R.id.btn_back_to_free)
        txtUsername = findViewById(R.id.txt_username)
        txtPassword = findViewById(R.id.txt_password)
        btnBackToFree!!.setOnClickListener(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBackToFree -> onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}