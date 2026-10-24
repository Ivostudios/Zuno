package com.example.consolelauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Token presi dal frame Figma "app version page" (836x390). */
object Tokens {
    val GradientTop = Color(0x03000000)      // rgba(0,0,0,0.01)
    val GradientBottom = Color(0xFF513416)
    val Glass = Color(0x80000000)            // rgba(0,0,0,0.5)
    val White50 = Color(0x80FFFFFF)
    val White25 = Color(0x40FFFFFF)
    val TileDim = Color(0x1A000000)          // overlay 10% sui tile non selezionati

    // TODO: aggiungi res/font/archivo_black.ttf, archivo_bold.ttf, nanumgothic_bold.ttf
    // e sostituisci con FontFamily(Font(R.font.archivo_black, FontWeight.Black)) ecc.
    val Archivo: FontFamily = FontFamily.SansSerif
    val Nanum: FontFamily = FontFamily.SansSerif
}
