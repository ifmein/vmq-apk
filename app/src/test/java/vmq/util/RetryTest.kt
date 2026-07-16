package vmq.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RetryTest {
    @Test
    fun `succeeds on first attempt`() = runTest {
        var attempts = 0

        val result = retry { attempt ->
            attempts += 1
            "result-$attempt"
        }

        assertEquals("result-0", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `retries retryable exception and eventually succeeds`() = runTest {
        var attempts = 0

        val result = retry(RetryConfig(initialDelayMs = 1L)) {
            attempts += 1
            if (attempts < 3) throw RetryableException("temporary failure")
            "recovered"
        }

        assertEquals("recovered", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `exhausts all attempts and throws last exception`() = runTest {
        var attempts = 0

        try {
            retry(RetryConfig(maxAttempts = 3, initialDelayMs = 1L)) { attempt ->
                attempts += 1
                throw RetryableException("failure-$attempt")
            }
            fail("Expected RetryableException")
        } catch (error: RetryableException) {
            assertEquals("failure-2", error.message)
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `non retryable exception propagates immediately without retry`() = runTest {
        var attempts = 0

        try {
            retry { 
                attempts += 1
                throw IllegalArgumentException("invalid request")
            }
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals("invalid request", error.message)
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `delayFor computes capped exponential backoff`() {
        val config = RetryConfig(initialDelayMs = 1_000L, maxDelayMs = 2_500L)

        assertEquals(1_000L, config.delayFor(0))
        assertEquals(2_000L, config.delayFor(1))
        assertEquals(2_500L, config.delayFor(2))
    }
}
