package vn.unlimit.vpngate.fragment.paidserver

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.databinding.FragmentProfileBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.Calendar

class ProfileFragment : Fragment(), View.OnClickListener, DatePickerDialog.OnDateSetListener,
    View.OnFocusChangeListener {
    private lateinit var binding: FragmentProfileBinding
    private var timeZonesDisplay: Array<out String>? = null
    private var timeZonesValue: Array<out String>? = null
    private var datePickerDialog: DatePickerDialog? = null
    private val calendar = Calendar.getInstance()
    private val loadingDialog = LoadingDialog.newInstance()
    private var userViewModel: UserViewModel? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        binding.btnBack.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        binding.txtBirthday.onFocusChangeListener = this
        binding.txtTimezone.onFocusChangeListener = this
        binding.txtBirthday.setOnClickListener(this)
        timeZonesDisplay = resources.getStringArray(R.array.list_time_zone_display)
        timeZonesValue = resources.getStringArray(R.array.list_time_zone_value)
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            timeZonesDisplay!!
        ).also { adapter ->
            binding.txtTimezone.setAdapter(adapter)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    private fun bindViewModel() {
        userViewModel = (activity as PaidServerActivity).userViewModel
        userViewModel?.isLoading?.observe(viewLifecycleOwner) {
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
        binding.txtUsername.setText(userViewModel?.userInfo?.value?.username)
        binding.txtEmail.setText(userViewModel?.userInfo?.value?.email)
        binding.txtFullName.setText(userViewModel?.userInfo?.value?.fullname)
        if (userViewModel?.userInfo?.value?.birthday != null) {
            val birthday = userViewModel?.userInfo?.value?.birthday
            val dateArr = birthday!!.split("/")
            calendar.set(Calendar.YEAR, dateArr[2].toInt())
            calendar.set(Calendar.MONTH, dateArr[1].toInt() - 1)
            calendar.set(Calendar.DATE, dateArr[0].toInt())
            binding.txtBirthday.setText(birthday)
        }
        datePickerDialog = DatePickerDialog(
            requireContext(),
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DATE)
        )
        binding.txtTimezone.setText(userViewModel?.userInfo?.value?.timeZone)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnBack -> findNavController().popBackStack()
            binding.txtBirthday -> datePickerDialog!!.show()
            binding.btnSave -> {
                var errorMsg: CharSequence? = null
                normalizeTimeZone()
                when (true) {
                    binding.txtFullName.text.isNullOrEmpty() -> {
                        binding.txtFullName.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_full_name)
                        )
                    }

                    binding.txtBirthday.text.isNullOrEmpty() -> {
                        binding.txtBirthday.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_birthday)
                        )
                    }

                    binding.txtTimezone.text.isNullOrEmpty() -> {
                        binding.txtTimezone.requestFocus()
                        errorMsg = getString(
                            R.string.validate_field_cannot_empty,
                            getText(R.string.prompt_timezone)
                        )
                    }

                    (timeZonesValue!!.indexOf(binding.txtTimezone.text.toString()) == -1) -> {
                        binding.txtTimezone.requestFocus()
                        getText(R.string.invalid_timezone)
                    }

                    else -> {}
                }
                if (errorMsg != null) {
                    return Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
                userViewModel?.updateProfile(
                    binding.txtFullName.text.toString(),
                    binding.txtBirthday.text.toString(),
                    binding.txtTimezone.text.toString(),
                    object : RequestListener {
                        override fun onSuccess(result: Any?) {
                            Toast.makeText(
                                requireContext(),
                                getText(R.string.update_profile_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            userViewModel?.fetchUser(forceFetch = true)
                            if (loadingDialog.isVisible) {
                                loadingDialog.dismiss()
                            }
                            findNavController().popBackStack()
                        }

                        override fun onError(error: String?) {
                            Toast.makeText(
                                requireContext(),
                                getText(R.string.update_profile_error),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (loadingDialog.isVisible) {
                                loadingDialog.dismiss()
                            }
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
        binding.txtBirthday.setText("${checkDigit(dayOfMonth)}/${checkDigit(month + 1)}/$year")
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
}