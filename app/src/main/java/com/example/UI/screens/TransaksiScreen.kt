package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DailyIncomeSummary
import com.example.model.ExpenseCategory
import com.example.model.ExpenseRecord
import com.example.model.FinanceCalculator
import com.example.model.FundLocation
import com.example.model.GroupedExpenseDisplay
import com.example.model.TransactionListEntry
import com.example.model.TransferRecord
import com.example.state.FinanceUiState
import com.example.state.FinanceViewModel
import com.example.ui.components.CleanCard
import com.example.ui.components.IconBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaksiScreen(
  uiState: FinanceUiState,
  viewModel: FinanceViewModel,
  onNavigateToStatistik: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts

  LaunchedEffect(Unit) {
    viewModel.refreshTodayDate()
  }

  // Default selalu hari ini; otomatis mengikuti tanggal baru bila hari berganti
  var selectedDate by remember(uiState.todayDate) { mutableStateOf(uiState.todayDate) }
  var showDatePicker by remember { mutableStateOf(false) }

  var typeFilter by remember { mutableStateOf("Semua") }
  var selectedLocation by remember { mutableStateOf<FundLocation?>(null) }

  var selectedExpenseGroupKey by remember { mutableStateOf<String?>(null) }
  var editingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
  var selectedIncomeDayDetail by remember { mutableStateOf<DailyIncomeSummary?>(null) }

  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var showAddTransferDialog by remember { mutableStateOf(false) }
  var showCategoryManager by remember { mutableStateOf(false) }

  val dailySummary =
    remember(selectedDate, uiState.dailySummariesByDate, uiState.raw.expenses, uiState.raw.transfers) {
      FinanceCalculator.computeDailyTransactionSummary(
        date = selectedDate,
        dailySummariesByDate = uiState.dailySummariesByDate,
        expenses = uiState.raw.expenses,
        transfers = uiState.raw.transfers,
      )
    }

  val entriesOnSelectedDate =
    remember(uiState.transactionEntries, selectedDate) {
      uiState.transactionEntries.filter { it.date == selectedDate }
    }

  val filteredEntries =
    remember(entriesOnSelectedDate, typeFilter, selectedLocation) {
      entriesOnSelectedDate.filter { entry ->
        val typeOk =
          when (typeFilter) {
            "Pemasukan" -> entry is TransactionListEntry.DailyIncomeRow
            "Pengeluaran" -> entry is TransactionListEntry.GroupedExpenseRow
            "Transfer" -> entry is TransactionListEntry.TransferRow
            else -> true
          }
        if (!typeOk) return@filter false

        val loc = selectedLocation ?: return@filter true
        when (entry) {
          is TransactionListEntry.DailyIncomeRow ->
            when (loc) {
              FundLocation.GOPAY ->
                (entry.summary.totalArgo + entry.summary.totalBonus + entry.summary.totalTipGoPay) > 0L
              FundLocation.CASH -> entry.summary.totalTipCash > 0L
              FundLocation.JAGO_CICILAN, FundLocation.JAGO_PRIBADI -> false
            }
          is TransactionListEntry.GroupedExpenseRow ->
            entry.group.items.any { it.source == loc }
          is TransactionListEntry.TransferRow ->
            entry.transfer.from == loc || entry.transfer.to == loc
        }
      }
    }

  val selectedExpenseGroupDetail =
    remember(selectedExpenseGroupKey, uiState.transactionEntries) {
      val key = selectedExpenseGroupKey ?: return@remember null
      uiState.transactionEntries
        .filterIsInstance<TransactionListEntry.GroupedExpenseRow>()
        .firstOrNull { it.group.groupKey == key }
        ?.group
    }

  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = FinanceCalculator.isoDateToUtcMillis(selectedDate),
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              selectedDate = FinanceCalculator.utcMillisToIsoDate(millis)
            }
            showDatePicker = false
          },
          modifier = Modifier.testTag("transaksi_date_picker_confirm"),
        ) {
          Text("Pilih", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Batal")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  selectedExpenseGroupDetail?.let { group ->
    AlertDialog(
      onDismissRequest = { selectedExpenseGroupKey = null },
      containerColor = MaterialTheme.colorScheme.surface,
      title = {
        Text(
          text = "Rincian ${group.categoryName}",
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text =
              "${FinanceCalculator.formatDateWithDayIndo(group.date)} • ${group.items.size} item digabung",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
          group.items.forEach { item ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = SurfaceTinted,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = FinanceCalculator.formatRupiah(item.amount, hide),
                    style = MaterialTheme.typography.titleSmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Bold,
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                      text = item.source.displayName,
                      backgroundColor = EmeraldContainer,
                      textColor = OnEmeraldContainer,
                    )
                    IconButton(
                      onClick = { editingExpense = item },
                      modifier = Modifier.size(34.dp).testTag("expense_item_edit_${item.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit pengeluaran",
                        tint = InkSecondary,
                        modifier = Modifier.size(17.dp),
                      )
                    }
                    IconButton(
                      onClick = {
                        viewModel.deleteExpense(item.id)
                        if (group.items.size <= 1) {
                          selectedExpenseGroupKey = null
                        }
                      },
                      modifier = Modifier.size(34.dp).testTag("expense_item_delete_${item.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Hapus pengeluaran",
                        tint = ExpenseRed,
                        modifier = Modifier.size(17.dp),
                      )
                    }
                  }
                }
                if (item.note.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                  )
                }
                if (item.linkedBillId != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Terhubung dengan Tagihan & Kewajiban",
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoBlue,
                  )
                }
              }
            }
          }
          HorizontalDivider(
            thickness = 0.75.dp,
            color = OutlineSubtle.copy(alpha = 0.55f),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Total Pengeluaran",
              style = MaterialTheme.typography.titleSmall,
              color = InkPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = FinanceCalculator.formatRupiah(group.totalAmount, hide),
              style = MaterialTheme.typography.titleSmall,
              color = ExpenseRed,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedExpenseGroupKey = null }) {
          Text("Tutup", color = EmeraldPrimary)
        }
      },
    )
  }

  selectedIncomeDayDetail?.let { summary ->
    AlertDialog(
      onDismissRequest = { selectedIncomeDayDetail = null },
      containerColor = MaterialTheme.colorScheme.surface,
      title = {
        Text(
          text = "Pemasukan ${FinanceCalculator.formatDateIndo(summary.date)}",
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "${summary.orderCount} order selesai • ${summary.totalPackages} paket",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
          DetailLine("Argo (masuk GoPay)", FinanceCalculator.formatRupiah(summary.totalArgo, hide))
          DetailLine(
            "Bonus kumulatif terakhir (masuk GoPay)",
            FinanceCalculator.formatRupiah(summary.totalBonus, hide),
          )
          DetailLine(
            "Tip GoPay (masuk GoPay)",
            FinanceCalculator.formatRupiah(summary.totalTipGoPay, hide),
          )
          DetailLine(
            "Tip Cash (masuk Cash)",
            FinanceCalculator.formatRupiah(summary.totalTipCash, hide),
          )
          HorizontalDivider()
          DetailLine(
            "Total Pemasukan Harian",
            FinanceCalculator.formatRupiah(summary.totalIncome, hide),
            bold = true,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedIncomeDayDetail = null }) {
          Text("Tutup", color = EmeraldPrimary)
        }
      },
    )
  }

  if (showAddExpenseDialog || editingExpense != null) {
    ExpenseFormDialog(
      defaultDate = selectedDate,
      existingExpense = editingExpense,
      categories = uiState.raw.categories,
      onDismiss = {
        showAddExpenseDialog = false
        editingExpense = null
      },
      onSave = { date, categoryName, amount, source, note ->
        val currentEdit = editingExpense
        if (currentEdit == null) {
          viewModel.addExpense(date, categoryName, amount, source, note)
        } else {
          val updated =
            currentEdit.copy(
              date = date,
              categoryName = categoryName,
              amount = amount,
              source = source,
              note = note,
            )
          viewModel.updateExpense(updated)
          selectedExpenseGroupKey = "${updated.date}|${updated.categoryName}"
        }
        showAddExpenseDialog = false
        editingExpense = null
      },
    )
  }

  if (showAddTransferDialog) {
    AddTransferDialog(
      defaultDate = selectedDate,
      onDismiss = { showAddTransferDialog = false },
      onSave = { date, from, to, amount, note ->
        viewModel.addTransfer(date, from, to, amount, note)
        showAddTransferDialog = false
      },
    )
  }

  if (showCategoryManager) {
    ManageExpenseCategoriesDialog(
      categories = uiState.raw.categories,
      onAdd = viewModel::addCategory,
      onRename = viewModel::renameCategory,
      onDelete = viewModel::deleteCategory,
      onDismiss = { showCategoryManager = false },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("transaksi_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Navigasi Tanggal (< Tanggal > + Pemilih Tanggal)
      item {
        CleanCard(modifier = Modifier.testTag("transaksi_date_navigator")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { selectedDate = FinanceCalculator.shiftIsoDate(selectedDate, -1) },
              modifier = Modifier.testTag("transaksi_prev_date"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Tanggal sebelumnya",
                tint = EmeraldPrimary,
              )
            }

            Row(
              modifier =
                Modifier
                  .weight(1f)
                  .clip(MaterialTheme.shapes.small)
                  .clickable { showDatePicker = true }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("transaksi_date_picker_button"),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Pilih tanggal",
                tint = EmeraldPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = FinanceCalculator.formatDateWithDayIndo(selectedDate),
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("transaksi_selected_date_label"),
              )
              if (selectedDate == uiState.todayDate) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                  text = "Hari Ini",
                  backgroundColor = EmeraldContainer,
                  textColor = OnEmeraldContainer,
                )
              }
            }

            IconButton(
              onClick = { selectedDate = FinanceCalculator.shiftIsoDate(selectedDate, 1) },
              modifier = Modifier.testTag("transaksi_next_date"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Tanggal berikutnya",
                tint = EmeraldPrimary,
              )
            }
          }

          if (selectedDate != uiState.todayDate) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
            ) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = EmeraldContainer.copy(alpha = 0.65f),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { selectedDate = uiState.todayDate }
                    .testTag("transaksi_jump_today"),
              ) {
                Text(
                  text = "Kembali ke Hari Ini (${FinanceCalculator.formatDateIndo(uiState.todayDate)})",
                  style = MaterialTheme.typography.labelSmall,
                  color = OnEmeraldContainer,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }

      // Ringkasan Transaksi Harian per Tanggal (Pemasukan, Pengeluaran, dan Selisih; Transfer terpisah)
      item {
        CleanCard(modifier = Modifier.testTag("transaksi_daily_summary_card")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ringkasan Harian",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = FinanceCalculator.formatDateWithDayIndo(selectedDate),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
              )
            }
            StatusPill(
              text = "Transfer terpisah",
              backgroundColor = InfoBlueBg,
              textColor = InfoBlue,
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DailySummaryMetricTile(
              label = "Pemasukan",
              value = FinanceCalculator.formatRupiah(dailySummary.totalIncome, hide),
              valueColor = IncomeGreen,
              containerColor = SurfaceTinted,
              valueTestTag = "transaksi_daily_income_value",
              modifier = Modifier.weight(1f).testTag("transaksi_daily_income_box"),
            )
            DailySummaryMetricTile(
              label = "Pengeluaran",
              value = FinanceCalculator.formatRupiah(dailySummary.totalExpense, hide),
              valueColor = ExpenseRed,
              containerColor = SurfaceTinted,
              valueTestTag = "transaksi_daily_expense_value",
              modifier = Modifier.weight(1f).testTag("transaksi_daily_expense_box"),
            )
            val diffColor =
              when {
                dailySummary.difference > 0L -> IncomeGreen
                dailySummary.difference < 0L -> ExpenseRed
                else -> InkSecondary
              }
            val diffContainer =
              when {
                dailySummary.difference > 0L -> IncomeGreenBg.copy(alpha = 0.55f)
                dailySummary.difference < 0L -> ExpenseRedBg.copy(alpha = 0.55f)
                else -> SurfaceTinted
              }
            DailySummaryMetricTile(
              label = "Selisih",
              value = FinanceCalculator.formatDifferenceStatus(dailySummary.difference, hide),
              valueColor = diffColor,
              containerColor = diffContainer,
              valueTestTag = "transaksi_daily_diff_value",
              modifier = Modifier.weight(1f).testTag("transaksi_daily_diff_box"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text =
              "Transfer pada tanggal ini: ${FinanceCalculator.formatRupiah(dailySummary.totalTransfer, hide)} (tidak dihitung ke pemasukan, pengeluaran, maupun selisih).",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
        }
      }

      // Aksi Cepat Pengeluaran, Transfer, dan Kelola Kategori
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { showAddExpenseDialog = true },
            modifier = Modifier.weight(1f).height(46.dp).testTag("transaksi_add_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
              ),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pengeluaran", maxLines = 1)
          }
          OutlinedButton(
            onClick = { showAddTransferDialog = true },
            modifier = Modifier.weight(1f).height(46.dp).testTag("transaksi_transfer_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              Icons.Outlined.SwapHoriz,
              contentDescription = null,
              tint = InfoBlue,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Transfer", color = InkPrimary, maxLines = 1)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { showCategoryManager = true },
            modifier = Modifier.weight(1f).height(42.dp).testTag("transaksi_manage_categories"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              Icons.Outlined.Category,
              contentDescription = null,
              tint = EmeraldPrimary,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kelola Kategori (${uiState.raw.categories.size})", color = EmeraldPrimary)
          }
          OutlinedButton(
            onClick = onNavigateToStatistik,
            modifier = Modifier.height(42.dp).testTag("transaksi_statistik_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Statistik", color = InkPrimary)
          }
        }
      }

      // Filter Tipe & Lokasi Dana
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(listOf("Semua", "Pemasukan", "Pengeluaran", "Transfer")) { label ->
            FilterChip(
              selected = typeFilter == label,
              onClick = { typeFilter = label },
              label = { Text(label) },
              modifier = Modifier.testTag("transaksi_filter_$label"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldContainer,
                  selectedLabelColor = OnEmeraldContainer,
                ),
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedLocation == null,
              onClick = { selectedLocation = null },
              label = { Text("Semua Dana") },
            )
          }
          items(FundLocation.entries) { loc ->
            FilterChip(
              selected = selectedLocation == loc,
              onClick = { selectedLocation = if (selectedLocation == loc) null else loc },
              label = { Text(loc.shortName) },
            )
          }
        }
      }

      // Daftar Transaksi pada tanggal terpilih (terkelompok sesuai aturan)
      if (entriesOnSelectedDate.isEmpty()) {
        item {
          CleanCard(modifier = Modifier.testTag("transaksi_empty_state")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconBadge(
                icon = Icons.Outlined.ReceiptLong,
                containerColor = EmeraldContainer.copy(alpha = 0.7f),
                iconColor = EmeraldPrimary,
                size = 42,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (selectedDate == uiState.todayDate) {
                      "Belum ada transaksi hari ini"
                    } else {
                      "Belum ada transaksi pada ${FinanceCalculator.formatDateIndo(selectedDate)}"
                    },
                  style = MaterialTheme.typography.titleSmall,
                  color = InkPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text =
                    if (selectedDate == uiState.todayDate) {
                      "Tambahkan pengeluaran atau transfer di atas, atau catat order di halaman Income. Riwayat tanggal sebelumnya dan saldo tetap tersimpan."
                    } else {
                      "Tidak ada pemasukan, pengeluaran, maupun transfer pada tanggal ini. Gunakan tombol panah atau pemilih tanggal untuk melihat hari lain."
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
              }
            }
          }
        }
      } else if (filteredEntries.isEmpty()) {
        item {
          CleanCard(modifier = Modifier.testTag("transaksi_filter_empty_state")) {
            Text(
              text =
                "Tidak ada transaksi yang cocok dengan filter pada ${FinanceCalculator.formatDateIndo(selectedDate)}.",
              style = MaterialTheme.typography.bodyMedium,
              color = InkSecondary,
            )
          }
        }
      } else {
        items(filteredEntries) { entry ->
          when (entry) {
            is TransactionListEntry.DailyIncomeRow ->
              DailyIncomeTransactionCard(
                summary = entry.summary,
                hideAmounts = hide,
                onClick = { selectedIncomeDayDetail = entry.summary },
              )
            is TransactionListEntry.GroupedExpenseRow ->
              GroupedExpenseTransactionCard(
                group = entry.group,
                hideAmounts = hide,
                onClick = { selectedExpenseGroupKey = entry.group.groupKey },
              )
            is TransactionListEntry.TransferRow ->
              TransferTransactionCard(transfer = entry.transfer, hideAmounts = hide)
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun DetailLine(label: String, value: String, bold: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = if (bold) InkPrimary else InkSecondary,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = InkPrimary,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
    )
  }
}

@Composable
private fun DailyIncomeTransactionCard(
  summary: DailyIncomeSummary,
  hideAmounts: Boolean,
  onClick: () -> Unit,
) {
  CleanCard(onClick = onClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconBadge(
        icon = Icons.Outlined.ArrowDownward,
        containerColor = IncomeGreenBg,
        iconColor = IncomeGreen,
        size = 42,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Pemasukan Kerja Harian",
          style = MaterialTheme.typography.titleSmall,
          color = InkPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            "${FinanceCalculator.formatDateWithDayIndo(summary.date)} • ${summary.orderCount} order (${summary.totalPackages} pkt)",
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "+${FinanceCalculator.formatRupiah(summary.totalIncome, hideAmounts)}",
          style = MaterialTheme.typography.titleMedium,
          color = IncomeGreen,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Ketuk rincian",
          style = MaterialTheme.typography.labelSmall,
          color = EmeraldPrimary,
        )
      }
    }
  }
}

