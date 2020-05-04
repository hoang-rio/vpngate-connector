package vn.unlimit.vpngate.fragment.paidserver.servers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import vn.unlimit.vpngate.R

class ServersFragment : Fragment() {

    private lateinit var serversViewModel: ServersViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        serversViewModel =
                ViewModelProviders.of(this).get(ServersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_server_list, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        serversViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}
