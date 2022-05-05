package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R

class ProfileFragment : Fragment(), View.OnClickListener {
    private var btnBack: View? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        btnBack = rootView.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        return rootView
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBack -> findNavController().popBackStack()
        }
    }
}