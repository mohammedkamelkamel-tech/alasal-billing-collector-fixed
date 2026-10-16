package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@Composable
fun LoginScreen(
    passwordIsSet: Boolean,
    biometricEnabled: Boolean,
    onSavePassword: (String) -> Boolean,
    onLoginWithPassword: suspend (String) -> Pair<Boolean, String>,
    onLoginWithBiometric: suspend () -> Pair<Boolean, String>,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val setup = !passwordIsSet

    fun submit() {
        error = ""
        if (password.length < 6) { error = "كلمة المرور يجب أن تكون 6 أحرف أو أرقام على الأقل"; return }
        if (setup && password != confirmation) { error = "كلمتا المرور غير متطابقتين"; return }
        if (setup && !onSavePassword(password)) { error = "تعذر حفظ كلمة المرور"; return }
        busy = true
        scope.launch {
            val result = onLoginWithPassword(password)
            busy = false
            if (result.first) onLoginSuccess() else error = result.second
        }
    }

    fun authenticateBiometric() {
        val activity = context as? FragmentActivity
        if (activity == null) { error = "تعذر فتح نافذة البصمة على هذا الجهاز"; return }
        if (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            error = "البصمة غير متاحة أو غير مسجلة في إعدادات الهاتف"; return
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                scope.launch {
                    val response = onLoginWithBiometric()
                    if (response.first) onLoginSuccess() else error = response.second
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { error = errString.toString() }
            override fun onAuthenticationFailed() { error = "لم يتم التعرف على البصمة، حاول مجدداً" }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("محطة العسل لفواتير الكهرباء")
            .setSubtitle("تحقق من هويتك بالبصمة")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("استخدام كلمة المرور")
            .build())
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        com.example.ui.theme.ElectricBlue, com.example.ui.theme.ElectricBlueDark
    ))).padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(10.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = com.example.ui.theme.ElectricBlue, modifier = Modifier.size(48.dp))
                Text("محطة العسل لفواتير الكهرباء", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (setup) "أنشئ كلمة مرور للدخول إلى التطبيق" else "أدخل كلمة المرور للمتابعة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = "" },
                    label = { Text(if (setup) "كلمة المرور الجديدة" else "كلمة المرور") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "إظهار/إخفاء") } },
                    modifier = Modifier.fillMaxWidth()
                )
                if (setup) OutlinedTextField(
                    value = confirmation, onValueChange = { confirmation = it; error = "" },
                    label = { Text("تأكيد كلمة المرور") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = { submit() }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text(if (busy) "جارٍ التحقق..." else if (setup) "إنشاء كلمة المرور والدخول" else "تسجيل الدخول")
                }
                if (!setup && biometricEnabled) OutlinedButton(onClick = { authenticateBiometric() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("الدخول بالبصمة")
                }
            }
        }
    }
}
