package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pixplicity.sharp.Sharp
import org.json.JSONObject
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener

class SignUpActivity : AppCompatActivity(), View.OnClickListener {

    private var btnBackToFree: Button? = null
    private var btnSignUp: Button? = null
    private var btnLogin: Button? = null
    private var txtUserName: EditText? = null
    private var txtEmail: EditText? = null
    private var txtPassword: EditText? = null
    private var txtRetypePassword: EditText? = null
    private var ivCaptcha: ImageView? = null
    private var captchaSecret: String? = null
    private val userApiRequest = UserApiRequest()

    companion object {
        const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        btnBackToFree = findViewById(R.id.btn_back_to_free)
        btnSignUp = findViewById(R.id.btn_signup)
        btnSignUp!!.setOnClickListener(this)
        btnLogin = findViewById(R.id.btn_login)
        btnLogin!!.setOnClickListener(this)
        txtUserName = findViewById(R.id.txt_username)
        txtEmail = findViewById(R.id.txt_email)
        txtPassword = findViewById(R.id.txt_password)
        txtRetypePassword = findViewById(R.id.txt_retype_password)
        ivCaptcha = findViewById(R.id.iv_captcha)
        ivCaptcha!!.setOnClickListener(this)
    }

    private fun backToFree() {
        val intentFree = Intent(this, MainActivity::class.java)
        startActivity(intentFree)
    }

    private fun loadCaptcha(isReload: Boolean? = false) {
        lateinit var loadingDialog: LoadingDialog
        if (isReload!!) {
            loadingDialog = LoadingDialog.newInstance()
            loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)
        }
        userApiRequest.getCaptcha(object : RequestListener {
            override fun onSuccess(result: Any?) {
                val svgImage: String = (result as JSONObject).getString("image")
                captchaSecret = result.getString("secret")
                Sharp.loadString(svgImage).into(ivCaptcha as View)
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }

            override fun onError(error: String?) {
                Toast.makeText(this@SignUpActivity, getString(R.string.error_get_catpcha), Toast.LENGTH_SHORT).show()
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadCaptcha()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBackToFree -> backToFree()
            btnLogin -> onBackPressed()
            ivCaptcha -> loadCaptcha(true)
            btnSignUp -> {
                if (txtUserName!!.text.isEmpty()) {
                    Toast.makeText(this, getString(R.string.validate_field_cannot_empty, getString(R.string.prompt_user)), Toast.LENGTH_SHORT).show()
                    txtUserName?.requestFocus()
                    return
                }
                if (txtEmail!!.text.isEmpty()) {
                    Toast.makeText(this, getString(R.string.validate_field_cannot_empty, getString(R.string.prompt_email)), Toast.LENGTH_SHORT).show()
                    txtEmail?.requestFocus()
                    return
                }
                if (txtPassword!!.text.isEmpty()) {
                    Toast.makeText(this, getString(R.string.validate_field_cannot_empty, getString(R.string.prompt_password)), Toast.LENGTH_SHORT).show()
                    txtPassword?.requestFocus()
                    return
                }
                if (txtRetypePassword!!.text.isEmpty()) {
                    Toast.makeText(this, getString(R.string.validate_field_cannot_empty, getString(R.string.prompt_retype_password)), Toast.LENGTH_SHORT).show()
                    txtRetypePassword?.requestFocus()
                    return
                }
                TODO("Must implement local validate here")
            }
        }
    }
}