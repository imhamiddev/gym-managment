package dev.imhamid.gymyar.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.imhamid.gymyar.MainActivity
import dev.imhamid.gymyar.R
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.data.Settings
import dev.imhamid.gymyar.util.TEHRAN
import dev.imhamid.gymyar.util.fa
import dev.imhamid.gymyar.util.remainingDays
import dev.imhamid.gymyar.util.todayTehran
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "tuition"
    private const val WORK = "tuition-reminder"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "یادآوری شهریه", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "یادآوری پایان شهریه (۲ روز قبل، ۱ روز قبل و روز پایان)"
                },
            )
        }
    }

    /** Daily check at about 09:00 Tehran time. No Firebase: the phone itself looks at your tuition. */
    fun schedule(context: Context) {
        val now = ZonedDateTime.now(TEHRAN)
        var next = now.with(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

class ReminderWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!Settings.remindersEnabled || !Api.signedIn.value || !Reminders.canNotify(context)) return Result.success()
        // Works offline too: Repo falls back to the last cached answer.
        val r = Repo.athleteHome()
        if (r !is Res.Ok) return Result.success()
        val m = r.value.membership ?: return Result.success()
        if (!m.isApproved || m.payments.isEmpty()) return Result.success()

        val today = todayTehran()
        val remaining = remainingDays(m.payments.map { it.paidDate to it.durationDays }, today) ?: return Result.success()
        if (remaining !in 0..2) return Result.success()

        val endDate = m.payments.maxOf { it.endDate }
        val key = "$endDate#$remaining"
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        if (prefs.getString("last", null) == key) return Result.success()

        val text = when (remaining) {
            2 -> "${2.fa()} روز تا پایان شهریه‌ات مانده است."
            1 -> "فقط ${1.fa()} روز تا پایان شهریه‌ات مانده است."
            else -> "شهریه‌ات تمام شده است. برای تمدید با مربی هماهنگ کن."
        }
        Reminders.createChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآوری شهریه · ${m.gym.name}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1001, notification) }
        prefs.edit().putString("last", key).apply()
        return Result.success()
    }
}
