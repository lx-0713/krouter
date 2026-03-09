package com.kmp.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.kmp.common.MockDatabase
import com.kmp.common.RoutePath
import com.kmp.common.model.User
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent

/**
 * 登录页组件。
 *
 * 调用 [MockDatabase.login] 验证账号密码，验证成功后：
 * 1. 创建 [User] 对象
 * 2. 通过 [KRouter.replaceAll] 跳转到首页，同时将 [User] 对象序列化写入路由参数
 * 3. 使用 `replaceAll` 而非 `push` 是为了清空历史栈，防止用户通过系统"返回"回到登录页
 *
 * @see HomeComponent
 * @see MockDatabase.login
 */
@KRoute(RoutePath.LOGIN)
class LoginComponent(componentContext: ComponentContext) : KRouterComponent(componentContext) {

    /**
     * 登录验证成功后调用，将 [user] 写入路由参数并跳转到首页。
     *
     * 通过 [KBundle.putObject][com.kmp.krouter.KBundle.putObject] 序列化传递 [User]，
     * [HomeComponent] 在初始化时通过 `getBundle().getObject<User>("user")` 还原。
     */
    private fun onLoginSuccess(user: User) {
        KRouter.replaceAll(RoutePath.HOME) { putObject("user", user) }
    }

    @Composable
    override fun Content() {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("登录", style = MaterialTheme.typography.h4)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // 有错误信息时显示，否则用等高 Spacer 保持布局稳定
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } ?: Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val error = MockDatabase.login(username, password)
                    if (error == null) {
                        onLoginSuccess(User(username))
                    } else {
                        errorMessage = error
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("登录")
            }
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { KRouter.push(RoutePath.REGISTER) }) {
                Text("还没有账号？点击注册")
            }
        }
    }
}
