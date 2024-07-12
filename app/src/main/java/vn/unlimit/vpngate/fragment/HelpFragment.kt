package vn.unlimit.vpngate.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.FragmentHelpBinding

/**
 * Created by hoangnd on 2/6/2018.
 */
class HelpFragment : Fragment(), View.OnClickListener {
    private val minContentLength = 50
    private val minNameLength = 10
    private var isValidName = false
    private var isValidContent = false
    private lateinit var binding: FragmentHelpBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelpBinding.inflate(layoutInflater)
        binding.edtName.hint = String.format(resources.getString(R.string.name_hint), minNameLength)
        binding.edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                isValidName = if (editable.length < minNameLength) {
                    binding.txtErrorName.text = String.format(
                        resources.getString(R.string.name_error_message),
                        minNameLength
                    )
                    false
                } else {
                    binding.txtErrorName.text = ""
                    true
                }
            }
        })
        binding.edtContent.hint = String.format(
            resources.getString(R.string.content_hint),
            minContentLength
        )
        binding.edtContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                isValidContent = if (editable.length < minContentLength) {
                    binding.txtErrorContent.text = String.format(
                        resources.getString(R.string.content_error_message),
                        minContentLength
                    )
                    false
                } else {
                    binding.txtErrorContent.text = ""
                    true
                }
            }
        })
        binding.btnSend.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(view: View) {
        if (view == binding.btnSend) {
            try {
                if (isValidName && isValidContent) {
                    val subject =
                        resources.getString(R.string.help_request_from) + " " + binding.edtName.text
                    val body = binding.edtContent.text.toString()
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