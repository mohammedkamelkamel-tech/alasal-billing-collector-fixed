package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import com.example.data.model.PermissionKeys
import com.example.data.model.RoleType
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningYellow
import com.example.utils.CurrencyFormatter

@Composable
fun DashboardScreen(
    currentUserProfile: UserProfile,
    canPerformAction: (String) -> Boolean,
    bills: List<BillEntity>,
    users: List<UserEntity>,
    onAddBillClick: () -> Unit,
    onBillClick: (BillEntity) -> Unit,
    onMeterReadingClick: () -> Unit
) {
    val isAdmin = currentUserProfile.roleType == RoleType.SUPERVISOR
    val totalUsers = users.size
    val totalBills = bills.size
    val collected = bills.sumOf { it.paidAmount }
    val outstanding = bills.sumOf { it.remainingAmount.coerceAtLeast(0.0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "لوحة التحكم",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "متابعة الفواتير والتحصيل والعدادات في مكان واحد",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SummaryGrid(
                totalUsers = totalUsers,
                totalBills = totalBills,
                collected = collected,
                outstanding = outstanding
            )
        }

        item {
            SectionTitle("الوصول السريع")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(
                    title = "قراءة العداد",
                    icon = Icons.Filled.ElectricMeter,
                    color = ElectricBlue,
                    modifier = Modifier.weight(1f),
                    enabled = canPerformAction(PermissionKeys.CAN_ADD_BILL),
                    onClick = onMeterReadingClick
                )
                QuickAction(
                    title = "فاتورة جديدة",
                    icon = Icons.Filled.ReceiptLong,
                    color = VibrantGreen,
                    modifier = Modifier.weight(1f),
                    enabled = canPerformAction(PermissionKeys.CAN_ADD_BILL),
                    onClick = onAddBillClick
                )
                QuickAction(
                    title = "التحصيل",
                    icon = Icons.Filled.Payments,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = {}
                )
            }
        }

        item {
            SectionTitle("حالة الفواتير")
            Spacer(Modifier.height(8.dp))
            InvoiceStatusRow(bills)
        }

        item {
            SectionTitle(if (isAdmin) "آخر الفواتير" else "آخر الفواتير المسجلة")
        }

        if (bills.isEmpty()) {
            item {
                EmptyDashboardCard("لا توجد فواتير حتى الآن")
            }
        } else {
            items(bills.take(6), key = { it.id }) { bill ->
                BillSummaryCard(bill = bill, onClick = { onBillClick(bill) })
            }
        }
    }
}

@Composable
private fun SummaryGrid(totalUsers: Int, totalBills: Int, collected: Double, outstanding: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardStatCard("إجمالي المشتركين", totalUsers.toString(), Icons.Filled.People, ElectricBlue, Modifier.weight(1f))
            DashboardStatCard("إجمالي الفواتير", totalBills.toString(), Icons.Filled.ReceiptLong, VibrantGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardStatCard("المحصل", CurrencyFormatter.riyal(collected), Icons.Filled.Payments, Color(0xFFFF9800), Modifier.weight(1f))
            DashboardStatCard("المتبقي", CurrencyFormatter.riyal(outstanding), Icons.Filled.AccountBalanceWallet, ErrorRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = title, tint = color) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: ImageVector, color: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(96.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) color else color.copy(alpha = .35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InvoiceStatusRow(bills: List<BillEntity>) {
    val paid = bills.count { it.status == BillStatus.PAID.name }
    val pending = bills.count { it.status != BillStatus.PAID.name }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatusSummary("مدفوعة", paid, VibrantGreen, Modifier.weight(1f))
        StatusSummary("غير مدفوعة", pending, ErrorRed, Modifier.weight(1f))
        StatusSummary("الكل", bills.size, ElectricBlue, Modifier.weight(1f))
    }
}

@Composable
private fun StatusSummary(title: String, count: Int, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = color)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
}

@Composable
private fun EmptyDashboardCard(text: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BillSummaryCard(bill: BillEntity, onClick: () -> Unit) {
    val status = BillStatus.fromString(bill.status)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("bill_card_${bill.invoiceNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(status.color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = status.color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.userName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("فاتورة #${bill.invoiceNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الاستهلاك: ${bill.consumptionKwh.toInt()} ك.و.س", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.riyal(bill.totalAmount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = status.color)
                Spacer(Modifier.height(5.dp))
                StatusChip(status)
            }
        }
    }
}

@Composable
fun StatusChip(status: BillStatus) {
    Surface(shape = RoundedCornerShape(10.dp), color = status.color.copy(alpha = .12f)) {
        Text(status.titleAr, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = status.color)
    }
}
