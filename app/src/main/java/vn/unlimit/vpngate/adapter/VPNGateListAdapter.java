package vn.unlimit.vpngate.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import vn.unlimit.vpngate.App;
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
    private final Context mContext;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnScrollListener onScrollListener;
    private final VPNGateConnectionList _list;
    private final LayoutInflater layoutInflater;
    private int lastPosition = 0;

    public VPNGateListAdapter(Context context) {
        mContext = context;
        _list = new VPNGateConnectionList();
        this.layoutInflater = LayoutInflater.from(context);
    }

    @SuppressLint("NotifyDataSetChanged")
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
        return TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @SuppressLint("RecyclerView") int position) {
        if (onScrollListener != null) {
            if (position > lastPosition || position == 0) {
                onScrollListener.onScrollDown();
            } else if (position < lastPosition) {
                onScrollListener.onScrollUp();
            }
        }
        if (viewHolder instanceof VHTypeVPN) {
            ((VHTypeVPN) viewHolder).bindViewHolder(position);
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
        return new VHTypeVPN(layoutInflater.inflate(R.layout.item_vpn, parent, false));
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
        View lnTCP;
        TextView txtTCP;
        View lnUDP;
        TextView txtUDP;
        View lnL2TP;
        View lnSSTP;

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
            lnTCP = itemView.findViewById(R.id.ln_tcp);
            txtTCP = itemView.findViewById(R.id.txt_tcp_port);
            lnUDP = itemView.findViewById(R.id.ln_udp);
            txtUDP = itemView.findViewById(R.id.txt_udp_port);
            lnL2TP = itemView.findViewById(R.id.ln_l2tp);
            lnSSTP = itemView.findViewById(R.id.ln_sstp);
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        void bindViewHolder(int position) {
            try {
                VPNGateConnection vpnGateConnection = _list.get(getRealPosition(position));
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
                DataUtil dataUtil = App.getInstance().getDataUtil();
                boolean isIncludeUdp = dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true);
                if (!isIncludeUdp || vpnGateConnection.getTcpPort() == 0) {
                    lnTCP.setVisibility(View.GONE);
                } else {
                    lnTCP.setVisibility(View.VISIBLE);
                    txtTCP.setText(String.valueOf(vpnGateConnection.getTcpPort()));
                }
                if (!isIncludeUdp || vpnGateConnection.getUdpPort() == 0) {
                    lnUDP.setVisibility(View.GONE);
                } else {
                    lnUDP.setVisibility(View.VISIBLE);
                    txtUDP.setText(String.valueOf(vpnGateConnection.getUdpPort()));
                }
                lnL2TP.setVisibility(vpnGateConnection.isL2TPSupport() ? View.VISIBLE : View.GONE);
                lnSSTP.setVisibility(vpnGateConnection.isSSTPSupport() ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int getRealPosition(int position) {
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
