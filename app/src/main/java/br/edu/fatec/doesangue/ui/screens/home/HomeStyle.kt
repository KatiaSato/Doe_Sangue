package br.edu.fatec.doesangue.ui.screens.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.fatec.doesangue.ui.theme.Poppins

// Tokens da proposta aprovada, limitados à Home; não alteram o tema das outras telas.
internal object HomeStyle {
    val Soft = Color(0xFFFFF1F1)
    val Success = Color(0xFF25733A)
    val SuccessSoft = Color(0xFFEDF7EF)
    val Warning = Color(0xFF9A5700)
    val WarningSoft = Color(0xFFFFF6E7)
    val CardShape = RoundedCornerShape(18.dp)
    val TileShape = RoundedCornerShape(14.dp)
    private val Base = TextStyle(
        fontFamily = Poppins,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val Greeting = Base.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
    val Hero = Base.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val Title = Base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val Label = Base.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Supporting = Base.copy(fontSize = 12.sp, lineHeight = 18.sp)
    val Caption = Base.copy(fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val BloodType = Base.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
}
