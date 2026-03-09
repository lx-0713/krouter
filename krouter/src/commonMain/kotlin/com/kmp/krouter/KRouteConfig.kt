package com.kmp.krouter

import kotlinx.serialization.Serializable

/**
 * 路由栈中每个条目的配置，描述"跳转到哪个页面、携带什么参数"。
 *
 * 作为 Decompose `ChildStack` 的配置类型（`C`），必须实现 [Serializable] 以支持：
 * - **进程恢复**：应用被系统杀死后，Decompose 会将整个路由栈序列化保存，
 *   重启时从持久化数据还原路由栈及各页面参数
 * - **返回栈管理**：Decompose 内部用此配置判断栈条目是否相同（用于 `pushNew` / `bringToFront`）
 *
 * [bundle] 字段使用 [KBundleSerializer] 进行序列化，支持基础类型和 `@Serializable` 对象。
 *
 * @property path 路由路径，对应被 [@KRoute][KRoute] 标注的组件
 * @property bundle 传递给目标组件的路由参数，默认为空
 * @see KRouter
 * @see KBundle
 * @see KBundleSerializer
 */
@Serializable
data class KRouteConfig(
    val path: String,
    val bundle: @Serializable(with = KBundleSerializer::class) KBundle = KBundle()
)
