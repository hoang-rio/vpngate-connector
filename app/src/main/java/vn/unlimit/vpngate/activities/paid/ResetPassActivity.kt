package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.ActivityResetPassBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.provider.PaidServerProvider
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.regex.Pattern

class ResetPassActivity : EdgeToEdgeActivity(), View.OnClickListener {
    private var resetPassToken: String? = null
    private var userViewModel: UserViewModel? = null
    private var loadingDialog: LoadingDialog? = null
    private var isCheckingToken = false
    private var isPressedResetPass = false
    private lateinit var binding: ActivityResetPassBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityResetPassBinding.inflate(layoutInflater)
        viewBinding = binding
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()
        window.statusBarColor = resources.getColor(R.color.colorPaidServer, theme)
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = false
        binding.btnBack.setOnClickListener(this)
        binding.btnBackToFreeError.setOnClickListener(this)
        binding.btnBackToFree.setOnClickListener(this)
        binding.btnResetPass.setOnClickListener(this)
        binding.txtReNewPassword.setOnEditorActionListener { _, actionId, event ->
            if (isSubmitAction(actionId, event)) {
                binding.btnResetPass.performClick()
                true
            } else {
                false
            }
        }
        val initialScrimHeight = binding.statusBarScrim.layoutParams.height
        val initialNavLeftPadding = binding.navDetail.paddingLeft
        val initialNavRightPadding = binding.navDetail.paddingRight
        val initialFormBottom = binding.lnForm.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.navDetail) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBarScrim.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = initialScrimHeight + insets.top
            }
            binding.navDetail.updatePadding(
                left = initialNavLeftPadding + insets.left,
                right = initialNavRightPadding + insets.right
            )
            binding.lnForm.updatePadding(bottom = initialFormBottom + insets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.navDetail)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel!!.isLoading.observe(this, Observer {
            if (it) {
                loadingDialog =
                    if (loadingDialog != null) loadingDialog else LoadingDialog.newInstance()
                loadingDialog?.show(supportFragmentManager, LoadingDialog::class.simpleName)
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
                binding.lnCheckingToken.visibility = View.GONE
                binding.lnInvalidToken.visibility = View.GONE
                binding.lnForm.visibility = View.VISIBLE
            } else {
                // Invalid token => show error layout
                binding.lnCheckingToken.visibility = View.GONE
                binding.lnForm.visibility = View.GONE
                binding.lnInvalidToken.visibility = View.VISIBLE
            }
        })
    }

    private fun isSubmitAction(actionId: Int, event: KeyEvent?): Boolean {
        return actionId == EditorInfo.IME_ACTION_DONE ||
            actionId == EditorInfo.IME_ACTION_GO ||
            (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
    }

    override fun onResume() {
        super.onResume()
        resetPassToken = intent.getStringExtra(PaidServerProvider.RESET_PASS_TOKEN)
        if (resetPassToken == null) {
            Toast.makeText(
                this,
                getString(R.string.reset_pass_token_can_not_empty),
                Toast.LENGTH_LONG
            ).show()
            onBackPressedDispatcher.onBackPressed()
            finish()
            return
        }
        isCheckingToken = true
        userViewModel!!.checkResetPassToken(resetPassToken!!)
    }

    private fun doResetPass() {
        if (isPressedResetPass || userViewModel?.isLoading?.value == true) {
            return
        }
        val newPassword = binding.txtNewPassword.text.toString()
        val reRenewPassword = binding.txtReNewPassword.text.toString()
        val matcher = Pattern.compile(SignUpActivity.PASSWORD_REGEX).matcher(newPassword)
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
        isPressedResetPass = true
        userViewModel!!.resetPassword(resetPassToken!!, newPassword, reRenewPassword)
    }

    private fun backToFree() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnBack -> backToFree()
            binding.btnBackToFree -> backToFree()
            binding.btnBackToFreeError -> backToFree()
            binding.btnResetPass -> doResetPass()
        }
    }
}