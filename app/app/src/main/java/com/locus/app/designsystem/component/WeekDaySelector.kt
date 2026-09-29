package com.locus.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.theme.*
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun WeekDaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            val dayOfWeek = date.dayOfWeek.getDisplayName(
                JavaTextStyle.NARROW, Locale.CHINESE
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .shadow(4.dp, CircleShape, ambientColor = Amber, spotColor = Amber)
                                    .clip(CircleShape)
                                    .background(Amber)
                            } else {
                                Modifier
                                    .clip(CircleShape)
                                    .background(InkSurface)
                            }
                        )
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = LocusTypography.labelLarge,
                        color = if (isSelected) InkBackground else Stone,
                    )
                }
                Text(
                    text = dayOfWeek,
                    style = LocusTypography.labelTiny,
                    color = StoneDark,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12100F)
@Composable
private fun PreviewWeekDaySelector() {
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
