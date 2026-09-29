package com.locus.app.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.theme.*

/**
 * 冲动急救悬浮按钮：琥珀底 + 双层扩散光环 + 呼吸投影 + 按压回弹。
 */
@Composable
fun SosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 光环扩散：scale 1.0→1.35，alpha 0.5→0，循环
    val transition = rememberInfiniteTransition(label = "sos")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sosRingScale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sosRingAlpha",
    )
    val ring2Scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, 900, FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sosRing2Scale",
    )
    val ring2Alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, 900, FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sosRing2Alpha",
    )
    val shadowElevation = rememberBreathingAlpha(10f, 20f, 3000, label = "sosShadow")

    Box(
        modifier = modifier.bounceClick(scaleDown = 0.94f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 扩散光环（跟随按钮尺寸，不影响测量）
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(ringScale)
                .alpha(ringAlpha)
                .clip(RoundedCornerShape(LocusRadius.full))
                .background(Amber),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(ring2Scale)
                .alpha(ring2Alpha)
                .clip(RoundedCornerShape(LocusRadius.full))
                .background(Amber),
        )

        // 按钮本体
        Row(
            modifier = Modifier
                .shadow(
                    elevation = shadowElevation.dp,
                    shape = RoundedCornerShape(LocusRadius.full),
                    ambientColor = Amber,
                    spotColor = Amber,
                )
                .clip(RoundedCornerShape(LocusRadius.full))
                .background(Amber)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = InkBackground,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "冲动急救",
                style = LocusTypography.bodyMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = InkBackground,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12100F)
@Composable
private fun PreviewSosButton() {
    LocusTheme {
        Box(
            modifier = Modifier
                .background(InkBackground)
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            SosButton(onClick = {})
        }
    }
}
