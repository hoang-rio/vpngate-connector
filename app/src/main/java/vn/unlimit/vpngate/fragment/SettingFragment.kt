package vn.unlimit.vpngate.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.InetAddresses.isNumericAddress
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.core.OpenVPNService
import kittoku.osc.preference.OscPrefKey
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
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
    private lateinit var btnClearCache: Button
    private lateinit var lnClearCache: View
    private lateinit var dataUtil: DataUtil
    private lateinit var swBlockAds: SwitchCompat
    private lateinit var lnBlockAds: View
    private lateinit var lnBlockAdsWrap: View
    private lateinit var swUdp: SwitchCompat
    private lateinit var lnUdp: View
    private lateinit var mContext: Context
    private lateinit var lnDns: View
    private lateinit var lnDnsWrap: View
    private lateinit var swDns: SwitchCompat
    private lateinit var lnDnsIP: View
    private lateinit var txtDns1: EditText
    private lateinit var txtDns2: EditText
    private lateinit var lnDomain: View
    private lateinit var swDomain: SwitchCompat
    private lateinit var lnProtocol: View
    private lateinit var lnNotifySpeed: View
    private lateinit var swNotifySpeed: SwitchCompat
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_setting, container, false)
        dataUtil = instance!!.dataUtil!!
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val spinnerCacheTime = rootView.findViewById<AppCompatSpinner>(R.id.spin_cache_time)
        spinnerCacheTime.onItemSelectedListener = this
        btnClearCache = rootView.findViewById(R.id.btn_clear_cache)
        btnClearCache.setOnClickListener(this)
        lnBlockAds = rootView.findViewById(R.id.ln_block_ads)
        lnBlockAdsWrap = rootView.findViewById(R.id.ln_block_ads_wrap)
        lnDnsWrap = rootView.findViewById(R.id.ln_dns_wrap)
        lnBlockAds.setOnClickListener(this)
        swBlockAds = rootView.findViewById(R.id.sw_block_ads)
        swBlockAds.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false))
        swBlockAds.setOnCheckedChangeListener(this)
        lnUdp = rootView.findViewById(R.id.ln_udp)
        lnUdp.setOnClickListener(this)
        swUdp = rootView.findViewById(R.id.sw_udp)
        swUdp.setChecked(dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true))
        swUdp.setOnCheckedChangeListener(this)
        lnClearCache = rootView.findViewById(R.id.ln_clear_cache)
        val txtCacheExpires = rootView.findViewById<TextView>(R.id.txt_cache_expire)
        val spinnerInit = SpinnerInit(context, spinnerCacheTime)
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
            lnClearCache.visibility = View.GONE
        } else {
            lnClearCache.visibility = View.VISIBLE
            txtCacheExpires.text =
                dataUtil.connectionCacheExpires?.let {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(
                        it
                    )
                }
        }
        lnDns = rootView.findViewById(R.id.ln_dns)
        lnDns.setOnClickListener(this)
        lnDnsIP = rootView.findViewById(R.id.ln_dns_ip)
        swDns = rootView.findViewById(R.id.sw_dns)
        if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
            swDns.setChecked(true)
            lnDnsIP.visibility = View.VISIBLE
        } else {
            swDns.setChecked(false)
            lnDnsIP.visibility = View.GONE
        }
        swDns.setOnCheckedChangeListener(this)
        val inputFilters = this.ipInputFilters
        txtDns1 = rootView.findViewById(R.id.txt_dns_1)
        txtDns1.setFilters(inputFilters)
        txtDns1.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8"))
        txtDns1.onFocusChangeListener = this
        txtDns2 = rootView.findViewById(R.id.txt_dns_2)
        txtDns2.setFilters(inputFilters)
        txtDns2.setText(dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, ""))
        txtDns2.onFocusChangeListener = this
        lnDomain = rootView.findViewById(R.id.ln_domain)
        lnDomain.setOnClickListener(this)
        swDomain = rootView.findViewById(R.id.sw_domain)
        swDomain.setChecked(dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false))
        swDomain.setOnCheckedChangeListener(this)
        lnProtocol = rootView.findViewById(R.id.ln_default_protocol)
        val spinnerProto = rootView.findViewById<AppCompatSpinner>(R.id.spin_proto)
        spinnerProto.onItemSelectedListener = this
        val listProtocol = resources.getStringArray(R.array.list_protocol)
        val spinnerInitProto = SpinnerInit(context, spinnerProto)
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
        lnNotifySpeed = rootView.findViewById(R.id.ln_notify_speed)
        lnNotifySpeed.setOnClickListener(this)
        swNotifySpeed = rootView.findViewById(R.id.sw_notify_speed)
        swNotifySpeed.setChecked(dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true))
        swNotifySpeed.setOnCheckedChangeListener(this)
        val lnStartUpScreen = rootView.findViewById<View>(R.id.ln_startup_screen)
        if (dataUtil.lastVPNConnection != null) {
            lnStartUpScreen.visibility = View.VISIBLE
            val spinnerStartupScreen = rootView.findViewById<AppCompatSpinner>(R.id.spin_screen)
            val listScreen = resources.getStringArray(R.array.startup_screen)
            val spinnerInitScreen = SpinnerInit(context, spinnerStartupScreen)
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
            lnStartUpScreen.visibility = View.GONE
        }

        return rootView
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
            if (view == txtDns1) {
                dnsIP = txtDns1.text.toString()
                settingKey = DataUtil.CUSTOM_DNS_IP_1
            } else {
                dnsIP = txtDns2.text.toString()
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
            lnBlockAdsWrap.visibility = View.GONE
            lnDnsWrap.visibility = View.GONE
        } else {
            lnBlockAdsWrap.visibility = View.VISIBLE
            lnDnsWrap.visibility = View.VISIBLE
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
            lnClearCache.visibility = View.GONE
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
            btnClearCache -> clearListServerCache(true)
            lnBlockAds -> swBlockAds.isChecked = !swBlockAds.isChecked
            lnUdp ->  swUdp.isChecked = !swUdp.isChecked
            lnDns -> swDns.isChecked = !swDns.isChecked
            lnDomain -> swDomain.isChecked = !swDomain.isChecked
            lnNotifySpeed -> swNotifySpeed.isChecked = !swNotifySpeed.isChecked
        }
    }

    private fun hideKeyBroad() {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            txtDns1.windowToken,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    override fun onCheckedChanged(switchCompat: CompoundButton, isChecked: Boolean) {
        val params = Bundle()
        params.putString("enabled", isChecked.toString() + "")
        if (switchCompat == swUdp) {
            dataUtil.setBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, isChecked)
            lnProtocol.visibility = if (isChecked) View.VISIBLE else View.GONE
            clearListServerCache(false)
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Include_UDP_Setting", params)
            return
        }
        val editor = prefs.edit()
        if (switchCompat == swDns) {
            dataUtil.setBooleanSetting(DataUtil.USE_CUSTOM_DNS, isChecked)
            if (isChecked) {
                if (swBlockAds.isChecked) {
                    // Turn off Block Ads if custom DNS is enabled
                    swBlockAds.isChecked = false
                    dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)
                }
                lnDnsIP.visibility = View.VISIBLE
                txtDns1.requestFocus()
                val imm =
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(txtDns1, InputMethodManager.SHOW_IMPLICIT)
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), true)
            } else {
                hideKeyBroad()
                lnDnsIP.visibility = View.GONE
                editor.remove(OscPrefKey.DNS_CUSTOM_ADDRESS.toString())
                editor.putBoolean(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER.toString(), false)
            }
            editor.apply()
            FirebaseAnalytics.getInstance(mContext).logEvent("Change_Custom_DNS_Setting", params)
            return
        }
        if (switchCompat == swDomain) {
            dataUtil.setBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, isChecked)
            FirebaseAnalytics.getInstance(mContext)
                .logEvent("Change_Use_Domain_To_Connect_Setting", params)
            return
        }
        if (switchCompat == swNotifySpeed) {
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
        if (switchCompat == swBlockAds) {
            Toast.makeText(
                context,
                getText(R.string.setting_apply_on_next_connection_time),
                Toast.LENGTH_SHORT
            ).show()
            dataUtil.setBooleanSetting(DataUtil.SETTING_BLOCK_ADS, isChecked)
            if (isChecked && swDns.isChecked) swDns.isChecked = false
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
