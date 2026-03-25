package org.pysh.janus.ui

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.pysh.janus.R
import org.pysh.janus.data.WhitelistManager
import org.pysh.janus.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun AppFeaturePage(
    packageName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val whitelistManager = remember { WhitelistManager(context) }
    var isWhitelisted by remember {
        mutableStateOf(packageName in whitelistManager.getWhitelist())
    }

    val pm = context.packageManager
    val appInfo = remember {
        try {
            pm.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    val appName = remember { appInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName }
    val iconBitmap = remember {
        val density = context.resources.displayMetrics.density
        val sizePx = (48 * density).toInt()
        val drawable = appInfo?.let { pm.getApplicationIcon(it) }
            ?: pm.defaultActivityIcon
        drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.app_feature),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 12.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                BasicComponent(
                    title = appName,
                    summary = packageName,
                    startAction = {
                        Image(
                            painter = BitmapPainter(iconBitmap),
                            contentDescription = appName,
                            modifier = Modifier.size(48.dp),
                        )
                    },
                )
            }

            Card {
                SuperSwitch(
                    title = stringResource(R.string.add_music_whitelist),
                    summary = stringResource(if (isWhitelisted) R.string.whitelist_on else R.string.whitelist_off),
                    checked = isWhitelisted,
                    onCheckedChange = { checked ->
                        isWhitelisted = checked
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val whitelist = whitelistManager.getWhitelist().toMutableSet()
                                if (checked) {
                                    whitelist.add(packageName)
                                } else {
                                    whitelist.remove(packageName)
                                }
                                whitelistManager.saveWhitelist(whitelist)
                                RootUtils.restartBackScreen()
                            }
                            Toast.makeText(
                                context,
                                if (checked) context.getString(R.string.whitelist_added) else context.getString(R.string.whitelist_removed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }
        }
    }
}
