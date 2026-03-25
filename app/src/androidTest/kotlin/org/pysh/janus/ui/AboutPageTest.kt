package org.pysh.janus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pysh.janus.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AboutPage UI 测试 — 对应 TC-SET-003~007
 */
@RunWith(AndroidJUnit4::class)
class AboutPageTest {

    @get:Rule
    val rule = createComposeRule()

    private fun s(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    // TC-SET-003: 关于页面信息展示
    @Test
    fun aboutPage_showsAppInfo() {
        rule.setContent { MiuixTheme { AboutPage(onBack = {}) } }
        rule.onNodeWithText(s(R.string.about)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.app_name)).assertIsDisplayed()
    }

    // TC-SET-004: 关于页面链接展示
    @Test
    fun aboutPage_showsLinks() {
        rule.setContent { MiuixTheme { AboutPage(onBack = {}) } }
        rule.onNodeWithText(s(R.string.project)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.about_github)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.author)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.about_author_name)).assertIsDisplayed()
    }

    // TC-SET-007: 返回按钮回调
    @Test
    fun aboutPage_clickBack_callsCallback() {
        var backClicked = false
        rule.setContent { MiuixTheme { AboutPage(onBack = { backClicked = true }) } }
        rule.onNodeWithContentDescription(s(R.string.back)).performClick()
        assertTrue(backClicked)
    }
}
