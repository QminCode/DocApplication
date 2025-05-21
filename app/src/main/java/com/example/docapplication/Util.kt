package com.example.docapplication

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.WindowManager
import android.view.WindowInsets
import androidx.navigation.NavOptionsBuilder
import java.util.UUID

/**
 * @author: playboi_YzY
 * @date: 2025/5/7 20:14
 * @description:
 * @version:
 */
object DocumentUtils {
    /**
     * Generates a unique document ID.
     *
     * This method uses UUID (Universally Unique Identifier) to create a unique
     * string that can serve as a document ID. UUIDs are widely used for their
     * ability to generate unique identifiers across distributed systems.
     *
     * @return A unique string that can be used as a document ID.
     */
    fun generateDocumentId(): String {
        return UUID.randomUUID().toString()
    }
}

/**
 * Utility functions for common tasks.
 */
object SystemUIUtils {

    /**
     * Gets the height of the status bar in pixels.
     *
     * @param context The context.
     * @return The height of the status bar, or 0 if it cannot be determined.
     */
    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }
    /**
     * Gets the height of the navigation bar in pixels.
     *
     * @param context The context.
     * @return The height of the navigation bar, or 0 if it cannot be determined.
     */
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
    fun getActionBarHeight(context: Context): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        } else {
            0
        }
    }
    /**
     * Gets the screen height in pixels.
     *
     * @param context The context.
     * @return The height of the screen.
     */
    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val windowInsets = windowMetrics.windowInsets
            val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val display = windowManager.defaultDisplay
            val point = android.graphics.Point()
            display.getSize(point)
            point.y
        }
    }


}
// 路由工具类
sealed class Screen(val route: String) {
    object DocumentList : Screen("document_list")
    object DocumentFlow : Screen("document_flow_graph") //为父图添加一条路由
    object WebViewContainer : Screen("web_view_container/{documentId}") {
        fun createRoute(documentId: String) = "web_view_container/$documentId"
    }
    object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: String) = "document_detail/$documentId"
    }
    /**
     * 创建NavOptionsBuilder配置，用于弹出到此页面的路由。
     *
     * @param inclusive 如果为true，也把这个屏幕从堆栈中弹出。
     * @return 将NavOptionsBuilder配置为弹出的lambda.
     */
    fun popUpToInclusive(inclusive: Boolean): NavOptionsBuilder.() -> Unit {
        return {
            popUpTo(route) {
                this.inclusive = inclusive
            }
        }
    }
}