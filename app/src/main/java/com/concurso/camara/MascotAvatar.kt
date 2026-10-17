package com.concurso.camara

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MascotMood(
    val title: String,
    val subtitle: String,
    val bgColorHex: Long
) {
    HAPPY(
        "Orgulho da Câmara! 🐶✨",
        "Ritmo de aprovado! O cachorro da posse está felizão com você.",
        0xFF10B981
    ),
    COOL(
        "Modo Analista Ativado 😎",
        "Gabaritando tudo no QConcursos! Ninguém segura o Mario Bonito.",
        0xFF3B82F6
    ),
    SUSPICIOUS(
        "Hmm... Tô de olho! 🤨",
        "Faz algumas horas que você não abre um simulado... Vai deixar acumular?",
        0xFFF59E0B
    ),
    SAD(
        "Você me esqueceu? 🥺",
        "O Regimento Interno está chorando no cantinho esperando sua leitura...",
        0xFF64748B
    ),
    PLEADING(
        "POR FAVOR, SÓ 5 QUESTÕES! 😭",
        "Eu imploro, Mario! Clica em uma matéria e abre o QConcursos agora!",
        0xFF8B5CF6
    ),
    ANGRY(
        "AGORA O BICHO PEGOU! 😡🔥",
        "Sem desculpas! A vaga na Câmara não cai do céu! VAI ESTUDAR AGORA!",
        0xFFEF4444
    ),
    DESPAIR(
        "COLAPSO LEGISLATIVO!! 😱💀",
        "SOCORRO! Mais de um dia sem simulado! Salve a nossa nomeação!!",
        0xFF7F1D1D
    )
}

