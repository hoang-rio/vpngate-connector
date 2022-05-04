package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.viewmodels.UserViewModel


class ChangePassFragment : Fragment(), View.OnClickListener {
    private var btnBack: View? = null
    private var userViewModel: UserViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_change_pass, container, false)
        btnBack = rootView.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    override fun onClick(v: View?) {
        findNavController().popBackStack()
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
    }
}