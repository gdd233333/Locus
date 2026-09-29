package com.locus.app.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.theme.*

data class ChipItem(
    val id: String,
    val label: String,
)

@Composable
fun FilterChipRow(
    chips: List<ChipItem>,
    selectedChipId: String?,
    onChipSelected: (ChipItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val isSelected = chip.id == selectedChipId
            // 选中态全动画过渡：底色 / 文字色 / 描边 / 琥珀光晕
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Amber else InkSurface,
                animationSpec = tween(300), label = "chipBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) InkBackground else Stone,
                animationSpec = tween(300), label = "chipText",
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Amber.copy(alpha = 0.45f) else Line,
                animationSpec = tween(300), label = "chipBorder",
            )
            val glowElevation by animateDpAsState(
                targetValue = if (isSelected) 8.dp else 0.dp,
                animationSpec = tween(300), label = "chipGlow",
            )
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = glowElevation,
                        shape = RoundedCornerShape(LocusRadius.full),
                        ambientColor = Amber,
                        spotColor = Amber,
                    )
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(LocusRadius.full))
                    .bounceClick(scaleDown = 0.92f) { onChipSelected(chip) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = chip.label,
                    style = LocusTypography.labelMedium,
                    color = textColor,
                )
            }
        }
    }
}

private val previewChips = listOf(
    ChipItem("all", "此刻推荐"),
    ChipItem("emergency", "冲动急救"),
    ChipItem("quick", "5 分钟"),
    ChipItem("medium", "30 分钟"),
    ChipItem("outdoor", "出门走走"),
)

@Preview(showBackground = true, backgroundColor = 0xFF12100F)
@Composable
private fun PreviewFilterChipRow() {
    LocusTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InkBackground)
                .padding(vertical = 24.dp),
        ) {
            FilterChipRow(
                chips = previewChips,
                selectedChipId = "quick",
                onChipSelected = {},
            )
        }
    }
}
