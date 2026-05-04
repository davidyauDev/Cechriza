package com.cechriza.app

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cechriza.app.data.local.database.AttendanceDatabase
import com.cechriza.app.data.local.entity.AttendanceType
import com.cechriza.app.data.preferences.UserPreferences
import com.cechriza.app.data.repository.AttendanceRepository
import com.cechriza.app.ui.navigation.BottomNavBar
import com.cechriza.app.ui.navigation.NavItemList
import com.cechriza.app.ui.Attendance.AttendanceScreen
import com.cechriza.app.ui.Attendance.AttendanceViewModel
import com.cechriza.app.ui.Attendance.AttendanceViewModelFactory
import com.cechriza.app.ui.camera.CameraScreen
import com.cechriza.app.ui.account.AccountScreen
import com.cechriza.app.ui.home.HomeScreen
import com.cechriza.app.ui.home.RoutesScreen
import com.cechriza.app.ui.login.LoginScreen
import com.cechriza.app.ui.solicitudes.create.SolicitudCreateScreen
import com.cechriza.app.ui.solicitudes.list.SolicitudListScreen
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.work.*
import com.cechriza.attendance.R
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.cechriza.app.work.SyncAttendancesWorker
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    val token by userPreferences.userToken.collectAsState(initial = "")
    val infiniteTransition = rememberInfiniteTransition(label = "splash-transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-alpha"
    )

    LaunchedEffect(token) {
        delay(1200)
        onReady(token.isNotEmpty())
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_cechriza),
            contentDescription = "Logo Cechriza",
            modifier = Modifier
                .size(170.dp)
                .scale(scale)
                .alpha(alpha)
        )
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
    val returnToPrevious: () -> Unit = {
        navController.popBackStack()
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

        composable("account") {
            AccountScreen(
                onNotificationsClick = {},
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("solicitudes_list") {
            SolicitudListScreen(
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
                        navController.navigate("solicitudes_create/$preset")
                    }
                }
            )
        }

        composable("solicitudes_create") {
            SolicitudCreateScreen(
                onHomeClick = returnToPrevious,
                onNotificationsClick = returnToPrevious,
                onRegisterSuccess = returnToPrevious,
                initialPreset = null
            )
        }

        composable("solicitudes_create/{preset}") { backStackEntry ->
            val preset = backStackEntry.arguments?.getString("preset")
            SolicitudCreateScreen(
                onHomeClick = returnToPrevious,
                onNotificationsClick = returnToPrevious,
                onRegisterSuccess = returnToPrevious,
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
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showWelcomePopup by rememberSaveable { mutableStateOf(true) }
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
            onNavigateSolicitudList = {
                selectedIndex = 2
            }
        )

        if (showWelcomePopup) {
            LaunchWelcomePopup(
                onDismiss = { showWelcomePopup = false },
                onGoSolicitudes = {
                    selectedIndex = 2
                    showWelcomePopup = false
                }
            )
        }
    }
}

@Composable
private fun LaunchWelcomePopup(
    onDismiss: () -> Unit,
    onGoSolicitudes: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BrandSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bienvenido a Cechriza",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gestiona tus solicitudes en segundos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = BrandMuted
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(14.dp))
                        .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Crea, revisa y sigue el estado de tus solicitudes desde un solo lugar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandText
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.logo_cechriza),
                    contentDescription = "Logo Cechriza",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .scale(0.9f)
                        .clickable { onGoSolicitudes() }
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun ContentScreen(
    selectedIndex: Int,
    navController: NavHostController,
    attendanceViewModel: AttendanceViewModel,
    onNavigateHome: () -> Unit,
    onNavigateSolicitudList: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedIndex) {
        0 -> HomeScreen(navController, attendanceViewModel = attendanceViewModel, modifier = modifier)
        1 -> AttendanceScreen(
            attendanceViewModel = attendanceViewModel,
            modifier = modifier,
            onHomeClick = onNavigateHome,
            onNotificationsClick = onNavigateSolicitudList
        )
        2 -> SolicitudListScreen(
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
                    navController.navigate("solicitudes_create/$preset")
                }
            }
        )
        3 -> RoutesScreen(
            navController = navController,
            modifier = modifier
        )
    }
}
