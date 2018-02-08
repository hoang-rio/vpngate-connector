package vn.unlimit.vpngate.ultils;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by hoangnd on 2/8/2018.
 */

public class Analytics {
    private FirebaseAnalytics analytics;

    private Analytics(Context context) {
        analytics = FirebaseAnalytics.getInstance(context);
    }

    @NonNull
    public static Analytics with(Context context) {
        return new Analytics(context);
    }

    public void logEvent(String id, String name, String type) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type);
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}
