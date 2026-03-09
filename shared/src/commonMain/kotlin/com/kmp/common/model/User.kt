package com.kmp.common.model

import kotlinx.serialization.Serializable

/**
 * 已登录的用户信息。
 */
@Serializable
data class User(val username: String)