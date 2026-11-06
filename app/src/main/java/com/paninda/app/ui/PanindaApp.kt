package com.paninda.app.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.home.HomeScreen
import com.paninda.app.ui.sell.SellScreen
import com.paninda.app.ui.sell.RecordSaleScreen
import com.paninda.app.ui.counts.*
import com.paninda.app.ui.receiving.*
import com.paninda.app.ui.vendors.*
import com.paninda.app.ui.reports.*
import com.paninda.app.ui.inventory.*
import com.paninda.app.ui.history.*
import com.paninda.app.ui.tools.ToolsScreen
import com.paninda.app.ui.sync.SyncStatusScreen
import com.paninda.app.ui.settings.SettingsScreen
import com.paninda.app.ui.notifications.*
import com.paninda.app.ui.navigation.BottomNavigation
import com.paninda.app.ui.connection.*
import com.paninda.app.data.sync.ResolveGoogleConsent
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PanindaApp(vm: ShopViewModel, openStockAlerts: Boolean = false, onStockAlertsOpened: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val resolveGoogle = rememberGoogleConsent()
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.common_loading))
            }
        }
        state.loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.common_load_failed))
                Button(vm::load) { Text(stringResource(R.string.common_retry)) }
            }
        }
        !state.connected -> ConnectionFlow(vm, resolveGoogle)
        else -> ConnectedApp(vm, state, resolveGoogle, openStockAlerts, onStockAlertsOpened)
    }
}

