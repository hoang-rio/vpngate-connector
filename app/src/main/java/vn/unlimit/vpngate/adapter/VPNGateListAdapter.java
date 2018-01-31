package vn.unlimit.vpngate.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import vn.unlimit.vpngate.GlideApp;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;

/**
 * Created by hoangnd on 1/29/2018.
 */

public class VPNGateListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnScrollListener onScrollListener;
    private VPNGateConnectionList _list;
    private LayoutInflater layoutInflater;
    private int lastPosition = 0;

    public VPNGateListAdapter(Context context) {
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
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (onScrollListener != null) {
            if (position > lastPosition || position == 0) {
                onScrollListener.onScrollDown();
            } else if (position < lastPosition) {
                onScrollListener.onScrollUp();
            }
        }
        ((VHTypeVPN) viewHolder).bindViewHolder(position);
        lastPosition = position;
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.vpn_item, parent, false);
        return new VHTypeVPN(itemView);
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

        @Override
        public boolean onLongClick(View view) {
            try {
                if (onItemLongClickListener != null) {
                    VPNGateConnection item = _list.get(getAdapterPosition());
                    onItemLongClickListener.onItemLongClick(item, getAdapterPosition());
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
                    VPNGateConnection item = _list.get(getAdapterPosition());
                    onItemClickListener.onItemClick(item, getAdapterPosition());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
