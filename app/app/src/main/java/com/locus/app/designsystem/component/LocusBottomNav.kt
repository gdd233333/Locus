package com.locus.app.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.theme.*

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LocusBottomNav(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 顶部分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Line)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LocusSpacing.bottomNavHeight)
                .background(InkBackground.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item.route == selectedRoute
                val color by animateColorAsState(
                    targetValue = if (isSelected) Amber else StoneDark,
                    animationSpec = tween(300),
                    label = "navColor"
                )
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 4.dp else 0.dp,
                    animationSpec = tween(400, easing = LocusMotion.Spring),
                    label = "navDot"
                )
                // 选中时图标弹跳放大
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "navIconScale"
                )
                // 图标背后的选中光晕
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(400),
                    label = "navGlow"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onItemSelected(item) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (glowAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .alpha(glowAlpha)
                                    .clip(CircleShape)
                                    .background(AmberDim)
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = color,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = item.label,
                        style = LocusTypography.labelTiny,
                        color = color,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(Amber)
                    )
                }
            }
        }
    }
}

private val previewNavItems = listOf(
    NavItem("sober", "守护", Icons.Filled.Lock),
    NavItem("inspire", "灵感", Icons.Filled.Star),
    NavItem("timelog", "记录", Icons.Filled.DateRange),
    NavItem("review", "复盘", Icons.Filled.Refresh),
)

@Preview(showBackground = true, backgroundColor = 0xFF12100F)
@Composable
private fun PreviewLocusBottomNav() {
    LocusTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(InkBackground),
            contentAlignment = Alignment.BottomCenter,
        ) {
            LocusBottomNav(
                items = previewNavItems,
                selectedRoute = "sober",
                onItemSelected = {},
            )
        }
    }
}
