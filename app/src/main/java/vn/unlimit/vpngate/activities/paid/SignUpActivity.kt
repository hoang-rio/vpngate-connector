package vn.unlimit.vpngate.activities.paid

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
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
import java.util.*
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity(), View.OnClickListener, DatePickerDialog.OnDateSetListener, View.OnFocusChangeListener {
    private var btnSignUp: Button? = null
    private var btnLogin: Button? = null
    private var txtUserName: EditText? = null
    private var txtFullName: EditText? = null
    private var txtEmail: EditText? = null
    private var txtBirthday: EditText? = null
    private val calendar = Calendar.getInstance()
    private var datePickerDialog: DatePickerDialog? = null
    private var txtTimeZone: AutoCompleteTextView? = null
    private var txtPassword: EditText? = null
    private var txtRetypePassword: EditText? = null
    private var txtCaptchaAnswer: EditText? = null
    private var ivCaptcha: ImageView? = null
    private var captchaSecret: String? = null
    private val userApiRequest = UserApiRequest()
    private var userViewModel: UserViewModel? = null
    private var loadingDialog: LoadingDialog? = null
    private var timeZonesDisplay: Array<out String>? = null
    private var timeZonesValue: Array<out String>? = null
    private val userNameRegex = "^[a-z0-9]{5,30}$"
    private var isPressedSignup = false

    companion object {
        private const val TAG = "SignUpActivity"
        const val passWordRegex = "^[-\\w.$@*!]{5,30}$"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        btnSignUp = findViewById(R.id.btn_sign_up)
        btnSignUp!!.setOnClickListener(this)
        btnLogin = findViewById(R.id.btn_login)
        btnLogin!!.setOnClickListener(this)
        txtUserName = findViewById(R.id.txt_username)
        txtFullName = findViewById(R.id.txt_full_name)
        txtEmail = findViewById(R.id.txt_email)
        txtBirthday = findViewById(R.id.txt_birthday)
        txtBirthday!!.onFocusChangeListener = this
        txtBirthday!!.setOnClickListener(this)
        txtTimeZone = findViewById(R.id.txt_timezone)
        timeZonesDisplay = resources.getStringArray(R.array.list_time_zone_display)
        timeZonesValue = resources.getStringArray(R.array.list_time_zone_value)
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, timeZonesDisplay!!).also { adapter ->
            txtTimeZone!!.setAdapter(adapter)
        }
        txtTimeZone!!.onFocusChangeListener = this
        txtPassword = findViewById(R.id.txt_password)
        txtRetypePassword = findViewById(R.id.txt_retype_password)
        txtCaptchaAnswer = findViewById(R.id.txt_captcha_answer)
        ivCaptcha = findViewById(R.id.iv_captcha)
        ivCaptcha!!.setOnClickListener(this)
        datePickerDialog = DatePickerDialog(this, this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE))
        txtUserName!!.requestFocus()
        loadingDialog = LoadingDialog.newInstance()
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoading.observe(this, { isLoading ->
            if (isLoading && !loadingDialog!!.isVisible) {
                loadingDialog!!.show(supportFragmentManager, LoadingDialog::class.java.name)
            } else if (loadingDialog!!.isVisible) {
                loadingDialog!!.dismiss()
            }
        })
    }

    private fun buildErrorList(): String {
        Log.d(TAG, userViewModel!!.errorList.value.toString())
        return userViewModel!!.errorList.value.toString()
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
                Toast.makeText(this@SignUpActivity, getString(R.string.error_get_captcha), Toast.LENGTH_SHORT).show()
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
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun checkDigit(number: Int): String? {
        return if (number <= 9) "0$number" else number.toString()
    }

    @SuppressLint("SetTextI18n")
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        txtBirthday!!.setText("${checkDigit(dayOfMonth)}/${checkDigit(month + 1)}/$year")
    }

    private fun checkEmptyField(editText: EditText?, fieldPromptResId: Int) {
        if (editText!!.text.isEmpty()) {
            editText.requestFocus()
            throw Exception(getString(R.string.validate_field_cannot_empty, getString(fieldPromptResId)))
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        when (v) {
            txtBirthday -> if (hasFocus) datePickerDialog!!.show()
            txtTimeZone -> {
                if (!hasFocus) {
                    val tmpArray = txtTimeZone!!.text.split(": ")
                    if (tmpArray.size == 2) {
                        txtTimeZone!!.setText(tmpArray[1])
                    }
                }
            }
        }
    }

    private fun handleSignUp() {
        try {
            checkEmptyField(txtUserName, R.string.prompt_user)
            checkEmptyField(txtEmail, R.string.prompt_email)
            checkEmptyField(txtBirthday, R.string.prompt_birthday)
            checkEmptyField(txtTimeZone, R.string.timezone_field)
            checkEmptyField(txtPassword, R.string.prompt_password)
            checkEmptyField(txtRetypePassword, R.string.prompt_retype_password)
            checkEmptyField(txtCaptchaAnswer, R.string.prompt_captcha_answer)
            // Check username regex
            var matcher = Pattern.compile(userNameRegex).matcher(txtUserName!!.text)
            if (!matcher.matches()) {
                throw Exception(getString(R.string.username_is_invalid))
            }
            // Check password regex
            matcher = Pattern.compile(passWordRegex).matcher(txtPassword!!.text)
            if (!matcher.matches()) {
                throw Exception(getString(R.string.password_is_invalid))
            }
            // Check retype password
            if (txtPassword!!.text.toString() != txtRetypePassword!!.text.toString()) {
                throw Exception(getString(R.string.re_type_password_does_not_match))
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(txtEmail!!.text.toString()).matches()) {
                throw Exception(getString(R.string.email_is_invalid))
            }
            // Validate timeZone
            if (timeZonesValue!!.indexOf(txtTimeZone!!.text.toString()) == -1) {
                txtTimeZone!!.requestFocus()
                throw Exception(resources.getString(R.string.invalid_timezone))
            }
            userViewModel!!.isRegisterSuccess.observe(this, Observer { isRegisterSuccess ->
                if (!isPressedSignup) {
                    return@Observer
                }
                isPressedSignup = false
                if (isRegisterSuccess) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show()
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    startActivity(intentLogin)
                    finish()
                } else if (!userViewModel!!.isLoading.value!!) {
                    Toast.makeText(this, getString(R.string.register_failed, buildErrorList()), Toast.LENGTH_LONG).show()
                }
            })
            isPressedSignup = true
            userViewModel!!.register(
                    txtUserName!!.text.toString(),
                    txtFullName!!.text.toString(),
                    txtEmail!!.text.toString(),
                    txtPassword!!.text.toString(),
                    txtRetypePassword!!.text.toString(),
                    txtBirthday!!.text.toString(),
                    txtTimeZone!!.text.toString(),
                    txtCaptchaAnswer!!.text.toString().toInt(),
                    captchaSecret!!
            )
        } catch (th: Throwable) {
            Toast.makeText(this, th.message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Validate sign up form error", th)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            btnLogin -> onBackPressed()
            ivCaptcha -> loadCaptcha(true)
            txtBirthday -> datePickerDialog!!.show()
            btnSignUp -> handleSignUp()
        }
    }
}