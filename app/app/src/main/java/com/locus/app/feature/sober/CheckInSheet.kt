package com.locus.app.feature.sober

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.theme.Amber
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.InkSurface
import com.locus.app.designsystem.theme.InkSurface2
import com.locus.app.designsystem.theme.Line
import com.locus.app.designsystem.theme.LocusRadius
import com.locus.app.designsystem.theme.LocusTypography
import com.locus.app.designsystem.theme.Smoke
import com.locus.app.designsystem.theme.Stone
import com.locus.app.designsystem.theme.StoneDark

/** 打卡弹层：心情 1~5 + 可选备注 → 确认写入 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(
    onDismiss: () -> Unit,
    onConfirm: (mood: Int, note: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mood by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

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
            Text("今日打卡", style = LocusTypography.headlineSmall, color = Smoke)
            Spacer(Modifier.height(6.dp))
            Text("今天守住了吗？", style = LocusTypography.bodySmall, color = Stone)
            Spacer(Modifier.height(24.dp))

            // 心情五档（默认不选，选中项琥珀放大弹跳）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (1..5).forEach { value ->
                    val selected = mood == value
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.18f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "moodScale$value",
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(if (selected) Amber else InkSurface2)
                            .border(1.dp, if (selected) Amber else Line, CircleShape)
                            .bounceClick(scaleDown = 0.9f) { mood = value },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$value",
                            style = LocusTypography.labelLarge,
                            color = if (selected) InkBackground else Stone,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 备注（可选）
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = LocusTypography.bodyLarge.copy(color = Smoke),
                cursorBrush = SolidColor(Amber),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LocusRadius.md))
                    .background(InkSurface2)
                    .border(1.dp, Line, RoundedCornerShape(LocusRadius.md))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (note.isEmpty()) {
                        Text("想记点什么？", style = LocusTypography.bodyLarge, color = StoneDark)
                    }
                    inner()
                },
            )

            Spacer(Modifier.height(24.dp))

            // 确认：选了心情才可点
            val enabled = mood != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LocusRadius.md))
                    .background(if (enabled) Amber else InkSurface2)
                    .then(
                        if (enabled) {
                            Modifier.bounceClick {
                                onConfirm(mood ?: return@bounceClick, note.ifBlank { null })
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "确认打卡",
                    style = LocusTypography.bodyMedium,
                    color = if (enabled) InkBackground else StoneDark,
                )
            }
        }
    }
}