@Composable
fun MascotFaceCanvas(
    mood: MascotMood,
    size: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(
            color = Color(mood.bgColorHex),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val earColor = Color(0xFFB86B14)
        val headColor = when (mood) {
            MascotMood.ANGRY -> Color(0xFFEA580C)
            MascotMood.DESPAIR -> Color(0xFFD97706)
            else -> Color(0xFFF59E0B)
        }
        val muzzleColor = Color(0xFFFEF3C7)

        val earDrop = if (mood == MascotMood.SAD || mood == MascotMood.PLEADING) h * 0.12f else 0f
        drawOval(
            color = earColor,
            topLeft = Offset(w * 0.14f, h * 0.16f + earDrop),
            size = Size(w * 0.22f, h * 0.32f)
        )
        drawOval(
            color = earColor,
            topLeft = Offset(w * 0.64f, h * 0.16f + earDrop),
            size = Size(w * 0.22f, h * 0.32f)
        )

        drawOval(
            color = headColor,
            topLeft = Offset(w * 0.20f, h * 0.22f),
            size = Size(w * 0.60f, h * 0.58f)
        )

        drawOval(
            color = muzzleColor,
            topLeft = Offset(w * 0.28f, h * 0.45f),
            size = Size(w * 0.44f, h * 0.32f)
        )

        val tiePath = Path().apply {
            moveTo(w * 0.46f, h * 0.77f)
            lineTo(w * 0.54f, h * 0.77f)
            lineTo(w * 0.57f, h * 0.93f)
            lineTo(w * 0.50f, h * 0.97f)
            lineTo(w * 0.43f, h * 0.93f)
            close()
        }
        drawPath(tiePath, color = Color(0xFF1D4ED8))

        if (mood == MascotMood.COOL) {
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(w * 0.25f, h * 0.36f),
                size = Size(w * 0.22f, h * 0.14f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(w * 0.53f, h * 0.36f),
                size = Size(w * 0.22f, h * 0.14f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(w * 0.20f, h * 0.40f),
                end = Offset(w * 0.80f, h * 0.40f),
                strokeWidth = w * 0.03f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(w * 0.29f, h * 0.47f),
                end = Offset(w * 0.36f, h * 0.38f),
                strokeWidth = w * 0.025f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(w * 0.57f, h * 0.47f),
                end = Offset(w * 0.64f, h * 0.38f),
                strokeWidth = w * 0.025f
            )
        } else {
            val eyeRadius = if (mood == MascotMood.PLEADING || mood == MascotMood.DESPAIR) w * 0.11f else w * 0.095f
            drawCircle(Color.White, radius = eyeRadius, center = Offset(w * 0.37f, h * 0.43f))
            drawCircle(Color.White, radius = eyeRadius, center = Offset(w * 0.63f, h * 0.43f))

            val pupilRadius = when (mood) {
                MascotMood.DESPAIR -> w * 0.028f
                MascotMood.PLEADING -> w * 0.075f
                else -> w * 0.055f
            }
            val pupilColor = if (mood == MascotMood.ANGRY) Color(0xFF7F1D1D) else Color(0xFF0F172A)
            drawCircle(pupilColor, radius = pupilRadius, center = Offset(w * 0.37f, h * 0.43f))
            drawCircle(pupilColor, radius = pupilRadius, center = Offset(w * 0.63f, h * 0.43f))

            drawCircle(Color.White, radius = w * 0.022f, center = Offset(w * 0.35f, h * 0.40f))
            drawCircle(Color.White, radius = w * 0.022f, center = Offset(w * 0.61f, h * 0.40f))
        }

        if (mood == MascotMood.SAD || mood == MascotMood.PLEADING || mood == MascotMood.DESPAIR) {
            val tearHeight = if (mood == MascotMood.PLEADING) h * 0.25f else h * 0.12f
            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(w * 0.32f, h * 0.50f),
                size = Size(w * 0.07f, tearHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(w * 0.61f, h * 0.50f),
                size = Size(w * 0.07f, tearHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        val browColor = Color(0xFF451A03)
        val strokeW = w * 0.04f
        when (mood) {
            MascotMood.ANGRY -> {
                drawLine(browColor, Offset(w * 0.26f, h * 0.30f), Offset(w * 0.46f, h * 0.38f), strokeW)
                drawLine(browColor, Offset(w * 0.74f, h * 0.30f), Offset(w * 0.54f, h * 0.38f), strokeW)
            }
            MascotMood.SAD, MascotMood.PLEADING, MascotMood.DESPAIR -> {
                drawLine(browColor, Offset(w * 0.26f, h * 0.37f), Offset(w * 0.44f, h * 0.30f), strokeW)
                drawLine(browColor, Offset(w * 0.74f, h * 0.37f), Offset(w * 0.56f, h * 0.30f), strokeW)
            }
            MascotMood.SUSPICIOUS -> {
                drawLine(browColor, Offset(w * 0.27f, h * 0.35f), Offset(w * 0.45f, h * 0.35f), strokeW)
                drawLine(browColor, Offset(w * 0.55f, h * 0.30f), Offset(w * 0.73f, h * 0.26f), strokeW)
            }
            else -> {
                drawLine(browColor, Offset(w * 0.28f, h * 0.31f), Offset(w * 0.44f, h * 0.29f), strokeW)
                drawLine(browColor, Offset(w * 0.56f, h * 0.29f), Offset(w * 0.72f, h * 0.31f), strokeW)
            }
        }

        drawOval(
            color = Color(0xFF1E293B),
            topLeft = Offset(w * 0.44f, h * 0.51f),
            size = Size(w * 0.12f, h * 0.08f)
        )

        when (mood) {
            MascotMood.HAPPY, MascotMood.COOL -> {
                drawArc(
                    color = browColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.38f, h * 0.56f),
                    size = Size(w * 0.24f, h * 0.14f),
                    style = Stroke(width = w * 0.03f)
                )
                if (mood == MascotMood.HAPPY) {
                    drawOval(
                        color = Color(0xFFFB7185),
                        topLeft = Offset(w * 0.45f, h * 0.62f),
                        size = Size(w * 0.10f, h * 0.12f)
                    )
                }
            }
            MascotMood.SUSPICIOUS -> {
                drawLine(
                    color = browColor,
                    start = Offset(w * 0.40f, h * 0.65f),
                    end = Offset(w * 0.60f, h * 0.62f),
                    strokeWidth = w * 0.035f
                )
            }
            MascotMood.SAD, MascotMood.PLEADING -> {
                drawArc(
                    color = browColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.40f, h * 0.63f),
                    size = Size(w * 0.20f, h * 0.10f),
                    style = Stroke(width = w * 0.035f)
                )
            }
            MascotMood.ANGRY -> {
                drawRoundRect(
                    color = Color(0xFF450A0A),
                    topLeft = Offset(w * 0.37f, h * 0.61f),
                    size = Size(w * 0.26f, h * 0.10f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawLine(Color.White, Offset(w * 0.39f, h * 0.66f), Offset(w * 0.61f, h * 0.66f), strokeWidth = w * 0.025f)
            }
            MascotMood.DESPAIR -> {
                drawOval(
                    color = Color(0xFF450A0A),
                    topLeft = Offset(w * 0.43f, h * 0.60f),
                    size = Size(w * 0.14f, h * 0.17f)
                )
            }
        }
    }
}

object MascotBitmapGenerator {
    fun createMoodBitmap(mood: MascotMood): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = mood.bgColorHex.toInt()
        canvas.drawRoundRect(
            RectF(0f, 0f, size.toFloat(), size.toFloat()),
            size * 0.12f,
            size * 0.12f,
            paint
        )

        paint.color = AndroidColor.parseColor("#B86B14")
        canvas.drawOval(RectF(size * 0.06f, size * 0.10f, size * 0.36f, size * 0.48f), paint)
        canvas.drawOval(RectF(size * 0.64f, size * 0.10f, size * 0.94f, size * 0.48f), paint)

        paint.color = when (mood) {
            MascotMood.ANGRY -> AndroidColor.parseColor("#EA580C")
            MascotMood.DESPAIR -> AndroidColor.parseColor("#D97706")
            else -> AndroidColor.parseColor("#F59E0B")
        }
        canvas.drawOval(RectF(size * 0.15f, size * 0.18f, size * 0.85f, size * 0.86f), paint)

        paint.color = AndroidColor.parseColor("#FEF3C7")
        canvas.drawOval(RectF(size * 0.25f, size * 0.47f, size * 0.75f, size * 0.81f), paint)

        paint.color = AndroidColor.WHITE
        val eyeRadius = size * 0.105f
        canvas.drawCircle(size * 0.37f, size * 0.43f, eyeRadius, paint)
        canvas.drawCircle(size * 0.63f, size * 0.43f, eyeRadius, paint)

        paint.color = if (mood == MascotMood.ANGRY) AndroidColor.parseColor("#7F1D1D") else AndroidColor.parseColor("#0F172A")
        val pRadius = if (mood == MascotMood.DESPAIR) size * 0.03f else size * 0.055f
        canvas.drawCircle(size * 0.37f, size * 0.43f, pRadius, paint)
        canvas.drawCircle(size * 0.63f, size * 0.43f, pRadius, paint)

        paint.color = AndroidColor.WHITE
        canvas.drawCircle(size * 0.345f, size * 0.395f, size * 0.025f, paint)
        canvas.drawCircle(size * 0.605f, size * 0.395f, size * 0.025f, paint)

        paint.color = AndroidColor.parseColor("#FB7185")
        paint.alpha = 150
        canvas.drawOval(RectF(size * 0.20f, size * 0.55f, size * 0.31f, size * 0.62f), paint)
        canvas.drawOval(RectF(size * 0.69f, size * 0.55f, size * 0.80f, size * 0.62f), paint)
        paint.alpha = 255

        if (mood == MascotMood.SAD || mood == MascotMood.PLEADING || mood == MascotMood.DESPAIR) {
            paint.color = AndroidColor.parseColor("#38BDF8")
            canvas.drawRoundRect(RectF(size * 0.32f, size * 0.51f, size * 0.38f, size * 0.69f), size * 0.03f, size * 0.03f, paint)
            canvas.drawRoundRect(RectF(size * 0.62f, size * 0.51f, size * 0.68f, size * 0.69f), size * 0.03f, size * 0.03f, paint)
        }

        paint.color = AndroidColor.parseColor("#451A03")
        paint.strokeWidth = size * 0.035f
        paint.strokeCap = Paint.Cap.ROUND
        if (mood == MascotMood.ANGRY) {
            canvas.drawLine(size * 0.26f, size * 0.30f, size * 0.46f, size * 0.38f, paint)
            canvas.drawLine(size * 0.74f, size * 0.30f, size * 0.54f, size * 0.38f, paint)
        } else if (mood == MascotMood.SAD || mood == MascotMood.PLEADING || mood == MascotMood.DESPAIR) {
            canvas.drawLine(size * 0.26f, size * 0.37f, size * 0.44f, size * 0.30f, paint)
            canvas.drawLine(size * 0.74f, size * 0.37f, size * 0.56f, size * 0.30f, paint)
        }

        paint.color = AndroidColor.parseColor("#1E293B")
        canvas.drawOval(RectF(size * 0.44f, size * 0.52f, size * 0.56f, size * 0.60f), paint)
        paint.color = AndroidColor.parseColor("#94A3B8")
        canvas.drawOval(RectF(size * 0.465f, size * 0.525f, size * 0.505f, size * 0.545f), paint)

        paint.color = AndroidColor.parseColor("#451A03")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.025f
        val smile = AndroidPath().apply {
            moveTo(size * 0.40f, size * 0.64f)
            cubicTo(size * 0.44f, size * 0.74f, size * 0.56f, size * 0.74f, size * 0.60f, size * 0.64f)
        }
        canvas.drawPath(smile, paint)
        paint.style = Paint.Style.FILL

        paint.color = AndroidColor.parseColor("#1D4ED8")
        val tie = AndroidPath().apply {
            moveTo(size * 0.46f, size * 0.80f)
            lineTo(size * 0.54f, size * 0.80f)
            lineTo(size * 0.58f, size * 0.89f)
            lineTo(size * 0.50f, size * 0.96f)
            lineTo(size * 0.42f, size * 0.89f)
            close()
        }
        canvas.drawPath(tie, paint)

        return bmp
    }
}
