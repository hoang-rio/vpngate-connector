package vn.unlimit.vpngate.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import vn.unlimit.vpngate.R;

/**
 * Created by hoangnd on 2/1/2018.
 */

public class SpinnerInit {
    int selectedItem = -1;
    Context mContext;
    AppCompatSpinner mSpinner;
    ArrayAdapter<String> dataAdapter;
    boolean initializingList;
    OnItemSelectedListener onItemSelectedListener;
    OnItemSelectedIndexListener onItemSelectedIndexListener;
    AdapterView.OnItemSelectedListener onItemSelectedQuality = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                if (initializingList) {
                    initializingList = false;
                } else if (selectedItem != i) {
                    if (onItemSelectedListener != null) {
                        onItemSelectedListener.onItemSelected(adapterView.getItemAtPosition(i).toString());
                    }
                    if (onItemSelectedIndexListener != null) {
                        onItemSelectedIndexListener.onItemSelected(adapterView.getItemAtPosition(i).toString(), i);
                    }
                    selectedItem = i;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    public SpinnerInit(Context context, AppCompatSpinner spinner) {
        mContext = context;
        mSpinner = spinner;
        spinner.setOnItemSelectedListener(onItemSelectedQuality);
    }

    public void setStringArray(String[] array, String kbps) {
        selectedItem = 0;
        if (kbps != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(kbps)) {
                    selectedItem = i;
                    break;
                }
            }
        }
        final ArrayList<String> lst = new ArrayList<String>(Arrays.asList(array));
        if (dataAdapter == null) {
            dataAdapter = new ArrayAdapter<String>(mContext, R.layout.item_spinner, lst) {

                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                    View v = super.getDropDownView(position, null, parent);
                    if (position < lst.size()) {
                        if (position == selectedItem) {
                            v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorOverlay));
                        } else {
                            v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorTransparent));
                        }
                    }
                    return v;
                }

                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spinner, parent, false);
                    }
                    parent.setPadding(0, 0, 0, 0);
                    if (position < lst.size()) {
                        ((TextView) convertView).setText(lst.get(position));
                        parent.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    return convertView;
                }
            };
            initializingList = true;
            mSpinner.setAdapter(dataAdapter);
            mSpinner.setSelection(selectedItem);
        } else {
            dataAdapter.clear();
            dataAdapter.addAll(lst);
            dataAdapter.notifyDataSetChanged();
            mSpinner.setSelection(selectedItem);
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public void setOnItemSelectedIndexListener(OnItemSelectedIndexListener onItemSelectedIndexListener) {
        this.onItemSelectedIndexListener = onItemSelectedIndexListener;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(String name);
    }

    public interface OnItemSelectedIndexListener {
        void onItemSelected(String name, int index);
    }
}
