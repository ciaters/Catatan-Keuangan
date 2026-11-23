package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BillItem
import com.example.model.BillMonthEntry
import com.example.model.BillStatus
import com.example.model.FinanceCalculator
import com.example.model.FundLocation
import com.example.state.FinanceUiState
import com.example.state.FinanceViewModel
import com.example.ui.components.CleanCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer

@Composable
fun TagihanKewajibanScreen(
  uiState: FinanceUiState,
  viewModel: FinanceViewModel,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts
  val filters = remember { listOf("Semua", "Belum dibayar", "Terlambat", "Sudah dibayar") }
  var selectedFilter by remember { mutableStateOf("Semua") }

  var payingBill by remember { mutableStateOf<Pair<BillItem, BillMonthEntry>?>(null) }
  var editingAmountBill by remember { mutableStateOf<Pair<BillItem, BillMonthEntry>?>(null) }

  val filteredBills =
    remember(uiState.raw.bills, uiState.activeMonthKey, selectedFilter) {
      uiState.raw.bills.filter { bill ->
        val activeEntry =
          bill.history.firstOrNull { it.monthKey == uiState.activeMonthKey }
            ?: bill.history.lastOrNull()
            ?: return@filter false
        when (selectedFilter) {
          "Belum dibayar" -> activeEntry.status == BillStatus.BELUM_DIBAYAR
          "Terlambat" -> activeEntry.status == BillStatus.TERLAMBAT
          "Sudah dibayar" -> activeEntry.status == BillStatus.SUDAH_DIBAYAR
          else -> true
        }
      }
    }

  payingBill?.let { (bill, entry) ->
    PayBillDialog(
      bill = bill,
      entry = entry,
      onDismiss = { payingBill = null },
      onConfirmPay = { source ->
        viewModel.payBill(bill.id, entry.monthKey, source)
        payingBill = null
      },
    )
  }

  editingAmountBill?.let { (bill, entry) ->
    EditBillAmountDialog(
      bill = bill,
      entry = entry,
      onDismiss = { editingAmountBill = null },
      onSave = { newAmount ->
        viewModel.updateBillAmountForMonth(bill.id, entry.monthKey, newAmount)
        editingAmountBill = null
      },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Ringkasan 3 Status Tagihan
      item {
        CleanCard {
          Text(
            text = "Ringkasan Tagihan (${uiState.activeMonthLabel})",
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            BillStatusMini(
              label = "Belum dibayar",
              amount = FinanceCalculator.formatRupiah(uiState.unpaidBillsTotal, hide),
              color = WarmAmber,
              containerColor = WarmAmberContainer.copy(alpha = 0.55f),
              modifier = Modifier.weight(1f),
            )
            BillStatusMini(
              label = "Terlambat",
              amount = FinanceCalculator.formatRupiah(uiState.overdueBillsTotal, hide),
              color = ExpenseRed,
              containerColor = ExpenseRedBg.copy(alpha = 0.55f),
              modifier = Modifier.weight(1f),
            )
            BillStatusMini(
              label = "Sudah dibayar",
              amount = FinanceCalculator.formatRupiah(uiState.paidBillsTotal, hide),
              color = IncomeGreen,
              containerColor = IncomeGreenBg.copy(alpha = 0.55f),
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text =
              "Tagihan Belum dibayar dan Terlambat tidak mengurangi saldo. Pembayaran memilih GoPay atau Cash dan membuat tepat 1 transaksi pengeluaran.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
        }
      }

      // Filter 3 Status
      item {
        SectionHeader(title = "Daftar Tagihan & Kewajiban")
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(filters) { label ->
            FilterChip(
              selected = selectedFilter == label,
              onClick = { selectedFilter = label },
              label = { Text(label) },
              modifier = Modifier.testTag("tagihan_filter_$label"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldContainer,
                  selectedLabelColor = OnEmeraldContainer,
                ),
            )
          }
        }
      }

      items(filteredBills, key = { it.id }) { bill ->
        val activeEntry =
          bill.history.firstOrNull { it.monthKey == uiState.activeMonthKey }
            ?: bill.history.lastOrNull()
        if (activeEntry != null) {
          BillObligationCard(
            bill = bill,
            activeEntry = activeEntry,
            hideAmounts = hide,
            onEditAmount = { editingAmountBill = bill to activeEntry },
            onPay = { payingBill = bill to activeEntry },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun BillStatusMini(
  label: String,
  amount: String,
  color: Color,
  containerColor: Color = SurfaceTinted,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = containerColor,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = amount,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun BillObligationCard(
  bill: BillItem,
  activeEntry: BillMonthEntry,
  hideAmounts: Boolean,
  onEditAmount: () -> Unit,
  onPay: () -> Unit,
) {
  var showHistory by remember { mutableStateOf(false) }
  val pastEntries = bill.history.filter { it.monthKey != activeEntry.monthKey }

  val (pillBg, pillText) =
    when (activeEntry.status) {
      BillStatus.BELUM_DIBAYAR -> WarmAmberContainer to WarmAmber
      BillStatus.TERLAMBAT -> ExpenseRedBg to ExpenseRed
      BillStatus.SUDAH_DIBAYAR -> IncomeGreenBg to IncomeGreen
    }

  CleanCard(modifier = Modifier.testTag("tagihan_item_${bill.id}")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = bill.name,
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "${bill.categoryNote} • ${activeEntry.dueDateLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      StatusPill(
        text = activeEntry.status.label,
        backgroundColor = pillBg,
        textColor = pillText,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "Nominal ${activeEntry.monthLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = InkMuted,
        )
        Text(
          text = FinanceCalculator.formatRupiah(activeEntry.amount, hideAmounts),
          style = MaterialTheme.typography.headlineSmall,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
        )
      }
      if (activeEntry.status == BillStatus.SUDAH_DIBAYAR && activeEntry.paidSource != null) {
        Text(
          text = "Dibayar via ${activeEntry.paidSource.displayName}",
          style = MaterialTheme.typography.labelMedium,
          color = IncomeGreen,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    if (activeEntry.status != BillStatus.SUDAH_DIBAYAR) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onEditAmount,
          modifier = Modifier.weight(1f),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Ubah Nominal", color = InkPrimary)
        }
        Button(
          onClick = onPay,
          modifier = Modifier.weight(1f).testTag("tagihan_bayar_${bill.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Bayar")
        }
      }
    }

    if (pastEntries.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      TextButton(onClick = { showHistory = !showHistory }) {
        Icon(
          Icons.Outlined.History,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text =
            if (showHistory) "Tutup Riwayat Bulan Sebelumnya"
            else "Lihat Riwayat Bulan Sebelumnya (${pastEntries.size})",
          color = EmeraldPrimary,
        )
      }
      AnimatedVisibility(visible = showHistory) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          HorizontalDivider(
            thickness = 0.75.dp,
            color = OutlineSubtle.copy(alpha = 0.55f),
          )
          pastEntries.forEach { old ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text =
                  "${old.monthLabel} (${old.status.label}" +
                    (if (old.paidSource != null) " • ${old.paidSource.shortName}" else "") +
                    ")",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
              Text(
                text = FinanceCalculator.formatRupiah(old.amount, hideAmounts),
                style = MaterialTheme.typography.bodySmall,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PayBillDialog(
  bill: BillItem,
  entry: BillMonthEntry,
  onDismiss: () -> Unit,
  onConfirmPay: (FundLocation) -> Unit,
) {
  var selectedSource by remember { mutableStateOf(FundLocation.GOPAY) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Bayar ${bill.name}",
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Nominal: ${FinanceCalculator.formatRupiah(entry.amount)}",
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            "Pilih sumber pembayaran (GoPay atau Cash). Jika dana ada di Jago Tabungan Cicilan, transfer dulu ke GoPay/Cash. Pembayaran membuat tepat 1 transaksi pengeluaran terhubung.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FundLocation.directSpendLocations.forEach { loc ->
            FilterChip(
              selected = selectedSource == loc,
              onClick = { selectedSource = loc },
              label = { Text(loc.displayName) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmPay(selectedSource) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Konfirmasi Bayar")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun EditBillAmountDialog(
  bill: BillItem,
  entry: BillMonthEntry,
  onDismiss: () -> Unit,
  onSave: (Long) -> Unit,
) {
  var amountText by remember { mutableStateOf(entry.amount.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Nominal ${entry.monthLabel}",
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Masukkan nominal manual untuk ${bill.name}. Riwayat bulan sebelumnya tetap tersimpan.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter(Char::isDigit) },
          label = { Text("Nominal (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(amountText.toLongOrNull() ?: entry.amount) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Nominal")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}
