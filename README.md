# KRouter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lx-0713/krouter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.lx-0713/krouter)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

基于 [Decompose](https://github.com/arkivanov/Decompose) 的 **Kotlin Multiplatform（KMP）** 声明式路由库，提供注解自动注册、路由参数传递与页面结果回调，适用于 Compose Multiplatform 的 Android / iOS 等多端项目。

---

## 特性

| 特性 | 说明 |
|------|------|
| **注解路由** | 使用 `@KRoute("/path")` 标记页面组件，KSP 编译期自动生成路由表，无需手写注册代码 |
| **字符串路径跳转** | `KRouter.push("/home") { putString("key", "value") }`，与 Android ARouter 用法类似 |
| **完整导航 API** | `push` / `pushNew` / `replaceCurrent` / `replaceAll` / `pop` / `popImmediate` / `popTo` / `bringToFront` |
| **KBundle 参数** | 支持基础类型（String、Int、Long、Float、Double、Boolean）及 `@Serializable` 对象，进程恢复时可完整还原 |
| **页面结果回调** | `pushForResult` + `onComponentResult`，目标页通过 `KRouter.postResult { }` 回传数据，类似 Activity 的 `onActivityResult` |
| **自包含 UI** | 每个页面组件实现 `Content()` Composable，根组件用 Decompose `Children` 统一渲染，无需 `when` 分发 |
| **零侵入基类** | `KRouterComponent` 提供 `getBundle()`、`pushForResult()`、`componentScope` 与生命周期绑定 |

---

## 环境要求

- **Kotlin** 1.9+（推荐与当前项目一致）
- **Compose Multiplatform**（Compose Runtime 用于 `@Composable Content()`）
- **Decompose** 3.x（路由栈与 `ComponentContext`）
- **KSP**（用于处理 `@KRoute` 注解）

---

## 安装

在需要使用路由的共享模块（如 `shared`）的 `build.gradle.kts` 中添加依赖：

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")  // KBundle 的 putObject/getObject 依赖
    id("com.android.library")       // 若包含 Android
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    // androidTarget()、iosX64() 等...
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.github.lx-0713:krouter:1.0.1")
                // krouter 已传递 Decompose、Compose Runtime、kotlinx-serialization、Coroutines
            }
        }
    }
}

// KSP：让编译器处理 @KRoute，并生成 GeneratedRouteTable
dependencies {
    add("kspCommonMainMetadata", "io.github.lx-0713:krouter-compiler:1.0.1")
}

// 使 KSP 生成的代码对 commonMain 可见
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
```

> **确保项目的 `settings.gradle.kts` 或根 `build.gradle.kts` 中包含 `mavenCentral()` 仓库：**
> ```kotlin
> repositories {
>     mavenCentral()
> }
> ```

---

## 快速开始

### 1. 定义路由路径常量（可选但推荐）

集中管理路径，避免硬编码：

```kotlin
// 例如 shared/src/commonMain/kotlin/RoutePath.kt
object RoutePath {
    const val LOGIN = "/login"
    const val HOME = "/home"
    const val DETAIL = "/detail"
    const val SETTINGS = "/settings"
}
```

### 2. 定义页面组件

每个页面 = 一个继承 `KRouterComponent` 的类，并用 `@KRoute(path)` 标注。参数通过 `getBundle()` 获取。

**仅展示 UI、无参数：**

```kotlin
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouterComponent

@KRoute(RoutePath.SETTINGS)
class SettingsComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    @Composable
    override fun Content() {
        Column {
            Text("设置页")
            Button(onClick = { KRouter.pop() }) { Text("返回") }
        }
    }
}
```

**带路由参数（基础类型）：**

```kotlin
@KRoute(RoutePath.DETAIL)
class DetailComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    private val itemId: String = getBundle().getString("itemId")

    @Composable
    override fun Content() {
        Text("商品 ID: $itemId")
        Button(onClick = { KRouter.pop() }) { Text("返回") }
    }
}
```

**带路由参数（对象，需 `@Serializable`）：**

```kotlin
@Serializable
data class User(val username: String)

