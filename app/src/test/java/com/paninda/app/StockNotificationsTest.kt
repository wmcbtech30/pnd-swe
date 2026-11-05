package com.paninda.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.notifications.*
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.data.sync.*
import com.paninda.app.domain.model.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StockNotificationsTest {
    private val context get() = RuntimeEnvironment.getApplication() as PanindaApplication

    @Test fun publisherChecksPermissionUsesMonochromeIconAndOpensStockAlerts() {
        val publisher = StockNotificationPublisher(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val product = TestFixtures.snapshot().products.single().copy(quantity = 2)
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(publisher.publish(listOf(product), true))
        assertFalse(publisher.active())
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(publisher.publish(listOf(product), true))
        val notification = manager.activeNotifications.single().notification
        assertEquals(R.drawable.ic_notification_monochrome, notification.smallIcon.resId)
        assertTrue(notification.contentIntent.isImmutable)
        assertTrue(shadowOf(notification.contentIntent).savedIntent.getBooleanExtra(StockNotificationPublisher.OPEN_ALERTS, false))
        assertEquals(StockNotificationPublisher.CHANNEL, notification.channelId)
        publisher.cancel()
        assertFalse(publisher.active())
    }

    @Test fun committedSalesCountsSyncAndSettingsSharePersistentDeliveryState() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PanindaDatabase::class.java).build()
        val repo = ShopRepository(db)
        val sink = FakeSink()
        var controller = StockNotifications(context, db, sink)
        val store = StockAlertStore(context.getSharedPreferences("stock_notifications", Context.MODE_PRIVATE))
        try {
            repo.initialize()
            repo.saveProduct(null, ProductDraft("Notebook", "ALERT-001", "Vendor", "Stationery", "9.50", "12", "3"))
            TestFixtures.connect(repo)
            controller.markPrompted()
            assertFalse(controller.status.value.requested)
            controller.start()
            suspend fun product() = repo.snapshot().products.single()
            suspend fun count(quantity: Int) {
                val p = product()
                assertTrue(repo.recordCount(CountDraft(UUID.randomUUID().toString(), "Recorder", "Physical check",
                    listOf(CountEntryDraft(p.id, p.quantity, p.version, quantity.toString())))) is RecordResult.Saved)
            }
            val p = product()
            repo.recordSale(SaleDraft(UUID.randomUUID().toString(), "Recorder", listOf(SaleLineDraft(p.id, "9", "9.50"))))
            until { sink.alerts.get() == 1 }
            count(2)
            until { sink.latest.singleOrNull()?.quantity == 2 }
            assertEquals(1, sink.alerts.get())
            repo.recordSale(SaleDraft(UUID.randomUUID().toString(), "Recorder", listOf(SaleLineDraft(p.id, "2", "9.50"))))
            until { sink.alerts.get() == 2 && store.read()[p.id]?.out == true }
            controller.stop()
            controller = StockNotifications(context, db, sink)
            val quietBefore = sink.quiet.get()
            controller.start()
            until { sink.quiet.get() > quietBefore }
            assertEquals(2, sink.alerts.get())
            count(5)
            until { !sink.visible && store.read().isEmpty() }
            count(3)
            until { sink.alerts.get() == 3 }
            sink.allowed = false; controller.refresh()
            count(5)
            until { store.read().isEmpty() }
            val snapshot = repo.snapshot()
            val remote = snapshot.copy(products = snapshot.products.map { it.copy(quantity = 1, version = it.version + 1) },
                movements = snapshot.movements + StockMovement(UUID.randomUUID().toString(), p.id, -4, "ADJUSTMENT", "remote", TestFixtures.now, "Remote correction"))
            repo.completeSync(requireNotNull(repo.binding()), remote, SnapshotFingerprint.of(snapshot), download = true)
            until { store.read()[p.id] == StockAlertLatch() }
            assertEquals(3, sink.alerts.get())
            controller.setEnabled(false)
            sink.allowed = true; controller.refresh()
            assertFalse(controller.status.value.enabled)
            controller.setEnabled(true)
            until { sink.alerts.get() == 4 }
            repo.disconnect()
            until { !sink.visible }
            assertEquals(4, sink.alerts.get())
            repo.resetLocalData()
            until { store.read().isEmpty() }
        } finally {
            controller.stop()
            db.close()
        }
    }

    private suspend fun until(condition: () -> Boolean) = withTimeout(8_000) { while (!condition()) delay(10) }
    private class FakeSink : StockAlertSink {
        @Volatile var allowed = true
        @Volatile var visible = false
        @Volatile var latest: List<Product> = emptyList()
        val alerts = AtomicInteger()
        val quiet = AtomicInteger()
        override fun permitted() = allowed
        override fun active() = visible
        override fun cancel() { visible = false }
        override fun publish(products: List<Product>, alert: Boolean): Boolean {
            if (!allowed) return false
            latest = products; visible = true
            if (alert) alerts.incrementAndGet() else quiet.incrementAndGet()
            return true
        }
    }
}
