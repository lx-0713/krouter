package com.kmp.common

/**
 * 应用内所有路由路径的常量定义，集中管理避免硬编码字符串散落各处。
 *
 * 每个常量对应一个被 [@KRoute][com.kmp.krouter.KRoute] 标注的组件，
 * 在跳转时作为第一个参数传入：
 * ```kotlin
 * KRouter.push(RoutePath.DETAIL) { putString("itemId", "123") }
 * ```
 */
object RoutePath {
    /** 登录页 — [com.kmp.common.components.LoginComponent] */
    const val LOGIN = "/login"

    /** 注册页 — [com.kmp.common.components.RegisterComponent] */
    const val REGISTER = "/register"

    /** 首页 — [com.kmp.common.components.HomeComponent] */
    const val HOME = "/home"

    /** 商品详情页 — [com.kmp.common.components.DetailComponent] */
    const val DETAIL = "/detail"

    /** 设置页 — [com.kmp.common.components.SettingsComponent] */
    const val SETTINGS = "/settings"
}
