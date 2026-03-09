package com.kmp.krouter

/**
 * 将一个 [KRouterComponent] 子类标记为路由组件，并指定其路由路径。
 *
 * KSP 编译器插件（`krouter-compiler`）会扫描所有标注此注解的类，
 * 自动生成 `GeneratedRouteTable.register()` 方法，在应用启动时一次性完成路由注册。
 *
 * ## 用法
 * ```kotlin
 * @KRoute(RoutePath.HOME)
 * class HomeComponent(ctx: ComponentContext) : KRouterComponent(ctx) {
 *     @Composable
 *     override fun Content() { /* UI */ }
 * }
 * ```
 *
 * ## 路由路径规范
 * - 以 `/` 开头，例如 `"/home"`、`"/detail"`
 * - 在整个应用中必须唯一，重复路径会导致后注册的覆盖先注册的
 * - 推荐统一定义在常量对象中（例如 `RoutePath`），避免硬编码字符串散落各处
 *
 * @param path 路由路径，用于在 [KRouter] 中唯一标识该组件
 * @see KRouter.registerRoute
 * @see KRouterComponent
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KRoute(val path: String)
