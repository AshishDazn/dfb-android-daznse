package com.sample.smartremote

import com.google.gson.Gson
import com.sample.smartremote.data.RemoteDevice
import com.sample.smartremote.data.WebSocketResponse
import com.sample.smartremote.data.repository.AuthRepository
import com.sample.smartremote.data.repository.RemoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class RemoteRepositoryTest {

    private lateinit var repository: RemoteRepository
    private val webSocketService = mock(WebSocketService::class.java)
    private val authRepository = mock(AuthRepository::class.java)
    private val gson = Gson()

    @Before
    fun setup() {
        repository = RemoteRepository(webSocketService, authRepository, gson)
    }

    @Test
    fun `test handleDeviceList with single device`() = runBlocking {
        val json = """
            {
                "event": "tv_list",
                "devices": [
                    {"id": "dev1", "nickName": "TV 1"}
                ]
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, WebSocketResponse::class.java)
        
        // We need to call the private handleDeviceList method or trigger it via message
        // For testing, I'll make handleDeviceList internal or test via reflection/public trigger
        // Let's use reflection for simplicity in this task context if I don't want to change visibility
        val method = RemoteRepository::class.java.getDeclaredMethod("handleDeviceList", WebSocketResponse::class.java)
        method.isAccessible = true
        method.invoke(repository, response)

        val devices = repository.devices.first()
        val selectedId = repository.selectedDeviceId.first()

        assertEquals(1, devices.size)
        assertEquals("dev1", devices[0].id)
        assertEquals("dev1", selectedId)
    }

    @Test
    fun `test handleDeviceList with multiple devices selects All Devices`() = runBlocking {
        val json = """
            {
                "event": "tv_list",
                "devices": [
                    {"id": "dev1", "nickName": "TV 1"},
                    {"id": "dev2", "nickName": "TV 2"}
                ]
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, WebSocketResponse::class.java)
        
        val method = RemoteRepository::class.java.getDeclaredMethod("handleDeviceList", WebSocketResponse::class.java)
        method.isAccessible = true
        method.invoke(repository, response)

        val devices = repository.devices.first()
        val selectedId = repository.selectedDeviceId.first()

        assertEquals(3, devices.size) // __all__ + dev1 + dev2
        assertEquals("__all__", devices[0].id)
        assertEquals("__all__", selectedId)
    }
}
