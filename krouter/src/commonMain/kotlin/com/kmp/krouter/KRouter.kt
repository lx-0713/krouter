package com.kmp.krouter

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.concurrent.Volatile

/**
 * KRouter 路由核心，提供全局静态 API 进行页面跳转和结果传递。
 *
 * ## 路由注册
 * 使用 [@KRoute][KRoute] 注解标记组件类，KSP 编译器插件自动生成路由表，
 * 在根组件初始化时调用 `GeneratedRouteTable.register()` 完成注册。
 *
 * ## 页面跳转
 * ```kotlin
 * KRouter.push(RoutePath.DETAIL) { putString("itemId", "123") }
 * KRouter.replaceAll(RoutePath.HOME) { putObject("user", user) }
 * KRouter.pop()
 * ```
 *
 * ## 页面结果回调
 * ```kotlin
 * // 发起方：
 * pushForResult(RoutePath.SETTINGS)
 * override fun onComponentResult(bundle: KBundle) { /* 处理结果 */ }
 *
 * // 目标方返回时：
 * KRouter.postResult { putString("msg", "操作成功") }
 * KRouter.pop()
 * ```
 *
 * @see KRouterComponent
 * @see KBundle
 * @see KRoute
 */
object KRouter {

    /** 路由表，key 为路由路径，value 为对应组件的工厂函数。 */
    private val routeBuilders = mutableMapOf<String, (ComponentContext) -> KRouterComponent>()

    /**
     * 注册一条路由，将 [path] 与组件工厂函数 [builder] 绑定。
     *
     * 通常由 KSP 生成的 `GeneratedRouteTable.register()` 自动调用，无需手动调用。
     *
     * @param path 路由路径，例如 `"/home"`
     * @param builder 接收 [ComponentContext] 并返回对应 [KRouterComponent] 实例的工厂函数
     */
    fun registerRoute(path: String, builder: (ComponentContext) -> KRouterComponent) {
        routeBuilders[path] = builder
    }

    /**
     * 待传递给下一个即将创建的组件的路由参数。
     *
     * 在 [createChildStack] 的 `childFactory` 中，组件创建前赋值，组件构造完成后立即清空。
     * [KRouterComponent] 在构造时从此处读取自身的入参（[KRouterComponent._bundle]）。
     *
     * 标注 [@Volatile] 防止多线程可见性问题（尽管 Compose 通常在主线程运行）。
     */
    @Volatile
    internal var _pendingBundle: KBundle = KBundle()

    /**
     * 路由事件通道，缓冲容量为 16。
     *
     * 每次调用 push/pop 等导航方法时向此 Flow 发送一个操作 lambda，
     * 由 [createChildStack] 中的收集协程在主线程顺序执行，保证导航操作的时序性。
     */
    private val routeEvents = MutableSharedFlow<StackNavigation<KRouteConfig>.() -> Unit>(
        extraBufferCapacity = 16
    )

    /**
     * 当前活跃的 Decompose 路由栈导航器，由 [createChildStack] 初始化。
     *
     * 标注 [@Volatile] 以确保跨线程访问时读到最新值。
     */
    @Volatile
    private var navigation: StackNavigation<KRouteConfig>? = null

    /**
     * 创建并启动 Decompose 路由栈，绑定到宿主组件的生命周期。
     *
     * 通常只在根组件（`RootComponent`）的 `init` 块中调用一次：
     * ```kotlin
     * childStack = KRouter.createChildStack(this, RoutePath.LOGIN)
     * ```
     *
     * @param host 路由栈的宿主组件，其 [KRouterComponent.componentScope] 用于收集路由事件
     * @param initialPath 初始页面的路由路径
     * @return Decompose 的 [Value]<[ChildStack]>，可绑定到 Composable 中渲染
     */
    fun createChildStack(
        host: KRouterComponent,
        initialPath: String
    ): Value<ChildStack<KRouteConfig, KRouterComponent>> {
        val nav = StackNavigation<KRouteConfig>()
        navigation = nav

        // 在宿主组件的协程作用域中收集路由事件，宿主销毁时自动取消
        routeEvents.onEach { action -> nav.action() }.launchIn(host.componentScope)

        return host.childStack(
            source = nav,
            serializer = KRouteConfig.serializer(),
            initialConfiguration = KRouteConfig(initialPath),
            handleBackButton = false,
            childFactory = { config, ctx ->
                val builder = routeBuilders[config.path]
                    ?: error("KRouter: 未注册路由 '${config.path}'，请检查 @KRoute 注解或 GeneratedRouteTable.register()")
                // 将路由参数暂存，使目标组件在构造时能通过 getBundle() 读取，随后立即清空
                _pendingBundle = config.bundle
                builder(ctx).also { _pendingBundle = KBundle() }
            },
        )
    }

    /** 根据 [path] 和参数构建 DSL [block] 创建路由配置。 */
    private inline fun buildConfig(path: String, block: KBundle.() -> Unit): KRouteConfig =
        KRouteConfig(path = path, bundle = KBundle().apply(block))

    /**
     * 将目标页压入路由栈（允许重复同一路由）。
     *
     * @param path 目标路由路径
     * @param block 传递给目标页的参数构建 DSL
     */
    fun push(path: String, block: KBundle.() -> Unit = {}) {
        routeEvents.tryEmit { push(buildConfig(path, block)) }
    }

    /**
     * 将目标页压入路由栈（栈中已存在同路由时不重复创建）。
     *
     * @param path 目标路由路径
     * @param block 传递给目标页的参数构建 DSL
     */
    fun pushNew(path: String, block: KBundle.() -> Unit = {}) {
        routeEvents.tryEmit { pushNew(buildConfig(path, block)) }
    }

    /**
     * 替换当前路由栈顶的页面。
     *
     * @param path 目标路由路径
     * @param block 传递给目标页的参数构建 DSL
     */
    fun replaceCurrent(path: String, block: KBundle.() -> Unit = {}) {
        routeEvents.tryEmit { replaceCurrent(buildConfig(path, block)) }
    }

    /**
     * 清空整个路由栈并替换为目标页（常用于登录成功后跳转首页、退出登录等场景）。
     *
     * @param path 目标路由路径
     * @param block 传递给目标页的参数构建 DSL
     */
    fun replaceAll(path: String, block: KBundle.() -> Unit = {}) {
        routeEvents.tryEmit { replaceAll(buildConfig(path, block)) }
    }

    /**
     * 返回上一页（弹出栈顶）。通过 [routeEvents] 异步执行，确保在主线程有序处理。
     */
    fun pop() {
        routeEvents.tryEmit { pop() }
    }

    /**
     * 立即同步返回上一页，直接操作 [navigation]，绕过事件队列。
     *
     * 专为 Decompose 的系统返回手势回调（`onBack`）设计：手势回调通常在主线程同步触发，
     * 此时使用异步的 [pop] 可能出现竞态，而 [popImmediate] 保证即时响应。
     */
    fun popImmediate() {
        navigation?.pop()
    }

    /**
     * 弹出路由栈直到指定 [index] 位置（0 为栈底）。
     *
     * @param index 目标位置的栈索引
     */
    fun popTo(index: Int) {
        routeEvents.tryEmit { popTo(index) }
    }

    /**
     * 将指定路由的页面移到栈顶（若不存在则新建）。
     *
     * @param path 目标路由路径
     * @param block 传递给目标页的参数构建 DSL
     */
    fun bringToFront(path: String, block: KBundle.() -> Unit = {}) {
        routeEvents.tryEmit { bringToFront(buildConfig(path, block)) }
    }

    // ── 页面结果回调机制 ──────────────────────────────────────────────────────

    /**
     * 结果接收方栈，采用 LIFO（后进先出）顺序。
     *
     * [KRouterComponent.pushForResult] 跳转时将发起方压栈，
     * [postResult] 时弹出最后一个接收方并触发其 [KRouterComponent.onComponentResult]。
     */
    private val resultReceiverStack = ArrayDeque<KRouterComponent>()

    /**
     * 将 [component] 注册为下一次 [postResult] 的结果接收方。
     *
     * 由 [KRouterComponent.pushForResult] 内部调用，外部无需手动调用。
     */
    internal fun pushResultReceiver(component: KRouterComponent) {
        resultReceiverStack.addLast(component)
    }

    /**
     * 从结果接收栈中移除指定 [component]。
     *
     * 在组件销毁时（[Lifecycle.onDestroy]）自动调用，防止持有已销毁组件的引用。
     */
    internal fun removeResultReceiver(component: KRouterComponent) {
        resultReceiverStack.removeAll { it === component }
    }

    /**
     * 向最近一次 [pushForResult] 的发起方发送结果数据。
     *
     * 通常在目标页执行 [pop] 前调用：
     * ```kotlin
     * KRouter.postResult { putString("msg", "操作成功") }
     * KRouter.pop()
     * ```
     *
     * @param block 构建结果数据的 DSL，在 [KBundle] 上下文中调用
     * @see KRouterComponent.pushForResult
     * @see KRouterComponent.onComponentResult
     */
    fun postResult(block: KBundle.() -> Unit = {}) {
        resultReceiverStack.removeLastOrNull()
            ?.onComponentResult(KBundle().apply(block))
    }
}
