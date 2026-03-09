package com.kmp.krouter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * 区分 JSON 对象字符串与普通字符串，防止 [KBundle.getObject] 误解码 [KBundle.putString] 存入的值。
 *
 * 标注 [@PublishedApi] 是因为 [KBundle.putObject] / [KBundle.getObject] 为 `public inline` 函数，
 * 而 Kotlin 要求 `public inline` 函数内访问的内部成员必须加 [@PublishedApi]。
 */
@PublishedApi
internal data class JsonEncoded(val json: String)

/**
 * 专用于 [KBundle] 对象序列化的 Json 实例。
 *
 * - `ignoreUnknownKeys`：兼容字段变更（版本升级后旧数据不会反序列化失败）
 * - `encodeDefaults`：确保含默认值的字段也被写入 JSON，状态恢复时完整还原
 */
@PublishedApi
internal val bundleJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 跨平台数据容器，类似 Android 的 `Bundle`，用于路由参数传递和页面结果回调。
 *
 * 所有数据以**扁平 Map** 结构存储，Decompose 进行状态保存与恢复时通过 [KBundleSerializer] 完整序列化。
 *
 * ## 基础类型
 * ```kotlin
 * KRouter.push(RoutePath.DETAIL) {
 *     putString("title", "Hello")
 *     putInt("count", 42)
 * }
 * // 目标组件中：
 * val title = getBundle().getString("title")
 * val count  = getBundle().getInt("count")
 * ```
 *
 * ## 对象传参（需 `@Serializable`）
 * ```kotlin
 * @Serializable
 * data class User(val name: String, val age: Int)
 *
 * bundle.putObject("user", User("Alice", 25))
 * val user: User? = bundle.getObject("user")
 * ```
 *
 * 嵌套对象自动支持（只要所有层级都标注 `@Serializable`）：
 * ```kotlin
 * @Serializable
 * data class Order(val user: User, val amount: Double)
 *
 * bundle.putObject("order", Order(user, 99.0))
 * val order: Order? = bundle.getObject("order")
 * ```
 *
 * ## 页面结果回调
 * ```kotlin
 * // 目标页返回时携带数据：
 * KRouter.postResult { putString("msg", "操作成功") }
 *
 * // 发起页接收回调：
 * override fun onComponentResult(bundle: KBundle) {
 *     val msg = bundle.getString("msg")
 * }
 * ```
 *
 * @see KBundleSerializer
 * @see KRouter.postResult
 * @see KRouterComponent.pushForResult
 * @see KRouterComponent.onComponentResult
 */
class KBundle {

    /**
     * 底层数据存储，key 为字段名，value 为基础类型或 [JsonEncoded]。
     *
     * 标注 [@PublishedApi] 以允许 `public inline` 函数（[putObject] / [getObject]）访问。
     */
    @PublishedApi
    internal val data = mutableMapOf<String, Any?>()

    // ── 写入 ─────────────────────────────────────────────────────────────────

    /** 存入 [String]，返回自身以支持链式调用。 */
    fun putString(key: String, value: String) = apply { data[key] = value }

    /** 存入 [Int]，返回自身以支持链式调用。 */
    fun putInt(key: String, value: Int) = apply { data[key] = value }

    /** 存入 [Long]，返回自身以支持链式调用。 */
    fun putLong(key: String, value: Long) = apply { data[key] = value }

    /** 存入 [Float]，返回自身以支持链式调用。 */
    fun putFloat(key: String, value: Float) = apply { data[key] = value }

    /** 存入 [Double]，返回自身以支持链式调用。 */
    fun putDouble(key: String, value: Double) = apply { data[key] = value }

    /** 存入 [Boolean]，返回自身以支持链式调用。 */
    fun putBoolean(key: String, value: Boolean) = apply { data[key] = value }

    /**
     * 存入 `@Serializable` 对象，内部以 JSON 字符串扁平存储，进程恢复后可完整还原。
     *
     * **[T] 必须标注 `@Serializable`**，否则 `serializer<T>()` 在运行时会抛出
     * `SerializationException`（kotlinx.serialization 编译器插件仅在编译期优化已知的
     * `@Serializable` 类型，并不阻止非 `@Serializable` 类型通过编译）。
     *
     * ```kotlin
     * @Serializable
     * data class User(val name: String)
     *
     * bundle.putObject("user", User("Alice"))   // ✓ 正常
     * bundle.putObject("raw", SomeClass())      // ✗ 运行时崩溃
     * ```
     *
     * @param key 存储键
     * @param value 要存入的对象，必须标注 `@Serializable`
     */
    inline fun <reified T : Any> putObject(key: String, value: T) = apply {
        data[key] = JsonEncoded(bundleJson.encodeToString(serializer<T>(), value))
    }

    // ── 读取 ─────────────────────────────────────────────────────────────────

    /** 获取 [String]；key 不存在时返回 [default]（默认空字符串）。 */
    fun getString(key: String, default: String = ""): String = data[key] as? String ?: default

