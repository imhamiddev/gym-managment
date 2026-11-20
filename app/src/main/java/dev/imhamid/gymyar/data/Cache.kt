package dev.imhamid.gymyar.data

import dev.imhamid.gymyar.GymyarApp
import java.io.File

/** Last successful server answers, kept per user so the app can still show data without internet. */
object Cache {
    private fun dir(): File = File(GymyarApp.instance.filesDir, "cache").also { it.mkdirs() }

    private fun file(uid: String, name: String): File =
        File(dir(), "${uid}_${name}".replace(Regex("[^A-Za-z0-9_\\-]"), "_") + ".json")

    fun put(uid: String, name: String, raw: String) {
        runCatching { file(uid, name).writeText(raw) }
    }

    fun get(uid: String, name: String): String? =
        runCatching { file(uid, name).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clearAll() {
        runCatching { dir().listFiles()?.forEach { it.delete() } }
    }
}
