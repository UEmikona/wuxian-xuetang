package com.example.buildlearn.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.buildlearn.ui.AppViewModel
import com.example.buildlearn.ui.SessionState
import com.example.buildlearn.ui.achievements.AchievementsScreen
import com.example.buildlearn.ui.auth.AuthScreen
import com.example.buildlearn.ui.category.CategoryScreen
import com.example.buildlearn.ui.course.CourseScreen
import com.example.buildlearn.ui.home.HomeScreen
import com.example.buildlearn.ui.learning.LearningScreen
import com.example.buildlearn.ui.notes.NotesScreen
import com.example.buildlearn.ui.placeholder.AboutScreen
import com.example.buildlearn.ui.placeholder.PlaceholderScreen
import com.example.buildlearn.ui.player.LessonScreen
import com.example.buildlearn.ui.profile.ProfileScreen
import com.example.buildlearn.ui.search.ALL_CATEGORIES
import com.example.buildlearn.ui.search.SearchScreen

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CATEGORY = "category"
    const val LEARNING = "learning"
    const val PROFILE = "profile"

    const val SEARCH = "search?category={category}"
    const val COURSE = "course/{courseId}"
    const val LESSON = "lesson/{courseId}/{lessonId}"
    const val NOTES = "notes"
    const val ACHIEVEMENTS = "achievements"
    const val PLACEHOLDER = "placeholder/{title}"
    const val ABOUT = "about"

    fun search(category: String = ALL_CATEGORIES) = "search?category=${Uri.encode(category)}"
    fun course(courseId: String) = "course/$courseId"
    fun lesson(courseId: String, lessonId: String) = "lesson/$courseId/$lessonId"
    fun placeholder(title: String) = "placeholder/${Uri.encode(title)}"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "首页", Icons.Default.Home),
    BottomTab(Routes.CATEGORY, "分类", Icons.Default.List),
    BottomTab(Routes.LEARNING, "学习", Icons.Default.School),
    BottomTab(Routes.PROFILE, "我的", Icons.Default.Person)
)

@Composable
fun BuildLearnNavHost(appViewModel: AppViewModel) {
    val sessionState by appViewModel.sessionState.collectAsStateWithLifecycle()

    if (sessionState == SessionState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val isLoggedIn = sessionState is SessionState.LoggedIn

    // 登录状态变化时统一跳转：登录后进入首页，退出后回到登录页。
    LaunchedEffect(isLoggedIn) {
        val target = if (isLoggedIn) Routes.HOME else Routes.AUTH
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    val openCourse: (String) -> Unit = { navController.navigate(Routes.course(it)) }
    val openLesson: (String, String) -> Unit = { c, l -> navController.navigate(Routes.lesson(c, l)) }
    val back: () -> Unit = { navController.popBackStack() }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.HOME else Routes.AUTH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.AUTH) { AuthScreen() }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenCourse = openCourse,
                    onContinue = openLesson,
                    onSearch = { navController.navigate(Routes.search(it)) }
                )
            }
            composable(Routes.CATEGORY) {
                CategoryScreen(onOpenCategory = { navController.navigate(Routes.search(it)) })
            }
            composable(Routes.LEARNING) {
                LearningScreen(onOpenCourse = openCourse)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onOpenPlaceholder = { navController.navigate(Routes.placeholder(it)) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) }
                )
            }

            composable(
                route = Routes.SEARCH,
                arguments = listOf(navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ALL_CATEGORIES
                })
            ) {
                SearchScreen(onBack = back, onOpenCourse = openCourse)
            }
            composable(
                route = Routes.COURSE,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) {
                CourseScreen(onBack = back, onOpenLesson = openLesson)
            }
            composable(
                route = Routes.LESSON,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("lessonId") { type = NavType.StringType }
                )
            ) {
                LessonScreen(onBack = back)
            }
            composable(Routes.NOTES) {
                NotesScreen(onBack = back, onOpenLesson = openLesson)
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(onBack = back)
            }
            composable(
                route = Routes.PLACEHOLDER,
                arguments = listOf(navArgument("title") { type = NavType.StringType })
            ) { entry ->
                PlaceholderScreen(
                    title = entry.arguments?.getString("title").orEmpty(),
                    onBack = back
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = back)
            }
        }
    }
}
