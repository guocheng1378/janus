package org.pysh.janus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.pysh.janus.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val StatusGreenBg = Color(0xFF1B5E20)
private val StatusRedBg = Color(0xFFB71C1C)
private val StatusGrayBg = Color(0xFF424242)

@Composable
fun StatusCard(
    isModuleActive: Boolean,
    hasRoot: Boolean?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        StatusItem(
            title = stringResource(if (isModuleActive) R.string.module_activated else R.string.module_not_activated),
            summary = stringResource(if (isModuleActive) R.string.module_activated_summary else R.string.module_not_activated_summary),
            isOk = isModuleActive,
            isLoading = false,
        )

        Spacer(modifier = Modifier.height(12.dp))

        StatusItem(
            title = stringResource(
                when (hasRoot) {
                    true -> R.string.root_ok
                    false -> R.string.root_missing
                    null -> R.string.root_checking
                }
            ),
            summary = when (hasRoot) {
                true -> stringResource(R.string.root_ok_summary)
                false -> stringResource(R.string.root_missing_summary)
                null -> null
            },
            isOk = hasRoot == true,
            isLoading = hasRoot == null,
        )
    }
}

@Composable
private fun StatusItem(
    title: String,
    summary: String?,
    isOk: Boolean,
    isLoading: Boolean,
) {
    val bgColor = when {
        isLoading -> StatusGrayBg
        isOk -> StatusGreenBg
        else -> StatusRedBg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                isLoading -> MiuixIcons.Report
                isOk -> MiuixIcons.Ok
                else -> MiuixIcons.Report
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MiuixTheme.textStyles.headline2,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (summary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body2,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
