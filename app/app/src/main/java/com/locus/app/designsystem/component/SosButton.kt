package com.locus.app.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locus.app.designsystem.theme.*

@Composable
fun SosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos")
    val shadowElevation by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosShadow"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(LocusRadius.full),
                ambientColor = Amber,
                spotColor = Amber,
            ),
        shape = RoundedCornerShape(LocusRadius.full),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = InkBackground,
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "冲动急救",
            style = LocusTypography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12100F)
@Composable
private fun PreviewSosButton() {
    LocusTheme {
        Box(
            modifier = Modifier
                .background(InkBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            SosButton(onClick = {})
        }
    }
}
