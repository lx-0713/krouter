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
import com.kmp.common.model.User
import com.kmp.krouter.KBundle
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent

/**
 * 首页组件，登录成功后展示。
 *
 * ### 参数接收
 * 从 [LoginComponent] 通过路由参数传来的 [User] 对象，
 * 在构造时由 `getBundle().getObject<User>("user")` 还原。
 * 若参数缺失（异常情况），回退为默认用户。
 *
 * ### 页面结果回调
 * 通过 [pushForResult] 跳转到设置页后，设置页调用 [KRouter.postResult] 携带数据返回，
 * 触发 [onComponentResult] 更新 [settingsResult]，界面自动重组显示回调内容。
 *
 * @see LoginComponent
 * @see SettingsComponent
 */
@KRoute(RoutePath.HOME)
class HomeComponent(ctx: ComponentContext) : KRouterComponent(ctx) {

    /** 当前登录用户，从路由参数中还原；登录页通过 `putObject("user", user)` 传入。 */
    private val user: User = getBundle().getObject<User>("user") ?: User("未知用户")

    /**
     * 从设置页携带返回的结果数据。
     *
     * 使用 Compose 的 [mutableStateOf] 持有状态：当 [onComponentResult] 更新此值时，
     * 正在展示此组件的 Composable 会自动重组，无需手动订阅。
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
                text = "欢迎，${user.username}！",
                style = MaterialTheme.typography.h5
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
                onClick = { KRouter.push(RoutePath.DETAIL) { putString("itemId", "ITEM_1024") } },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("进入商品详情页")
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { goToSettings() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("前往设置页（等待回调）")
            }
        }
    }
}
