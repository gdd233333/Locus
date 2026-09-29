package com.locus.app.feature.inspire

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.R
import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import com.locus.app.designsystem.component.ChipItem
import com.locus.app.designsystem.component.FilterChipRow
import com.locus.app.designsystem.theme.*
import kotlinx.coroutines.launch

@Composable
fun InspireScreen(
    viewModel: InspireViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocusSpacing.contentBottomPadding),
    ) {
        // 标题区
        Column(modifier = Modifier.padding(horizontal = 28.dp).padding(top = 24.dp)) {
            Text("现在，做点别的", style = LocusTypography.displaySmall, color = Smoke)
            Spacer(Modifier.height(6.dp))
            Text("根据当下状态，为你推荐三件事", style = LocusTypography.bodySmall, color = Stone)
        }

        Spacer(Modifier.height(20.dp))

        // 筛选 chips：第一个是"此刻推荐"（即不过滤）
        val chips = listOf(ChipItem("all", "此刻推荐")) +
            ActivityCategory.entries.take(5).map { ChipItem(it.name, it.displayName) }
        FilterChipRow(
            chips = chips,
            selectedChipId = uiState.selectedCategory?.name ?: "all",
            onChipSelected = { chip ->
                val category = if (chip.id == "all") null
                else ActivityCategory.valueOf(chip.id)
                viewModel.selectCategory(category)
            },
        )

        Spacer(Modifier.height(20.dp))

        // 活动卡片列表：交错入场动画
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.activities.forEachIndexed { index, activity ->
                ActivityCard(
                    activity = activity,
                    index = index,
                    visible = !uiState.isShuffling,
                    textureRes = when (index % 3) {
                        0 -> R.drawable.marble_texture_2
                        1 -> R.drawable.marble_texture_3
                        else -> R.drawable.marble_texture_4
                    },
                )
            }
        }

        // 换一批提示
        Text(
            text = "都不想做？点这里换一批 →",
            style = LocusTypography.bodySmall,
            color = StoneDark,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(24.dp)
                .clickable { viewModel.shuffle() },
        )
    }
}

/** 活动卡片：右侧大理石纹理 + 交错 fadeUp 入场 */
@Composable
private fun ActivityCard(
    activity: Activity,
    index: Int,
    visible: Boolean,
    textureRes: Int,
) {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(24f) }

    LaunchedEffect(visible, activity.id) {
        if (visible) {
            // 交错延迟：每张卡比上一张晚 100ms
            kotlinx.coroutines.delay((index * 100).toLong())
            launch { alpha.animateTo(1f, tween(700, easing = LocusMotion.EaseOut)) }
            launch { translationY.animateTo(0f, tween(700, easing = LocusMotion.EaseOut)) }
        } else {
            launch { alpha.animateTo(0f, tween(300)) }
            launch { translationY.animateTo(12f, tween(300)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translationY.value.dp.toPx()
            }
            .clip(RoundedCornerShape(LocusRadius.xl))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl)),
    ) {
        // 右侧大理石纹理装饰
        Image(
            painter = painterResource(textureRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(120.dp)
                .fillMaxHeight()
                .alpha(0.5f),
            contentScale = ContentScale.Crop,
        )

        Column(modifier = Modifier.padding(24.dp)) {
            // 时间标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${activity.durationMinutes} MIN",
                    style = LocusTypography.labelSmall,
                    color = Amber,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(activity.title, style = LocusTypography.headlineSmall, color = Smoke)
            Spacer(Modifier.height(6.dp))
            Text(
                text = activity.description,
                style = LocusTypography.bodySmall,
                color = Stone,
                modifier = Modifier.fillMaxWidth(0.75f),
            )
            Spacer(Modifier.height(14.dp))
            // 标签
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                activity.tags.take(2).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(InkSurface2)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(tag, style = LocusTypography.labelTiny, color = Stone)
                    }
                }
            }
        }
    }
}
