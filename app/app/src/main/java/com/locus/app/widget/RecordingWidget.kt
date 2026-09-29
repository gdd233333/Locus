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
import java.time.Instant

/** 时间记录小组件（4x1）：琥珀饰线 + 进行中记录 + 计时 + 结束按钮；空闲时引导去记录页 */
class RecordingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as LocusApplication).container
        val active = container.timeLogRepository.getActiveLog().first()
        val elapsed = active?.durationSeconds(Instant.now()) ?: 0L

        provideContent {
            RecordingContent(
                activityName = active?.activityName,
                elapsedSeconds = elapsed,
            )
        }
    }
}

@Composable
private fun RecordingContent(activityName: String?, elapsedSeconds: Long) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .cornerRadius(24.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 饰线：记录中实心琥珀（像录音指示灯），空闲暗色
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(36.dp)
                .cornerRadius(2.dp)
                .background(if (activityName != null) WidgetColors.amber else WidgetColors.stoneDark),
        ) {}
        Spacer(GlanceModifier.width(14.dp))

        if (activityName != null) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(deepLinkIntent(LocalContext.current, DEEP_LINK_TIMELOG))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activityName,
                    style = TextStyle(color = WidgetColors.smoke, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = formatElapsed(elapsedSeconds),
                    style = TextStyle(color = WidgetColors.amber, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
            Box(
                modifier = GlanceModifier
                    .background(WidgetColors.amberDim)
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<EndRecordingAction>())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "结束",
                    style = TextStyle(
                        color = WidgetColors.amber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        } else {
            // 空闲态：点进记录页，由 app 内弹层完成输入
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(deepLinkIntent(LocalContext.current, DEEP_LINK_TIMELOG))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "此刻空闲",
                    style = TextStyle(color = WidgetColors.muted, fontSize = 13.sp),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "点这里开始记录 →",
                    style = TextStyle(color = WidgetColors.amber, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

class RecordingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecordingWidget()
}
