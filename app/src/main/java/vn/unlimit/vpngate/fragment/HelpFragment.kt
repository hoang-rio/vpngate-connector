package vn.unlimit.vpngate.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.R

/**
 * Created by hoangnd on 2/6/2018.
 */
class HelpFragment : Fragment(), View.OnClickListener {
    private var edtName: EditText? = null
    private var edtContent: EditText? = null
    private var btnSend: Button? = null
    private var txtErrorName: TextView? = null
    private var txtErrorContent: TextView? = null
    private val minContentLength = 50
    private val minNameLength = 10
    private var isValidName = false
    private var isValidContent = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_help, container, false)
        txtErrorName = rootView.findViewById(R.id.txt_error_name)
        txtErrorContent = rootView.findViewById(R.id.txt_error_content)
        edtName = rootView.findViewById(R.id.edt_name)
        edtName?.hint = String.format(resources.getString(R.string.name_hint), minNameLength)
        edtName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun afterTextChanged(editable: Editable) {
                isValidName = if (editable.length < minNameLength) {
                    txtErrorName?.text = String.format(
                        resources.getString(R.string.name_error_message),
                        minNameLength
                    )
                    false
                } else {
                    txtErrorName?.text = ""
                    true
                }
            }
        })
        edtContent = rootView.findViewById(R.id.edt_content)
        edtContent?.hint = String.format(
            resources.getString(R.string.content_hint),
            minContentLength
        )
        edtContent?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun afterTextChanged(editable: Editable) {
                isValidContent = if (editable.length < minContentLength) {
                    txtErrorContent?.text = String.format(
                        resources.getString(R.string.content_error_message),
                        minContentLength
                    )
                    false
                } else {
                    txtErrorContent?.setText("")
                    true
                }
            }
        })
        btnSend = rootView.findViewById(R.id.btn_send)
        btnSend?.setOnClickListener(this)
        return rootView
    }

    override fun onClick(view: View) {
        if (view == btnSend) {
            try {
                if (isValidName && isValidContent) {
                    val subject =
                        resources.getString(R.string.help_request_from) + " " + edtName!!.text
                    val body = edtContent!!.text.toString()
                    val mailIntent = Intent(Intent.ACTION_SENDTO)
                    mailIntent.data = Uri.parse(
                        String.format(
                            "mailto:support@vpngate-connector.com?subject=%s&body=%s",
                            subject,
                            body
                        )
                    )
                    startActivity(
                        Intent.createChooser(
                            mailIntent,
                            resources.getString(R.string.send_email)
                        )
                    )
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.email_error_fix),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}