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

    private fun drawText(
        canvas: Canvas, text: String, paint: TextPaint, left: Float, top: Float, width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
    ): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.25f)
            .build()
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    /** A4-ratio poster: title, big QR, three steps. [color] = blue header, otherwise pure black and white. */
    fun poster(context: Context, gymName: String, payload: String, color: Boolean): Bitmap {
        val w = 1240
        val h = 1754
        val accent = if (color) Color.parseColor("#0A7CF0") else Color.BLACK
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
        val regular = ResourcesCompat.getFont(context, R.font.vazirmatn_regular)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = accent }
        if (color) c.drawRect(0f, 0f, w.toFloat(), 330f, fill)

        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 84f; this.color = if (color) Color.WHITE else Color.BLACK
        }
        val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 42f; this.color = if (color) Color.parseColor("#DCEBFD") else Color.DKGRAY
        }
        var y = 80f
        y = drawText(c, "ثبت حضور", title, 80f, y, w - 160)
        y = drawText(c, gymName, title, 80f, y + 4f, w - 160)
        drawText(c, "QR را با اپ باشگاه‌یار اسکن کنید", sub, 80f, y + 8f, w - 160)

        val qrSize = 860
        val qr = bitmap(payload, qrSize)
        val qx = (w - qrSize) / 2f
        val qy = 400f
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; this.color = accent }
        c.drawRoundRect(RectF(qx - 40, qy - 40, qx + qrSize + 40, qy + qrSize + 40), 48f, 48f, frame)
        c.drawBitmap(qr, qx, qy, null)

        val step = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 52f; this.color = Color.BLACK }
        var sy = qy + qrSize + 110f
        val steps = listOf("۱  اپ باشگاه‌یار را باز کنید", "۲  دکمه‌ی «ثبت حضور با QR» را بزنید", "۳  QR را اسکن کنید؛ حضور شما خودکار ثبت می‌شود")
        for (s in steps) sy = drawText(c, s, step, 100f, sy, w - 200, Layout.Alignment.ALIGN_OPPOSITE) + 18f
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
