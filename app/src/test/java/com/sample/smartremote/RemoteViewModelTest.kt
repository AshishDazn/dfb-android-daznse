package com.sample.smartremote

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.RemoteState
import com.sample.smartremote.data.SocketEventsHelper
import com.sample.smartremote.data.WebSocketResponse
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {

    private lateinit var viewModel: RemoteViewModel
    private val audioService = mockk<AudioService>(relaxed = true)
    private val webSocketService = mockk<WebSocketService>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RemoteViewModel(audioService, webSocketService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendRemoteAction sends correct event for Home icon`() {
        val deviceId = "test_device"
        viewModel.selectDevice(deviceId)
        
        viewModel.sendRemoteAction(Icons.Rounded.Home)
        
        verify { webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, SocketEventsHelper.Home)) }
    }

    @Test
    fun `sendRemoteAction sends correct event for Up icon`() {
        val deviceId = "test_device"
        viewModel.selectDevice(deviceId)
        
        viewModel.sendRemoteAction(Icons.Rounded.KeyboardArrowUp)
        
        verify { webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, SocketEventsHelper.Up)) }
    }

    @Test
    fun `TV list update injects All Devices when multiple devices connect`() {
        val listener = viewModel.javaClass.getDeclaredField("wsListener").apply { isAccessible = true }.get(viewModel) as WebSocketListener
        val response = WebSocketResponse(
            event = SocketEventsHelper.EVENT_TV_LIST,
            type = null,
            transcript = null,
            customerId = "customer1",
            devices = listOf(
                com.sample.smartremote.data.RemoteDeviceResponse("1", "TV 1"),
                com.sample.smartremote.data.RemoteDeviceResponse("2", "TV 2")
            )
        )
        
        listener.onMessage(mockk(), com.google.gson.Gson().toJson(response))
        
        assertEquals(3, viewModel.devices.value.size)
        assertEquals("__all__", viewModel.devices.value[0].id)
        assertEquals("All Devices", viewModel.devices.value[0].name)
    }

    @Test
    fun `Voice command 'go home' triggers Home remote action`() {
        val deviceId = "test_device"
        viewModel.selectDevice(deviceId)
        val listener = viewModel.javaClass.getDeclaredField("wsListener").apply { isAccessible = true }.get(viewModel) as WebSocketListener
        val response = WebSocketResponse(
            event = null,
            type = "final_transcript",
            transcript = "go home",
            customerId = null,
            devices = null
        )
        
        listener.onMessage(mockk(), com.google.gson.Gson().toJson(response))
        
        verify { webSocketService.sendEventData(SocketEventsHelper.sendRemoteAction(deviceId, SocketEventsHelper.Home)) }
    }
}
