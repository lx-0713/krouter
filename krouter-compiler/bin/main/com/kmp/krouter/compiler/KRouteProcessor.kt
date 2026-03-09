package com.kmp.krouter.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * KRoute 注解处理器 — 扫描所有 @KRoute 注解，自动生成路由注册表 GeneratedRouteTable。
 *
 * ## 设计说明
 *
 * - 支持 KSP 多轮处理：每轮收集当前轮次可验证的符号，无法验证的推迟到下一轮。
 * - 所有路由在 [finish] 中统一生成聚合文件，保证多轮场景下不漏不重。
 * - 重复路径注册会产生 warning，后注册的类覆盖先注册的（与 ARouter 行为一致）。
 *
 * ## 生成示例
 *
 * ```kotlin
 * // 输入（所有组件构造函数只需 ComponentContext，Bundle 由框架自动注入）：
 * @KRoute("/home")
 * class HomeComponent(ctx: ComponentContext) : KRouterComponent(ctx)
 *
 * @KRoute("/detail")
 * class DetailComponent(ctx: ComponentContext) : KRouterComponent(ctx)
 *
 * // 生成（Bundle 通过框架在构造前自动注入，无需工厂传参）：
 * package com.kmp.krouter.generated
 *
 * import com.kmp.krouter.KRouter
 * import com.kmp.lx.demo.components.DetailComponent
 * import com.kmp.lx.demo.components.HomeComponent
 *
 * object GeneratedRouteTable {
 *     fun register() {
 *         KRouter.registerRoute("/home") { ctx -> HomeComponent(ctx) }
 *         KRouter.registerRoute("/detail") { ctx -> DetailComponent(ctx) }
 *     }
 * }
 * ```
 */
class KRouteProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    /** 跨多轮累积的路由信息，key = 路由路径，保证路径唯一 */
    private val routeMap = linkedMapOf<String, RouteInfo>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation("com.kmp.krouter.KRoute")
            .filterIsInstance<KSClassDeclaration>()

        val valid    = symbols.filter { it.validate() }.toList()
        val deferred = symbols.filter { !it.validate() }.toList()

        valid.forEach { classDecl ->
            val annotation = classDecl.annotations
                .first { it.shortName.asString() == "KRoute" }
            val path = annotation.arguments
                .first { it.name?.asString() == "path" }
                .value as? String

            if (path.isNullOrBlank()) {
                logger.error(
                    "KRouter: @KRoute 的 path 不能为空，请检查 ${classDecl.simpleName.asString()}",
                    classDecl
                )
                return@forEach
            }

            val qualifiedName = classDecl.qualifiedName?.asString()
            if (qualifiedName == null) {
                logger.error(
                    "KRouter: 无法获取 ${classDecl.simpleName.asString()} 的全限定名，" +
                        "请确保它不是局部类或匿名类",
                    classDecl
                )
                return@forEach
            }

            val info = RouteInfo(
                path          = path,
                className     = classDecl.simpleName.asString(),
                qualifiedName = qualifiedName
            )

            if (routeMap.containsKey(path)) {
                val existing = routeMap.getValue(path)
                logger.warn(
                    "KRouter: 路由路径 \"$path\" 重复注册，" +
                        "${existing.qualifiedName} 将被 $qualifiedName 覆盖",
                    classDecl
                )
            }
            routeMap[path] = info
        }

        return deferred
    }

    /**
     * 所有轮次处理完毕后调用，此时 [routeMap] 包含全部有效路由，统一写入生成文件。
     */
    override fun finish() {
        if (routeMap.isEmpty()) return
        generateRouteTable(routeMap.values.toList())
    }

    private fun generateRouteTable(routes: List<RouteInfo>) {
        val packageName = "com.kmp.krouter.generated"

        val outputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName  = packageName,
            fileName     = "GeneratedRouteTable"
        )

        // 组件类导入：按全限定名字典序排列，提升可读性
        val componentImports = routes.map { it.qualifiedName }.distinct().sorted()

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()
            // KRouter 是唯一显式引用的框架类型，组件类型通过构造调用引用
            writer.appendLine("import com.kmp.krouter.KRouter")
            componentImports.forEach { writer.appendLine("import $it") }
            writer.appendLine()
            writer.appendLine("/**")
            writer.appendLine(" * KSP 自动生成的路由注册表，请勿手动修改。")
            writer.appendLine(" * 共注册 ${routes.size} 条路由。")
            writer.appendLine(" */")
            writer.appendLine("object GeneratedRouteTable {")
            writer.appendLine("    fun register() {")
            routes.forEach { route ->
                writer.appendLine("        KRouter.registerRoute(\"${route.path}\") { ctx -> ${route.className}(ctx) }")
            }
            writer.appendLine("    }")
            writer.appendLine("}")
            writer.appendLine()
        }

        logger.info("KRouter: 已生成 ${routes.size} 条路由注册表 → $packageName.GeneratedRouteTable")
    }

    private data class RouteInfo(
        val path: String,
        val className: String,
        val qualifiedName: String
    )
}