@Composable
private fun GroupedExpenseTransactionCard(
  group: GroupedExpenseDisplay,
  hideAmounts: Boolean,
  onClick: () -> Unit,
) {
  val sourcesSummary = group.items.map { it.source.shortName }.distinct().joinToString(" & ")
  CleanCard(onClick = onClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconBadge(
        icon = Icons.Outlined.ArrowUpward,
        containerColor = ExpenseRedBg,
        iconColor = ExpenseRed,
        size = 42,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = group.categoryName,
          style = MaterialTheme.typography.titleSmall,
          color = InkPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            "${FinanceCalculator.formatDateIndo(group.date)} • $sourcesSummary" +
              if (group.items.size > 1) " (${group.items.size} item)" else "",
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "-${FinanceCalculator.formatRupiah(group.totalAmount, hideAmounts)}",
          style = MaterialTheme.typography.titleMedium,
          color = ExpenseRed,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Buka rincian",
          style = MaterialTheme.typography.labelSmall,
          color = InkMuted,
        )
      }
    }
  }
}

@Composable
private fun TransferTransactionCard(
  transfer: TransferRecord,
  hideAmounts: Boolean,
) {
  CleanCard(
    containerColor = InfoBlueBg.copy(alpha = 0.4f),
    borderColor = InfoBlue.copy(alpha = 0.16f),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconBadge(
        icon = Icons.Outlined.SwapHoriz,
        containerColor = InfoBlueBg,
        iconColor = InfoBlue,
        size = 42,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Transfer: ${transfer.from.shortName} → ${transfer.to.shortName}",
          style = MaterialTheme.typography.titleSmall,
          color = InkPrimary,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            "${FinanceCalculator.formatDateIndo(transfer.date)}" +
              if (transfer.note.isNotBlank()) " • ${transfer.note}" else "",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = FinanceCalculator.formatRupiah(transfer.amount, hideAmounts),
          style = MaterialTheme.typography.titleSmall,
          color = InfoBlue,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Non-arus kas",
          style = MaterialTheme.typography.labelSmall,
          color = InkMuted,
        )
      }
    }
  }
}

