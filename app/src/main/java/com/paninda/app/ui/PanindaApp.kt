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
import com.paninda.app.ui.inventory.*
import com.paninda.app.ui.history.*
import com.paninda.app.ui.tools.ToolsScreen
import com.paninda.app.ui.sync.SyncStatusScreen
import com.paninda.app.ui.settings.SettingsScreen
import com.paninda.app.ui.navigation.BottomNavigation
import com.paninda.app.ui.onboarding.OnboardingScreen
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PanindaApp(vm: ShopViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
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
        !state.connected -> {
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val errorText = stringResource(R.string.common_action_failed)
            var connecting by remember { mutableStateOf(false) }
            Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { AppSnackbar(snackbar) }) { padding ->
                OnboardingScreen(Modifier.padding(padding)) {
                    if (!connecting) {
                        connecting = true
                        scope.launch(Dispatchers.Main.immediate) {
                            try { vm.connectLocal() }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { snackbar.showSnackbar(errorText) }
                            finally { connecting = false }
                        }
                    }
                }
            }
        }
        else -> ConnectedApp(vm, state)
    }
}

@Composable
private fun ConnectedApp(vm: ShopViewModel, state: ShopState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty().ifEmpty { "home" }
    val editor = route == "product/new" || route.endsWith("/edit")
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun message(id: Int) { scope.launch(Dispatchers.Main.immediate) { snackbar.showSnackbar(context.getString(id)) } }
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
                HomeScreen(state, onSell = { root("sell") }, onHistory = { nav.navigate("history") },
                    onReceipt = { nav.navigate("transaction/" + Uri.encode(it)) },
                    onStock = { nav.navigate("inventory?stock=" + it.name) }, onSync = { nav.navigate("sync") })
            }
            composable("sell") { SellScreen(state.products) { message(R.string.sell_pending) } }
            composable("inventory?stock={stock}", arguments = listOf(navArgument("stock") { defaultValue = "ALL" })) { backStack ->
                val filter = runCatching { StockFilter.valueOf(backStack.arguments?.getString("stock") ?: "ALL") }.getOrDefault(StockFilter.ALL)
                InventoryScreen(state.products, filter, onNew = { nav.navigate("product/new") },
                    onProduct = { nav.navigate("product/" + Uri.encode(it)) })
            }
            composable("history") { HistoryScreen(state.transactions) { nav.navigate("transaction/" + Uri.encode(it)) } }
            composable("tools") { ToolsScreen({ nav.navigate("sync") }, { nav.navigate("settings") }, ::message) }
            composable("sync") {
                SyncStatusScreen(state, { nav.popBackStack() }) {
                    action { vm.checkOffline(); snackbar.showSnackbar(context.getString(R.string.sync_unavailable)) }
                }
            }
            composable("settings") {
                SettingsScreen({ nav.popBackStack() }, onDisconnect = { action { vm.disconnect() } }, onReset = {
                    action {
                        vm.resetLocalData()
                        if (!nav.popBackStack("home", inclusive = false)) {
                            nav.navigate("home") { launchSingleTop = true }
                        }
                        snackbar.showSnackbar(context.getString(R.string.settings_reset_done))
                    }
                })
            }
            composable("product/new") { NewProductScreen(state.products, vm) { nav.popBackStack() } }
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
