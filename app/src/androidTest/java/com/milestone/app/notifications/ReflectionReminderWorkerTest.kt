package com.milestone.app.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The reminder worker, run for real on a device.
 *
 * Its dependencies are resolved through a Hilt entry point at execution time
 * rather than injected at construction, which is the kind of wiring that compiles
 * perfectly and then throws the first time it actually runs — a week after the
 * user turned the setting on, in the background, where nobody sees the crash.
 * Running it once here is the only way that failure surfaces before release.
 */
@RunWith(AndroidJUnit4::class)
class ReflectionReminderWorkerTest {

    @Test
    fun the_worker_resolves_its_dependencies_and_finishes() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<ReflectionReminderWorker>(context).build()

        val result = worker.doWork()

        // Success either way: posting and deciding not to post are both the
        // worker doing its job. A failure here means the entry point is broken.
        assertTrue(result is ListenableWorker.Result.Success)
    }
}
