package com.mochyy.mochymusic.ui.screens.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.theme.InterFontFamily

// The profile page's photo, name and card-number type, and the copies that fly between the card and
// the page: both ends use these so the same text is the same size and weight at each end.

val ProfilePhotoSize = 98.dp
val ProfilePhotoGrey = Color(0xFF4F4F4F)

// both match the ID card's name and number (weight, tracking), so the copies flying between them
// are the same text at both ends, only bigger or smaller
val ProfileNameStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 60.sp,
    lineHeight = 1.1.em,
    letterSpacing = (-0.05).em,
    color = Color.White,
)

val ProfileTagStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = (-0.05).em,
    color = Color(0xFFB5B5B5),
)

fun profileTag(cardNumber: String) = "PROFILE USR#$cardNumber"
