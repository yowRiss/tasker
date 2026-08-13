package com.tasker.android.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.ui.theme.TaskerTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val colors = TaskerTheme.colors

    // Navigate on success
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Logo / wordmark
            Text(
                text  = "Tasker",
                style = MaterialTheme.typography.displaySmall,
                color = colors.accent,
            )

            // Auth mode toggle (Sign In / Register tabs)
            TabRow(
                selectedTabIndex = if (uiState.authMode == AuthMode.LOGIN) 0 else 1,
                containerColor = colors.surfaceAlt,
                contentColor = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Tab(
                    selected = uiState.authMode == AuthMode.LOGIN,
                    onClick = { viewModel.onAuthModeChange(AuthMode.LOGIN) },
                    text = { Text("Sign In", style = MaterialTheme.typography.titleSmall) },
                )
                Tab(
                    selected = uiState.authMode == AuthMode.REGISTER,
                    onClick = { viewModel.onAuthModeChange(AuthMode.REGISTER) },
                    text = { Text("Register", style = MaterialTheme.typography.titleSmall) },
                )
            }

            Spacer(Modifier.height(4.dp))

            // Offline registration notice banner in Register mode
            if (uiState.authMode == AuthMode.REGISTER) {
                Surface(
                    color = colors.surfaceAlt,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Offline-first registration: Works even when API is down. Credentials are saved locally and synced when online.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // Username field
            OutlinedTextField(
                value         = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label         = { Text("Username") },
                leadingIcon   = { Icon(Icons.Outlined.Person, null) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
                isError  = uiState.errorMessage != null,
                colors   = taskerOutlinedTextFieldColors(),
            )

            // Password field
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value         = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label         = { Text("Password") },
                leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon  = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Visibility
                                          else Icons.Outlined.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = if (uiState.authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.submit()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
                isError  = uiState.errorMessage != null,
                colors   = taskerOutlinedTextFieldColors(),
            )

            // Confirm Password field (Register mode only)
            if (uiState.authMode == AuthMode.REGISTER) {
                var confirmPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value         = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label         = { Text("Confirm Password") },
                    leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon  = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Outlined.Visibility
                                              else Icons.Outlined.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.submit()
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError  = uiState.errorMessage != null,
                    colors   = taskerOutlinedTextFieldColors(),
                )
            }

            // Error message
            uiState.errorMessage?.let { msg ->
                Text(
                    text  = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Remember me
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked  = uiState.rememberMe,
                    onCheckedChange = viewModel::onRememberMeChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accent,
                    ),
                )
                Text(
                    text  = "Stay signed in for 7 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            // Submit button (Sign In / Register)
            Button(
                onClick  = viewModel::submit,
                enabled  = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                ),
                shape    = MaterialTheme.shapes.medium,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (uiState.authMode == AuthMode.LOGIN) "Sign in" else "Register",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Toggle mode text button link
            TextButton(
                onClick = {
                    viewModel.onAuthModeChange(
                        if (uiState.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                    )
                },
            ) {
                Text(
                    text = if (uiState.authMode == AuthMode.LOGIN)
                        "Don't have an account? Register"
                    else
                        "Already have an account? Sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent,
                )
            }

            // ── Server settings (collapsible) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = viewModel::onToggleServerSettings,
                ) {
                    Icon(
                        Icons.Outlined.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textTertiary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Server",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (uiState.showServerSettings) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (uiState.showServerSettings) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp),
                        tint = colors.textTertiary,
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.showServerSettings,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                OutlinedTextField(
                    value = uiState.apiHost,
                    onValueChange = viewModel::onApiHostChange,
                    label = { Text("API Host URL") },
                    leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                    placeholder = { Text("https://api.example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = taskerOutlinedTextFieldColors(),
                )
            }
        }
    }
}

@Composable
fun taskerOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = TaskerTheme.colors.accent,
    unfocusedBorderColor = TaskerTheme.colors.border,
    focusedLabelColor    = TaskerTheme.colors.accent,
    unfocusedLabelColor  = TaskerTheme.colors.textTertiary,
    cursorColor          = TaskerTheme.colors.accent,
    focusedContainerColor   = TaskerTheme.colors.surfaceAlt,
    unfocusedContainerColor = TaskerTheme.colors.surfaceAlt,
)
