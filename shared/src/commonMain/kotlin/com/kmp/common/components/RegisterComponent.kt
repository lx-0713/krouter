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
import com.kmp.krouter.KRoute
import com.kmp.krouter.KRouter
import com.kmp.krouter.KRouterComponent

/**
 * 注册页组件。
 *
 * 用户填写用户名、密码和确认密码后提交注册：
 * - 调用 [MockDatabase.register] 执行业务验证
 * - 注册成功后调用 [KRouter.pop] 返回登录页，由用户手动填写账号登录
 * - 注册失败时在界面显示错误原因
 *
 * @see LoginComponent
 * @see MockDatabase.register
 */
@KRoute(RoutePath.REGISTER)
class RegisterComponent(componentContext: ComponentContext) : KRouterComponent(componentContext) {

    /** 注册成功后返回登录页（pop 而非 replaceAll，保留登录页在栈中）。 */
    private fun onRegisterSuccess() {
        KRouter.pop()
    }

    @Composable
    override fun Content() {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
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
            Text("注册", style = MaterialTheme.typography.h4)
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

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("确认密码") },
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
                    if (password != confirmPassword) {
                        errorMessage = "两次输入的密码不一致"
                        return@Button
                    }
                    val error = MockDatabase.register(username, password)
                    if (error == null) onRegisterSuccess() else errorMessage = error
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("立即注册")
            }
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { KRouter.pop() }) {
                Text("已有账号？点击登录")
            }
        }
    }
}
