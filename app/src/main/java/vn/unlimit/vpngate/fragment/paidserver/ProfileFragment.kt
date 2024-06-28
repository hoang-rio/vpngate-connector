package vn.unlimit.vpngate.fragment.paidserver

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.Calendar

class ProfileFragment : Fragment(), View.OnClickListener, DatePickerDialog.OnDateSetListener,
    View.OnFocusChangeListener {
    private var btnBack: View? = null
    private var btnSave: View? = null
    private var txtUserName: EditText? = null
    private var txtEmail: EditText? = null
    private var txtFullName: EditText? = null
    private var txtBirthday: EditText? = null
    private var txtTimeZone: AutoCompleteTextView? = null
    private var timeZonesDisplay: Array<out String>? = null
    private var timeZonesValue: Array<out String>? = null
    private var datePickerDialog: DatePickerDialog? = null
    private val calendar = Calendar.getInstance()
    private val loadingDialog = LoadingDialog.newInstance()
    private var userViewModel: UserViewModel? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        btnBack = rootView.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        btnSave = rootView.findViewById(R.id.btn_save)
        btnSave?.setOnClickListener(this)
        txtUserName = rootView.findViewById(R.id.txt_username)
        txtEmail = rootView.findViewById(R.id.txt_email)
        txtFullName = rootView.findViewById(R.id.txt_full_name)
        txtBirthday = rootView.findViewById(R.id.txt_birthday)
        txtBirthday?.onFocusChangeListener = this
        txtTimeZone = rootView.findViewById(R.id.txt_timezone)
        txtTimeZone?.onFocusChangeListener = this
        txtBirthday?.setOnClickListener(this)
        timeZonesDisplay = resources.getStringArray(R.array.list_time_zone_display)
        timeZonesValue = resources.getStringArray(R.array.list_time_zone_value)
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            timeZonesDisplay!!
        ).also { adapter ->
            txtTimeZone!!.setAdapter(adapter)
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = (activity as PaidServerActivity).userViewModel
        userViewModel?.isLoading?.observe((activity as PaidServerActivity)) {
            if (it && !loadingDialog.isVisible) {
                return@observe loadingDialog.show(
                    requireActivity().supportFragmentManager,
                    LoadingDialog::class.java.name
                )
            }
            if (loadingDialog.isVisible) {
                loadingDialog.dismiss()
            }
        }
        txtUserName!!.setText(userViewModel?.userInfo?.value?.username)
        txtEmail!!.setText(userViewModel?.userInfo?.value?.email)
        txtFullName!!.setText(userViewModel?.userInfo?.value?.fullname)
        if (userViewModel?.userInfo?.value?.birthday != null) {
            val birthday = userViewModel?.userInfo?.value?.birthday
            val dateArr = birthday!!.split("/")
            calendar.set(Calendar.YEAR, dateArr[2].toInt())
            calendar.set(Calendar.MONTH, dateArr[1].toInt() - 1)
            calendar.set(Calendar.DATE, dateArr[0].toInt())
            txtBirthday!!.setText(birthday)
        }
        datePickerDialog = DatePickerDialog(
            requireContext(),
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DATE)
        )
        txtTimeZone!!.setText(userViewModel?.userInfo?.value?.timeZone)
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBack -> findNavController().popBackStack()
            txtBirthday -> datePickerDialog!!.show()
            btnSave -> {
                var errorMsg: CharSequence? = null
                when (true) {
                    txtFullName!!.text.isNullOrEmpty() -> {
                        txtFullName!!.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_full_name)
                        )
                    }

                    txtBirthday!!.text.isNullOrEmpty() -> {
                        txtBirthday!!.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_birthday)
                        )
                    }

                    txtTimeZone!!.text.isNullOrEmpty() -> {
                        txtTimeZone!!.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_timezone)
                        )
                    }

                    (timeZonesValue!!.indexOf(txtTimeZone!!.text.toString()) == -1) -> {
                        txtTimeZone!!.requestFocus()
                        getText(R.string.invalid_timezone)
                    }

                    else -> {}
                }
                if (errorMsg != null) {
                    return Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
                userViewModel?.updateProfile(
                    txtFullName!!.text.toString(),
                    txtBirthday!!.text.toString(),
                    txtTimeZone!!.text.toString(),
                    object : RequestListener {
                        override fun onSuccess(result: Any?) {
                            Toast.makeText(
                                requireContext(),
                                getText(R.string.update_profile_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            userViewModel?.fetchUser()
                            findNavController().popBackStack()
                        }

                        override fun onError(error: String?) {
                            Toast.makeText(
                                requireContext(),
                                getText(R.string.update_profile_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    private fun checkDigit(number: Int): String {
        return if (number <= 9) "0$number" else number.toString()
    }

    @SuppressLint("SetTextI18n")
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        txtBirthday!!.setText("${checkDigit(dayOfMonth)}/${checkDigit(month + 1)}/$year")
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
}