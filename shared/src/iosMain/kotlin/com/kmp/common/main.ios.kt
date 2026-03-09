package com.kmp.common

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.kmp.common.components.RootComponent
import platform.UIKit.UIViewController

actual fun getPlatformName(): String = "iOS"

/** 全局 BackDispatcher，供 Swift 层驱动 Decompose 预测性返回手势 */
private val backDispatcher = BackDispatcher()

/**
 * iOS 入口 — 创建 RootComponent 并返回 ComposeUIViewController。
 * RootComponent 在 lambda 外创建，确保生命周期和 BackDispatcher 不随 recomposition 重建。
 */
fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    lifecycle.resume()
    val root = RootComponent(
        DefaultComponentContext(lifecycle = lifecycle, backHandler = backDispatcher)
    )
    return ComposeUIViewController { App(root) }
}

// ===== Swift 层调用：驱动 Decompose 交互式返回动画 =====

/** 手势开始（Swift UIScreenEdgePanGestureRecognizer .began） */
fun onBackGestureStarted() {
    backDispatcher.startPredictiveBack(BackEvent(progress = 0f))
}

/** 手势进度更新（Swift .changed，progress: 0.0 ~ 1.0） */
fun onBackGestureProgress(progress: Float) {
    backDispatcher.progressPredictiveBack(BackEvent(progress = progress))
}

/** 手势完成 → 确认返回（Swift .ended 且超过阈值） */
fun onBackGestureCompleted() {
    backDispatcher.back()
}

/** 手势取消 → 恢复当前页面（Swift .ended 未超过阈值 或 .cancelled） */
fun onBackGestureCancelled() {
    backDispatcher.cancelPredictiveBack()
}