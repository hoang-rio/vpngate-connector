package vn.unlimit.vpngate.customview

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * NestedScrollView that guards against a framework bug where
 * [NestedScrollView.onSizeChanged] calls [android.view.ViewGroup.offsetDescendantRectToMyCoords]
 * on the currently-focused descendant, which crashes with
 * "IllegalArgumentException: parameter must be a descendant of this view" if that view was
 * detached during the same layout pass (e.g. window resize while the view hierarchy is being
 * mutated).
 */
class SafeNestedScrollView : NestedScrollView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        try {
            super.onSizeChanged(w, h, oldw, oldh)
        } catch (ignored: IllegalArgumentException) {
        }
    }
}
