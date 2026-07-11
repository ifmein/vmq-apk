package vmq.data

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ListenerStatusStoreTest {
    private lateinit var store: ListenerStatusStore

    @Before
    fun setup() {
        val context = RuntimeEnvironment.application.applicationContext
        store = ListenerStatusStore(context)
    }

    @Test
    fun `default values are empty`() {
        assertFalse(store.isListenerConnected)
        assertNull(store.lastListenerConnectedAt)
        assertNull(store.lastNotificationEventAt)
        assertNull(store.lastHeartbeatSuccessAt)
        assertNull(store.lastHeartbeatErrorMessage)
    }

    @Test
    fun `recordListenerConnected updates status`() {
        store.recordListenerConnected()

        assertTrue(store.isListenerConnected)
        assertTrue(store.lastListenerConnectedAt!!.isAfter(Instant.now().minusSeconds(5)))
    }

    @Test
    fun `recordListenerDisconnected updates status`() {
        store.recordListenerConnected()
        store.recordListenerDisconnected()

        assertFalse(store.isListenerConnected)
    }

    @Test
    fun `recordNotificationEvent updates timestamp`() {
        store.recordNotificationEvent()

        assertTrue(store.lastNotificationEventAt!!.isAfter(Instant.now().minusSeconds(5)))
    }

    @Test
    fun `recordHeartbeatSuccess updates success timestamp`() {
        store.recordHeartbeatSuccess()

        assertTrue(store.lastHeartbeatSuccessAt!!.isAfter(Instant.now().minusSeconds(5)))
        assertNull(store.lastHeartbeatErrorMessage)
    }

    @Test
    fun `recordHeartbeatFailure updates error message`() {
        store.recordHeartbeatFailure("Connection timeout")

        assertNull(store.lastHeartbeatSuccessAt)
        assertEquals("Connection timeout", store.lastHeartbeatErrorMessage)
    }

    @Test
    fun `isListenerConnected can be set directly`() {
        store.isListenerConnected = true
        assertTrue(store.isListenerConnected)

        store.isListenerConnected = false
        assertFalse(store.isListenerConnected)
    }

    @Test
    fun `lastHeartbeatErrorMessage can be set directly`() {
        store.lastHeartbeatErrorMessage = "Test error"
        assertEquals("Test error", store.lastHeartbeatErrorMessage)

        store.lastHeartbeatErrorMessage = null
        assertNull(store.lastHeartbeatErrorMessage)
    }
}