@Composable
private fun DailySummaryMetricTile(
  label: String,
  value: String,
  valueColor: Color,
  containerColor: Color,
  valueTestTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = containerColor,
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = InkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = valueColor,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(valueTestTag),
      )
    }
  }
}

@Composable
private fun ExpenseFormDialog(
  defaultDate: String,
  existingExpense: ExpenseRecord?,
  categories: List<ExpenseCategory>,
  onDismiss: () -> Unit,
  onSave: (String, String, Long, FundLocation, String) -> Unit,
) {
  var date by remember(existingExpense) { mutableStateOf(existingExpense?.date ?: defaultDate) }
  var selectedCategory by remember(existingExpense) {
    mutableStateOf(
      existingExpense?.categoryName ?: categories.firstOrNull()?.name ?: "Kebutuhan Pribadi"
    )
  }
  var amountText by remember(existingExpense) {
    mutableStateOf((existingExpense?.amount ?: 25_000L).toString())
  }
  var selectedSource by remember(existingExpense) {
    mutableStateOf(existingExpense?.source ?: FundLocation.GOPAY)
  }
  var note by remember(existingExpense) { mutableStateOf(existingExpense?.note ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text =
          if (existingExpense == null) "Catat Pengeluaran (Tanpa Jam)"
          else "Edit Pengeluaran",
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
          text = "Pengeluaran langsung hanya boleh dari GoPay atau Cash. Dana Jago harus ditransfer dulu.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Tanggal (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Sumber Dana", style = MaterialTheme.typography.labelMedium, color = InkPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FundLocation.directSpendLocations.forEach { loc ->
            FilterChip(
              selected = selectedSource == loc,
              onClick = { selectedSource = loc },
              label = { Text(loc.displayName) },
            )
          }
        }
        Text("Kategori Pengeluaran", style = MaterialTheme.typography.labelMedium, color = InkPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat.name,
              onClick = { selectedCategory = cat.name },
              label = { Text(cat.name) },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter(Char::isDigit) },
          label = { Text("Nominal (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            date.trim().ifEmpty { defaultDate },
            selectedCategory,
            amountText.toLongOrNull() ?: 0L,
            selectedSource,
            note,
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun AddTransferDialog(
  defaultDate: String,
  onDismiss: () -> Unit,
  onSave: (String, FundLocation, FundLocation, Long, String) -> Unit,
) {
  var date by remember { mutableStateOf(defaultDate) }
  var fromLoc by remember { mutableStateOf(FundLocation.JAGO_CICILAN) }
  var toLoc by remember { mutableStateOf(FundLocation.GOPAY) }
  var amountText by remember { mutableStateOf("200000") }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Transfer Antar 4 Lokasi Dana",
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
          text = "Transfer tidak dihitung sebagai pemasukan maupun pengeluaran (tanpa jam).",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Tanggal (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Dari Lokasi", style = MaterialTheme.typography.labelMedium, color = InkPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(FundLocation.entries) { loc ->
            FilterChip(
              selected = fromLoc == loc,
              onClick = { fromLoc = loc },
              label = { Text(loc.shortName) },
            )
          }
        }
        Text("Ke Lokasi", style = MaterialTheme.typography.labelMedium, color = InkPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(FundLocation.entries) { loc ->
            FilterChip(
              selected = toLoc == loc,
              onClick = { toLoc = loc },
              label = { Text(loc.shortName) },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter(Char::isDigit) },
          label = { Text("Nominal Transfer (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = fromLoc != toLoc && (amountText.toLongOrNull() ?: 0L) > 0L,
        onClick = {
          onSave(
            date.trim().ifEmpty { defaultDate },
            fromLoc,
            toLoc,
            amountText.toLongOrNull() ?: 0L,
            note,
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Transfer")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
fun ManageExpenseCategoriesDialog(
  categories: List<ExpenseCategory>,
  onAdd: (String) -> Unit,
  onRename: (String, String) -> Unit,
  onDelete: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var newCatName by remember { mutableStateOf("") }
  var editingCat by remember { mutableStateOf<ExpenseCategory?>(null) }
  var editText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Kelola Kategori Pengeluaran",
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
          text = "Semua kategori bebas ditambah, diubah, atau dihapus sesuai kebutuhan pengguna.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedTextField(
            value = newCatName,
            onValueChange = { newCatName = it },
            label = { Text("Kategori baru") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = {
              onAdd(newCatName)
              newCatName = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
          ) {
            Text("Tambah")
          }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        categories.forEach { cat ->
          if (editingCat?.id == cat.id) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = {
                  onRename(cat.id, editText)
                  editingCat = null
                }
              ) { Text("Simpan") }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = cat.name,
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
                modifier = Modifier.weight(1f),
              )
              Row {
                IconButton(
                  onClick = {
                    editingCat = cat
                    editText = cat.name
                  },
                  modifier = Modifier.size(34.dp),
                ) {
                  Icon(Icons.Outlined.Edit, contentDescription = "Ubah kategori", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { onDelete(cat.id) },
                  modifier = Modifier.size(34.dp),
                ) {
                  Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Hapus kategori",
                    tint = ExpenseRed,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Selesai", color = EmeraldPrimary) }
    },
  )
}
