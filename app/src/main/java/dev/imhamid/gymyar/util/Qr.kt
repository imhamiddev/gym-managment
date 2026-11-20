package dev.imhamid.gymyar.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextUtils
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.imhamid.gymyar.R
import java.io.File

/** What the gym QR contains. The athlete app scans it and calls check_in(gym, token). */
object Qr {
    private const val SCHEME = "gymyar://checkin"

    fun payload(gymId: String, token: String) = "$SCHEME?g=$gymId&t=$token"

    /** Returns (gymId, token) or null if the text is not one of our QR codes. */
    fun parse(text: String): Pair<String, String>? {
        if (!text.startsWith(SCHEME)) return null
        val query = text.substringAfter('?', "")
        val map = query.split('&').mapNotNull {
            val i = it.indexOf('=')
            if (i <= 0) null else it.substring(0, i) to it.substring(i + 1)
        }.toMap()
        val g = map["g"]
        val t = map["t"]
        return if (!g.isNullOrBlank() && !t.isNullOrBlank()) g to t else null
    }

    fun bitmap(text: String, size: Int, fg: Int = Color.BLACK, bg: Int = Color.WHITE): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val row = y * size
            for (x in 0 until size) pixels[row + x] = if (m[x, y]) fg else bg
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun layoutOf(
        text: String, paint: TextPaint, width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER, maxLines: Int = Int.MAX_VALUE,
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setLineSpacing(0f, 1.15f)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

    /** Draws centered text with its top at [top]; returns the bottom edge. */
    private fun drawText(canvas: Canvas, text: String, paint: TextPaint, left: Float, top: Float, width: Int, maxLines: Int = Int.MAX_VALUE): Float {
        val layout = layoutOf(text, paint, width, maxLines = maxLines)
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    /** Draws text centered on the point ([cx], [cy]). */
    private fun drawCentered(canvas: Canvas, text: String, paint: TextPaint, cx: Float, cy: Float, width: Int) {
        val layout = layoutOf(text, paint, width)
        canvas.save()
        canvas.translate(cx - width / 2f, cy - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * A4 poster: one rounded frame holding the title, the QR in its own rounded frame and the three steps.
     * Steps run right to left (1 on the right). [color] = brand blue lines, otherwise pure black and white.
     */
    fun poster(context: Context, gymName: String, payload: String, color: Boolean): Bitmap {
        val w = 1240
        val h = 1754
        val ink = if (color) Color.parseColor("#2E6B00") else Color.BLACK
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
        val medium = ResourcesCompat.getFont(context, R.font.vazirmatn_medium)

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; this.color = ink }

        // outer frame
        val margin = 110f
        c.drawRoundRect(RectF(margin, margin, w - margin, h - margin), 64f, 64f, line)

        // title
        val padX = 70
        val innerW = (w - 2 * margin).toInt() - 2 * padX
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 92f; this.color = Color.BLACK }
        val titleBottom = drawText(c, "ثبت حضور $gymName", title, margin + padX, margin + 120f, innerW, maxLines = 2)

        // steps (anchored to the bottom of the frame)
        val circleR = 46f
        val stepText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = medium; textSize = 36f; this.color = Color.BLACK }
        val digit = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 52f; this.color = Color.BLACK }
        val steps = listOf(
            "۱" to "دوربین گوشی را\nروی QR بگیرید",
            "۲" to "وارد حساب\nخود شوید",
            "۳" to "حضور شما خودکار\nثبت می‌شود",
        )
        val colW = (w - 2 * margin - 100f) / 3f
        val textW = (colW - 10f).toInt()
        val stepsBottom = h - margin - 90f
        val textH = steps.maxOf { layoutOf(it.second, stepText, textW).height }
        val stepsTop = stepsBottom - (circleR * 2 + 30f + textH)
        steps.forEachIndexed { i, (n, label) ->
            val cx = w - margin - 50f - colW / 2f - i * colW
            val cy = stepsTop + circleR
            c.drawCircle(cx, cy, circleR, line.apply { strokeWidth = 7f })
            drawCentered(c, n, digit, cx, cy, (circleR * 2).toInt())
            drawText(c, label, stepText, cx - textW / 2f, cy + circleR + 30f, textW)
        }
        line.strokeWidth = 10f

        // QR in its own rounded frame, centered between the title and the steps
        val qrSize = 620
        val pad = 56f
        val frameSize = qrSize + 2 * pad
        val space = stepsTop - titleBottom
        val frameTop = titleBottom + ((space - frameSize) / 2f).coerceAtLeast(24f)
        val frameLeft = (w - frameSize) / 2f
        c.drawRoundRect(RectF(frameLeft, frameTop, frameLeft + frameSize, frameTop + frameSize), 56f, 56f, line)
        c.drawBitmap(bitmap(payload, qrSize), frameLeft + pad, frameTop + pad, null)
        return bmp
    }

    fun print(context: Context, bitmap: Bitmap, color: Boolean) {
        PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            colorMode = if (color) PrintHelper.COLOR_MODE_COLOR else PrintHelper.COLOR_MODE_MONOCHROME
            orientation = PrintHelper.ORIENTATION_PORTRAIT
        }.printBitmap("QR حضور", bitmap)
    }

    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "shared").also { it.mkdirs() }
        val file = File(dir, "gymyar-qr.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری QR").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
