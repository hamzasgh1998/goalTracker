package com.milestone.app.notifications

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
import com.milestone.app.MainActivity
import com.milestone.app.R
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.repository.ReflectionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The weekly nudge to sit down with the week.
 *
 * This is the one thing the app genuinely needs the operating system for. Every
 * other feature is available the moment the user opens Milestone; reflection is
 * the one that only happens if something reminds them, and an app they have to
 * remember to open is an app they stop opening.
 *
 * It is off until the user turns it on, fires once a week, and says nothing about
 * what they did or didn't do — the copy is an invitation, not a scoreboard.
 */
@Singleton
class ReflectionReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: AppClock,
) {

    /**
     * Brings the scheduled work in line with the user's setting.
     *
     * Safe to call on every launch and after every settings change: the work is
     * uniquely named and [ExistingPeriodicWorkPolicy.UPDATE] replaces the schedule
     * in place rather than stacking a second one.
     */
    fun apply(enabled: Boolean, hour: Int) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<ReflectionReminderWorker>(
            repeatInterval = Duration.ofDays(7),
        )
            .setInitialDelay(
                delayUntilNextReminder(ZonedDateTime.ofInstant(clock.now(), clock.zone()), hour),
            )
            .build()

        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    companion object {
        const val WORK_NAME = "weekly_reflection_reminder"
    }
}


/**
 * How long from [now] until the next Sunday at [hour], local time.
 *
 * A free function rather than a method because it is pure arithmetic, and keeping
 * it out of the scheduler means testing it needs no Android context at all.
 *
 * Resolved to a duration because WorkManager deals in elapsed time, not
 * wall-clock dates. A week that crosses a daylight-saving change will fire an
 * hour out once; rescheduling on every fire to avoid that is a lot of machinery
 * for an hour.
 */
internal fun delayUntilNextReminder(now: ZonedDateTime, hour: Int): Duration {
    val target = now
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .withHour(hour.coerceIn(0, 23))
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    // "Next or same" lands on today when it is already Sunday, which may well be
    // in the past by the time the user turns this on.
    val next = if (target.isAfter(now)) target else target.plusWeeks(1)
    return Duration.between(now, next)
}

/**
 * Posts the reminder, unless the user has already written this week.
 *
 * The check is the whole reason this is a worker rather than a plain alarm:
 * reminding someone to do something they have already done is how a notification
 * earns itself a permanent dismissal.
 */
class ReflectionReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /**
     * Dependencies are pulled from Hilt by hand.
     *
     * `@HiltWorker` would be tidier but needs the `androidx.hilt:hilt-work`
     * artifact and a custom `Configuration.Provider` on the Application, which is
     * a lot of wiring for one worker with two dependencies.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun reflectionRepository(): ReflectionRepository
        fun clock(): AppClock
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            Dependencies::class.java,
        )

        val weekStart = deps.clock().weekStart()
        val alreadyWritten = deps.reflectionRepository().getForWeek(weekStart) != null
        if (alreadyWritten) return Result.success()

        postReminder(context)
        return Result.success()
    }
}

/** Created lazily; calling this repeatedly is how the platform expects it. */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        CHANNEL_ID,
        "Weekly reflection",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "A once-a-week nudge to look back on the week."
        setShowBadge(false)
    }
    context.getSystemService(NotificationManager::class.java)
        ?.createNotificationChannel(channel)
}

/**
 * Posts the notification, silently doing nothing without permission.
 *
 * On Android 13+ the user can revoke notifications at any time, including between
 * scheduling the work and it running, so the permission is re-checked here rather
 * than trusted from when the setting was switched on.
 */
private fun postReminder(context: Context) {
    ensureNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_REFLECTION, true)
    }
    val pending = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("How was your week?")
        // No counts, no streak, no judgement — the app does not know yet whether
        // it was a good week, and saying so would be guessing.
        .setContentText("Five questions, a few minutes.")
        .setAutoCancel(true)
        .setContentIntent(pending)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    runCatching {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

const val CHANNEL_ID = "weekly_reflection"
const val EXTRA_OPEN_REFLECTION = "open_reflection"
private const val NOTIFICATION_ID = 1001
