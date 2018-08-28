package vn.unlimit.vpngate.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.utils.DataUtil;

/**
 * Created by hoangnd on 1/29/2018.
 */

public class VPNGateListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_NORMAL = 100000;
    private static final int TYPE_ADS = 100001;
    private Context mContext;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnScrollListener onScrollListener;
    private VPNGateConnectionList _list;
    private LayoutInflater layoutInflater;
    private int lastPosition = 0;
    private DataUtil mDataUtil;
    private int adsPerItem = 3;

    public VPNGateListAdapter(Context context, DataUtil dataUtil) {
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
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
            ((VHTypeAds) viewHolder).bindViewHolder();
        }
        lastPosition = position;
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADS) {
            return new VHTypeAds(layoutInflater.inflate(R.layout.item_adv, parent, false));
        }
        return new VHTypeVPN(layoutInflater.inflate(R.layout.item_vpn, parent, false));
    }

    private class VHTypeAds extends RecyclerView.ViewHolder {

        VHTypeAds(View itemView) {
            super(itemView);
        }

        void bindViewHolder() {
            try {
                final LinearLayout adContainer = itemView.findViewById(R.id.ad_container);
                if (App.isAdMobPrimary()) {
                    final AdView v = new AdView(mContext);
                    v.setAdSize(AdSize.SMART_BANNER);
                    if (BuildConfig.DEBUG) {
                        v.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                    } else {
                        v.setAdUnitId(mContext.getResources().getString(R.string.admob_banner_inside_list));
                    }
                    v.setAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            v.setVisibility(View.GONE);
                            final com.facebook.ads.AdView fAdView = new com.facebook.ads.AdView(mContext, mContext.getString(R.string.fan_banner_inside_list), com.facebook.ads.AdSize.BANNER_HEIGHT_90);
                            fAdView.setAdListener(new com.facebook.ads.AdListener() {
                                @Override
                                public void onError(Ad ad, AdError adError) {
                                    itemView.setVisibility(View.GONE);
                                    fAdView.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAdLoaded(Ad ad) {

                                }

                                @Override
                                public void onAdClicked(Ad ad) {

                                }

                                @Override
                                public void onLoggingImpression(Ad ad) {

                                }
                            });
                            adContainer.removeAllViews();
                            adContainer.addView(fAdView);
                            fAdView.loadAd();
                        }
                    });
                    adContainer.removeAllViews();
                    adContainer.addView(v);
                    v.loadAd(new AdRequest.Builder().build());
                } else {
                    final com.facebook.ads.AdView fAdView = new com.facebook.ads.AdView(mContext, mContext.getString(R.string.fan_banner_inside_list), com.facebook.ads.AdSize.BANNER_HEIGHT_90);
                    fAdView.setAdListener(new com.facebook.ads.AdListener() {
                        @Override
                        public void onError(Ad ad, AdError adError) {
                            fAdView.setVisibility(View.GONE);
                            final AdView v = new AdView(mContext);
                            v.setAdSize(AdSize.LARGE_BANNER);
                            if (BuildConfig.DEBUG) {
                                v.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                            } else {
                                v.setAdUnitId(mContext.getResources().getString(R.string.admob_banner_inside_list));
                            }
                            float density = mContext.getResources().getDisplayMetrics().density;
                            int height = Math.round(AdSize.SMART_BANNER.getHeight() * density);
                            AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, height);
                            v.setLayoutParams(params);
                            v.setAdListener(new AdListener() {
                                @Override
                                public void onAdFailedToLoad(int errorCode) {
                                    itemView.setVisibility(View.GONE);
                                    v.setVisibility(View.GONE);
                                }
                            });
                            adContainer.removeAllViews();
                            adContainer.addView(v);
                            v.loadAd(new AdRequest.Builder().build());
                        }

                        @Override
                        public void onAdLoaded(Ad ad) {

                        }

                        @Override
                        public void onAdClicked(Ad ad) {

                        }

                        @Override
                        public void onLoggingImpression(Ad ad) {

                        }
                    });
                    adContainer.removeAllViews();
                    adContainer.addView(fAdView);
                    fAdView.loadAd();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
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
                        .load(App.getInstance().getDataUtil().getBaseUrl() + "/images/flags/" + vpnGateConnection.getCountryShort() + ".png")
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
