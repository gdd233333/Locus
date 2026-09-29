package com.locus.app.feature.sober

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.locus.app.core.model.UrgeIntensity
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.theme.Amber
import com.locus.app.designsystem.theme.AmberDim
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.InkSurface
import com.locus.app.designsystem.theme.InkSurface2
import com.locus.app.designsystem.theme.Line
import com.locus.app.designsystem.theme.LocusRadius
import com.locus.app.designsystem.theme.LocusTypography
import com.locus.app.designsystem.theme.Smoke
import com.locus.app.designsystem.theme.Stone
import com.locus.app.designsystem.theme.StoneDark

/** SOS 急救弹层：强度三档 + 三个出口，未选强度时出口全部禁用 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgeEmergencySheet(
    onDismiss: () -> Unit,
    onRecordOnly: (UrgeIntensity) -> Unit,
    onStartSurfing: (UrgeIntensity) -> Unit,
    onFindActivity: (UrgeIntensity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var intensity by remember { mutableStateOf<UrgeIntensity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = InkSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 40.dp),
        ) {
            Text("冲动来了？", style = LocusTypography.headlineSmall, color = Smoke)
            Spacer(Modifier.height(6.dp))
            Text("很正常，它像海浪，会涨也会退", style = LocusTypography.bodySmall, color = Stone)
            Spacer(Modifier.height(24.dp))

            // 强度三档
            Text("现在的强度", style = LocusTypography.labelTiny, color = StoneDark)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UrgeIntensity.entries.forEach { level ->
                    val selected = intensity == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(LocusRadius.full))
                            .background(if (selected) Amber else InkSurface2)
                            .border(1.dp, if (selected) Amber else Line, RoundedCornerShape(LocusRadius.full))
                            .bounceClick(scaleDown = 0.92f) { intensity = level },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = level.displayName(),
                            style = LocusTypography.labelMedium,
                            color = if (selected) InkBackground else Stone,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val chosen = intensity
            ExitOption(
                icon = Icons.Filled.Edit,
                title = "我只是记录一下",
                subtitle = "记下来，我自己扛",
                enabled = chosen != null,
            ) { chosen?.let(onRecordOnly) }
            Spacer(Modifier.height(12.dp))
            ExitOption(
                icon = Icons.Filled.Waves,
                title = "我要做冲浪练习",
                subtitle = "10 分钟引导呼吸",
                enabled = chosen != null,
            ) { chosen?.let(onStartSurfing) }
            Spacer(Modifier.height(12.dp))
            ExitOption(
                icon = Icons.Filled.Star,
                title = "给我找点事做",
                subtitle = "推荐替代活动",
                enabled = chosen != null,
            ) { chosen?.let(onFindActivity) }
        }
    }
}

private fun UrgeIntensity.displayName(): String = when (this) {
    UrgeIntensity.MILD -> "轻"
    UrgeIntensity.MODERATE -> "中"
    UrgeIntensity.STRONG -> "强"
}

@Composable
private fun ExitOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(LocusRadius.lg))
            .background(InkSurface2)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.lg))
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmberDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = LocusTypography.bodyMedium, color = if (enabled) Smoke else Stone)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = LocusTypography.labelSmall, color = StoneDark)
        }
    }
}
