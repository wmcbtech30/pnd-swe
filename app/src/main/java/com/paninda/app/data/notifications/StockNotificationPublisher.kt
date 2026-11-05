package com.paninda.app.data.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paninda.app.MainActivity
import com.paninda.app.R
import com.paninda.app.domain.model.Product

internal interface StockAlertSink {
    fun permitted(): Boolean
    fun active(): Boolean
    fun cancel()
    fun publish(products: List<Product>, alert: Boolean): Boolean
}

internal class StockNotificationPublisher(private val context: Context) : StockAlertSink {
    companion object {
        const val CHANNEL = "stock_alerts"
        const val TAG = "paninda_stock_alerts"
        const val ID = 4101
        const val OPEN_ALERTS = "com.paninda.app.OPEN_STOCK_ALERTS"
    }
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL,
            context.getString(R.string.alert_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.alert_channel_description)
        })
    }
    override fun permitted(): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    override fun active() = manager.activeNotifications.any { it.tag == TAG && it.id == ID }
    override fun cancel() = manager.cancel(TAG, ID)
    override fun publish(products: List<Product>, alert: Boolean): Boolean {
        if (products.isEmpty() || !permitted()) return false
        val intent = Intent(context, MainActivity::class.java).putExtra(OPEN_ALERTS, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(context, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = context.resources.getQuantityString(R.plurals.alert_title, products.size, products.size)
        fun line(product: Product) = context.getString(if (product.isOutOfStock) R.string.alert_out_line else R.string.alert_low_line,
            product.name, product.quantity)
        val text = if (products.size == 1) line(products.first()) else context.getString(R.string.alert_summary,
            products.count { it.isLowStock }, products.count { it.isOutOfStock })
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        products.take(5).forEach { style.addLine(line(it)) }
        if (products.size > 5) style.setSummaryText(context.getString(R.string.alert_more, products.size - 5))
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setContentTitle(title).setContentText(text).setStyle(style).setContentIntent(tap)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(!alert).setSilent(!alert).build()
        return try { manager.notify(TAG, ID, notification); true } catch (_: SecurityException) { false }
    }
}
