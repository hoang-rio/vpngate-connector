package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.databinding.FragmentNotificationSettingBinding
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.viewmodels.DeviceViewModel

class NotificationSettingFragment : Fragment(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {
    private lateinit var binding: FragmentNotificationSettingBinding
    private var deviceViewModel: DeviceViewModel? = null
    private val loadingDialog = LoadingDialog.newInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationSettingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
        binding.swNotifySetting.setOnCheckedChangeListener(this)
    }

    private fun bindViewModel() {
        val paidServiceActivity = activity as PaidServerActivity
        deviceViewModel = ViewModelProvider(requireActivity())[DeviceViewModel::class.java]
        binding.swNotifySetting.isChecked =
            deviceViewModel!!.deviceInfo.value?.notificationSetting?.data == true
        deviceViewModel!!.deviceInfo.observe(viewLifecycleOwner) {
            binding.swNotifySetting.isChecked = it.notificationSetting?.data == true
        }
        deviceViewModel!!.isLoading.observe(viewLifecycleOwner) {
            binding.swNotifySetting.isEnabled = !it
            if (it && !loadingDialog.isVisible) {
                loadingDialog.show(
                    paidServiceActivity.supportFragmentManager,
                    LoadingDialog::class.java.name
                )
            } else {
                loadingDialog.dismiss()
            }
        }
        deviceViewModel!!.getNotificationSetting()
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        deviceViewModel!!.setNotificationSetting(isChecked)
    }

    override fun onClick(v: View?) {
        findNavController().popBackStack()
    }
}