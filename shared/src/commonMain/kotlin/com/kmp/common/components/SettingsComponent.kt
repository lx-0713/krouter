package com.kmp.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.kmp.common.RoutePath
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent

/**
 * 设置页组件。
 *
 * 该页面通常通过 [KRouterComponent.pushForResult] 打开，返回时可携带数据给调用方。
 * 提供以下操作：
 * - **携带数据返回**：调用 [KRouter.postResult] 向调用方发送结果，再通过 [KRouter.pop] 返回
 * - **直接返回**：仅调用 [KRouter.pop]，不携带任何数据
 * - **退出登录**：调用 [KRouter.replaceAll] 替换整个路由栈，跳转到登录页
 *
 * @see KRouterComponent.pushForResult
 * @see KRouter.postResult
 */
@KRoute(RoutePath.SETTINGS)
class SettingsComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("设置", style = MaterialTheme.typography.h4)
            Spacer(Modifier.height(32.dp))

            // 携带结果数据返回调用方
            Button(
                onClick = {
                    KRouter.postResult { putString("msg", "用户修改了字体大小") }
                    KRouter.pop()
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("携带数据返回")
            }
            Spacer(Modifier.height(16.dp))

            // 直接返回，不携带结果（调用方的 onComponentResult 不会被触发）
            OutlinedButton(
                onClick = { KRouter.pop() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("直接返回")
            }
            Spacer(Modifier.height(32.dp))

            // 退出登录：清空路由栈并跳转到登录页
            Button(
                onClick = { KRouter.replaceAll(RoutePath.LOGIN) },
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error
                )
            ) {
                Text("退出登录", color = MaterialTheme.colors.onError)
            }
        }
    }
}
