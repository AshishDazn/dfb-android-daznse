package com.sample.smartremote

import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.NSDataBase64DecodingOptions
import platform.posix.size_t
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual object Base64 {
    actual fun decode(src: String): ByteArray {
        val normalizedSrc = src
            .replace('-', '+')
            .replace('_', '/')
            .let { 
                val padding = it.length % 4
                if (padding > 0) it + "=".repeat(4 - padding) else it
            }
            
        val data = NSData.create(base64EncodedString = normalizedSrc, options = 0UL)
            ?: return ByteArray(0)
            
        val bytes = data.bytes?.reinterpret<ByteVar>()
        val length = data.length.toInt()
        
        return if (bytes != null) {
            ByteArray(length).apply {
                for (i in 0 until length) {
                    this[i] = bytes[i]
                }
            }
        } else {
            ByteArray(0)
        }
    }
}
