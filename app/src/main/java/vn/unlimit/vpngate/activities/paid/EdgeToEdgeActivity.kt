package vn.unlimit.vpngate.activities.paid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import vn.unlimit.vpngate.R

open class EdgeToEdgeActivity: AppCompatActivity() {
    lateinit var viewBinding: ViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = viewBinding.root
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            window.decorView.setBackgroundColor(resources.getColor(R.color.colorPaidServer, theme))
            v.updatePadding(
                left = initialLeft + insets.left,
                top = initialTop,
                right = initialRight + insets.right,
                bottom = initialBottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }
}