@Composable
private fun ConnectedApp(vm: ShopViewModel, state: ShopState, resolveGoogle: ResolveGoogleConsent,
    openStockAlerts: Boolean, onStockAlertsOpened: () -> Unit) {
    val nav = rememberNavController()
    val stockNotificationControls = rememberStockNotificationControls(state)
    LaunchedEffect(openStockAlerts) {
        if (openStockAlerts) {
            nav.navigate("stock-alerts") { launchSingleTop = true }
            onStockAlertsOpened()
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty().ifEmpty { "home" }
    val editor = route.startsWith("product/new") || route == "vendor/new" || route.startsWith("sale/new") || route == "counts/new" || route == "receiving/new" || route.endsWith("/edit")
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun cloudAction(disconnect: Boolean = false) {
        if (vm.cloudBusy) return
        scope.launch(Dispatchers.Main.immediate) {
            try {
                if (disconnect) vm.disconnect() else vm.sync(resolveGoogle)
                if (!disconnect) snackbar.showSnackbar(context.getString(R.string.sync_done))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (disconnect) {
                    snackbar.showSnackbar(context.getString(R.string.sync_disconnect_failed))
                } else {
                    // A cloud error belongs in the existing app, never in the onboarding flow.
                    if (nav.currentDestination?.route != "sync") nav.navigate("sync") { launchSingleTop = true }
                    snackbar.showSnackbar(context.getString(R.string.sync_retry_notice))
                }
            }
        }
    }
    fun action(block: suspend () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e("PanindaLocal", "Local action failed", e)
                snackbar.showSnackbar(context.getString(R.string.common_action_failed))
            }
        }
    }
    fun root(destination: String) {
        if (destination == "home") {
            // Home is the dashboard root, never a saved stack of pages opened from it.
            if (nav.currentDestination?.route != "home" && !nav.popBackStack("home", inclusive = false)) {
                nav.navigate("home") {
                    popUpTo(nav.graph.id)
                    launchSingleTop = true
                    restoreState = false
                }
            }
            return
        }
        val target = if (destination == "inventory") "inventory?stock=ALL" else destination
        nav.navigate(target) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(containerColor = Palette.White, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (!editor) BottomNavigation(route, ::root) },
        snackbarHost = { AppSnackbar(snackbar, Modifier.padding(bottom = if (route == "home" || route.startsWith("inventory")) 80.dp else 0.dp)) },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("home") {
                HomeScreen(state, onSell = { nav.navigate("sale/new") }, onHistory = { nav.navigate("history") },
                    onReceipt = { nav.navigate("transaction/" + Uri.encode(it)) },
                    onStock = { nav.navigate("inventory?stock=" + it.name) }, onSync = { nav.navigate("sync") })
            }
            composable("sell") { SellScreen(state.products) { id -> nav.navigate("sale/new?product=" + Uri.encode(id.orEmpty())) } }
            composable("sale/new?product={product}", arguments = listOf(navArgument("product") { defaultValue = "" })) { entry ->
                RecordSaleScreen(state, vm, entry.arguments?.getString("product"), { nav.popBackStack() }) { id ->
                    nav.popBackStack()
                    nav.navigate("transaction/" + Uri.encode(id))
                }
            }
            composable("counts") { StockCountsScreen(state.counts, { nav.popBackStack() }, { nav.navigate("counts/new") },
                { nav.navigate("counts/" + Uri.encode(it)) }) }
            composable("counts/new") { NewStockCountScreen(state, vm, { nav.popBackStack() }) { id ->
                nav.popBackStack()
                nav.navigate("counts/" + Uri.encode(id))
            } }
            composable("counts/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                StockCountDetailsScreen(requireNotNull(entry.arguments?.getString("id")), state.counts, vm, { nav.popBackStack() })
            }
            composable("inventory?stock={stock}", arguments = listOf(navArgument("stock") { defaultValue = "ALL" })) { backStack ->
                val filter = runCatching { StockFilter.valueOf(backStack.arguments?.getString("stock") ?: "ALL") }.getOrDefault(StockFilter.ALL)
                InventoryScreen(state.products, filter, onNew = { nav.navigate("product/new") },
                    onProduct = { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("history") { HistoryScreen(state.transactions) { nav.navigate("transaction/" + Uri.encode(it)) } }
            composable("tools") { ToolsScreen({ nav.navigate("receiving") }, { nav.navigate("stock-alerts") }, { nav.navigate("counts") }, { nav.navigate("reports") }, { nav.navigate("vendors") }, { nav.navigate("sync") }, { nav.navigate("settings") }) }
            composable("receiving") { ReceivingScreen(state.deliveries, { nav.popBackStack() }, { nav.navigate("receiving/new") },
                { nav.navigate("receiving/" + Uri.encode(it)) }) }
            composable("receiving/new") { NewReceivingScreen(state, vm, { nav.popBackStack() }) { id ->
                nav.popBackStack()
                nav.navigate("receiving/" + Uri.encode(id))
            } }
            composable("receiving/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                ReceivingDetailsScreen(requireNotNull(entry.arguments?.getString("id")), state.deliveries, vm, { nav.popBackStack() },
                    { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("stock-alerts") { StockAlertsScreen(state.products, { nav.popBackStack() },
                { nav.navigate("product/" + Uri.encode(it)) }) }
            composable("reports") {
                InventoryReportsScreen(state, { nav.popBackStack() }, { nav.navigate("reports/movements") },
                    { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("reports/movements") {
                StockMovementReportScreen(state, { nav.popBackStack() }, { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("sync") {
                SyncStatusScreen(state, { nav.popBackStack() }, vm.cloudBusy) { cloudAction() }
            }
            composable("settings") {
                SettingsScreen(state, vm.cloudBusy, { nav.popBackStack() }, onSync = { cloudAction() },
                    onDisconnect = { cloudAction(disconnect = true) }, onReset = {
                    action {
                        vm.resetLocalData()
                        if (!nav.popBackStack("home", inclusive = false)) {
                            nav.navigate("home") { launchSingleTop = true }
                        }
                        snackbar.showSnackbar(context.getString(R.string.settings_reset_done))
                    }
                }, notificationContent = { StockNotificationSettings(stockNotificationControls) })
            }
            composable("product/new?vendor={vendor}", arguments = listOf(navArgument("vendor") { defaultValue = "" })) { backStack ->
                NewProductScreen(state.products, vm, { nav.popBackStack() }, backStack.arguments?.getString("vendor")?.takeIf { it.isNotEmpty() })
            }
            composable("vendors") {
                VendorsScreen(state.vendors, state.products, { nav.popBackStack() }, { nav.navigate("vendor/new") },
                    { nav.navigate("vendor/" + Uri.encode(it)) })
            }
            composable("vendor/new") { NewVendorScreen(state.vendors, vm) { nav.popBackStack() } }
            composable("vendor/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                EditVendorScreen(requireNotNull(entry.arguments?.getString("id")), state.vendors, vm) { nav.popBackStack() }
            }
            composable("vendor/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = requireNotNull(entry.arguments?.getString("id"))
                VendorDetailsScreen(id, state, vm, { nav.popBackStack() }, { nav.navigate("vendor/" + Uri.encode(id) + "/edit") },
                    { nav.navigate("product/new?vendor=" + Uri.encode(id)) }, { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("product/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                EditProductScreen(requireNotNull(backStack.arguments?.getString("id")), state.products, vm) { nav.popBackStack() }
            }
            composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                val id = requireNotNull(backStack.arguments?.getString("id"))
                ProductDetailsScreen(id, state.products, vm, { nav.popBackStack() },
                    onEdit = { nav.navigate("product/" + Uri.encode(id) + "/edit") })
            }
            composable("transaction/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                TransactionDetailsScreen(requireNotNull(backStack.arguments?.getString("id")), state.transactions, vm) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun AppSnackbar(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(data, containerColor = Palette.Icon, contentColor = Palette.White,
            actionColor = Palette.White, dismissActionContentColor = Palette.White)
    }
}
