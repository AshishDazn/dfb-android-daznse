package com.sample.smartremote

expect object Base64 {
    fun decode(src: String): ByteArray
}
