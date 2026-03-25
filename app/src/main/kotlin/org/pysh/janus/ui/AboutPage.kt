package org.pysh.janus.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.pysh.janus.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Preview(showBackground = true)
@Composable
private fun AboutPagePreview() {
    MiuixTheme {
        AboutPage(onBack = {})
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutPage(onBack: () -> Unit) {
    val isInPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val icon = remember {
        if (isInPreview) {
            ImageBitmap(96, 96)
        } else {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val density = context.resources.displayMetrics.density
            val sizePx = (96 * density).toInt()
            drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.about),
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = BitmapPainter(icon),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(96.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(R.string.app_version_format, org.pysh.janus.BuildConfig.VERSION_NAME),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                SuperArrow(
                    title = stringResource(R.string.project),
                    endActions = {
                        Text(
                            text = stringResource(R.string.about_github),
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    },
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/penguinyzsh/janus"))
                        )
                    },
                )
                SuperArrow(
                    title = stringResource(R.string.author),
                    endActions = {
                        Text(
                            text = stringResource(R.string.about_author_name),
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    },
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/penguinyzsh"))
                        )
                    },
                )
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/UJBp9bNnIQ"))
                            )
                        },
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("qq_group", "119655862"))
                            Toast.makeText(context, context.getString(R.string.qq_group_copied), Toast.LENGTH_SHORT).show()
                        },
                    ),
                ) {
                    SuperArrow(
                        title = stringResource(R.string.qq_group),
                        endActions = {
                            Text(
                                text = stringResource(R.string.about_qq_group_number),
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        },
                    )
                }
            }
        }
    }
}
