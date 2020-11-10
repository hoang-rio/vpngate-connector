package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import vn.unlimit.vpngate.R


/**
 * A simple [Fragment] subclass.
 * Use the [BuyDataFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BuyDataFragment : Fragment(), View.OnClickListener {
    private var btnBack: ImageView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_buy_data, container, false)
        btnBack = root.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        return root
    }

    companion object {
        const val TAG = "BuyDataFragment"
    }

    override fun onClick(v: View?) {
        when(v) {
            btnBack -> findNavController().popBackStack()
        }
    }
}