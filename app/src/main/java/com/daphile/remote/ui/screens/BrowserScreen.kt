package com.daphile.remote.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LoadingState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
    navController: NavController,
    source: String,
    pluginCmd: String,
    itemId: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val viewModel: RadioViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<BrowserItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    fun runCommand(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (error: Throwable) {
                snackbar.showSnackbar(friendlyErrorMessage(error))
            }
        }
    }

    LaunchedEffect(source, pluginCmd, itemId, reloadKey) {
        loading = true
        errorText = null
        try {
            items = when (source) {
                "favorites" -> viewModel.fetchFavorites(itemId.takeIf { it != "0" })
                else -> viewModel.fetchPluginItems(pluginCmd, itemId)
            }
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = title, onBack = { navController.popBackStack() })
            when {
                loading -> LoadingState()
                errorText != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                }
                items.isEmpty() -> EmptyState(
                    icon = if (source == "favorites") Icons.Rounded.FavoriteBorder else Icons.Rounded.PlayArrow,
                    title = "这里还没有内容",
                    body = "请在 Daphile 网页端检查该插件或目录"
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    )
                ) {
                    items(items = items, key = { it.id }) { item ->
                        BrowserItemRow(
                            item = item,
                            iconUrl = resolveBrowserIcon(server?.baseUrl.orEmpty(), item.icon),
                            fallbackIcon = Icons.Rounded.FavoriteBorder,
                            onClick = {
                                if (item.hasItems && (item.type == "link" || !item.isAudio)) {
                                    navController.navigate(
                                        Routes.browser(source, pluginCmd, item.id, item.name)
                                    )
                                } else {
                                    runCommand {
                                        viewModel.playItem(
                                            pluginCmd.takeIf { source != "favorites" },
                                            item
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

private fun resolveBrowserIcon(baseUrl: String, icon: String): String? {
    if (icon.isBlank()) return null
    if (icon.startsWith("http://") || icon.startsWith("https://")) return icon
    return "$baseUrl/${icon.trimStart('/')}"
}
