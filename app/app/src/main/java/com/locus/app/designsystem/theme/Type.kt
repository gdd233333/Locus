package com.locus.app.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.locus.app.R

val DmSerifDisplay = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal)
)

val NotoSansSC = FontFamily(
    Font(R.font.noto_sans_sc_light, FontWeight.Light),
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold),
)

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_light, FontWeight.Light),
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

object LocusTypography {
    val displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Light,
        fontSize = 120.sp,
        lineHeight = 120.sp,
        letterSpacing = (-0.04).sp
    )
    val displayMedium = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.01).sp
    )
    val displaySmall = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 33.sp
    )
    val headlineMedium = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 31.sp
    )
    val headlineSmall = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 25.sp
    )
    val titleLarge = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp
    )
    val titleMedium = TextStyle(
        fontFamily = DmSerifDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.05.sp
    )
    val bodyLarge = TextStyle(
        fontFamily = NotoSansSC,
        fontWeight = FontWeight.Light,
        fontSize = 15.sp,
        lineHeight = 27.sp
    )
    val bodyMedium = TextStyle(
        fontFamily = NotoSansSC,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val bodySmall = TextStyle(
        fontFamily = NotoSansSC,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 21.sp
    )
    val labelLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    )
    val labelMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.15.sp
    )
    val labelSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    )
    val labelTiny = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.25.sp
    )
}
