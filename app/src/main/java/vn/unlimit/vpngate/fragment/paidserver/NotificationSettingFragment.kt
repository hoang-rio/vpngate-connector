package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.viewmodels.DeviceViewModel

class NotificationSettingFragment : Fragment(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {
    private var swNotificationSetting: SwitchCompat? = null
    private var deviceViewModel: DeviceViewModel? = null
    private var btnBack: View? = null
    private val loadingDialog = LoadingDialog.newInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swNotificationSetting = view.findViewById(R.id.sw_notify_setting)
        btnBack = view.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        bindViewModel()
        swNotificationSetting?.setOnCheckedChangeListener(this)
    }

    private fun bindViewModel() {
        val paidServiceActivity = activity as PaidServerActivity
        deviceViewModel = ViewModelProvider(requireActivity()).get(DeviceViewModel::class.java)
        swNotificationSetting?.isChecked =
            deviceViewModel!!.deviceInfo.value?.notificationSetting?.data == true
        deviceViewModel!!.deviceInfo.observe(viewLifecycleOwner) {
            swNotificationSetting?.isChecked = it.notificationSetting?.data == true
        }
        deviceViewModel!!.isLoading.observe(viewLifecycleOwner) {
            swNotificationSetting!!.isEnabled = !it
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