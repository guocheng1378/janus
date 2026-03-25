package org.pysh.janus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pysh.janus.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * SettingsPage UI 测试 — 对应 TC-SET-001~002
 */
@RunWith(AndroidJUnit4::class)
class SettingsPageTest {

    @get:Rule
    val rule = createComposeRule()

    private fun s(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    // TC-SET-001: 页面显示"关于"菜单项
    @Test
    fun settingsPage_showsAboutItem() {
        rule.setContent {
            MiuixTheme { SettingsPage(bottomPadding = 0.dp, onAboutClick = {}) }
        }
        rule.onNodeWithText(s(R.string.about)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.about_janus)).assertIsDisplayed()
    }

    // TC-SET-002: 点击"关于"触发回调
    @Test
    fun settingsPage_clickAbout_callsCallback() {
        var clicked = false
        rule.setContent {
            MiuixTheme {
                SettingsPage(bottomPadding = 0.dp, onAboutClick = { clicked = true })
            }
        }
        rule.onNodeWithText(s(R.string.about)).performClick()
        assertTrue(clicked)
    }
}
