package me.stroiker.flexibleloadgenerator

import me.stroiker.flexibleloadgenerator.utils.calculateTm90
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtilsKtTest {

    @Test
    fun `should calculate TM90 latency`() {
        val list = listOf<Long>(12, 2, 7, 3, 5, 1, 6)
        assertEquals(5, list.sum() / list.size) // avg
        assertEquals(4, calculateTm90(list))
        assertEquals(450, calculateTm90((1L.. 1000L).toList()))
        assertEquals(5, calculateTm90(listOf(5, 6)))
        assertEquals(5, calculateTm90(listOf(5)))
        assertEquals(0, calculateTm90(listOf()))
        assertEquals(-2, calculateTm90(listOf(-2)))
    }
}