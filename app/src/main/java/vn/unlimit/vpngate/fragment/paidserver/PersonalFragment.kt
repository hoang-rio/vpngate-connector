package vn.unlimit.vpngate.fragment.paidserver

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.common.base.Strings
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.FragmentPersonalBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.viewmodels.DeviceViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel

class PersonalFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentPersonalBinding
    private var deviceViewModel: DeviceViewModel? = null
    val userViewModel by lazy {
        ViewModelProvider(this)[UserViewModel::class.java]
    }
    val loadingDialog by lazy {
        LoadingDialog.newInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPersonalBinding.inflate(layoutInflater)
        binding.lnNotificationSetting.setOnClickListener(this)
        binding.lnProfile.setOnClickListener(this)
        binding.lnChangePassword.setOnClickListener(this)
        binding.lnAbout.setOnClickListener(this)
        binding.btnDeleteAccount.setOnClickListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceViewModel = ViewModelProvider(this)[DeviceViewModel::class.java]
        if (deviceViewModel!!.deviceInfo.value == null || Strings.isNullOrEmpty(deviceViewModel!!.deviceInfo.value?._id)) {
            binding.lnNotificationSetting.visibility = View.GONE
            view.findViewById<View>(R.id.line_notification_setting).visibility = View.GONE
        }
        userViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && !loadingDialog.isVisible) {
                loadingDialog.show(
                    requireActivity().supportFragmentManager,
                    LoadingDialog::class.java.name
                )
            } else if (loadingDialog.isVisible) {
                loadingDialog.dismiss()
            }
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ln_about -> {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra(MainActivity.TARGET_FRAGMENT, "about")
                startActivity(intent)
            }

            R.id.ln_notification_setting -> findNavController().navigate(R.id.navigation_notification_setting)
            R.id.ln_change_password -> findNavController().navigate(R.id.navigation_change_pass)
            R.id.ln_profile -> findNavController().navigate(R.id.navigation_profile)
            R.id.btn_delete_account -> deleteAccount()
        }
    }

    private fun deleteAccount() {
        AlertDialog.Builder(requireContext())
            .setPositiveButton(
                R.string.sure_btn
            ) { _, _ ->
                userViewModel.deleteAccount(object : RequestListener {
                    override fun onSuccess(result: Any?) {
                        val deleteResult = result as Boolean
                        if (deleteResult) {
                            userViewModel.localLogout(requireActivity())
                            Toast.makeText(
                                requireContext(),
                                R.string.account_deleted,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                R.string.account_activate_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onError(error: String?) {
                        //Nothing to do hear
                    }

                })
            }
            .setNegativeButton(R.string.cancel_btn) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create().apply {
                setTitle(R.string.delete_account_dialog_title)
                setMessage(getString(R.string.delete_account_description))
                show()
            }
    }
}