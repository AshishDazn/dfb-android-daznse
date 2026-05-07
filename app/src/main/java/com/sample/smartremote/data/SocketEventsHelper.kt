package com.sample.smartremote.data

import com.google.gson.Gson

object SocketEventsHelper {

    const val EVENT_TV_LIST = "tv_list"
    const val EVENT_NICK_NAME = "set_nickname"
    const val VolumeUp = "volumeup"
    const val VolumeDown = "volumedown"
    const val Home = "home"
    const val Schedule = "schedule"
    const val Settings = "settings"
    const val Up = "up"
    const val Down = "down"
    const val Left = "left"
    const val Right = "right"
    const val Ok = "ok"
    const val Back = "back"
    const val Mute = "mute"

    fun audioStartEvent(deviceId: String?): String{
        return Gson().toJson(StartAudioEvent(deviceId = deviceId))
    }

    fun audioEndEvent(deviceId: String?): String{
        return Gson().toJson(EndAudioEndEvent(deviceId = deviceId))
    }


    fun identifyDeviceEvent(deviceId: String?): String {
        return Gson().toJson(IdentifyDeviceEvent(deviceId = deviceId))
    }

    fun renameDevice(deviceId: String, newName: String): String {
        return Gson().toJson(SetDeviceNickName(deviceId = deviceId, nickName = newName, type = EVENT_NICK_NAME))
    }

    fun sendRemoteAction(deviceId: String, command: String): String {
        return Gson().toJson(SendRemoteAction(deviceId = deviceId, cmd = command))
    }
}

data class StartAudioEvent(
    val type: String = "audio_start",
    val sampleRate: Int = 16000,
    val deviceId: String?,
    val streaming: Boolean = true
)
data class EndAudioEndEvent(
    val type: String = "audio_end",
    val deviceId: String?
)
data class SetDeviceNickName(
    val deviceId: String,
    val nickName: String,
    val type: String
)

data class IdentifyDeviceEvent(
    val type: String = "identify",
    val deviceId: String?
)

data class SendRemoteAction(
    val deviceId: String,
    val cmd: String
)
