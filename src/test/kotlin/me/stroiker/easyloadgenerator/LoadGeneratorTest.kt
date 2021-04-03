package me.stroiker.easyloadgenerator

import me.stroiker.easyloadgenerator.config.LoadGeneratorProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import java.time.Duration
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal class LoadGeneratorTest {

    @Test
    fun `should create load profile, then start and stop generator - positive`() {
        LoadGenerator(LoadGeneratorProperties(), object : LoadGeneratorJob {
            override fun onEach() {}
        }).also { generator ->
            assertNull(generator.profile)
            generator.createLoadProfile(listOf(LoadPhase(LoadType.UP, Duration.ofSeconds(10L), 1.0, 1.0)))
            assertNotNull(generator.profile)
            assertEquals(0, generator.profile!!.getProgressPercentage())
            assertFalse(generator.getStatus())
            generator.start()
            assertTrue(generator.getStatus())
            generator.stop()
            assertFalse(generator.getStatus())
        }
    }

    @Test
    fun `should ignore repeated generator start - positive`() {
        LoadGenerator(LoadGeneratorProperties(), object : LoadGeneratorJob {
            override fun onEach() {}
        }).also { generator ->
            generator.createLoadProfile(listOf(LoadPhase(LoadType.UP, Duration.ofSeconds(10L), 1.0, 1.0)))
            assertNotNull(generator.profile)
            val spy = Mockito.spy(generator.profile!!)
            (generator::class.memberProperties.find { it.name == "profile" } as KMutableProperty<*>).also {
                it.setter.isAccessible = true
                it.setter.call(generator, spy)
            }
            generator.start()
            generator.start()
            Mockito.verify(spy, times(1)).run()
        }
    }

    @Test
    fun `should throw exception if generator starts without load profile has been created - negative`() {
        LoadGenerator(LoadGeneratorProperties(), object : LoadGeneratorJob {
            override fun onEach() {}
        }).also { generator ->
            assertThrows(IllegalStateException::class.java) { generator.start() }.also { ex ->
                assertEquals("Load profile must be created before starting", ex.message)
            }
        }
    }
}