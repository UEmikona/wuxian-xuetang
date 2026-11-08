package com.example.buildlearn

import android.app.Application
import com.example.buildlearn.data.repo.CourseRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BuildLearnApp : Application() {

    @Inject
    lateinit var courseRepository: CourseRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 启动时从 assets 载入课程目录（当前为空）。
        // 解析失败时保留原有目录，不让异常导致进程崩溃。
        appScope.launch { runCatching { courseRepository.reloadCatalogFromAssets() } }
    }
}
