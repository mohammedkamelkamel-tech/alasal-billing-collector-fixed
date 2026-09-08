package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VibrantGreen
import com.example.ui.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: BillingViewModel,
    onBackClick: () -> Unit
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncHistory by viewModel.syncHistory.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مزامنة البيانات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ElectricBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حالة الاتصال:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isDiscovering) {
                        Text("البحث عن الأجهزة القريبة...", color = Color.Gray)
                    } else {
                        Button(onClick = { viewModel.startDiscovery() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تحديث البحث")
                            Spacer(Modifier.width(4.dp))
                            Text("تحديث البحث")
                        }
                    }
                }
            }
            
            if (discoveredDevices.isEmpty() && !isDiscovering) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "لم يتم العثور على أجهزة متوافقة قريبة.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            items(discoveredDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Smartphone, contentDescription = "جهاز", tint = ElectricBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Circle, contentDescription = "متصل", tint = VibrantGreen, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("متصل عبر Wi-Fi", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.syncWithDevice(device) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = "مزامنة")
                            Spacer(Modifier.width(8.dp))
                            Text("مزامنة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            if (syncStatus.isNotBlank()) {
                item {
                    Text(
                        text = syncStatus,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        color = if (syncStatus.contains("فشل")) MaterialTheme.colorScheme.error else VibrantGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (syncHistory.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("سجل المزامنة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricBlue)
                }
                items(syncHistory) { history ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${history.date} - ${history.deviceName}", fontWeight = FontWeight.Bold)
                            Text(history.status, color = if (history.status.contains("نجاح")) VibrantGreen else MaterialTheme.colorScheme.error)
                            Text("إرسال: ${history.sentRecords} | استقبال: ${history.receivedRecords}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
