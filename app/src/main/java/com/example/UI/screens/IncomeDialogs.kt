package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.FinanceCalculator
import com.example.model.WorkOrder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary

@Composable
fun OrderFormDialog(
  defaultDate: String = FinanceCalculator.currentSystemIsoDate(),
  existingOrder: WorkOrder?,
  availableAreas: List<String>,
  onDismiss: () -> Unit,
  onSave: (
    date: String,
    startTime: String,
    endTime: String,
    argo: Long,
    packageCount: Int,
    tipGoPay: Long,
    tipCash: Long,
    pickupDistanceKm: Double,
    deliveryDistanceKm: Double,
    originArea: String,
    destinationArea: String,
    batteryStart: Int,
    batteryEnd: Int,
    note: String,
  ) -> Unit,
) {
  var date by remember { mutableStateOf(existingOrder?.date ?: defaultDate) }
  var startTime by remember { mutableStateOf(existingOrder?.startTime ?: "12:00") }
  var endTime by remember { mutableStateOf(existingOrder?.endTime ?: "12:45") }
  var argoText by remember { mutableStateOf((existingOrder?.argo ?: 55_000L).toString()) }
  var pkgText by remember { mutableStateOf((existingOrder?.packageCount ?: 5).toString()) }
  var tipGoPayText by remember { mutableStateOf((existingOrder?.tipGoPay ?: 0L).toString()) }
  var tipCashText by remember { mutableStateOf((existingOrder?.tipCash ?: 0L).toString()) }
  var pickupKmText by remember {
    mutableStateOf((existingOrder?.pickupDistanceKm ?: 1.2).toString())
  }
  var deliveryKmText by remember {
    mutableStateOf((existingOrder?.deliveryDistanceKm ?: 7.5).toString())
  }
  var originArea by remember { mutableStateOf(existingOrder?.originArea ?: "Tebet") }
  var destArea by remember { mutableStateOf(existingOrder?.destinationArea ?: "Kemang") }
  var batStartText by remember { mutableStateOf((existingOrder?.batteryStart ?: 51).toString()) }
  var batEndText by remember { mutableStateOf((existingOrder?.batteryEnd ?: 38).toString()) }
  var note by remember { mutableStateOf(existingOrder?.note ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = if (existingOrder == null) "Catat Order Baru" else "Edit Order",
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Argo, bonus & tip GoPay masuk ke GoPay. Tip Cash masuk ke Cash.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Tanggal Mulai (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Jam Mulai") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("Jam Selesai") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = argoText,
            onValueChange = { argoText = it.filter(Char::isDigit) },
            label = { Text("Argo (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = pkgText,
            onValueChange = { pkgText = it.filter(Char::isDigit) },
            label = { Text("Jml Paket") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tipGoPayText,
            onValueChange = { tipGoPayText = it.filter(Char::isDigit) },
            label = { Text("Tip GoPay") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = tipCashText,
            onValueChange = { tipCashText = it.filter(Char::isDigit) },
            label = { Text("Tip Cash") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = pickupKmText,
            onValueChange = { pickupKmText = it },
            label = { Text("Jarak Pick Up (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = deliveryKmText,
            onValueChange = { deliveryKmText = it },
            label = { Text("Jarak Antar (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = originArea,
          onValueChange = { originArea = it },
          label = { Text("Wilayah Awal (baru otomatis disimpan)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(availableAreas.take(6)) { area ->
            FilterChip(
              selected = originArea.equals(area, ignoreCase = true),
              onClick = { originArea = area },
              label = { Text(area) },
            )
          }
        }
        OutlinedTextField(
          value = destArea,
          onValueChange = { destArea = it },
          label = { Text("Wilayah Tujuan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = batStartText,
            onValueChange = { batStartText = it.filter(Char::isDigit) },
            label = { Text("Baterai Awal %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = batEndText,
            onValueChange = { batEndText = it.filter(Char::isDigit) },
            label = { Text("Baterai Akhir %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan (opsional)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            date.trim().ifEmpty { defaultDate },
            startTime.trim().ifEmpty { "12:00" },
            endTime.trim().ifEmpty { "12:45" },
            argoText.toLongOrNull() ?: 0L,
            pkgText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            tipGoPayText.toLongOrNull() ?: 0L,
            tipCashText.toLongOrNull() ?: 0L,
            pickupKmText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            deliveryKmText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            originArea,
            destArea,
            batStartText.toIntOrNull() ?: 100,
            batEndText.toIntOrNull() ?: 90,
            note,
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Order")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

@Composable
fun BlankFormDialog(
  defaultDate: String = FinanceCalculator.currentSystemIsoDate(),
  orders: List<WorkOrder>,
  availableAreas: List<String>,
  onDismiss: () -> Unit,
  onSave: (
    date: String,
    startTime: String,
    nextOrderId: String,
    area: String,
    batteryStart: Int,
    batteryEnd: Int,
    distanceKm: Double,
    note: String,
  ) -> Unit,
) {
  val sortedOrders =
    remember(orders, defaultDate) {
      orders.sortedWith(
        compareByDescending<WorkOrder> { it.date == defaultDate }
          .thenByDescending { it.date }
          .thenBy { it.startTime }
      )
    }
  var selectedOrder by remember { mutableStateOf(sortedOrders.firstOrNull()) }
  var startTime by remember { mutableStateOf("08:30") }
  var area by remember { mutableStateOf(selectedOrder?.originArea ?: availableAreas.firstOrNull().orEmpty()) }
  var distanceText by remember { mutableStateOf("0.0") }
  var batStartText by remember { mutableStateOf("86") }
  var batEndText by remember { mutableStateOf("84") }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Catat Waktu Kosong (Blank)",
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Blank selalu berakhir pada waktu mulai order berikutnya dan tidak menghasilkan pendapatan.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        if (sortedOrders.isEmpty()) {
          Text(
            text = "Belum ada order tujuan. Tambahkan order terlebih dahulu karena Blank tidak dibuat setelah order terakhir.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
          )
        } else {
          Text(
            text = "Pilih Order Berikutnya (akhir Blank):",
            style = MaterialTheme.typography.labelMedium,
            color = InkPrimary,
            fontWeight = FontWeight.SemiBold,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sortedOrders.take(6)) { ord ->
              FilterChip(
                selected = selectedOrder?.id == ord.id,
                onClick = { selectedOrder = ord },
                label = {
                  Text("${FinanceCalculator.formatDateIndo(ord.date)} • ${ord.startTime}")
                },
              )
            }
          }
          selectedOrder?.let { ord ->
            Text(
              text = "Selesai Blank otomatis: ${ord.startTime} (${ord.originArea} → ${ord.destinationArea})",
              style = MaterialTheme.typography.labelSmall,
              color = EmeraldPrimary,
            )
          }
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Jam Mulai Blank (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text("Wilayah Tunggu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("Jarak Tempuh Blank (km, boleh 0)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = batStartText,
              onValueChange = { batStartText = it.filter(Char::isDigit) },
              label = { Text("Baterai Awal %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = batEndText,
              onValueChange = { batEndText = it.filter(Char::isDigit) },
              label = { Text("Baterai Akhir %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Catatan (opsional)") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      if (selectedOrder != null) {
        Button(
          onClick = {
            val ord = selectedOrder!!
            onSave(
              ord.date,
              startTime.trim().ifEmpty { "08:30" },
              ord.id,
              area,
              batStartText.toIntOrNull() ?: 85,
              batEndText.toIntOrNull() ?: 83,
              distanceText.replace(',', '.').toDoubleOrNull() ?: 0.0,
              note,
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
        ) {
          Text("Simpan Blank")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
