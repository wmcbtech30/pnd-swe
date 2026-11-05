package com.paninda.app

import androidx.compose.ui.test.*
import android.net.ConnectivityManager
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import android.net.NetworkCapabilities
import org.robolectric.Shadows
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.paninda.app.domain.model.ShopSnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** One behavior smoke check. No layout assertions, screenshots, or device-size matrix. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PrototypeUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as PanindaApplication
    private val repo get() = app.repository
    @After fun close() { app.database.close() }

    @Test fun cleanStartProductPersistenceAndReset() {
        awaitText("Choose a data source to get started")
        assertEquals(ShopSnapshot(), runBlocking { repo.snapshot() })
        connect()
        compose.onNodeWithText("View all").performSemanticsAction(SemanticsActions.OnClick) { it() }
        awaitText("Search receipt number")
        compose.onNodeWithContentDescription("Home").performClick()
        awaitHome()
        compose.onNodeWithText("Search receipt number").assertDoesNotExist()
        compose.onNodeWithContentDescription("History").performClick()
        awaitText("Search receipt number")
        compose.onNodeWithContentDescription("Home").performClick()
        awaitHome()
        compose.onNodeWithContentDescription("Check sync status").performClick()
        val manager = compose.activity.getSystemService(ConnectivityManager::class.java)
        val callbacks = Shadows.shadowOf(manager).networkCallbacks.toList()
        val network = ShadowNetwork.newInstance(404)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        compose.runOnIdle {
            callbacks.forEach {
                it.onAvailable(network)
                it.onCapabilitiesChanged(network, capabilities)
            }
        }
        awaitText("File synced")
        compose.runOnIdle { callbacks.forEach { it.onLost(network) } }
        awaitText("You’re offline!")
        compose.onNodeWithText("Sync Now").performScrollTo().performClick()
        awaitText("Sync did not complete. Local work is preserved; check Sync Status and retry.")
        compose.onNodeWithText("Choose a data source to get started").assertDoesNotExist()
        assertTrue(runBlocking { repo.state.first().connected })
        compose.onNodeWithContentDescription("Home").performClick()
        awaitHome()
        compose.onNodeWithContentDescription("Inventory").performClick()
        compose.onNodeWithText("New Product").performClick()
        fill("Product name", "Test Notebook")
        fill("SKU", "TEST-UI-001")
        fill("Vendor name", "Test Vendor")
        fill("Category", "Stationery")
        fill("Selling price (Php)", "9.50")
        fill("Opening quantity", "12")
        fill("Low-stock threshold", "3")
        compose.onNodeWithText("Save product").performClick()
        awaitText("Test Notebook")
        compose.activityRule.scenario.recreate()
        awaitText("Test Notebook")
        assertEquals(12, runBlocking { repo.snapshot().products.single().quantity })
        settings()
        compose.onNodeWithText("Reset local data").performScrollTo().performClick()
        compose.onNodeWithText("Delete local data").performClick()
        awaitText("0.00")
        assertEquals(ShopSnapshot(), runBlocking { repo.snapshot() })
    }

    private fun awaitHome() {
        compose.waitUntil(8_000) {
            compose.onAllNodesWithContentDescription("Check sync status").fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun connect() {
        // Fixture binding only: this smoke check must never launch real Google consent.
        runBlocking { TestFixtures.connect(repo) }
        awaitHome()
    }
    private fun settings() {
        compose.onNodeWithContentDescription("Tools").performClick()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        awaitText("MANAGE YOUR DATA")
    }
    private fun fill(label: String, value: String) {
        val field = hasSetTextAction() and hasText(label)
        compose.onNodeWithTag("product-editor-list").performScrollToNode(field)
        compose.onNode(field).performTextReplacement(value)
    }
    private fun awaitText(text: String) {
        try {
            compose.waitUntil(8_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            System.err.println(compose.onRoot().printToString().take(10000))
            org.robolectric.shadows.ShadowLog.getLogsForTag("PanindaLocal").forEach { log ->
                log.throwable?.printStackTrace()
            }
            throw e
        }
    }
}
