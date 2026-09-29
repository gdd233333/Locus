package com.locus.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .then(
                        if (isSelected) {
                            Modifier.background(Amber)
                        } else {
                            Modifier
                                .background(InkSurface)
                                .border(1.dp, Line, RoundedCornerShape(LocusRadius.full))
                        }
                    )
                    .clickable { onChipSelected(chip) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = chip.label,
                    style = LocusTypography.labelMedium,
                    color = if (isSelected) InkBackground else Stone,
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
