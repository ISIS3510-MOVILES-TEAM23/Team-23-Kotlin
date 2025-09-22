package com.example.team_23_kotlin.data.bluetooth.models


data class MessageResult(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = message != null && error == null
    companion object {
        fun success(message: String): MessageResult =
            MessageResult(true, message)


        fun failure(error: String): MessageResult =
            MessageResult(false, null, error)


        fun default(): MessageResult = MessageResult(false)
    }
}