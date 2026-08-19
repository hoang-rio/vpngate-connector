package vn.unlimit.vpngate.customview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * FrameLayout wrapper around [NativeAdView] that guards against a GMA Next Gen SDK bug
 * where internal coroutines call [setVisibility] and [requestLayout] from background threads
 * (e.g. "GMA(BG)"), causing CalledFromWrongThreadException.
 *
 * Since NativeAdView is final in the Next-Gen SDK, we cannot extend it directly.
 * This wrapper intercepts [requestLayout] propagation from the child NativeAdView
 * and posts it to the main thread when detected on a non-main thread.
 *
 * The inner [NativeAdView] is created programmatically. XML child views (MediaView,
 * headline, body, etc.) are automatically reparented into it via [onFinishInflate].
 */
class ThreadSafeNativeAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /** The inner NativeAdView. Access this for [NativeAdView.registerNativeAd]. */
    val nativeAdView: NativeAdView = NativeAdView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    init {
        addView(nativeAdView)
    }

    /**
     * Called after all XML children have been added to this FrameLayout.
     * Moves them into the inner [NativeAdView] so that [NativeAdView.registerNativeAd]
     * can find and use the asset views.
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        // Reparent all XML children (except our programmatic NativeAdView) into it
        val childrenToMove = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== nativeAdView) {
                childrenToMove.add(child)
            }
        }
        for (child in childrenToMove) {
            removeView(child)
            nativeAdView.addView(child)
        }
    }

    /** Delegates [NativeAdView.registerNativeAd] to the inner view. */
    fun registerNativeAd(ad: NativeAd, mediaView: MediaView?) {
        nativeAdView.registerNativeAd(ad, mediaView)
    }

    /** Delegates [NativeAdView.destroy] to the inner view. */
    fun destroy() {
        nativeAdView.destroy()
    }

    // Asset setters that delegate to the inner NativeAdView
    fun setHeadlineView(view: View?) { nativeAdView.headlineView = view }
    fun setBodyView(view: View?) { nativeAdView.bodyView = view }
    fun setCallToActionView(view: View?) { nativeAdView.callToActionView = view }
    fun setIconView(view: View?) { nativeAdView.iconView = view }
    fun setPriceView(view: View?) { nativeAdView.priceView = view }
    fun setStarRatingView(view: View?) { nativeAdView.starRatingView = view }
    fun setStoreView(view: View?) { nativeAdView.storeView = view }
    fun setAdvertiserView(view: View?) { nativeAdView.advertiserView = view }

    /**
     * Intercept requestLayout propagation from child NativeAdView.
     * When the SDK calls setVisibility/requestLayout on the NativeAdView from a background thread,
     * the requestLayout call bubbles up to this wrapper. We detect the wrong thread and post to main.
     */
    override fun requestLayout() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { super.requestLayout() }
        } else {
            super.requestLayout()
        }
    }

    override fun setVisibility(visibility: Int) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { super.setVisibility(visibility) }
        } else {
            super.setVisibility(visibility)
        }
    }
}
