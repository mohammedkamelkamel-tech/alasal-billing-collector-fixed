package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminPasswordScreen(
    setupMode: Boolean,
    onSetupPassword: (String, String) -> Pair<Boolean, String>,
    onLoginPassword: suspend (String) -> Pair<Boolean, String>,
    onSuccess: () -> Unit,
    onCollectorsLogin: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(if (setupMode) "إنشاء كلمة مرور مدير النظام" else "دخول مدير النظام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (setupMode) "أنشئ كلمة مرور جديدة لا تقل عن 8 أحرف. ستبقى بيانات الفواتير والتحصيلات محفوظة."
                else "أدخل كلمة مرور الإدارة للمتابعة.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; message = null },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (setupMode) "كلمة المرور الجديدة" else "كلمة مرور الإدارة") },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "إظهار كلمة المرور") } }
            )
            if (setupMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmation, onValueChange = { confirmation = it; message = null },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("تأكيد كلمة المرور") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            message?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = !busy && password.isNotBlank() && (!setupMode || confirmation.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    busy = true
                    message = null
                    if (setupMode) {
                        val (ok, msg) = onSetupPassword(password, confirmation)
                        busy = false
                        message = msg
                        if (ok) onSuccess()
                    } else {
                        scope.launch {
                            val (ok, msg) = onLoginPassword(password)
                            busy = false
                            if (ok) onSuccess() else message = msg
                        }
                    }
                }
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (setupMode) "حفظ كلمة المرور" else "تسجيل الدخول")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onCollectorsLogin) { Text("الدخول بحساب المحصّل / المفتاح السري") }
        }
    }
}
