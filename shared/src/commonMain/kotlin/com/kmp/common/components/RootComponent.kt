package com.kmp.common.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.kmp.common.RoutePath
import com.kmp.krouter.KRouteConfig
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent
import com.kmp.krouter.generated.GeneratedRouteTable

/**
 * 应用根组件，作为整个页面导航的宿主（Host）。
 *
 * 负责以下初始化工作：
 * 1. 调用 [GeneratedRouteTable.register] 注册所有被 [@KRoute][com.kmp.krouter.KRoute] 标注的组件
 * 2. 调用 [KRouter.createChildStack] 创建路由栈，初始页为登录页 [LoginComponent]
 *
 * 在 [Content] 中通过 Decompose 的 [Children] 渲染当前路由栈顶的子组件，
 * 并应用 Android 13+ 原生手势预测返回动画（[predictiveBackAnimation]）；
 * 低版本回退到普通横向滑动动画（[stackAnimation] + [slide]）。
 *
 * @see KRouter
 * @see GeneratedRouteTable
 */
@OptIn(ExperimentalDecomposeApi::class)
class RootComponent(componentContext: ComponentContext) : KRouterComponent(componentContext) {

    /** 当前路由栈，由 [KRouter] 驱动，每个条目对应一个 [KRouterComponent] 实例。 */
    val childStack: Value<ChildStack<KRouteConfig, KRouterComponent>>

    init {
        // 将 @KRoute 注解生成的路由表注册到 KRouter
        GeneratedRouteTable.register()
        childStack = KRouter.createChildStack(this, RoutePath.LOGIN)
    }

    @Composable
    override fun Content() {
        MaterialTheme {
            Children(
                stack = childStack,
                animation = predictiveBackAnimation(
                    backHandler = backHandler,
                    fallbackAnimation = stackAnimation(slide()),
                    selector = { backEvent, _, _ ->
                        predictiveBackAnimatable(
                            initialBackEvent = backEvent,
                            // 退出页：随手势向右侧滑出，同时升高阴影以表现层级感
                            exitModifier = { progress, _ ->
                                Modifier.graphicsLayer {
                                    translationX = size.width * progress
                                    shadowElevation = 8f
                                }
                            },
                            // 进入页（下层页面）：随手势从左侧轻微移入，营造纵深感
                            enterModifier = { progress, _ ->
                                Modifier.graphicsLayer {
                                    translationX = size.width * -0.3f * (1f - progress)
                                }
                            }
                        )
                    },
                    onBack = { KRouter.popImmediate() }
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    it.instance.Content()
                }
            }
        }
    }
}
