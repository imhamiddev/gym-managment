package dev.imhamid.gymyar.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dev.imhamid.gymyar.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * In-app updates: asks the public GitHub Releases API for the latest release, compares it with the
 * installed version and downloads the matching APK so the system installer can replace the app.
 * (The repository is public, so no token is needed — and none must ever be put in the app.)
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/imhamiddev/gym-managment/releases/latest"

    data class Release(val version: String, val url: String, val size: Long)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Installed version without the "-debug" suffix, e.g. "1.3.0". */
    val currentVersion: String get() = BuildConfig.VERSION_NAME.substringBefore('-')

    /** The newest published release that has an APK for this build variant, or null (offline / none). */
    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
            val text = client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
                ?: return@runCatching null
            val o = Api.json.parseToJsonElement(text).jsonObject
            if (o["draft"]?.jsonPrimitive?.booleanOrNull == true || o["prerelease"]?.jsonPrimitive?.booleanOrNull == true) {
                return@runCatching null
            }
            val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
            val assets = o["assets"]?.jsonArray?.map { it.jsonObject } ?: return@runCatching null
            val wantDebug = BuildConfig.DEBUG
            val apk = assets.firstOrNull {
                val n = it["name"]?.jsonPrimitive?.contentOrNull ?: return@firstOrNull false
                n.endsWith(".apk") && n.endsWith("-debug.apk") == wantDebug
            } ?: return@runCatching null
            Release(
                version = tag.removePrefix("v"),
                url = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null,
                size = apk["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.getOrNull()
    }

    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    /** Downloads the APK into the app cache; [onProgress] gets 0..1 (or null when the size is unknown). */
    suspend fun download(context: Context, rel: Release, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").also { it.mkdirs() }
        dir.listFiles { f -> f.name.startsWith("update-") }?.forEach { it.delete() }
        val part = File(dir, "update-${rel.version}.part")
        val out = File(dir, "update-${rel.version}.apk")
        try {
            val req = Request.Builder().url(rel.url).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                val body = r.body ?: throw IOException("empty body")
                val total = body.contentLength().takeIf { it > 0 } ?: rel.size.takeIf { it > 0 }
                body.byteStream().use { input ->
                    part.outputStream().use { o ->
                        val buf = ByteArray(32 * 1024)
                        var done = 0L
                        var lastPct = -1
                        onProgress(if (total != null) 0f else null)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total != null) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != lastPct) { lastPct = pct; onProgress(done.toFloat() / total) }
                            }
                        }
                    }
                }
            }
            if (rel.size > 0 && part.length() != rel.size) throw IOException("incomplete download")
            if (!part.renameTo(out)) throw IOException("rename failed")
            out
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
    }

    /** Android 8+ asks the user to allow installs from this app once. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val i = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
