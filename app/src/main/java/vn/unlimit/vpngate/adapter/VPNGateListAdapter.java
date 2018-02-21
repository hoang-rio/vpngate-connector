package vn.unlimit.vpngate.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.startapp.android.publish.ads.banner.Banner;
import com.startapp.android.publish.ads.banner.BannerListener;

import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.ultils.DataUtil;

/**
 * Created by hoangnd on 1/29/2018.
 */

public class VPNGateListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_NORMAL = 100000;
    private static final int TYPE_ADS = 100001;
    private Context mContext;
    private Activity mActivity;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnScrollListener onScrollListener;
    private VPNGateConnectionList _list;
    private LayoutInflater layoutInflater;
    private int lastPosition = 0;
    private DataUtil mDataUtil;
    private int adsPerItem = 3;

    public VPNGateListAdapter(Activity activity, Context context, DataUtil dataUtil) {
        mActivity = activity;
        mDataUtil = dataUtil;
        mContext = context;
        _list = new VPNGateConnectionList();
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void initialize(VPNGateConnectionList vpnGateConnectionList) {
        try {
            _list.clear();
            if (vpnGateConnectionList != null) {
                _list.addAll(vpnGateConnectionList);
            }
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnItemClickListener(OnItemClickListener _onItemClickListener) {
        this.onItemClickListener = _onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener _onItemLongPressListener) {
        this.onItemLongClickListener = _onItemLongPressListener;
    }

    public void setOnScrollListener(OnScrollListener _onScrollListener) {
        this.onScrollListener = _onScrollListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (
                (position == 2 || (position > adsPerItem && (position + 1) % adsPerItem == 0))
                        && mDataUtil.hasAds()
                        && mDataUtil.getAdMobId() != null
                ) {
            return TYPE_ADS;
        }
        return TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (onScrollListener != null) {
            if (position > lastPosition || position == 0) {
                onScrollListener.onScrollDown();
            } else if (position < lastPosition) {
                onScrollListener.onScrollUp();
            }
        }
        if (viewHolder instanceof VHTypeVPN) {
            ((VHTypeVPN) viewHolder).bindViewHolder(position);
        } else if (viewHolder instanceof VHTypeAds) {
            ((VHTypeAds) viewHolder).bindViewHolder(position);
        }
        lastPosition = position;
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADS) {
            return new VHTypeAds(layoutInflater.inflate(R.layout.item_adv, parent, false));
        }
        View itemView = layoutInflater.inflate(R.layout.item_vpn, parent, false);
        return new VHTypeVPN(itemView);
    }

    private class VHTypeAds extends RecyclerView.ViewHolder {
        private View mItemView;

        VHTypeAds(View itemView) {
            super(itemView);
            mItemView = itemView;
        }

        void bindViewHolder(final int position) {
            final AdView v = new AdView(mContext);
            v.setAdSize(AdSize.SMART_BANNER);
            if (BuildConfig.DEBUG) {
                v.setAdUnitId("ca-app-pub-3940256099942544/6300978111_");
            } else {
                v.setAdUnitId(mContext.getResources().getString(R.string.admob_banner_inside_list));
            }
            float density = mContext.getResources().getDisplayMetrics().density;
            int height = Math.round(AdSize.SMART_BANNER.getHeight() * density);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, height);
            v.setLayoutParams(params);
            v.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int num) {
                    v.setVisibility(View.GONE);
                    final Banner startAppBanner = new Banner(mActivity);
                    RelativeLayout.LayoutParams bannerParameters =
                            new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                    bannerParameters.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    bannerParameters.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    startAppBanner.setBannerListener(new BannerListener() {
                        @Override
                        public void onReceiveAd(View view) {

                        }

                        @Override
                        public void onFailedToReceiveAd(View view) {
                            mItemView.setVisibility(View.GONE);
                            startAppBanner.hideBanner();
                        }

                        @Override
                        public void onClick(View view) {

                        }
                    });
                    ((RelativeLayout) mItemView.findViewById(R.id.ad_container)).addView(startAppBanner, bannerParameters);
                }
            });
            ((RelativeLayout) mItemView.findViewById(R.id.ad_container)).addView(v);
            v.loadAd(new AdRequest.Builder().build());
        }
    }

    private class VHTypeVPN extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView imgFlag;
        TextView txtCountry;
        TextView txtIp;
        TextView txtHostname;
        TextView txtScore;
        TextView txtUptime;
        TextView txtSpeed;
        TextView txtPing;
        TextView txtSession;
        TextView txtOwner;

        VHTypeVPN(View itemView) {
            super(itemView);
            imgFlag = itemView.findViewById(R.id.img_flag);
            txtCountry = itemView.findViewById(R.id.txt_country);
            txtIp = itemView.findViewById(R.id.txt_ip);
            txtHostname = itemView.findViewById(R.id.txt_hostname);
            txtScore = itemView.findViewById(R.id.txt_score);
            txtUptime = itemView.findViewById(R.id.txt_uptime);
            txtSpeed = itemView.findViewById(R.id.txt_speed);
            txtPing = itemView.findViewById(R.id.txt_ping);
            txtSession = itemView.findViewById(R.id.txt_session);
            txtOwner = itemView.findViewById(R.id.txt_owner);
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        void bindViewHolder(int position) {
            try {
                position = getRealPosition(position);
                VPNGateConnection vpnGateConnection = _list.get(position);
                GlideApp.with(mContext)
                        .load("http://www.vpngate.net/images/flags/" + vpnGateConnection.getCountryShort() + ".png")
                        .placeholder(R.color.colorOverlay)
                        .error(R.color.colorOverlay)
                        .into(imgFlag);
                txtCountry.setText(vpnGateConnection.getCountryLong());
                txtIp.setText(vpnGateConnection.getIp());
                txtHostname.setText(vpnGateConnection.getCalculateHostName());
                txtScore.setText(vpnGateConnection.getScoreAsString());
                txtUptime.setText(vpnGateConnection.getCalculateUpTime(mContext));
                txtSpeed.setText(vpnGateConnection.getCalculateSpeed());
                txtPing.setText(vpnGateConnection.getPingAsString());
                txtSession.setText(vpnGateConnection.getNumVpnSessionAsString());
                txtOwner.setText(vpnGateConnection.getOperator());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int getRealPosition(int position) {
            if (mDataUtil.hasAds() && position > adsPerItem - 1) {
                return position - (int) Math.round((double) position / adsPerItem);
            }
            return position;
        }

        @Override
        public boolean onLongClick(View view) {
            try {
                if (onItemLongClickListener != null) {
                    int clickedPost = getRealPosition(getAdapterPosition());
                    VPNGateConnection item = _list.get(clickedPost);
                    onItemLongClickListener.onItemLongClick(item, clickedPost);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void onClick(View view) {
            try {
                if (onItemClickListener != null) {
                    int clickedPost = getRealPosition(getAdapterPosition());
                    VPNGateConnection item = _list.get(clickedPost);
                    onItemClickListener.onItemClick(item, clickedPost);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
