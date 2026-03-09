package com.kmp.common

import androidx.compose.runtime.Composable
import com.kmp.common.components.RootComponent

actual fun getPlatformName(): String = "Android"

@Composable fun MainView(root: RootComponent) = App(root)
