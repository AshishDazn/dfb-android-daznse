package com.sample.smartremote

import android.util.Base64 as AndroidBase64

actual object Base64 {
    actual fun decode(src: String): ByteArray {
        return AndroidBase64.decode(src, AndroidBase64.URL_SAFE or AndroidBase64.NO_WRAP or AndroidBase64.NO_PADDING)
    }
}
