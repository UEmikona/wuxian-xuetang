package com.example.buildlearn.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 进程级协程作用域。用于 ViewModel 销毁时仍需完成的写入（例如离开播放页时保存进度），
 * 这些操作不能依赖已被取消的 viewModelScope。
 */
object AppScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
