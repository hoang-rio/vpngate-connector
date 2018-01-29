package vn.unlimit.vpngate.request;

/**
 * Created by dongh on 14/01/2018.
 */

public interface RequestListener {
    void onSuccess(Object result);

    void onError(String error);
}
