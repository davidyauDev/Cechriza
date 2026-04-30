package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.data.local.database.AttendanceDatabase
import com.example.myapplication.data.local.entity.AttendanceType
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.ui.navigation.BottomNavBar
import com.example.myapplication.ui.navigation.NavItemList
import com.example.myapplication.ui.Attendance.AttendanceScreen
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import com.example.myapplication.ui.Attendance.AttendanceViewModelFactory
import com.example.myapplication.ui.account.AccountScreen
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.home.RoutesScreen
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.notifications.NotificationsScreen
import com.example.myapplication.ui.comprobante.RegistrarComprobanteScreen
import com.example.myapplication.ui.requests.RequestsScreen
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.work.*
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.work.SyncAttendancesWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        // Programar WorkManager para sincronización periódica (cada 15 minutos mínimo)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<SyncAttendancesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "sync_attendances",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        // Ejecutar un trabajo inmediato al iniciar para intentar sincronizar pendientes
        val immediateWork = OneTimeWorkRequestBuilder<SyncAttendancesWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(immediateWork)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
fun SplashScreen(onReady: (Boolean) -> Unit, userPreferences: UserPreferences) {
    // Simple composable that reads the stored token and signals readiness
    val token by userPreferences.userToken.collectAsState(initial = "")
    LaunchedEffect(token) {
        // small delay to show splash if needed
        delay(300)
        onReady(token.isNotEmpty())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Cargando...", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current.applicationContext as Application
    val userPreferences = remember { UserPreferences(context) }
    val db = AttendanceDatabase.getDatabase(context)
    val dao = db.attendanceDao()
    val repository = AttendanceRepository(userPreferences, context, dao)
    val factory = AttendanceViewModelFactory(context, repository)
    val attendanceViewModel: AttendanceViewModel = viewModel(factory = factory)
    val navigateToNotifications: () -> Unit = {
        val popped = navController.popBackStack("notifications", inclusive = false)
        if (!popped) {
            navController.navigate("notifications") {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onReady = { logged ->
                if (logged) {
                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                } else {
                    navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                }
            }, userPreferences = userPreferences)
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            BottomNavScreen(navController = navController, attendanceViewModel = attendanceViewModel)
        }

        // Pantalla para mostrar rutas del día (navegable desde Drawer)
        composable("routes") {
            RoutesScreen(navController = navController)
        }

        composable("notifications") {
            NotificationsScreen(
                navController = navController,
                showBackButton = true,
                onAddRequestClick = { preset ->
                    if (preset.startsWith("comprobante")) {
                        val solicitudId = preset.substringAfter("comprobante:", "")
                        if (solicitudId.isNotBlank()) {
                            navController.navigate("comprobante_form/$solicitudId")
                        } else {
                            navController.navigate("comprobante_form")
                        }
                    } else {
                        navController.navigate("requests_form/$preset")
                    }
                }
            )
        }

        composable("requests_form") {
            RequestsScreen(
                onHomeClick = navigateToNotifications,
                onNotificationsClick = navigateToNotifications,
                onRegisterSuccess = navigateToNotifications,
                initialPreset = null
            )
        }

        composable("requests_form/{preset}") { backStackEntry ->
            val preset = backStackEntry.arguments?.getString("preset")
            RequestsScreen(
                onHomeClick = navigateToNotifications,
                onNotificationsClick = navigateToNotifications,
                onRegisterSuccess = navigateToNotifications,
                initialPreset = preset
            )
        }

        composable(
            route = "comprobante_form/{solicitudId}",
            arguments = listOf(
                navArgument("solicitudId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val solicitudIdArg = backStackEntry.arguments?.getInt("solicitudId") ?: -1
            RegistrarComprobanteScreen(
                initialSolicitudGastoId = solicitudIdArg.takeIf { it > 0 },
                onBackClick = navigateToNotifications,
                onUnauthorized = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onSaved = navigateToNotifications
            )
        }

        composable("comprobante_form") {
            RegistrarComprobanteScreen(
                initialSolicitudGastoId = null,
                onBackClick = navigateToNotifications,
                onUnauthorized = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onSaved = navigateToNotifications
            )
        }

        composable("camera/{attendanceType}") { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("attendanceType")
            val type = if (typeString == "ENTRADA") AttendanceType.ENTRADA else AttendanceType.SALIDA
            CameraScreen(
                navController = navController,
                attendanceType = type
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BottomNavScreen(navController: NavHostController, attendanceViewModel: AttendanceViewModel) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routesWithBottomBar = listOf("main")
    val showBottomBar = currentRoute in routesWithBottomBar

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navItemList = NavItemList.navItemList,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index -> selectedIndex = index }
                )
            }
        }
    ) { paddingValues ->
        ContentScreen(
            selectedIndex = selectedIndex,
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            attendanceViewModel = attendanceViewModel,
            onNavigateHome = { selectedIndex = 0 },
            onNavigateNotifications = {
                navController.navigate("notifications") {
                    launchSingleTop = true
                }
            },
            onLogout = {
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun ContentScreen(
    selectedIndex: Int,
    navController: NavHostController,
    attendanceViewModel: AttendanceViewModel,
    onNavigateHome: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedIndex) {
        0 -> HomeScreen(navController, attendanceViewModel = attendanceViewModel, modifier = modifier)
        1 -> AttendanceScreen(
            attendanceViewModel = attendanceViewModel,
            modifier = modifier,
            onHomeClick = onNavigateHome,
            onNotificationsClick = onNavigateNotifications
        )
        2 -> NotificationsScreen(
            navController = navController,
            modifier = modifier,
            showBackButton = false,
            onAddRequestClick = { preset ->
                if (preset.startsWith("comprobante")) {
                    val solicitudId = preset.substringAfter("comprobante:", "")
                    if (solicitudId.isNotBlank()) {
                        navController.navigate("comprobante_form/$solicitudId")
                    } else {
                        navController.navigate("comprobante_form")
                    }
                } else {
                    navController.navigate("requests_form/$preset")
                }
            }
        )
        3 -> AccountScreen(
            modifier = modifier,
            onNotificationsClick = onNavigateNotifications,
            onLogoutClick = onLogout
        )
    }
}
