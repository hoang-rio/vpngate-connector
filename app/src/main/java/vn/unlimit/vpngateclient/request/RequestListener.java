package vn.unlimit.vpngateclient.request;

/**
 * Created by dongh on 14/01/2018.
 */

public interface RequestListener {
    void onComplete(Object result);

    void onError(String error);
}
