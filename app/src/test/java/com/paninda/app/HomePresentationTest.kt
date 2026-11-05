package com.paninda.app

import com.paninda.app.domain.model.*
import com.paninda.app.ui.home.Microcopies
import com.paninda.app.ui.home.MicrocopyRotation
import org.junit.Assert.*
import com.paninda.app.data.sync.SyncFailures
import com.paninda.app.data.sync.OfflineException
import com.paninda.app.adapters.google.GoogleApiException
import java.net.SocketTimeoutException
import org.junit.Test

class HomePresentationTest {
    @Test fun syncGetterRetainsLastSuccessWithoutHidingPendingOrFailedWork() {
        val completed = ShopState(syncStatus = "synced", lastCheckedAt = 123456L)
        assertEquals(SyncPhase.SYNCED, completed.syncSummary.phase)
        assertEquals(SyncPhase.PENDING, completed.copy(pendingCount = 2).syncSummary.phase)
        val failed = completed.copy(syncStatus = "failed", pendingCount = 2).syncSummary
        assertEquals(SyncPhase.FAILED, failed.phase)
        assertEquals(123456L, failed.lastSuccessfulAt)
        assertNull(ShopState().syncSummary.lastSuccessfulAt)
        assertEquals(SyncPhase.READY, ShopState(syncStatus = "synced").syncSummary.phase)
        val offline = completed.copy(networkStatus = NetworkStatus.OFFLINE).syncSummary
        assertEquals(SyncPhase.OFFLINE, offline.phase)
        assertEquals(123456L, offline.lastSuccessfulAt)
        assertEquals(SyncPhase.UNREACHABLE, completed.copy(syncStatus = "unreachable", networkStatus = NetworkStatus.ONLINE).syncSummary.phase)
        assertEquals(SyncPhase.PENDING, completed.copy(syncStatus = "offline", pendingCount = 1,
            networkStatus = NetworkStatus.ONLINE).syncSummary.phase)
    }

    @Test fun offlineAndUnreachableFailuresAreDifferentRetryableStatuses() {
        assertEquals("offline", SyncFailures.status(OfflineException(), NetworkStatus.ONLINE))
        assertEquals("offline", SyncFailures.status(SocketTimeoutException(), NetworkStatus.OFFLINE))
        assertEquals("unreachable", SyncFailures.status(SocketTimeoutException(), NetworkStatus.ONLINE))
        assertEquals("unreachable", SyncFailures.status(GoogleApiException(403), NetworkStatus.ONLINE))
        assertEquals("unreachable", SyncFailures.status(GoogleApiException(404), NetworkStatus.ONLINE))
        assertEquals("access", SyncFailures.status(GoogleApiException(401), NetworkStatus.ONLINE))
    }

    @Test fun greetingsRespectTimeBoundariesAndRotateOnlyOncePerPeriodPerLaunch() {
        val expected = mapOf(0 to Microcopies.Period.LATE_NIGHT, 4 to Microcopies.Period.LATE_NIGHT,
            5 to Microcopies.Period.MORNING, 10 to Microcopies.Period.MORNING,
            11 to Microcopies.Period.LUNCH, 12 to Microcopies.Period.LUNCH,
            13 to Microcopies.Period.AFTERNOON, 16 to Microcopies.Period.AFTERNOON,
            17 to Microcopies.Period.EVENING, 21 to Microcopies.Period.EVENING,
            22 to Microcopies.Period.LATE_NIGHT, 23 to Microcopies.Period.LATE_NIGHT)
        expected.forEach { (hour, period) -> assertEquals(period, Microcopies.period(hour)) }
        val saved = mutableMapOf<Microcopies.Period, Int>()
        fun launch() = MicrocopyRotation({ saved[it] ?: -1 }, { period, index -> saved[period] = index })
        val first = launch()
        val morning = Microcopies.pools.getValue(Microcopies.Period.MORNING)
        assertEquals(morning[0], first.greeting(5))
        assertEquals(morning[0], first.greeting(10))
        first.greeting(12)
        assertEquals(morning[0], first.greeting(8))
        assertEquals(morning[1], launch().greeting(8))
        repeat(morning.size - 2) { launch().greeting(8) }
        assertEquals(morning[0], launch().greeting(8))
    }
}