@KRoute(RoutePath.HOME)
class HomeComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    private val user: User = getBundle().getObject<User>("user") ?: User("未知用户")

    @Composable
    override fun Content() {
        Text("欢迎，${user.username}！")
    }
}
```

### 3. 根组件：注册路由并创建栈

在应用入口使用的根组件中，调用 KSP 生成的 `GeneratedRouteTable.register()`，并用 `KRouter.createChildStack` 创建 Decompose 子栈：

```kotlin
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.kmp.krouter.KRouteConfig
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent
import com.kmp.krouter.generated.GeneratedRouteTable

class RootComponent(componentContext: ComponentContext) : KRouterComponent(componentContext) {

    val childStack: Value<ChildStack<KRouteConfig, KRouterComponent>>

    init {
        GeneratedRouteTable.register()
        childStack = KRouter.createChildStack(this, RoutePath.LOGIN)
    }

    @Composable
    override fun Content() {
        MaterialTheme {
            Children(
                stack = childStack,
                animation = stackAnimation(slide()), // 或 predictiveBackAnimation 等
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    it.instance.Content()
                }
            }
        }
    }
}
```

注意：`createChildStack` 的第二个参数为**初始路由路径**（例如登录页 `/login`）。`Children` 来自 Decompose 的 `extensions-compose`，需在 `commonMain` 中依赖：

```kotlin
api("com.arkivanov.decompose:extensions-compose:3.1.0")
```

### 4. 跳转 API 用法

在任意可访问 `KRouter` 的地方（通常为各个 `KRouterComponent` 内）：

```kotlin
// 压入新页面（可重复同一 path）
KRouter.push(RoutePath.DETAIL) { putString("itemId", "123") }

// 压入新页面，若栈中已有该 path 则不再新建
KRouter.pushNew(RoutePath.SETTINGS)

// 替换当前栈顶
KRouter.replaceCurrent(RoutePath.HOME) { putString("from", "detail") }

// 清空整栈并替换为目标页（常用于登录成功、退出登录）
KRouter.replaceAll(RoutePath.HOME) { putObject("user", user) }

// 返回上一页
KRouter.pop()

// 立即返回（用于系统返回键/手势回调，同步执行）
KRouter.popImmediate()

// 退到栈中指定索引（0 为栈底）
KRouter.popTo(0)

