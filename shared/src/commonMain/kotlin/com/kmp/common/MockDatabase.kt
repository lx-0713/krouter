package com.kmp.common

/**
 * 模拟数据库，用于演示登录与注册逻辑。
 *
 * 数据存储在内存中（进程终止后清空），不具备持久化能力，仅供演示使用。
 *
 * > 注意：此实现不是线程安全的，生产环境请替换为真实数据库或网络接口。
 */
object MockDatabase {

    /** 已注册的用户，key 为用户名，value 为密码（明文存储，仅用于演示）。 */
    private val users = mutableMapOf<String, String>()

    /**
     * 注册新用户。
     *
     * @param username 用户名，不能为空白字符串
     * @param password 密码，不能为空白字符串
     * @return 注册失败时返回可展示给用户的错误信息，注册成功时返回 `null`
     */
    fun register(username: String, password: String): String? {
        if (username.isBlank() || password.isBlank()) return "用户名或密码不能为空"
        if (users.containsKey(username)) return "该用户名已被注册"
        users[username] = password
        return null
    }

    /**
     * 验证用户登录。
     *
     * @param username 用户名
     * @param password 密码
     * @return 验证失败时返回可展示给用户的错误信息，验证成功时返回 `null`
     */
    fun login(username: String, password: String): String? {
        if (username.isBlank() || password.isBlank()) return "请输入用户名和密码"
        val storedPassword = users[username] ?: return "用户不存在，请先注册"
        if (storedPassword != password) return "密码不正确，请重试"
        return null
    }
}
