package vn.unlimit.vpngate.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnectionList;

/**
 * Created by hoangnd on 1/31/2018.
 */

public class SortBottomSheetDialog extends BottomSheetDialogFragment implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private static String PROPERTY_KEY = "PROPERTY_KEY";
    private static String TYPE_KEY = "TYPE_KEY";
    View btnApply;
    private String mSortProperty;
    private int mSortType = VPNGateConnectionList.ORDER.ASC;
    private RadioGroup rdgSortProperty;
    private RadioGroup rdgSortProperty2;
    private RadioGroup rdgSortType;
    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };
    private OnApplyClickListener onApplyClickListener;

    public static SortBottomSheetDialog newInstance(String sortProperty, int sortType) {
        SortBottomSheetDialog sortBottomSheetDialog = new SortBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(PROPERTY_KEY, sortProperty);
        args.putInt(TYPE_KEY, sortType);
        sortBottomSheetDialog.setArguments(args);
        return sortBottomSheetDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSortProperty = getArguments().getString(PROPERTY_KEY);
        mSortType = getArguments().getInt(TYPE_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(getActivity());

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;

                FrameLayout bottomSheet = d.findViewById(android.support.design.R.id.design_bottom_sheet);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        //

        return dialog;
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.layout_sort, null);
        rdgSortProperty = contentView.findViewById(R.id.rdg_sort_property);
        rdgSortProperty2 = contentView.findViewById(R.id.rdg_sort_property2);
        rdgSortType = contentView.findViewById(R.id.rdg_sort_type);
        btnApply = contentView.findViewById(R.id.btn_apply);
        btnApply.setOnClickListener(this);
        fillRadioGroup();
        rdgSortProperty.setOnCheckedChangeListener(this);
        rdgSortProperty2.setOnCheckedChangeListener(this);
        rdgSortType.setOnCheckedChangeListener(this);
        dialog.setContentView(contentView);
        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
    }

    private void fillRadioGroup() {
        //Fill property radio group
        switch (mSortProperty) {
            case VPNGateConnectionList.SortProperty.SESSION:
                rdgSortProperty.check(R.id.radio_session);
                break;
            case VPNGateConnectionList.SortProperty.COUNTRY:
                rdgSortProperty.check(R.id.radio_country);
                break;
            case VPNGateConnectionList.SortProperty.SPEED:
                rdgSortProperty.check(R.id.radio_speed);
                break;
            case VPNGateConnectionList.SortProperty.PING:
                rdgSortProperty2.check(R.id.radio_ping);
                break;
            case VPNGateConnectionList.SortProperty.SCORE:
                rdgSortProperty2.check(R.id.radio_score);
                break;
            case VPNGateConnectionList.SortProperty.UPTIME:
                rdgSortProperty2.check(R.id.radio_uptime);
                break;
            default:
                break;
        }
        //Fill type radio group
        if (mSortType == VPNGateConnectionList.ORDER.DESC) {
            rdgSortType.check(R.id.radio_sort_type_desc);
        } else {
            rdgSortType.check(R.id.radio_sort_type_asc);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (radioGroup.equals(rdgSortProperty) || radioGroup.equals(rdgSortProperty2)) {
            switch (checkedId) {
                case R.id.radio_session:
                    mSortProperty = VPNGateConnectionList.SortProperty.SESSION;
                    break;
                case R.id.radio_country:
                    mSortProperty = VPNGateConnectionList.SortProperty.COUNTRY;
                    break;
                case R.id.radio_speed:
                    mSortProperty = VPNGateConnectionList.SortProperty.SPEED;
                    break;
                case R.id.radio_ping:
                    mSortProperty = VPNGateConnectionList.SortProperty.PING;
                    break;
                case R.id.radio_uptime:
                    mSortProperty = VPNGateConnectionList.SortProperty.UPTIME;
                    break;
                case R.id.radio_score:
                    mSortProperty = VPNGateConnectionList.SortProperty.SCORE;
                    break;
                default:
                    break;
            }
            rdgSortProperty.setOnCheckedChangeListener(null);
            rdgSortProperty2.setOnCheckedChangeListener(null);
            if (radioGroup.equals(rdgSortProperty)) {
                rdgSortProperty2.clearCheck();
            } else {
                rdgSortProperty.clearCheck();
            }
            rdgSortProperty.setOnCheckedChangeListener(this);
            rdgSortProperty2.setOnCheckedChangeListener(this);
        } else if (radioGroup.equals(rdgSortType)) {
            if (checkedId == R.id.radio_sort_type_asc) {
                mSortType = VPNGateConnectionList.ORDER.ASC;
            } else {
                mSortType = VPNGateConnectionList.ORDER.DESC;
            }
        }
    }

    public void setOnApplyClickListener(OnApplyClickListener _onApplyClickListener) {
        if (!"".equals(mSortProperty)) {
            onApplyClickListener = _onApplyClickListener;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnApply) && onApplyClickListener != null) {
            onApplyClickListener.onApplyClick(mSortProperty, mSortType);
        }
        dismiss();
    }

    public interface OnApplyClickListener {
        void onApplyClick(String sortProperty, int sortType);
    }
}
