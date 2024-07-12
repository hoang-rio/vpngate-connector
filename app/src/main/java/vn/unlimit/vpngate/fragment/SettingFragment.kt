package vn.unlimit.vpngate.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.InetAddresses.isNumericAddress
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.core.OpenVPNService
import kittoku.osc.preference.OscPrefKey
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.FragmentSettingBinding
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.SpinnerInit
import vn.unlimit.vpngate.utils.SpinnerInit.OnItemSelectedIndexListener
import java.text.DateFormat

/**
 * Created by dongh on 31/01/2018.
 */
class SettingFragment : Fragment(), View.OnClickListener, AdapterView.OnItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, OnFocusChangeListener {
    private lateinit var dataUtil: DataUtil
    private lateinit var mContext: Context
    private lateinit var prefs: SharedPreferences

    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(layoutInflater)
        dataUtil = instance!!.dataUtil!!
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        binding.spinCacheTime.onItemSelectedListener = this
        binding.btnClearCache.setOnClickListener(this)
        binding.lnBlockAds.setOnClickListener(this)
        binding.swBlockAds.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false))
        binding.swBlockAds.setOnCheckedChangeListener(this)
        binding.lnUdp.setOnClickListener(this)
        binding.swUdp.setChecked(dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true))
        binding.swUdp.setOnCheckedChangeListener(this)
        val spinnerInit = SpinnerInit(context, binding.spinCacheTime)
        val listCacheTime = resources.getStringArray(R.array.setting_cache_time)
        spinnerInit.setStringArray(
            listCacheTime,
            listCacheTime[dataUtil.getIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, 0)]
        )
        spinnerInit.onItemSelectedIndexListener = object : OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                val params = Bundle()
                params.putString("selected_cache_value", listCacheTime[index])
                FirebaseAnalytics.getInstance(mContext).logEvent("Change_Cache_Time_Setting", params)
                dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, index)
            }
        }
        if (dataUtil.connectionCacheExpires == null) {
            binding.lnClearCache.visibility = View.GONE
        } else {
            binding.lnClearCache.visibility = View.VISIBLE
            binding.txtCacheExpire.text =
                dataUtil.connectionCacheExpires?.let {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(
                        it
                    )
                }
        }
        binding.lnDns.setOnClickListener(this)
        if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
            binding.swDns.setChecked(true)
            binding.lnDnsIp.visibility = View.VISIBLE
        } else {
            binding.swDns.setChecked(false)
            binding.lnDnsIp.visibility = View.GONE
        }
        binding.swDns.setOnCheckedChangeListener(this)
        val inputFilters = this.ipInputFilters
        binding.txtDns1.setFilters(inputFilters)
        binding.txtDns1.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8"))
        binding.txtDns1.onFocusChangeListener = this
        binding.txtDns2.setFilters(inputFilters)
        binding.txtDns2.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, ""))
        binding.txtDns2.onFocusChangeListener = this
        binding.lnDomain.setOnClickListener(this)
        binding.swDomain.setChecked(dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false))
        binding.swDomain.setOnCheckedChangeListener(this)
        binding.spinProto.onItemSelectedListener = this
        val listProtocol = resources.getStringArray(R.array.list_protocol)
        val spinnerInitProto = SpinnerInit(context, binding.spinProto)
        spinnerInitProto.setStringArray(
            listProtocol,
            listProtocol[dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0)]
        )
        spinnerInitProto.onItemSelectedIndexListener = object : OnItemSelectedIndexListener {
            override fun onItemSelected(name: String?, index: Int) {
                val params = Bundle()
                params.putString("selected_protocol", listProtocol[index])
                FirebaseAnalytics.getInstance(mContext)
                    .logEvent("Change_Default_Protocol_Setting", params)
                dataUtil.setIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, index)
            }

        }
        binding.lnNotifySpeed.setOnClickListener(this)
        binding.swNotifySpeed.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true))
        binding.swNotifySpeed.setOnCheckedChangeListener(this)
        if (dataUtil.lastVPNConnection != null) {
            binding.lnStartupScreen.visibility = View.VISIBLE
            val listScreen = resources.getStringArray(R.array.startup_screen)
            val spinnerInitScreen = SpinnerInit(context, binding.spinScreen)
            spinnerInitScreen.setStringArray(
                listScreen,
                listScreen[dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0)]
            )
            spinnerInitScreen.onItemSelectedIndexListener = object : OnItemSelectedIndexListener {
                override fun onItemSelected(name: String?, index: Int) {
                    val params = Bundle()
                    params.putString("selected_screen", listScreen[index])
                    FirebaseAnalytics.getInstance(mContext)
                        .logEvent("Change_StartUp_Screen_Setting", params)
                    dataUtil.setIntSetting(DataUtil.SETTING_STARTUP_SCREEN, index)
                    OpenVPNService.setNotificationActivityClass(if (index == 0) DetailActivity::class.java else MainActivity::class.java)
                }

            }
        } else {
            binding.lnStartupScreen.visibility = View.GONE
        }

        return binding.root
    }

    private val ipInputFilters: Array<InputFilter?>
        get() {
            val filters = arrayOfNulls<InputFilter>(1)
            filters[0] =
                InputFilter { source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int ->
                    if (end > start) {
                        val destTxt = dest.toString()
                        val resultingTxt = destTxt.substring(0, dstart) + source.subSequence(
                            start,
                            end
                        ) + destTxt.substring(dend)
                        if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?".toRegex())) {
                            return@InputFilter ""
                        } else {
                            val splits =
                                resultingTxt.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            for (`val` in splits) {
                                if (`val`.toInt() > 255) {
                                    return@InputFilter ""
                                }
                            }
                        }
                    }
                    null
                }
            return filters
        }

    override fun onFocusChange(view: View, isFocus: Boolean) {
        if (!isFocus) {
            val dnsIP: String
            val settingKey: String
            if (view == binding.txtDns1) {
                dnsIP = binding.txtDns1.text.toString()
                settingKey = DataUtil.CUSTOM_DNS_IP_1
            } else {
                dnsIP = binding.txtDns2.text.toString()
                settingKey = DataUtil.CUSTOM_DNS_IP_2
            }
            val isValidIp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isNumericAddress(dnsIP)
            } else {
                @Suppress("DEPRECATION")
                Patterns.IP_ADDRESS.matcher(dnsIP).matches()
            }
            if (isValidIp) {
                dataUtil.setStringSetting(settingKey, dnsIP)
                if (settingKey == DataUtil.CUSTOM_DNS_IP_1) {
                    val editor = prefs.edit()
                    editor.putString(OscPrefKey.DNS_CUSTOM_ADDRESS.toString(), dnsIP)
                    editor.apply()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onResume() {
        super.onResume()
        if (App.isImportToOpenVPN) {
            binding.lnBlockAdsWrap.visibility = View.GONE
            binding.lnDnsWrap.visibility = View.GONE
        } else {
            binding.lnBlockAdsWrap.visibility = View.VISIBLE
            binding.lnDnsWrap.visibility = View.VISIBLE
        }
    }

    private fun clearListServerCache(showToast: Boolean) {
        val activity = activity as MainActivity?
        if (dataUtil.clearConnectionCache()) {
            if (showToast) {
                Toast.makeText(
                    activity,
                    resources.getString(R.string.setting_clear_cache_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.lnClearCache.visibility = View.GONE
            sendClearCache()
        } else if (showToast) {
            Toast.makeText(
                activity,
                resources.getString(R.string.setting_clear_cache_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onClick(view: View) {
        when(view) {
            binding.btnClearCache -> clearListServerCache(true)
            binding.lnBlockAds -> binding.swBlockAds.isChecked = !binding.swBlockAds.isChecked
            binding.lnUdp ->  binding.swUdp.isChecked = !binding.swUdp.isChecked
            binding.lnDns -> binding.swDns.isChecked = !binding.swDns.isChecked
            binding.lnDomain -> binding.swDomain.isChecked = !binding.swDomain.isChecked
            binding.lnNotifySpeed -> binding.swNotifySpeed.isChecked = !binding.swNotifySpeed.isChecked
        }
    }

    private fun hideKeyBroad() {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            binding.txtDns1.windowToken,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    override fun onCheckedChanged(switchCompat: CompoundButton, isChecked: Boolean) {
        val params = Bundle()
        params.putString("enabled", isChecked.toString() + "")
        if (switchCompat == binding.swUdp) {
            dataUtil.setBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, isChecked)
            binding.lnDefaultProtocol.visibility = if (isChecked) View.VISIBLE else View.GONE
            clearListServerCache(false)
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Include_UDP_Setting", params)
            return
        }
        val editor = prefs.edit()
        if (switchCompat == binding.swDns) {
            dataUtil.setBooleanSetting(DataUtil.USE_CUSTOM_DNS, isChecked)
            if (isChecked) {
                if (binding.swBlockAds.isChecked) {
                    // Turn off Block Ads if custom DNS is enabled
                    binding.swBlockAds.isChecked = false
                    dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)
                }
                binding.lnDnsIp.visibility = View.VISIBLE
                binding.txtDns1.requestFocus()
                val imm =
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.txtDns1, InputMethodManager.SHOW_IMPLICIT)
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), true)
            } else {
                hideKeyBroad()
                binding.lnDnsIp.visibility = View.GONE
                editor.remove(OscPrefKey.DNS_CUSTOM_ADDRESS.toString())
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), false)
            }
            editor.apply()
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Custom_DNS_Setting", params)
            return
        }
        if (switchCompat == binding.swDomain) {
            dataUtil.setBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, isChecked)
            FirebaseAnalytics.getInstance(mContext)
                .logEvent("Change_Use_Domain_To_Connect_Setting", params)
            return
        }
        if (switchCompat == binding.swNotifySpeed) {
            Toast.makeText(
                context,
                getText(R.string.setting_apply_on_next_connection_time),
                Toast.LENGTH_SHORT
            ).show()
            dataUtil.setBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, isChecked)
            FirebaseAnalytics.getInstance(mContext)
                .logEvent("Change_Notify_Speed_Setting", params)
            return
        }
        if (dataUtil.hasAds() && isChecked) {
            switchCompat.isChecked = false
            Toast.makeText(context, getString(R.string.feature_available_in_pro), Toast.LENGTH_LONG)
                .show()
            return
        }
        //Only save setting in pro version
        if (switchCompat == binding.swBlockAds) {
            Toast.makeText(
                context,
                getText(R.string.setting_apply_on_next_connection_time),
                Toast.LENGTH_SHORT
            ).show()
            dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, isChecked)
            if (isChecked && binding.swDns.isChecked) binding.swDns.isChecked = false
            if (isChecked) {
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), true)
                editor.putString(
                    OscPrefKey.DNS_CUSTOM_ADDRESS.toString(),
                    FirebaseRemoteConfig.getInstance()
                        .getString(getString(R.string.dns_block_ads_primary_cfg_key))
                )
            } else {
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), false)
            }
            editor.apply()
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Block_Ads_Setting", params)
        }
    }

    private fun sendClearCache() {
        try {
            val intent = Intent(BaseProvider.ACTION.ACTION_CLEAR_CACHE)
            mContext.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        dataUtil.setIntSetting(DataUtil.SETTING_CACHE_TIME_KEY, position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
