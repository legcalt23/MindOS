package com.example.neu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeneratedUiScreen() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value
        val scale = minOf(screenW / 1080f, screenH / 1920f)

        fun Float.sx() = (this * scale).dp
        fun Float.sy() = (this * scale).dp
        fun Float.spScaled() = (this * scale).sp
        fun Int.sx() = (this * scale).dp
        fun Int.sy() = (this * scale).dp
        fun Int.spScaled() = (this * scale).sp
        fun Double.sx() = (this.toFloat() * scale).dp
        fun Double.sy() = (this.toFloat() * scale).dp
        fun Double.spScaled() = (this.toFloat() * scale).sp

        // Contenedor base 1080x1920 centrado
        Box(modifier = Modifier.size(1080.sx(), 1920.sy())) {
            
            // Marco principal
            Box(
                modifier = Modifier
                    .offset(x = 90.sx(), y = 80.sy())
                    .size(900.sx(), 1760.sy())
                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                // --- HEADER ---
                // Línea inferior
                Box(
                    modifier = Modifier
                        .offset(y = 168.5.sy())
                        .size(900.sx(), 1.5.dp)
                        .background(Color.Black)
                )
                
                // Zona atrás: línea vertical derecha
                Box(
                    modifier = Modifier
                        .offset(x = 158.5.sx(), y = 0.sy())
                        .size(1.5.dp, 170.sy())
                        .background(Color.Black)
                )
                
                // Icono flecha
                Canvas(
                    modifier = Modifier
                        .offset(x = 45.sx(), y = 50.sy())
                        .size(70.sx(), 70.sy())
                ) {
                    val path = Path().apply {
                        moveTo(size.width * 0.8f, 0f)
                        lineTo(size.width * 0.2f, size.height * 0.5f)
                        lineTo(size.width * 0.8f, size.height)
                    }
                    drawPath(path, Color.Black, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                
                // Avatar
                Box(
                    modifier = Modifier
                        .offset(x = 185.sx(), y = 30.sy())
                        .size(110.sx(), 110.sy())
                        .border(1.5.dp, Color.Black, CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color.Black, radius = size.width * 0.25f, center = Offset(size.width / 2, size.height * 0.35f), style = Stroke(1.5.dp.toPx()))
                        val path = Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.9f)
                            quadraticBezierTo(size.width * 0.15f, size.height * 0.6f, size.width / 2, size.height * 0.6f)
                            quadraticBezierTo(size.width * 0.85f, size.height * 0.6f, size.width * 0.85f, size.height * 0.9f)
                        }
                        drawPath(path, Color.Black, style = Stroke(1.5.dp.toPx()))
                    }
                }
                
                // Nombre
                Text(
                    text = "Abraham Montfo",
                    modifier = Modifier
                        .offset(x = 335.sx(), y = 55.sy())
                        .size(500.sx(), 80.sy()),
                    fontSize = 42.spScaled(),
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    maxLines = 1
                )
                
                // --- ÁREA DE CONVERSACIÓN ---
                
                // Círculos izquierdos
                Box(modifier = Modifier.offset(x = 22.sx(), y = 250.sy()).size(24.sx(), 24.sy()).border(1.5.dp, Color.Black, CircleShape))
                Box(modifier = Modifier.offset(x = 22.sx(), y = 890.sy()).size(24.sx(), 24.sy()).border(1.5.dp, Color.Black, CircleShape))
                
                // Mensaje 1
                Box(
                    modifier = Modifier
                        .offset(x = 65.sx(), y = 240.sy())
                        .size(380.sx(), 110.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp))
                        .padding(start = 30.sx()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Hola Fran!", fontSize = 42.spScaled(), fontWeight = FontWeight.Light, color = Color.Black, maxLines = 1)
                }
                
                // Mensaje 2
                Box(
                    modifier = Modifier
                        .offset(x = 65.sx(), y = 385.sy())
                        .size(560.sx(), 130.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp))
                        .padding(start = 30.sx()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Como te va la vida", fontSize = 42.spScaled(), fontWeight = FontWeight.Light, color = Color.Black, maxLines = 1)
                }
                
                // Mensaje 3 grande
                Box(
                    modifier = Modifier
                        .offset(x = 65.sx(), y = 560.sy())
                        .size(560.sx(), 300.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp))
                        .padding(start = 30.sx(), top = 45.sy()),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text("Me enteré de lo\nque le pasó a Mario...", fontSize = 40.spScaled(), fontWeight = FontWeight.Light, color = Color.Black, lineHeight = 60.spScaled(), maxLines = 2)
                }
                
                // Hora 13:31
                Text(
                    text = "13:31",
                    modifier = Modifier
                        .offset(x = 640.sx(), y = 770.sy())
                        .size(110.sx(), 60.sy()),
                    fontSize = 34.spScaled(),
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    maxLines = 1
                )
                
                // Bloque FOTO
                Box(
                    modifier = Modifier
                        .offset(x = 65.sx(), y = 900.sy())
                        .size(240.sx(), 120.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FOTO", fontSize = 40.spScaled(), fontWeight = FontWeight.Light, color = Color.Black, maxLines = 1)
                }
                
                // Hora 13:45
                Text(
                    text = "13:45",
                    modifier = Modifier
                        .offset(x = 325.sx(), y = 945.sy())
                        .size(110.sx(), 60.sy()),
                    fontSize = 34.spScaled(),
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    maxLines = 1
                )
                
                // Burbuja derecha
                Box(
                    modifier = Modifier
                        .offset(x = 370.sx(), y = 1080.sy())
                        .size(430.sx(), 145.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(14.dp))
                ) {
                    // Garabato flor
                    Canvas(modifier = Modifier.offset(x = 35.sx(), y = 25.sy()).size(90.sx(), 90.sy())) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width * 0.2f
                        val petalRadius = size.width * 0.25f
                        for (i in 0 until 8) {
                            val angle = i * 45f * (Math.PI / 180f)
                            val dx = (radius * 1.5f) * Math.cos(angle).toFloat()
                            val dy = (radius * 1.5f) * Math.sin(angle).toFloat()
                            drawCircle(Color.Black, radius = petalRadius, center = center + Offset(dx, dy), style = Stroke(1.5.dp.toPx()))
                        }
                        drawCircle(Color.White, radius = radius, center = center)
                        drawCircle(Color.Black, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
                    }
                    
                    // Garabato óvalos
                    Canvas(modifier = Modifier.offset(x = 175.sx(), y = 45.sy()).size(170.sx(), 45.sy())) {
                        val midY = size.height / 2
                        drawLine(Color.Black, start = Offset(0f, midY), end = Offset(size.width, midY), strokeWidth = 1.5.dp.toPx())
                        val numCircles = 6
                        val step = size.width / numCircles
                        val radiusX = step * 0.5f
                        val radiusY = size.height / 2
                        for (i in 0 until numCircles) {
                            drawOval(Color.Black, topLeft = Offset(step * i, midY - radiusY), size = Size(radiusX * 2, radiusY * 2), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                    
                    // Hora interna
                    Text(
                        text = "13:45",
                        modifier = Modifier.offset(x = 335.sx(), y = 90.sy()),
                        fontSize = 28.spScaled(),
                        fontWeight = FontWeight.Light,
                        color = Color.Black,
                        maxLines = 1
                    )
                }
                
                // Hora externa 14:32
                Text(
                    text = "14:32",
                    modifier = Modifier.offset(x = 250.sx(), y = 1170.sy()),
                    fontSize = 34.spScaled(),
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    maxLines = 1
                )
                
                // Flor inferior suelta
                Canvas(
                    modifier = Modifier
                        .offset(x = 70.sx(), y = 1285.sy())
                        .size(140.sx(), 140.sy())
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width * 0.2f
                    val petalRadius = size.width * 0.25f
                    for (i in 0 until 8) {
                        val angle = i * 45f * (Math.PI / 180f)
                        val dx = (radius * 1.5f) * Math.cos(angle).toFloat()
                        val dy = (radius * 1.5f) * Math.sin(angle).toFloat()
                        drawCircle(Color.Black, radius = petalRadius, center = center + Offset(dx, dy), style = Stroke(1.5.dp.toPx()))
                    }
                    drawCircle(Color.White, radius = radius, center = center)
                    drawCircle(Color.Black, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
                }
                
                // --- BARRA INFERIOR ---
                // Línea divisoria
                Box(
                    modifier = Modifier
                        .offset(y = 1558.5.sy())
                        .size(900.sx(), 1.5.dp)
                        .background(Color.Black)
                )
                
                // Cámara
                Box(
                    modifier = Modifier
                        .offset(x = 20.sx(), y = 1590.sy())
                        .size(110.sx(), 110.sy())
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(48.sx(), 48.sy())) {
                        drawRoundRect(Color.Black, topLeft = Offset(size.width * 0.1f, size.height * 0.25f), size = Size(size.width * 0.8f, size.height * 0.5f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                        drawCircle(Color.Black, radius = size.width * 0.15f, center = Offset(size.width / 2, size.height * 0.5f), style = Stroke(1.5.dp.toPx()))
                        drawRect(Color.Black, topLeft = Offset(size.width * 0.3f, size.height * 0.15f), size = Size(size.width * 0.4f, size.height * 0.1f), style = Stroke(1.5.dp.toPx()))
                    }
                }
                
                // Input
                Box(
                    modifier = Modifier
                        .offset(x = 155.sx(), y = 1600.sy())
                        .size(590.sx(), 90.sy())
                        .border(1.5.dp, Color.Black, RoundedCornerShape(14.dp))
                        .padding(start = 30.sx()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Escribe algo", fontSize = 40.spScaled(), fontWeight = FontWeight.Light, color = Color.Black, maxLines = 1)
                }
                
                // Micrófono
                Box(
                    modifier = Modifier
                        .offset(x = 770.sx(), y = 1590.sy())
                        .size(110.sx(), 110.sy())
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(52.sx(), 52.sy())) {
                        drawRoundRect(Color.Black, topLeft = Offset(size.width * 0.35f, size.height * 0.1f), size = Size(size.width * 0.3f, size.height * 0.4f), cornerRadius = CornerRadius(size.width * 0.15f), style = Stroke(1.5.dp.toPx()))
                        val path = Path().apply {
                            moveTo(size.width * 0.2f, size.height * 0.4f)
                            arcTo(Rect(size.width * 0.2f, size.height * 0.2f, size.width * 0.8f, size.height * 0.7f), 0f, 180f, false)
                        }
                        drawPath(path, Color.Black, style = Stroke(1.5.dp.toPx()))
                        drawLine(Color.Black, start = Offset(size.width / 2, size.height * 0.7f), end = Offset(size.width / 2, size.height * 0.9f), strokeWidth = 1.5.dp.toPx())
                        drawLine(Color.Black, start = Offset(size.width * 0.3f, size.height * 0.9f), end = Offset(size.width * 0.7f, size.height * 0.9f), strokeWidth = 1.5.dp.toPx())
                    }
                }
            }
        }
    }
}
