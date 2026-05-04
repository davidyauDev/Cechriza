package com.cechriza.app.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cechriza.attendance.R
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.network.RetrofitClient
import com.cechriza.app.data.repository.AuthRepository
import com.cechriza.app.ui.user.UserViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    val api = remember { RetrofitClient.apiWithoutToken }
    val repository = remember { AuthRepository(api) }
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(repository)
    )

    var empCode by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var empCodeError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    val loginState by loginViewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()

    val appBackground = Color(0xFFF6F7FB)
    val brandBlue = Color(0xFF22446C)
    val mutedText = Color(0xFF7A8594)

    SideEffect {
        systemUiController.setStatusBarColor(color = appBackground, darkIcons = true)
    }

    LaunchedEffect(loginState) {
        val state = loginState
        if (state is LoginState.Success) {
            userViewModel.setUserInMemory(
                state.user.name,
                state.token,
                state.user.id,
                state.user.email
            )
            userViewModel.setEmpCodeInMemory(state.user.empCode)

            SessionManager.setSession(
                context = context,
                userId = state.user.id,
                staffId = state.user.staffId,
                token = state.token,
                name = state.user.name,
                email = state.user.email,
                empCode = state.user.empCode
            )

            userViewModel.saveEmpCode(state.user.empCode)
            onLoginSuccess()
        }
    }

    fun submitLogin(): Boolean {
        val trimmedEmpCode = empCode.trim()
        var isValid = true

        empCodeError = null
        passwordError = null

        if (trimmedEmpCode.isBlank()) {
            empCodeError = "Ingresa tu codigo de empleado"
            isValid = false
        }

        if (password.isBlank()) {
            passwordError = "Ingresa tu contrasena"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "La contrasena debe tener al menos 6 caracteres"
            isValid = false
        }

        if (isValid) {
            focusManager.clearFocus()
            loginViewModel.login(trimmedEmpCode, password)
        }

        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_cechriza),
                contentDescription = "Logo de la empresa",
                modifier = Modifier.size(208.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Iniciar sesion",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ingresa tu codigo de empleado y contrasena",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = empCode,
                    onValueChange = {
                        empCode = it
                        empCodeError = null
                    },
                    label = { Text("Codigo de empleado") },
                    placeholder = { Text("Ingresa tu codigo") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    isError = empCodeError != null,
                    supportingText = {
                        empCodeError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Contrasena") },
                    placeholder = { Text("Ingresa tu contrasena") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Ocultar" else "Ver")
                        }
                    },
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = {
                        passwordError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitLogin() }
                    )
                )

                Button(
                    onClick = { submitLogin() },
                    enabled = loginState !is LoginState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
                ) {
                    if (loginState is LoginState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Ingresando...")
                    } else {
                        Text("Ingresar")
                    }
                }

                if (loginState is LoginState.Error) {
                    Text(
                        text = (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Acceso exclusivo para personal autorizado",
                style = MaterialTheme.typography.bodySmall,
                color = mutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}
