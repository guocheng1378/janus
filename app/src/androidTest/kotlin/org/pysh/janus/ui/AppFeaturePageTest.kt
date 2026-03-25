package org.pysh.janus.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pysh.janus.R
import org.pysh.janus.data.MediaAppInfo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AppFeaturePage UI 测试 — 对应 TC-APP-008~011
 */
@RunWith(AndroidJUnit4::class)
class AppFeaturePageTest {

    @get:Rule
    val rule = createComposeRule()

    private fun s(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun createTestApp(isMediaApp: Boolean = false) = MediaAppInfo(
        packageName = "com.test.example",
        appName = "Test App",
        icon = ColorDrawable(android.graphics.Color.BLUE),
        isMediaApp = isMediaApp,
    )

    // TC-APP-008: 详情页标题为"功能"
    @Test
    fun appFeaturePage_showsTitle() {
        rule.setContent {
            MiuixTheme { AppFeaturePage(app = createTestApp(), onBack = {}) }
        }
        rule.onNodeWithText(s(R.string.app_feature)).assertIsDisplayed()
    }

    // TC-APP-008: 详情页显示应用名称和包名
    @Test
    fun appFeaturePage_showsAppInfo() {
        rule.setContent {
            MiuixTheme { AppFeaturePage(app = createTestApp(), onBack = {}) }
        }
        rule.onNodeWithText("Test App").assertIsDisplayed()
        rule.onNodeWithText("com.test.example").assertIsDisplayed()
    }

    // TC-APP-008: 媒体应用显示"媒体"标签
    @Test
    fun appFeaturePage_mediaApp_showsMediaTag() {
        rule.setContent {
            MiuixTheme { AppFeaturePage(app = createTestApp(isMediaApp = true), onBack = {}) }
        }
        rule.onNodeWithText(s(R.string.media_tag)).assertIsDisplayed()
    }

    // TC-APP-008: 非媒体应用不显示"媒体"标签
    @Test
    fun appFeaturePage_nonMediaApp_noMediaTag() {
        rule.setContent {
            MiuixTheme { AppFeaturePage(app = createTestApp(isMediaApp = false), onBack = {}) }
        }
        rule.onNodeWithText(s(R.string.media_tag)).assertDoesNotExist()
    }

    // TC-APP-009/010: 白名单开关展示
    @Test
    fun appFeaturePage_showsWhitelistSwitch() {
        rule.setContent {
            MiuixTheme { AppFeaturePage(app = createTestApp(), onBack = {}) }
        }
        rule.onNodeWithText(s(R.string.add_music_whitelist)).assertIsDisplayed()
    }

    // TC-APP-011: 返回按钮触发回调
    @Test
    fun appFeaturePage_backButton_callsCallback() {
        var backCalled = false
        rule.setContent {
            MiuixTheme {
                AppFeaturePage(app = createTestApp(), onBack = { backCalled = true })
            }
        }
        rule.onNodeWithContentDescription(s(R.string.back)).performClick()
        assertTrue(backCalled)
    }
}
