package com.sample.smartremote.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sample.smartremote.ui.theme.SmartRemoteTheme

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DpadBackgroundGlowPreview() {
    SmartRemoteTheme {
        DpadBackgroundGlow(
            modifier = Modifier.size(300.dp),
        )
    }
}
