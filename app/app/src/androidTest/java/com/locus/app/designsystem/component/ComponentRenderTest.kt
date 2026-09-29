package com.locus.app.designsystem.component

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.LocusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ComponentRenderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun captureNow(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)!!
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Composable
    private fun DarkStage(content: @Composable () -> Unit) {
        LocusTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkBackground),
                contentAlignment = Alignment.BottomCenter,
            ) { content() }
        }
    }

    @Test
    fun bottomNav_renders() {
        rule.setContent {
            DarkStage {
                LocusBottomNav(
                    items = listOf(
                        NavItem("sober", "守护", Icons.Filled.Lock),
                        NavItem("inspire", "灵感", Icons.Filled.Star),
                        NavItem("timelog", "记录", Icons.Filled.DateRange),
                        NavItem("review", "复盘", Icons.Filled.Refresh),
                    ),
                    selectedRoute = "inspire",
                    onItemSelected = {},
                )
            }
        }
        rule.onNodeWithText("守护").assertIsDisplayed()
        rule.onNodeWithText("复盘").assertIsDisplayed()
        captureNow("bottom_nav")
    }

    @Test
    fun sosButton_renders() {
        // 无限呼吸动画会让 compose 永不 idle，改用手动时钟推进
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DarkStage { SosButton(onClick = {}) }
        }
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText("冲动急救").assertIsDisplayed()
        captureNow("sos_button")
    }

    @Test
    fun filterChipRow_renders() {
        rule.setContent {
            LocusTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InkBackground)
                        .padding(vertical = 24.dp),
                ) {
                    FilterChipRow(
                        chips = listOf(
                            ChipItem("all", "此刻推荐"),
                            ChipItem("emergency", "冲动急救"),
                            ChipItem("quick", "5 分钟"),
                            ChipItem("medium", "30 分钟"),
                            ChipItem("outdoor", "出门走走"),
                        ),
                        selectedChipId = "quick",
                        onChipSelected = {},
                    )
                }
            }
        }
        rule.onNodeWithText("此刻推荐").assertIsDisplayed()
        rule.onNodeWithText("出门走走").assertIsDisplayed()
        captureNow("filter_chips")
    }

    @Test
    fun weekDaySelector_renders() {
        rule.setContent {
            LocusTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InkBackground)
                        .padding(vertical = 24.dp),
                ) {
                    WeekDaySelector(
                        dates = List(7) { LocalDate.of(2026, 9, 28).plusDays(it.toLong()) },
                        selectedDate = LocalDate.of(2026, 9, 30),
                        onDateSelected = {},
                    )
                }
            }
        }
        rule.onNodeWithText("30").assertIsDisplayed()
        rule.onNodeWithText("28").assertIsDisplayed()
        captureNow("week_day_selector")
    }
}
