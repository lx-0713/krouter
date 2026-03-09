package com.kmp.common

import androidx.compose.runtime.Composable
import com.kmp.common.components.RootComponent

@Composable
fun App(root: RootComponent) = root.Content()

expect fun getPlatformName(): String
