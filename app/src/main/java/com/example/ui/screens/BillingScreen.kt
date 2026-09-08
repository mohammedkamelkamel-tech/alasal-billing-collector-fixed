package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import com.example.data.model.UserEntity
import com.example.ui.components.PaymentDialog
import com.example.utils.CurrencyFormatter
import com.example.data.model.PermissionKeys
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VibrantGreen

@Composable
fun BillingScreen(
    bills: List<BillEntity>,
    users: List<UserEntity>,
    canPerformAction: (String) -> Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filter: String,
    onFilterChange: (String) -> Unit,
    onBillClick: (BillEntity) -> Unit,
    onAddBillClick: () -> Unit,
    onPayClick: (BillEntity, Double, String) -> Unit
) {
    // نافذة الدفع الجزئي للفاتورة المحددة
    var payingBill by remember { mutableStateOf<BillEntity?>(null) }
    var selectedSubscriberId by remember { mutableStateOf<String?>(null) }

    payingBill?.let { target ->
        PaymentDialog(
            bill = target,
            onDismiss = { payingBill = null },
            onConfirm = { amount, method ->
                payingBill = null
                onPayClick(target, amount, method)
            }
        )
    }

    val context = LocalContext.current

    val selectedSubscriber = selectedSubscriberId?.let { id -> users.firstOrNull { it.id == id } }
    val selectedSubscriberBills = remember(bills, selectedSubscriberId, searchQuery, filter) {
        val id = selectedSubscriberId ?: return@remember emptyList()
        bills.filter { bill ->
            bill.userId == id &&
                (searchQuery.isEmpty() || bill.invoiceNumber.contains(searchQuery, ignoreCase = true)) &&
                when (filter) {
                    "UNPAID" -> bill.status == "UNPAID"
                    "OVERDUE" -> bill.status == "OVERDUE" || bill.status == "PARTIAL"
                    "PAID" -> bill.status == "PAID"
                    else -> true
                }
        }.sortedByDescending { it.createdAt }
    }

    val filteredBills = remember(bills, searchQuery, filter) {
        bills.filter { bill ->
            val matchesSearch = searchQuery.isEmpty() ||
                    bill.userName.contains(searchQuery, ignoreCase = true) ||
                    bill.invoiceNumber.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filter) {
                "UNPAID" -> bill.status == "UNPAID"
                // المتأخرات تشمل الفاتورة المدفوعة جزئياً لأنها ما زال عليها مبلغ مستحق.
                "OVERDUE" -> bill.status == "OVERDUE" || bill.status == "PARTIAL"
                "PAID" -> bill.status == "PAID"
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { it.createdAt }
    }

    val subscriberList = remember(bills, users, searchQuery, filter) {
        val eligibleBills = when (filter) {
            "UNPAID" -> bills.filter { it.status == "UNPAID" }
            "OVERDUE" -> bills.filter { it.status == "OVERDUE" || it.status == "PARTIAL" }
            "PAID" -> bills.filter { it.status == "PAID" }
            else -> bills
        }
        val byUser = eligibleBills.groupBy { it.userId }
        users.filter { user ->
            val matchesSearch = searchQuery.isEmpty() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.userIdCode.contains(searchQuery, ignoreCase = true)
            // في "جميع الفواتير" نعرض كل المشتركين، حتى من لم تصدر له فاتورة بعد.
            // في الفلاتر الأخرى نعرض فقط المشتركين الذين لديهم فاتورة مطابقة.
            matchesSearch && (filter == "ALL" || byUser[user.id].orEmpty().isNotEmpty())
        }.sortedWith(compareByDescending<UserEntity> { user -> eligibleBills.filter { it.userId == user.id }.maxOfOrNull { it.createdAt } ?: 0L }.thenBy { it.name })
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث باسم المشترك أو رقم الفاتورة...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("billing_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Filter Chips (LazyRow) لمنع انضغاط الأزرار رأسيًا
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter == "ALL",
                        onClick = { onFilterChange("ALL") },
                        label = { Text("جميع الفواتير", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = filter == "UNPAID",
                        onClick = { onFilterChange("UNPAID") },
                        label = { Text("غير مدفوعة", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("filter_unpaid_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = filter == "OVERDUE",
                        onClick = { onFilterChange("OVERDUE") },
                        label = { Text("متأخرة", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("filter_overdue_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = filter == "PAID",
                        onClick = { onFilterChange("PAID") },
                        label = { Text("مدفوعة", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("filter_paid_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filter == "ALL" && selectedSubscriberId == null) {
                if (subscriberList.isEmpty()) {
                    EmptyBillingState(text = "لا يوجد مشترك لديه فواتير بعد")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(subscriberList) { user ->
                            SubscriberBillsCard(
                                user = user,
                                bills = bills.filter { it.userId == user.id },
                                onClick = {
                                    selectedSubscriberId = user.id
                                    onSearchQueryChange("")
                                }
                            )
                        }
                    }
                }
            } else if (selectedSubscriberId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedSubscriberId = null; onSearchQueryChange("") }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع إلى المشتركين")
                    }
                    Text(
                        text = selectedSubscriber?.name ?: "فواتير المشترك",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${selectedSubscriberBills.size} فاتورة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedSubscriberBills.isEmpty()) {
                    EmptyBillingState(text = "لا توجد فواتير مطابقة لهذا المشترك")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(selectedSubscriberBills) { bill ->
                            DetailedBillCard(
                                bill = bill,
                                canPay = canPerformAction(PermissionKeys.CAN_PAY_BILL),
                                onClick = { onBillClick(bill) },
                                onPayClick = { payingBill = bill },
                                onPrintClick = { com.example.utils.PrintHelper.printBill(context, bill) }
                            )
                        }
                    }
                }
            } else if (filteredBills.isEmpty()) {
                EmptyBillingState(text = "لا توجد فواتير مطابقة للبحث أو الفلتر")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredBills) { bill ->
                        DetailedBillCard(
                            bill = bill,
                            canPay = canPerformAction(PermissionKeys.CAN_PAY_BILL),
                            onClick = { onBillClick(bill) },
                            onPayClick = { payingBill = bill },
                            onPrintClick = { com.example.utils.PrintHelper.printBill(context, bill) }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Adding Bill (Only if canAddBill is true)
        if (canPerformAction(PermissionKeys.CAN_ADD_BILL)) {
            FloatingActionButton(
                onClick = onAddBillClick,
                containerColor = VibrantGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_bill_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "إضافة فاتورة")
            }
        }
    }
}

@Composable
private fun EmptyBillingState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
private fun SubscriberBillsCard(
    user: UserEntity,
    bills: List<BillEntity>,
    onClick: () -> Unit
) {
    val outstanding = bills
        .filter { BillStatus.isOutstanding(it.status) }
        .sumOf { it.remainingAmount }
    val paid = bills.sumOf { it.paidAmount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("subscriber_bills_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(ElectricBlue, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "${bills.size} فاتورة • المدفوع: ${CurrencyFormatter.riyal(paid)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (outstanding > 0.0) {
                    Text(
                        "المتأخرات: ${CurrencyFormatter.riyal(outstanding)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(Icons.Filled.ChevronLeft, contentDescription = "عرض الفواتير")
        }
    }
}

@Composable
fun DetailedBillCard(
    bill: BillEntity,
    canPay: Boolean,
    onClick: () -> Unit,
    onPayClick: () -> Unit,
    onPrintClick: () -> Unit
) {
    val statusEnum = BillStatus.fromString(bill.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("bill_item_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = bill.userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "رقم الفاتورة: ${bill.invoiceNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )
                }
                StatusChip(status = statusEnum)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "قراءة العداد: السابقة: ${bill.prevReading.toInt()} | الحالية: ${bill.currentReading.toInt()} | الاستهلاك: ${bill.consumptionKwh.toInt()} ك.و.س",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المبلغ الإجمالي:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.riyalFull(bill.totalAmount),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = statusEnum.color
                    )
                }

                Text(
                    text = "تاريخ الاستحقاق: ${bill.dueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Visibility, contentDescription = "التفاصيل", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("التفاصيل", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onPrintClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }

                if (bill.status != "PAID" && canPay) {
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                    ) {
                        Icon(imageVector = Icons.Filled.Payment, contentDescription = "دفع", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دفع", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
