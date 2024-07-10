package vn.unlimit.vpngate.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import vn.unlimit.vpngate.R
import java.util.Arrays

/**
 * Created by hoangnd on 2/1/2018.
 */
class SpinnerInit(var mContext: Context?, var mSpinner: AppCompatSpinner) {
    private val TAG = "SpinnerInit"
    var selectedItem: Int = -1
    private var dataAdapter: ArrayAdapter<String>? = null
    var initializingList: Boolean = false
    var onItemSelectedListener: OnItemSelectedListener? = null
    var onItemSelectedIndexListener: OnItemSelectedIndexListener? = null
    private var onItemSelectedQuality: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                try {
                    if (initializingList) {
                        initializingList = false
                    } else if (selectedItem != i) {
                        if (onItemSelectedListener != null) {
                            onItemSelectedListener!!.onItemSelected(
                                adapterView.getItemAtPosition(i).toString()
                            )
                        }
                        if (onItemSelectedIndexListener != null) {
                            onItemSelectedIndexListener!!.onItemSelected(
                                adapterView.getItemAtPosition(
                                    i
                                ).toString(), i
                            )
                        }
                        selectedItem = i
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Got exception whenonItemSelected", e)
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        }

    init {
        mSpinner.onItemSelectedListener = onItemSelectedQuality
    }

    fun setStringArray(array: Array<String>, kbps: String?) {
        selectedItem = 0
        if (kbps != null) {
            for (i in array.indices) {
                if (array[i] == kbps) {
                    selectedItem = i
                    break
                }
            }
        }
        val lst = ArrayList(listOf(*array))
        if (dataAdapter == null && mContext != null) {
            dataAdapter = object : ArrayAdapter<String>(mContext!!, R.layout.item_spinner, lst) {
                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View? {
                    val v = super.getDropDownView(position, null, parent)
                    if (position < lst.size) {
                        if (position == selectedItem) {
                            v.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.colorOverlay
                                )
                            )
                        } else {
                            v.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.colorTransparent
                                )
                            )
                        }
                    }
                    return v
                }

                override fun getView(position: Int, inConvertView: View?, parent: ViewGroup): View {
                    var convertView = inConvertView
                    if (convertView == null) {
                        convertView = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_spinner, parent, false)
                    }
                    parent.setPadding(0, 0, 0, 0)
                    if (position < lst.size) {
                        (convertView as TextView?)!!.text = lst[position]
                        parent.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    return convertView!!
                }
            }
            initializingList = true
            mSpinner.adapter = dataAdapter
            mSpinner.setSelection(selectedItem)
        } else {
            dataAdapter!!.clear()
            dataAdapter!!.addAll(lst)
            dataAdapter!!.notifyDataSetChanged()
            mSpinner.setSelection(selectedItem)
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(name: String?)
    }

    interface OnItemSelectedIndexListener {
        fun onItemSelected(name: String?, index: Int)
    }
}