// 将某路由移到栈顶（若不存在则新建）
KRouter.bringToFront(RoutePath.HOME) { putString("refresh", "1") }
```

参数 DSL 即 `KBundle.() -> Unit`，支持：

- `putString(key, value)` / `getString(key)`、`putInt`/`getInt`、`putLong`/`getLong`、`putFloat`/`getFloat`、`putDouble`/`getDouble`、`putBoolean`/`getBoolean`
- `putObject(key, value)` / `getObject<T>(key)`：**value 的类型必须标注 `@Serializable`**，否则运行时会抛出 `SerializationException`；反序列化失败时 `getObject` 返回 `null`。

### 5. 页面结果回调（类似 onActivityResult）

**发起方（例如首页）：** 使用 `pushForResult` 跳转，并重写 `onComponentResult` 接收结果：

```kotlin
@KRoute(RoutePath.HOME)
class HomeComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    private var settingsResult: KBundle? by mutableStateOf(null)

    private fun goToSettings() = pushForResult(RoutePath.SETTINGS)

    override fun onComponentResult(bundle: KBundle) {
        settingsResult = bundle
    }

    @Composable
    override fun Content() {
        Column {
            settingsResult?.let { Text("设置页返回: ${it.getString("msg")}") }
            Button(onClick = { goToSettings() }) { Text("打开设置页") }
        }
    }
}
```

**目标页（设置页）：** 返回前调用 `KRouter.postResult { }` 写入结果，再 `KRouter.pop()`：

```kotlin
Button(onClick = {
    KRouter.postResult { putString("msg", "用户修改了字体大小") }
    KRouter.pop()
}) {
    Text("携带数据返回")
}
```

`postResult` 的 lambda 内使用 `KBundle` 的 `putString`、`putInt`、`putObject` 等即可；接收方在 `onComponentResult(bundle: KBundle)` 里用 `getString`、`getObject` 等读取。

---

## API 速查

### KRouter（全局单例）

| 方法 | 说明 |
|------|------|
| `createChildStack(host, initialPath)` | 创建路由栈，需在根组件中调用一次；返回 `Value<ChildStack<KRouteConfig, KRouterComponent>>` |
| `push(path, block)` | 压入新页面，可带参数 |
| `pushNew(path, block)` | 压入新页面，若栈中已有该 path 不重复创建 |
| `replaceCurrent(path, block)` | 替换栈顶 |
| `replaceAll(path, block)` | 清空栈并替换为目标页 |
| `pop()` | 退栈（通过事件队列异步） |
| `popImmediate()` | 立即退栈（同步，用于系统返回回调） |
| `popTo(index)` | 退到指定栈索引 |
| `bringToFront(path, block)` | 将某 path 移到栈顶 |
| `postResult(block)` | 向最近一次 `pushForResult` 的发起方发送结果数据 |

### KRouterComponent（页面基类）

| 方法/属性 | 说明 |
|-----------|------|
| `getBundle(): KBundle` | 获取当前页的路由参数（在构造/初始化时调用） |
| `pushForResult(path, block)` | 跳转到目标页并注册为结果接收方 |
| `onComponentResult(bundle)` | 子类重写以接收目标页 `postResult` 的数据 |
| `componentScope: CoroutineScope` | 与组件生命周期绑定的协程作用域，销毁时自动 cancel |
| `Content()` | Composable 方法，子类实现具体 UI |

### KBundle（参数与结果容器）

| 写入 | 读取 |
|------|------|
| `putString(key, value)` | `getString(key, default)` / `getStringOrNull(key)` |
| `putInt` / `putLong` / `putFloat` / `putDouble` / `putBoolean` | `getInt(key, default)` / `getIntOrNull(key)` 等 |
| `putObject(key, value)` | `getObject<T>(key): T?`（T 需 `@Serializable`） |

---

## 依赖版本（krouter 模块）

| 依赖 | 版本 | 用途 |
|------|------|------|
| Decompose | 3.1.0 | 路由栈、ComponentContext、状态保存与恢复 |
| Compose Runtime | 与工程一致 | `@Composable Content()` |
| kotlinx-serialization-json | 1.6.3 | KRouteConfig、KBundle 对象序列化 |
| kotlinx-coroutines-core | 1.8.0 | 路由事件流、componentScope |
| KSP | 1.9.21-1.0.16 | 处理 @KRoute，生成路由表 |

---

## 常见问题

**Q: 未标注 `@Serializable` 的对象用 `putObject` 会怎样？**  
A: 运行时会抛出 `SerializationException`。只有标注了 `@Serializable` 的类型才能安全使用 `putObject` / `getObject`。

**Q: 如何做“返回拦截”（例如弹窗确认）？**  
A: 在根组件的 `Children` 中，将系统返回交给 `KRouter.popImmediate()` 前，可先在自己的 `BackHandler` 里弹窗，用户确认后再调用 `KRouter.pop()` 或 `popImmediate()`。

**Q: 路由表是何时生成的？**  
A: 编译时由 KSP 扫描所有 `@KRoute` 类生成 `GeneratedRouteTable`；运行时在根组件 `init` 中调用 `GeneratedRouteTable.register()` 完成注册。

---

## 许可证

[Apache-2.0](https://opensource.org/licenses/Apache-2.0)
