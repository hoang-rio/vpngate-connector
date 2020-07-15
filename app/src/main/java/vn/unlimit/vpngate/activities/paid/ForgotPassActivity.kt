package vn.unlimit.vpngate.activities.paid

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pixplicity.sharp.Sharp
import org.json.JSONObject
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel

class ForgotPassActivity : AppCompatActivity(), View.OnClickListener {
    private var loadingDialog: LoadingDialog? = null
    private var ivCaptcha: ImageView? = null
    private var captchaSecret: String? = null
    private val userApiRequest = UserApiRequest()
    private var userViewModel: UserViewModel? = null
    private var txtCaptchaAnswer: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        loadingDialog = LoadingDialog.newInstance()
        ivCaptcha = findViewById(R.id.iv_captcha)
        ivCaptcha!!.setOnClickListener(this)
        txtCaptchaAnswer = findViewById(R.id.txt_captcha_answer)
        bindViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadCaptcha()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, Observer<Boolean> { isLoading ->
            if (isLoading) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                loadingDialog!!.dismiss()
            }
        })
    }

    private fun loadCaptcha(isReload: Boolean = false) {
        lateinit var loadingDialog: LoadingDialog
        if (isReload) {
            loadingDialog = LoadingDialog.newInstance(getString(R.string.reloading_captcha))
            loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)
        }
        userApiRequest.getCaptcha(object : RequestListener {
            override fun onSuccess(result: Any?) {
                val svgImage: String = (result as JSONObject).getString("image")
                captchaSecret = result.getString("secret")
                Sharp.loadString(svgImage).into(ivCaptcha!!)
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }

            override fun onError(error: String?) {
                Toast.makeText(this@ForgotPassActivity, getString(R.string.error_get_captcha), Toast.LENGTH_SHORT).show()
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }
        })
    }

    override fun onClick(view: View?) {
        when (view) {
            ivCaptcha -> loadCaptcha(true)
        }
    }

}