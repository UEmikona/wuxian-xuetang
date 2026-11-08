@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.player

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.example.buildlearn.data.db.LessonEntity
import com.example.buildlearn.data.db.NoteEntity
import com.example.buildlearn.data.prefs.SessionStore
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.di.AppScope
import com.example.buildlearn.playback.PlaybackService
import com.example.buildlearn.ui.common.EmptyState
import com.example.buildlearn.ui.common.formatMs
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TICK_MS = 5_000L
private const val CONTROLS_HIDE_MS = 3_000L
private const val SEEK_STEP_MS = 10_000L

/** 可选播放倍速。 */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/** 可选定时关闭（分钟），null 表示关闭。 */
val SLEEP_OPTIONS: List<Int?> = listOf(null, 15, 30, 60)

fun speedLabel(speed: Float): String = "${"%.2f".format(speed).trimEnd('0').trimEnd('.').let { if (it.contains('.')) it else "$it.0" }}x"

/** 课时列表中的一行。 */
data class PlaylistItem(
    val id: String,
    val title: String,
    val durationMs: Long,
    val completed: Boolean,
    val current: Boolean
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val learning: LearningRepository,
    private val session: SessionStore
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val initialLessonId: String = checkNotNull(savedStateHandle["lessonId"])

    private val _lessonId = MutableStateFlow(initialLessonId)
    val lessonId: StateFlow<String> = _lessonId.asStateFlow()

    private val _lesson = MutableStateFlow<LessonEntity?>(null)
    val lesson: StateFlow<LessonEntity?> = _lesson.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _userId = MutableStateFlow<Long?>(null)

    val notes: StateFlow<List<NoteEntity>> = _userId.filterNotNull().flatMapLatest { uid ->
        _lessonId.flatMapLatest { lid -> learning.observeNotesForLesson(uid, lid) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 本课程的课时列表（播放页内切换用）。 */
    val playlist: StateFlow<List<PlaylistItem>> = _userId.filterNotNull().flatMapLatest { uid ->
        combine(
            courses.observeLessons(courseId),
            learning.observeProgress(uid),
            _lessonId
        ) { lessons, progress, current ->
            val done = progress.filter { it.completed }.map { it.lessonId }.toSet()
            lessons.map {
                PlaylistItem(it.id, it.title, it.durationMs, it.id in done, it.id == current)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackSpeed: StateFlow<Float> = session.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

    val autoplayNext: StateFlow<Boolean> = session.autoplayNext
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _sleepMinutes = MutableStateFlow<Int?>(null)
    val sleepMinutes: StateFlow<Int?> = _sleepMinutes.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var userId: Long = 0L
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playerListener: Player.Listener? = null
    private var sleepJob: Job? = null
    private var ticks = 0

    init {
        viewModelScope.launch {
            userId = auth.currentUserId.filterNotNull().first()
            _userId.value = userId
            val l = courses.getLesson(initialLessonId)
            _lesson.value = l
            _loaded.value = true
            if (l != null) connect()
        }
        startTicker()
    }

    /** 每 5 秒：播放中则累计学习时长并保存位置；约每 30 秒检查一次成就。 */
    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val c = _controller.value ?: continue
                if (c.isPlaying && c.currentMediaItem?.mediaId == _lessonId.value) {
                    learning.addStudySeconds(userId, TICK_MS / 1000)
                    persist(c)
                    ticks++
                    if (ticks % 6 == 0) checkAchievements()
                }
            }
        }
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = future.get()
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) persist(c)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _buffering.value = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_ENDED) onEnded(c)
                }

                override fun onPlayerError(error: PlaybackException) {
                    _buffering.value = false
                    _messages.tryEmit("播放失败，请检查视频地址或网络")
                }
            }
            c.addListener(listener)
            playerListener = listener
            _controller.value = c
            viewModelScope.launch {
                _lesson.value?.let { prepareMedia(c, it, autoPlay = false) }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    /** 若后台服务中还没有这节课则载入；已完成的课时从头开始，否则从上次位置继续。 */
    private suspend fun prepareMedia(c: MediaController, lesson: LessonEntity, autoPlay: Boolean) {
        val changed = c.currentMediaItem?.mediaId != lesson.id
        if (changed) {
            val p = learning.getProgress(userId, lesson.id)
            val start = if (p == null || p.completed) 0L else p.positionMs
            val item = MediaItem.Builder()
                .setMediaId(lesson.id)
                .setUri(lesson.videoUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(lesson.title).build())
                .build()
            c.setMediaItem(item, start)
            c.prepare()
        }
        c.setPlaybackSpeed(playbackSpeed.value)
        if (autoPlay) c.play() else if (changed) c.playWhenReady = false
    }

    /** 保存当前位置。若本次使课时由未完成变为完成，则提示并检查成就。 */
    private fun persist(c: Player) {
        val lid = _lessonId.value
        val lesson = _lesson.value ?: return
        if (lesson.id != lid || c.currentMediaItem?.mediaId != lid) return
        val pos = c.currentPosition
        val dur = if (c.duration > 0) c.duration else lesson.durationMs
        viewModelScope.launch {
            val newlyCompleted = learning.savePosition(userId, lid, pos, dur)
            if (newlyCompleted) {
                _messages.emit("本课时已完成")
                checkAchievements()
            }
        }
    }

    private fun onEnded(c: Player) {
        persist(c)
        viewModelScope.launch {
            val next = neighborLessonId(+1)
            when {
                next == null -> _messages.emit("本课程已全部学完")
                autoplayNext.value -> openLesson(next, autoPlay = true)
            }
        }
    }

    /** 在本课程的课时列表中取相邻课时的 id。 */
    private suspend fun neighborLessonId(delta: Int): String? {
        val all = courses.getLessonsOf(courseId)
        val idx = all.indexOfFirst { it.id == _lessonId.value }
        if (idx < 0) return null
        return all.getOrNull(idx + delta)?.id
    }

    /** 切换到本课程的另一课时（不离开播放页）。 */
    fun openLesson(id: String, autoPlay: Boolean = false) {
        if (id == _lessonId.value && autoPlay) {
            _controller.value?.play()
            return
        }
        val c = _controller.value
        if (c != null) persist(c)
        viewModelScope.launch {
            val l = courses.getLesson(id) ?: return@launch
            _lessonId.value = id
            _lesson.value = l
            if (c != null) prepareMedia(c, l, autoPlay)
        }
    }

    fun setSpeed(speed: Float) {
        _controller.value?.setPlaybackSpeed(speed)
        viewModelScope.launch { session.setPlaybackSpeed(speed) }
    }

    fun setAutoplayNext(enabled: Boolean) {
        viewModelScope.launch { session.setAutoplayNext(enabled) }
    }

    /** 定时关闭：到时间后暂停播放。传入 null 取消。 */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        _sleepMinutes.value = minutes
        if (minutes == null) return
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            _controller.value?.pause()
            _sleepMinutes.value = null
            _messages.emit("定时已到，已暂停播放")
        }
    }

    fun togglePlay() {
        val c = _controller.value ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekBy(deltaMs: Long) {
        val c = _controller.value ?: return
        val max = if (c.duration > 0) c.duration else Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0L, max))
    }

    fun seekTo(positionMs: Long) {
        _controller.value?.seekTo(positionMs)
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        val pos = _controller.value?.currentPosition ?: 0L
        val lid = _lessonId.value
        viewModelScope.launch {
            learning.addNote(userId, lid, pos, text)
            _messages.emit("笔记已添加")
            checkAchievements()
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { learning.deleteNote(userId, noteId) }
    }

    fun markCompleted() {
        val lid = _lessonId.value
        viewModelScope.launch {
            learning.markCompleted(userId, lid)
            _messages.emit("已标记为完成")
            checkAchievements()
        }
    }

    private suspend fun checkAchievements() {
        learning.unlockNewAchievements(userId).forEach { code ->
            _messages.emit("解锁成就：${code.title}")
        }
    }

    override fun onCleared() {
        val c = _controller.value
        if (c != null) {
            val lid = _lessonId.value
            if (c.currentMediaItem?.mediaId == lid) {
                val lesson = _lesson.value
                val pos = c.currentPosition
                val dur = if (c.duration > 0) c.duration else lesson?.durationMs ?: 0L
                val uid = userId
                AppScope.scope.launch { learning.savePosition(uid, lid, pos, dur) }
            }
            c.pause()
            playerListener?.let { c.removeListener(it) }
        }
        sleepJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

// ---------------------------------------------------------------------------
// 界面
// ---------------------------------------------------------------------------

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LessonScreen(
    onBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val lesson by viewModel.lesson.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val controller by viewModel.controller.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val buffering by viewModel.buffering.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val autoplay by viewModel.autoplayNext.collectAsState()
    val sleep by viewModel.sleepMinutes.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }

    val activity = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    // 全屏：强制横屏并隐藏状态栏与导航栏；退出时恢复。
    DisposableEffect(fullscreen) {
        val window = activity?.window
        if (activity != null && window != null) {
            activity.requestedOrientation = if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            val insets = WindowCompat.getInsetsController(window, window.decorView)
            if (fullscreen) {
                insets.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    BackHandler(enabled = fullscreen) { fullscreen = false }

    val currentIndex = playlist.indexOfFirst { it.current }
    val hasNext = currentIndex in 0 until playlist.size - 1
    val title = lesson?.title.orEmpty()

    if (fullscreen && controller != null) {
        PlayerStage(
            controller = controller!!,
            title = title,
            fullscreen = true,
            buffering = buffering,
            speed = speed,
            hasNext = hasNext,
            keepVisible = showSettings || showPlaylist,
            onToggleFullscreen = { fullscreen = !fullscreen },
            onBack = { fullscreen = false },
            onNext = {
                if (hasNext) viewModel.openLesson(playlist[currentIndex + 1].id, autoPlay = true)
            },
            onOpenSettings = { showSettings = true },
            onOpenPlaylist = { showPlaylist = true },
            onTogglePlay = viewModel::togglePlay,
            onSeekBy = viewModel::seekBy,
            onSeekTo = viewModel::seekTo,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title.ifBlank { "课时" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            when {
                !loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                lesson == null -> EmptyState(
                    title = "课时不存在",
                    subtitle = "该课时可能已从课程目录中移除。",
                    modifier = Modifier.padding(padding)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val c = controller
                            if (c == null) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Color.White) }
                            } else {
                                PlayerStage(
                                    controller = c,
                                    title = title,
                                    fullscreen = false,
                                    buffering = buffering,
                                    speed = speed,
                                    hasNext = hasNext,
                                    keepVisible = showSettings || showPlaylist,
                                    onToggleFullscreen = { fullscreen = !fullscreen },
                                    onBack = { fullscreen = false },
                                    onNext = {
                                        if (hasNext) viewModel.openLesson(playlist[currentIndex + 1].id, autoPlay = true)
                                    },
                                    onOpenSettings = { showSettings = true },
                                    onOpenPlaylist = { showPlaylist = true },
                                    onTogglePlay = viewModel::togglePlay,
                                    onSeekBy = viewModel::seekBy,
                                    onSeekTo = viewModel::seekTo,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                )
                            }
                        }
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "第 ${currentIndex + 1} 课 / 共 ${playlist.size} 课" +
                                        (sleep?.let { " · 定时关闭 ${it} 分钟" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { showPlaylist = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("课时列表")
                                }
                                OutlinedButton(
                                    onClick = { if (hasNext) viewModel.openLesson(playlist[currentIndex + 1].id, autoPlay = true) },
                                    enabled = hasNext,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("下一课")
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { showNoteDialog = true }, modifier = Modifier.weight(1f)) {
                                    Text("添加笔记")
                                }
                                OutlinedButton(onClick = viewModel::markCompleted, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("标记完成")
                                }
                            }
                        }
                        item {
                            Text(
                                "笔记（${notes.size}）",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        if (notes.isEmpty()) {
                            item {
                                Text(
                                    "暂无笔记。播放时点击“添加笔记”，会自动记录当前时间点。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        items(notes, key = { it.id }) { note ->
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f)) {
                                        TextButton(onClick = { viewModel.seekTo(note.positionMs) }) {
                                            Text("${formatMs(note.positionMs)} 跳转")
                                        }
                                        Text(note.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除笔记")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = {
                viewModel.addNote(it)
                showNoteDialog = false
            }
        )
    }

    if (showSettings) {
        PlayerSettingsSheet(
            speed = speed,
            autoplay = autoplay,
            sleepMinutes = sleep,
            onSpeed = viewModel::setSpeed,
            onAutoplay = viewModel::setAutoplayNext,
            onSleep = viewModel::setSleepTimer,
            onDismiss = { showSettings = false }
        )
    }

    if (showPlaylist) {
        PlaylistSheet(
            items = playlist,
            onPick = { item ->
                showPlaylist = false
                viewModel.openLesson(item.id, autoPlay = true)
            },
            onDismiss = { showPlaylist = false }
        )
    }
}

/**
 * 播放器画面，包含视频与自动隐藏的控件层。
 *
 * 交互：
 * - 单击：显示或隐藏控件。播放中，控件在约 3 秒无操作后自动隐藏。
 * - 双击左半边 / 右半边：后退 / 前进 10 秒。
 * - 底部进度条：可拖动预览并跳转。
 */
@Composable
private fun PlayerStage(
    controller: MediaController,
    title: String,
    fullscreen: Boolean,
    buffering: Boolean,
    speed: Float,
    hasNext: Boolean,
    keepVisible: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var position by remember { mutableLongStateOf(controller.currentPosition) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<String?>(null) }
    var flashKey by remember { mutableIntStateOf(0) }

    // 每 250 毫秒刷新一次进度。拖动期间不覆盖拖动位置。
    LaunchedEffect(controller) {
        while (true) {
            isPlaying = controller.isPlaying
            if (!dragging) position = controller.currentPosition
            duration = controller.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    // 播放中、无操作超过 3 秒则隐藏控件；暂停、拖动或弹窗打开时保持显示。
    LaunchedEffect(controlsVisible, isPlaying, interaction, dragging, keepVisible) {
        if (controlsVisible && isPlaying && !dragging && !keepVisible) {
            delay(CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }

    // 双击提示短暂显示后消失。
    LaunchedEffect(flashKey) {
        if (flash != null) {
            delay(600)
            flash = null
        }
    }

    val shownPosition = if (dragging) (dragFraction * duration).toLong() else position
    val fraction = if (duration > 0) (shownPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        interaction++
                    },
                    onDoubleTap = { offset ->
                        val left = offset.x < size.width / 2f
                        onSeekBy(if (left) -SEEK_STEP_MS else SEEK_STEP_MS)
                        flash = if (left) "-10 秒" else "+10 秒"
                        flashKey++
                        interaction++
                    }
                )
            }
    ) {
        PlayerSurface(
            player = controller,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize()
        )

        if (buffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(44.dp),
                color = Color.White
            )
        }

        flash?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(it, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            ) {
                // 顶部：全屏返回 / 标题 / 课时列表 / 设置
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fullscreen) {
                        IconButton(onClick = { onBack(); interaction++ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出全屏", tint = Color.White)
                        }
                    }
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onOpenPlaylist(); interaction++ }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "课时列表", tint = Color.White)
                    }
                    IconButton(onClick = { onOpenSettings(); interaction++ }) {
                        Icon(Icons.Default.Settings, contentDescription = "播放设置", tint = Color.White)
                    }
                }

                // 中部：后退 / 播放暂停 / 前进
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconButton(onClick = { onSeekBy(-SEEK_STEP_MS); interaction++ }) {
                        Icon(Icons.Default.Replay10, contentDescription = "后退 10 秒", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(
                        onClick = { onTogglePlay(); interaction++ },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    IconButton(onClick = { onSeekBy(SEEK_STEP_MS); interaction++ }) {
                        Icon(Icons.Default.Forward10, contentDescription = "前进 10 秒", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // 底部：进度条 / 时间 / 倍速 / 下一课 / 全屏
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Slider(
                        value = fraction,
                        onValueChange = {
                            dragging = true
                            dragFraction = it
                            interaction++
                        },
                        onValueChangeFinished = {
                            if (duration > 0) onSeekTo((dragFraction * duration).toLong())
                            position = (dragFraction * duration).toLong()
                            dragging = false
                            interaction++
                        },
                        enabled = duration > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatMs(shownPosition)} / ${formatMs(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onOpenSettings(); interaction++ }) {
                            Text(speedLabel(speed), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (hasNext) {
                            IconButton(onClick = { onNext(); interaction++ }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "下一课", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { onToggleFullscreen(); interaction++ }) {
                            Icon(
                                imageVector = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (fullscreen) "退出全屏" else "全屏",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    speed: Float,
    autoplay: Boolean,
    sleepMinutes: Int?,
    onSpeed: (Float) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onSleep: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("播放速度", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PLAYBACK_SPEEDS) { s ->
                    FilterChip(
                        selected = speed == s,
                        onClick = { onSpeed(s) },
                        label = { Text(speedLabel(s)) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自动播放下一课", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "本课播放完毕后自动开始下一课",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoplay, onCheckedChange = onAutoplay)
            }
            Text("定时关闭", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SLEEP_OPTIONS) { m ->
                    FilterChip(
                        selected = sleepMinutes == m,
                        onClick = { onSleep(m) },
                        label = { Text(if (m == null) "关闭" else "$m 分钟") }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSheet(
    items: List<PlaylistItem>,
    onPick: (PlaylistItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("课时列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(item) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                item.current -> Icons.Default.PlayCircle
                                item.completed -> Icons.Default.CheckCircle
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (item.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (item.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                when {
                                    item.current -> "正在播放"
                                    item.completed -> "已完成"
                                    else -> "未完成"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.durationMs > 0) {
                            Text(formatMs(item.durationMs), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NoteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加笔记") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("笔记内容") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
