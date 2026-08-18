package vn.unlimit.vpngate.customview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * NativeAdView that guards against a GMA Next Gen SDK bug where internal coroutines
 * call [setVisibility] and [requestLayout] from background threads (e.g. "GMA(BG)"),
 * causing CalledFromWrongThreadException.
 *
 * This wrapper intercepts those calls and posts them to the main thread when detected
 * on a non-main thread.
 */
class ThreadSafeNativeAdView : NativeAdView {

    private val mainHandler = Handler(Looper.getMainLooper())

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    override fun setVisibility(visibility: Int) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { super.setVisibility(visibility) }
        } else {
            super.setVisibility(visibility)
        }
    }

    override fun requestLayout() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { super.requestLayout() }
        } else {
            super.requestLayout()
        }
    }
}
