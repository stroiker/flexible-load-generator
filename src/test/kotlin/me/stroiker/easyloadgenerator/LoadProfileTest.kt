package me.stroiker.easyloadgenerator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.FutureTask

internal class LoadProfileTest {

    @Test
    fun `should invoke job exactly times with invocation count scale and right invoke order - positive`() {
        var jobCounter = 0L

        LoadProfile.builder()
            .phase(LoadPhase(LoadType.UP, Duration.ofSeconds(20L), 1.0, 1.0))
            .phase(LoadPhase(LoadType.FLAT, Duration.ofSeconds(30L), 1.0, 1.0))
            .phase(LoadPhase(LoadType.DOWN, Duration.ofSeconds(10L), 1.0, 1.0))
            .job { times -> FutureTask({ repeat(times.toInt()) { jobCounter++ } }, true).also { it.run() } }
            .onEndCallback { jobCounter *= 2 }
            .build().also { profile ->
                profile.run()
                assertEquals(1714L, jobCounter)
            }
    }

    @Test
    fun `should throw exception if job is not configured - negative`() {
        LoadProfile.builder()
            .also { profile ->
                Assertions.assertThrows(IllegalStateException::class.java) { profile.build() }.also { ex ->
                    assertEquals("Job must be configured", ex.message)
                }
            }
    }

    @Test
    fun `should throw exception if callback is not configured - negative`() {
        LoadProfile.builder()
            .phase(LoadPhase(LoadType.UP, Duration.ofSeconds(20L), 1.0, 1.0))
            .job { times -> FutureTask({ }, true).also { it.run() } }
            .also { profile ->
                Assertions.assertThrows(IllegalStateException::class.java) { profile.build() }.also { ex ->
                    assertEquals("Callback must be configured", ex.message)
                }
            }
    }
}