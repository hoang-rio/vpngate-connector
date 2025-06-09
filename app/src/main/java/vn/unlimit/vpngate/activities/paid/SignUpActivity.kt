package vn.unlimit.vpngate.activities.paid

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pixplicity.sharp.Sharp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.ActivitySignUpBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.models.response.CaptchaResponse
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.Calendar
import java.util.regex.Pattern

class SignUpActivity : EdgeToEdgeActivity(), View.OnClickListener,
    DatePickerDialog.OnDateSetListener, View.OnFocusChangeListener {
    private val calendar = Calendar.getInstance()
    private var datePickerDialog: DatePickerDialog? = null
    private var captchaSecret: String? = null
    private var userViewModel: UserViewModel? = null
    private var loadingDialog: LoadingDialog? = null
    private var timeZonesDisplay: Array<out String>? = null
    private var timeZonesValue: Array<out String>? = null
    private var isPressedSignup = false
    private lateinit var binding: ActivitySignUpBinding

    companion object {
        private const val TAG = "SignUpActivity"
        const val PASSWORD_REGEX = "^[-\\w.$@*!]{5,30}$"
        private const val USER_NAME_REGEX = "^[a-z0-9]{5,30}$"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        this.viewBinding = binding
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.btnSignUp.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
        binding.txtBirthday.onFocusChangeListener = this
        binding.txtBirthday.setOnClickListener(this)
        timeZonesDisplay = resources.getStringArray(R.array.list_time_zone_display)
        timeZonesValue = resources.getStringArray(R.array.list_time_zone_value)
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            timeZonesDisplay!!
        ).also { adapter ->
            binding.txtTimezone.setAdapter(adapter)
        }
        binding.txtTimezone.onFocusChangeListener = this
        binding.ivCaptcha.setOnClickListener(this)
        datePickerDialog = DatePickerDialog(
            this,
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DATE)
        )
        binding.txtUsername.requestFocus()
        loadingDialog = LoadingDialog.newInstance()
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel!!.isLoading.observe(this) { isLoading ->
            if (isLoading && !loadingDialog!!.isVisible) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                loadingDialog!!.dismiss()
            }
        }
    }

    private fun buildErrorList(): String {
        var errorMessage = ""
        val errorList = userViewModel!!.errorList.value
        Log.d(TAG, errorList.toString())
        if (errorList!!.has("username")) {
            when (errorList.getInt("username")) {
                100 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_exist_in_system,
                    getString(R.string.prompt_user)
                ) + "\n"

                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_user)
                ) + "\n"

                108 -> errorMessage = errorMessage + getString(R.string.invalid_username) + "\n"
            }
        }
        if (errorList.has("fullname")) {
            when (errorList.getInt("fullname")) {
                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_full_name)
                ) + "\n"
            }
        }
        if (errorList.has("email")) {
            when (errorList.getInt("email")) {
                100 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_exist_in_system,
                    getString(R.string.prompt_email)
                ) + "\n"

                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_email)
                ) + "\n"

                108 -> errorMessage = errorMessage + getString(R.string.email_is_invalid) + "\n"
            }
        }
        if (errorList.has("password")) {
            when (errorList.get("password")) {
                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_password)
                ) + "\n"

                108 -> errorMessage = errorMessage + getString(R.string.password_is_invalid) + "\n"
            }
        }
        if (errorList.has("birthday")) {
            if (errorList.get("birthday") == 109) {
                errorMessage += getString(R.string.birthday_is_invalid)
            }
        }
        if (errorList.has("repassword")) {
            when (errorList.get("repassword")) {
                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_retype_password)
                ) + "\n"

                109 -> errorMessage =
                    errorMessage + getString(R.string.re_type_password_does_not_match) + "\n"
            }
        }
        if (errorList.has("captcha")) {
            when (errorList.get("captcha")) {
                107 -> errorMessage = errorMessage + getString(
                    R.string.validate_field_cannot_empty,
                    getString(R.string.prompt_captcha_answer)
                ) + "\n"
                109 -> errorMessage = errorMessage + getString(R.string.captcha_answer_is_not_correct) + "\n"
            }
        }
        return errorMessage
    }

    private fun loadCaptcha(isReload: Boolean = false) {
        lateinit var loadingDialog: LoadingDialog
        if (isReload) {
            loadingDialog = LoadingDialog.newInstance(getString(R.string.reloading_captcha))
            loadingDialog.show(supportFragmentManager, LoadingDialog::class.java.name)
        }
        userViewModel?.getCaptcha(object : RequestListener {
            override fun onSuccess(result: Any?) {
                val svgImage: String = (result as CaptchaResponse).image
                captchaSecret = result.secret
                Sharp.loadString(svgImage).into(binding.ivCaptcha)
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }

            override fun onError(error: String?) {
                Toast.makeText(
                    this@SignUpActivity,
                    getString(R.string.error_get_captcha),
                    Toast.LENGTH_SHORT
                ).show()
                if (isReload) {
                    loadingDialog.dismiss()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        datePickerDialog!!.datePicker.maxDate = calendar.time.time
        loadCaptcha()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun checkDigit(number: Int): String {
        return if (number <= 9) "0$number" else number.toString()
    }

    @SuppressLint("SetTextI18n")
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        binding.txtBirthday.setText("${checkDigit(dayOfMonth)}/${checkDigit(month + 1)}/$year")
    }

    private fun checkEmptyField(editText: EditText?, fieldPromptResId: Int) {
        if (editText!!.text.isEmpty()) {
            editText.requestFocus()
            throw Exception(
                getString(
                    R.string.validate_field_cannot_empty,
                    getString(fieldPromptResId)
                )
            )
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        when (v) {
            binding.txtBirthday -> if (hasFocus) datePickerDialog!!.show()
            binding.txtTimezone -> {
                if (!hasFocus) {
                    normalizeTimeZone()
                }
            }
        }
    }

    private fun normalizeTimeZone() {
        if (binding.txtTimezone.text == null || binding.txtTimezone.text.isEmpty()) {
            return
        }
        val tmpArray = binding.txtTimezone.text.split(": ")
        if (tmpArray.size == 2) {
            binding.txtTimezone.setText(tmpArray[1])
        }
    }

    private fun handleSignUp() {
        try {
            checkEmptyField(binding.txtUsername, R.string.prompt_user)
            checkEmptyField(binding.txtFullName, R.string.prompt_full_name)
            checkEmptyField(binding.txtEmail, R.string.prompt_email)
            checkEmptyField(binding.txtBirthday, R.string.prompt_birthday)
            checkEmptyField(binding.txtTimezone, R.string.timezone_field)
            checkEmptyField(binding.txtPassword, R.string.prompt_password)
            checkEmptyField(binding.txtRetypePassword, R.string.prompt_retype_password)
            checkEmptyField(binding.txtCaptchaAnswer, R.string.prompt_captcha_answer)
            // Check username regex
            var matcher = Pattern.compile(USER_NAME_REGEX).matcher(binding.txtUsername.text)
            if (!matcher.matches()) {
                throw Exception(getString(R.string.username_is_invalid))
            }
            // Check password regex
            matcher = Pattern.compile(PASSWORD_REGEX).matcher(binding.txtPassword.text)
            if (!matcher.matches()) {
                throw Exception(getString(R.string.password_is_invalid))
            }
            // Check retype password
            if (binding.txtPassword.text.toString() != binding.txtRetypePassword.text.toString()) {
                throw Exception(getString(R.string.re_type_password_does_not_match))
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(binding.txtEmail.text.toString()).matches()) {
                throw Exception(getString(R.string.email_is_invalid))
            }
            normalizeTimeZone()
            // Validate timeZone
            if (timeZonesValue!!.indexOf(binding.txtTimezone.text.toString()) == -1) {
                binding.txtTimezone.requestFocus()
                throw Exception(resources.getString(R.string.invalid_timezone))
            }
            userViewModel!!.isRegisterSuccess.observe(this, Observer { isRegisterSuccess ->
                if (!isPressedSignup) {
                    return@Observer
                }
                isPressedSignup = false
                if (isRegisterSuccess) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG)
                        .show()
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    startActivity(intentLogin)
                    finish()
                } else if (!userViewModel!!.isLoading.value!!) {
                    val alertDialog: AlertDialog = AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface?.dismiss() }
                        .create()
                    alertDialog.setTitle(getString(R.string.register_failed_title))
                    alertDialog.setMessage(buildErrorList())
                    alertDialog.show()
                    loadCaptcha(false)
                }
            })
            isPressedSignup = true
            userViewModel!!.register(
                binding.txtUsername.text.toString(),
                binding.txtFullName.text.toString(),
                binding.txtEmail.text.toString(),
                binding.txtPassword.text.toString(),
                binding.txtRetypePassword.text.toString(),
                binding.txtBirthday.text.toString(),
                binding.txtTimezone.text.toString(),
                binding.txtCaptchaAnswer.text.toString().toInt(),
                captchaSecret!!
            )
        } catch (th: Throwable) {
            if (th.message != null) {
                Toast.makeText(this, th.message, Toast.LENGTH_SHORT).show()
            }
            Log.e(TAG, "Validate sign up form error", th)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnLogin -> onBackPressedDispatcher.onBackPressed()
            binding.ivCaptcha -> loadCaptcha(true)
            binding.txtBirthday -> datePickerDialog!!.show()
            binding.btnSignUp -> handleSignUp()
        }
    }
}