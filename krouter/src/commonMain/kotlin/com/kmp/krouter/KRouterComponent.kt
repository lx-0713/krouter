package com.kmp.krouter

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 所有页面组件的基类，整合 Decompose 的 [ComponentContext] 与 KRouter 的路由能力。
 *
 * 每个被 [@KRoute][KRoute] 标注的组件都必须继承此类，并实现 [Content] 方法提供 Composable UI。
 *
 * ## 参数接收
 * 通过 [getBundle] 获取跳转时传入的路由参数：
 * ```kotlin
 * class DetailComponent(ctx: ComponentContext) : KRouterComponent(ctx) {
 *     val itemId = getBundle().getString("itemId")
 * }
 * ```
 *
 * ## 页面结果回调
 * 使用 [pushForResult] 跳转并接收目标页返回的数据：
 * ```kotlin
 * fun goToSettings() = pushForResult(RoutePath.SETTINGS)
 *
 * override fun onComponentResult(bundle: KBundle) {
 *     val msg = bundle.getString("msg")
 * }
 * ```
 *
 * ## 协程作用域
 * [componentScope] 与组件生命周期绑定，组件销毁时自动取消所有协程：
 * ```kotlin
 * componentScope.launch {
 *     val data = api.fetchData()
 *     // 更新 UI 状态…
 * }
 * ```
 *
 * @param componentContext 由 Decompose 路由框架在创建组件时注入
 * @see KRouter
 * @see KBundle
 */
abstract class KRouterComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    /**
     * 当前组件的路由入参，在构造时从 [KRouter._pendingBundle] 中读取。
     *
     * 通过 [getBundle] 对外暴露（只读），防止外部意外修改参数。
     */
    private val _bundle: KBundle = KRouter._pendingBundle

    /**
     * 与组件生命周期绑定的协程作用域（主线程调度器）。
     *
     * 组件销毁（[Lifecycle.State.DESTROYED]）时自动取消，无需手动管理。
     * 可用于发起网络请求、收集 Flow 等异步任务。
     */
    val componentScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onDestroy() {
                componentScope.cancel()
                // 组件销毁时从结果接收栈中移除，防止内存泄漏和错误回调
                KRouter.removeResultReceiver(this@KRouterComponent)
            }
        })
    }

    /**
     * 获取跳转到当前页时传入的路由参数。
     *
     * 通常在组件构造阶段调用，用于初始化页面所需的属性：
     * ```kotlin
     * val itemId: String = getBundle().getString("itemId")
     * val user: User? = getBundle().getObject<User>("user")
     * ```
     */
    protected fun getBundle(): KBundle = _bundle

    /**
     * 跳转到目标页，并将当前组件注册为结果接收方。
     *
     * 目标页调用 [KRouter.postResult] 时，当前组件的 [onComponentResult] 会被触发。
     *
     * ```kotlin
     * fun goToSettings() = pushForResult(RoutePath.SETTINGS)
     *
     * override fun onComponentResult(bundle: KBundle) {
     *     val msg = bundle.getString("msg")
     * }
     * ```
     *
     * @param path 目标页路由路径
     * @param block 可选的参数构建 DSL，传递给目标页的路由参数
     * @see KRouter.postResult
     * @see onComponentResult
     */
    protected fun pushForResult(path: String, block: KBundle.() -> Unit = {}) {
        KRouter.pushResultReceiver(this)
        KRouter.push(path, block)
    }

    /**
     * 接收目标页通过 [KRouter.postResult] 返回的结果数据。
     *
     * 默认为空实现，按需重写：
     * ```kotlin
     * override fun onComponentResult(bundle: KBundle) {
     *     settingsResult = bundle
     * }
     * ```
     *
     * @param bundle 目标页携带的结果数据
     * @see pushForResult
     */
    open fun onComponentResult(bundle: KBundle) {}

    /**
     * 渲染当前页面的 Composable UI，由子类实现。
     *
     * 此方法由 [com.kmp.common.components.RootComponent] 在路由栈渲染时调用。
     */
    @Composable
    abstract fun Content()
}
