package com.locus.app.feature.inspire

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.CardShimmer
import com.locus.app.designsystem.component.ChipItem
import com.locus.app.designsystem.component.FilterChipRow
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.theme.*
import kotlinx.coroutines.launch

@Composable
fun InspireScreen(
    viewModel: InspireViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        AuroraBackground(modifier = Modifier.fillMaxSize(), intensity = 0.8f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            // 标题区
            Column(modifier = Modifier.padding(horizontal = 28.dp).padding(top = 12.dp)) {
                Text("现在，做点别的", style = LocusTypography.displaySmall, color = Smoke)
                Spacer(Modifier.height(6.dp))
                Text("根据当下状态，为你推荐三件事", style = LocusTypography.bodySmall, color = Stone)
            }

            Spacer(Modifier.height(20.dp))

            // 筛选 chips：第一个是"此刻推荐"（即不过滤），8 个类目全部可筛
            val chips = listOf(ChipItem("all", "此刻推荐")) +
                ActivityCategory.entries.map { ChipItem(it.name, it.displayName) }
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
                    )
                }
            }

            // 换一批：发光 pill 按钮
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
                    .bounceClick { viewModel.shuffle() }
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .background(InkSurface)
                    .border(1.dp, AmberDim, RoundedCornerShape(LocusRadius.full))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("都不想做？换一批", style = LocusTypography.bodySmall, color = Amber)
                }
            }
        }
    }
}

/** 活动卡片：卡片内动态流光 + 交错 fadeUp 入场 */
@Composable
private fun ActivityCard(
    activity: Activity,
    index: Int,
    visible: Boolean,
) {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(24f) }
    val scale = remember { Animatable(0.96f) }

    LaunchedEffect(visible, activity.id) {
        if (visible) {
            // 交错延迟：每张卡比上一张晚 100ms
            kotlinx.coroutines.delay((index * 100).toLong())
            launch { alpha.animateTo(1f, tween(700, easing = LocusMotion.EaseOut)) }
            launch { translationY.animateTo(0f, tween(700, easing = LocusMotion.EaseOut)) }
            launch { scale.animateTo(1f, tween(700, easing = LocusMotion.EaseOut)) }
        } else {
            launch { alpha.animateTo(0f, tween(300)) }
            launch { translationY.animateTo(12f, tween(300)) }
            launch { scale.animateTo(0.97f, tween(300)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translationY.value.dp.toPx()
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(LocusRadius.xl))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl)),
    ) {
        // 动态眩光：替代原右侧大理石贴图，每张卡相位错开
        CardShimmer(
            modifier = Modifier.matchParentSize(),
            phaseOffset = index * 1.7f,
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
