package com.locus.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.locus.app.MainActivity

/**
 * 小组件配色。
 * 注意：ColorProvider(Int) 会把 Int 当作颜色资源 ID，这里必须传 Color 值。
 */
internal object WidgetColors {
    val background = ColorProvider(Color(0xFF12100F))
    val card = ColorProvider(Color(0xFF1C1A18))
    val amber = ColorProvider(Color(0xFFD9A566))
    val amberDim = ColorProvider(Color(0x33D9A566))
    val smoke = ColorProvider(Color(0xFFE8E3DA))
    val muted = ColorProvider(Color(0xFF8A857C))
    val stoneDark = ColorProvider(Color(0xFF4A463F))
}

internal const val DEEP_LINK_SOBER = "locus://sober"
internal const val DEEP_LINK_TIMELOG = "locus://timelog"

internal fun deepLinkIntent(context: Context, uri: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)

/** 已计时长 → "HH:MM:SS"（小组件每分钟刷新一次，秒位保持稳定递增） */
internal fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