    /** 获取 [String]；key 不存在时返回 `null`。 */
    fun getStringOrNull(key: String): String? = data[key] as? String

    /** 获取 [Int]；key 不存在时返回 [default]（默认 0）。 */
    fun getInt(key: String, default: Int = 0): Int = data[key] as? Int ?: default

    /** 获取 [Int]；key 不存在时返回 `null`。 */
    fun getIntOrNull(key: String): Int? = data[key] as? Int

    /** 获取 [Long]；key 不存在时返回 [default]（默认 0L）。 */
    fun getLong(key: String, default: Long = 0L): Long = data[key] as? Long ?: default

    /** 获取 [Long]；key 不存在时返回 `null`。 */
    fun getLongOrNull(key: String): Long? = data[key] as? Long

    /** 获取 [Float]；key 不存在时返回 [default]（默认 0f）。 */
    fun getFloat(key: String, default: Float = 0f): Float = data[key] as? Float ?: default

    /** 获取 [Float]；key 不存在时返回 `null`。 */
    fun getFloatOrNull(key: String): Float? = data[key] as? Float

    /** 获取 [Double]；key 不存在时返回 [default]（默认 0.0）。 */
    fun getDouble(key: String, default: Double = 0.0): Double = data[key] as? Double ?: default

    /** 获取 [Double]；key 不存在时返回 `null`。 */
    fun getDoubleOrNull(key: String): Double? = data[key] as? Double

    /** 获取 [Boolean]；key 不存在时返回 [default]（默认 `false`）。 */
    fun getBoolean(key: String, default: Boolean = false): Boolean =
        data[key] as? Boolean ?: default

    /** 获取 [Boolean]；key 不存在时返回 `null`。 */
    fun getBooleanOrNull(key: String): Boolean? = data[key] as? Boolean

    /**
     * 获取 `@Serializable` 对象（对应 [putObject]）。
     *
     * ```kotlin
     * val user: User? = bundle.getObject("user")
     * ```
     *
     * @param key 存储键
     * @return 还原的对象；key 不存在、类型不匹配或反序列化失败时返回 `null`
     */
    inline fun <reified T : Any> getObject(key: String): T? =
        (data[key] as? JsonEncoded)?.let {
            runCatching { bundleJson.decodeFromString(serializer<T>(), it.json) }.getOrNull()
        }

    // ── 工具 ─────────────────────────────────────────────────────────────────

    /** 是否包含指定 [key]。 */
    fun containsKey(key: String): Boolean = key in data

    /** 是否为空（不含任何条目）。 */
    fun isEmpty(): Boolean = data.isEmpty()

    override fun toString(): String =
        "KBundle(${data.entries.joinToString { "${it.key}=${it.value}" }})"
}

/**
 * [KBundle] 的 kotlinx.serialization 序列化器，供 Decompose 状态保存与恢复使用。
 *
 * 底层以 `Map<String, String>` 格式存储，每个 value 携带单字符类型前缀，反序列化时据此还原正确类型：
 *
 * | 前缀 | 类型 | 示例 value |
 * |:----:|------|-----------|
 * | `s`  | String  | `s:hello` |
 * | `i`  | Int     | `i:42` |
 * | `l`  | Long    | `l:1000000000000` |
 * | `f`  | Float   | `f:3.14` |
 * | `d`  | Double  | `d:3.14159265` |
 * | `b`  | Boolean | `b:true` |
 * | `n`  | null    | `n:` |
 * | `j`  | `@Serializable` 对象（JSON 字符串） | `j:{"name":"Alice"}` |
 */
internal object KBundleSerializer : KSerializer<KBundle> {

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: KBundle) {
        val encoded = value.data.mapValues { (_, v) ->
            when (v) {
                null -> "n:"
                is String -> "s:$v"
                is Int -> "i:$v"
                is Long -> "l:$v"
                is Float -> "f:$v"
                is Double -> "d:$v"
                is Boolean -> "b:$v"
                is JsonEncoded -> "j:${v.json}"
                else -> "s:$v"
            }
        }
        encoder.encodeSerializableValue(mapSerializer, encoded)
    }

    override fun deserialize(decoder: Decoder): KBundle {
        val map = decoder.decodeSerializableValue(mapSerializer)
        return KBundle().apply {
            map.forEach { (k, encoded) ->
                val colonIdx = encoded.indexOf(':')
                if (colonIdx < 0) {
                    data[k] = encoded; return@forEach
                }
                val prefix = encoded.substring(0, colonIdx)
                val raw = encoded.substring(colonIdx + 1)
                data[k] = when (prefix) {
                    "s" -> raw
                    "i" -> raw.toIntOrNull() ?: 0
                    "l" -> raw.toLongOrNull() ?: 0L
                    "f" -> raw.toFloatOrNull() ?: 0f
                    "d" -> raw.toDoubleOrNull() ?: 0.0
                    "b" -> raw.toBooleanStrictOrNull() ?: false
                    "n" -> null
                    "j" -> JsonEncoded(raw)
                    else -> raw
                }
            }
        }
    }
}
