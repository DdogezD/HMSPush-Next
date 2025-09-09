@file:OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package one.yufz.hmspush.app.fake

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.yufz.hmspush.R
import one.yufz.hmspush.app.LocalNavHostController
import one.yufz.hmspush.app.widget.SearchBar

@Composable
fun FakeDeviceScreen(viewModel: FakeDeviceViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navHostController = LocalNavHostController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.fake_device))
                    }
                },
                actions = {
                    var searching by remember { mutableStateOf(false) }

                    if (!searching) {
                        IconButton(onClick = { searching = true }) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                        }
                    } else {
                        SearchBar(
                            searchText = uiState.filterKeywords,
                            placeholderText = stringResource(id = R.string.menu_search),
                            onNavigateBack = { searching = false },
                            onSearchTextChanged = { viewModel.filter(it) }
                        )
                    }
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            items(uiState.filteredConfigList) { config ->
                AppCard(config, viewModel) { enabled ->
                    viewModel.update(config.copy(enabled = enabled))
                }
            }
        }
    }
}

@Composable
private fun AppCard(config: AppConfig, viewModel: FakeDeviceViewModel, onCheckedChange: (Boolean) -> Unit) {
    var showProcessDialog by remember { mutableStateOf(false) }
    var newProcess by remember { mutableStateOf("") }

    val drawable by loadAppIcon(LocalContext.current, config.packageName)
    val drawablePainter = rememberDrawablePainter(drawable)

    Column {
        ListItem(
            leadingContent = { Icon(painter = drawablePainter, contentDescription = "icon", tint = Color.Unspecified, modifier = Modifier.size(56.dp).padding(8.dp)) },
            headlineContent = {
                Text(
                    text = config.name,
                    style = if (config.skipBuild) LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)
                    else LocalTextStyle.current
                )
            },
            supportingContent = { Text(config.packageName) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = config.enabled, onCheckedChange = onCheckedChange)
                    IconButton(onClick = { showProcessDialog = true }) { Icon(Icons.Default.MoreVert, contentDescription = "showProcess") }
                }
            }
        )

        if (showProcessDialog) {
            val processes by rememberUpdatedState(config.processes)

            AlertDialog(
                onDismissRequest = { showProcessDialog = false },
                title = {
                    Text(
                        text = config.name,
                        style = if (config.skipBuild) LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)
                        else LocalTextStyle.current
                    )
                },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Text("伪装厂商与品牌", modifier = Modifier.weight(1f))
                            Switch(
                                checked = !config.skipBuild,
                                onCheckedChange = { enabled ->
                                    viewModel.setSkipBuild(config, !enabled)
                                }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 2.dp))
                        if (processes.isEmpty()) {
                            Text("设备伪装默认对全部进程生效")
                        } else {
                            processes.forEach { process ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(".$process", modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.removeProcess(config, process) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = newProcess,
                            onValueChange = { newProcess = it },
                            label = { Text("添加进程") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newProcess.isNotBlank()) {
                                        viewModel.addProcess(config, newProcess.trim())
                                        newProcess = ""
                                    }
                                }
                            )
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (newProcess.isNotBlank()) {
                                viewModel.addProcess(config, newProcess.trim())
                                newProcess = ""
                            }
                            showProcessDialog = false
                        }
                    ) { Text("确定") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showProcessDialog = false }) { Text("取消") }
                }
            )
        }
    }
}


@Composable
private fun loadAppIcon(context: Context, packageName: String): MutableState<Drawable?> {
    val drawable = remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        launch(Dispatchers.IO) {
            drawable.value = context.packageManager.getApplicationIcon(packageName)
        }
    }

    return drawable
}
