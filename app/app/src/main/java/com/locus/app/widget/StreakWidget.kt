package com.locus.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.locus.app.LocusApplication
import com.locus.app.R
import kotlinx.coroutines.flow.first

/** 守护天数小组件（4x2）：琥珀饰线 + 大数字 + 状态行 + 打卡按钮，点空白处进守护页 */
class StreakWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as LocusApplication).container
        val days = container.streakRepository.getCurrentStreakDays().first()
        val checkedIn = container.streakRepository.getTodayCheckIn().first() != null

        provideContent {
            StreakContent(days = days, checkedIn = checkedIn)
        }
    }
}

@Composable
private fun StreakContent(days: Int, checkedIn: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .cornerRadius(24.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 琥珀饰线：打卡日实心琥珀，未打卡暗色（状态的第一视觉信号）
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(56.dp)
                .cornerRadius(2.dp)
                .background(if (checkedIn) WidgetColors.amber else WidgetColors.stoneDark),
        ) {}
        Spacer(GlanceModifier.width(14.dp))

        // 左半区整体作为 deep link 热区（defaultWeight 把按钮推到右侧）
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(deepLinkIntent(LocalContext.current, DEEP_LINK_SOBER))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "连续守护",
                style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp),
            )
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$days",
                    style = TextStyle(
                        color = WidgetColors.amber,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "天",
                    modifier = GlanceModifier.padding(bottom = 8.dp),
                    style = TextStyle(color = WidgetColors.muted, fontSize = 15.sp),
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (checkedIn) "✓ 今日已守护" else "还没打卡",
                style = TextStyle(
                    color = if (checkedIn) WidgetColors.amber else WidgetColors.muted,
                    fontSize = 13.sp,
                ),
            )
        }

        if (!checkedIn) {
            Box(
                modifier = GlanceModifier
                    .background(WidgetColors.amber)
                    .cornerRadius(18.dp)
                    .clickable(actionRunCallback<CheckInAction>())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "打卡",
                    style = TextStyle(
                        color = WidgetColors.background,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
