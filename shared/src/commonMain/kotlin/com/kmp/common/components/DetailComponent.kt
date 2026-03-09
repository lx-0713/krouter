package com.kmp.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.kmp.common.RoutePath
import com.kmp.krouter.KBundle
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent

/**
 * 商品详情页组件。
 *
 * 接收来自首页的 `itemId` 参数（通过 `putString("itemId", ...)` 传入），
 * 同时支持跳转到设置页并接收回调结果。
 *
 * @see HomeComponent
 * @see SettingsComponent
 */
@KRoute(RoutePath.DETAIL)
class DetailComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    /** 从路由参数中取出的商品 ID，由首页通过 `putString("itemId", ...)` 传入。 */
    private val itemId: String = getBundle().getString("itemId")

    /**
     * 从设置页携带返回的结果数据。
     *
     * 使用 [mutableStateOf] 持有状态，[onComponentResult] 更新后界面自动重组。
     */
    private var settingsResult: KBundle? by mutableStateOf(null)

    /** 跳转到设置页，并注册为结果接收方（设置页返回时回调 [onComponentResult]）。 */
    private fun goToSettings() = pushForResult(RoutePath.SETTINGS)

    /** 接收设置页的返回结果，更新 [settingsResult] 触发界面重组。 */
    override fun onComponentResult(bundle: KBundle) {
        settingsResult = bundle
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "商品详情",
                style = MaterialTheme.typography.h5
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ID：$itemId",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            // 设置页回调结果（有数据时显示）
            settingsResult?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "设置页回调：${it.getString("msg")}",
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.body2
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { goToSettings() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("前往设置页（等待回调）")
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { KRouter.pop() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("返回")
            }
        }
    }
}
