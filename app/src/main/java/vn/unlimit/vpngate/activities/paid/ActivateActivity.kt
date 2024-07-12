package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.ActivityActivateBinding
import vn.unlimit.vpngate.provider.PaidServerProvider
import vn.unlimit.vpngate.viewmodels.UserViewModel

class ActivateActivity : AppCompatActivity() {
    private var userViewModel: UserViewModel? = null
    private var isDoingActivate = false
    private lateinit var binding: ActivityActivateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }
        findViewById<Button>(R.id.btn_back_to_free).setOnClickListener {
            val freeIntent = Intent(this, MainActivity::class.java)
            startActivity(freeIntent)
            finish()
        }
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        binding.lnActivating.visibility = View.INVISIBLE
        binding.lnActivated.visibility = View.INVISIBLE
        binding.lnActivating.visibility = View.VISIBLE
        userViewModel!!.isLoading.observe(this, Observer {
            if (!isDoingActivate) {
                return@Observer
            }
            if (it) {
                binding.lnActivating.visibility = View.INVISIBLE
                binding.lnActivated.visibility = View.INVISIBLE
                binding.lnActivating.visibility = View.VISIBLE
            } else if (userViewModel!!.isUserActivated.value!!) {
                binding.lnActivating.visibility = View.INVISIBLE
                binding.lnActivating.visibility = View.INVISIBLE
                binding.lnActivated.visibility = View.VISIBLE
            } else {
                // Activate failed
                binding.lnActivating.visibility = View.INVISIBLE
                binding.lnActivated.visibility = View.INVISIBLE
                binding.lnActivating.visibility = View.VISIBLE
                var errorDetailResId = R.string.account_activate_failed_invalid_request
                if (userViewModel!!.errorCode == 104) {
                    errorDetailResId = R.string.account_activate_failed_already_activate
                }
                findViewById<TextView>(R.id.txt_error).text =
                    getString(R.string.account_activate_failed, getText(errorDetailResId))
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            doActivate()
        }, 1000)
    }

    private fun doActivate() {
        val userId = intent.getStringExtra(PaidServerProvider.USER_ID)
        val activateCode = intent.getStringExtra(PaidServerProvider.ACTIVATE_CODE)
        if (userId == null || activateCode == null) {
            // Activate failed
            binding.lnActivating.visibility = View.INVISIBLE
            binding.lnActivated.visibility = View.INVISIBLE
            binding.lnActivating.visibility = View.VISIBLE
            return
        }
        isDoingActivate = true
        userViewModel!!.activateUser(userId, activateCode)
    }
}