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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FinanceCalculator
import com.example.model.LocationBalanceDetail
import com.example.model.MonthlyExpenseSummary
import com.example.state.FinanceUiState
import com.example.ui.components.CleanCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted
import com.example.ui.theme.SurfaceVariantSoft

@Composable
fun RingkasanKeuanganScreen(
  uiState: FinanceUiState,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts
  var selectedExpenseMonthKey by remember { mutableStateOf(uiState.activeMonthKey) }
  val availableExpenseMonths =
    remember(uiState.activeMonthKey, uiState.raw.expenses) {
      val prevMonth = FinanceCalculator.shiftMonthKey(uiState.activeMonthKey, -1)
      val nextMonth = FinanceCalculator.shiftMonthKey(uiState.activeMonthKey, 1)
      val dataMonths = uiState.raw.expenses.mapNotNull { it.date.take(7).takeIf { m -> m.length == 7 } }
      (listOf(prevMonth, uiState.activeMonthKey, nextMonth) + dataMonths).distinct().sorted()
    }
  val monthlyExpenseSummary =
    remember(selectedExpenseMonthKey, uiState.raw.expenses, uiState.raw.categories) {
      FinanceCalculator.computeMonthlyExpenseSummary(
        monthKey = selectedExpenseMonthKey,
        expenses = uiState.raw.expenses,
        userCategories = uiState.raw.categories,
      )
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("ringkasan_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Membedakan Saldo Aktual vs Selisih Pendapatan & Pengeluaran Bulan Berjalan
      item {
        CleanCard {
          Text(
            text = "Saldo Aktual vs Arus Kas Bulan Berjalan",
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Saldo Aktual Saat Ini",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = FinanceCalculator.formatRupiah(uiState.totalActualBalance, hide),
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Saldo awal + koreksi + arus kas",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Selisih Masuk & Keluar",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = FinanceCalculator.formatSignedRupiah(uiState.monthlyNetCashflow, hide),
                style = MaterialTheme.typography.titleLarge,
                color = if (uiState.monthlyNetCashflow >= 0L) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Bulan berjalan (tanpa transfer)",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
              )
            }
          }

          HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.75.dp,
            color = OutlineSubtle.copy(alpha = 0.55f),
          )
          SummaryPairRow(
            label = "Total Pemasukan Bulan Ini",
            value = FinanceCalculator.formatRupiah(uiState.monthlyTotalIncome, hide),
            valueColor = IncomeGreen,
          )
          SummaryPairRow(
            label = "Total Pengeluaran Bulan Ini",
            value = FinanceCalculator.formatRupiah(uiState.monthlyTotalExpense, hide),
            valueColor = ExpenseRed,
          )
          SummaryPairRow(
            label = "Transfer Internal (non-pendapatan/pengeluaran)",
            value = FinanceCalculator.formatRupiah(uiState.monthlyTotalTransfer, hide),
            valueColor = InfoBlue,
          )
        }
      }

      // Ringkasan Pengeluaran Bulanan per Kategori (GoPay + Cash, tanpa Transfer, urut terbesar -> terkecil)
      item(key = "ringkasan_pengeluaran_bulanan") {
        MonthlyExpenseSummaryCard(
          summary = monthlyExpenseSummary,
          availableMonths = availableExpenseMonths,
          hideAmounts = hide,
          onSelectMonth = { selectedExpenseMonthKey = it },
          onPrevMonth = {
            selectedExpenseMonthKey = FinanceCalculator.shiftMonthKey(selectedExpenseMonthKey, -1)
          },
          onNextMonth = {
            selectedExpenseMonthKey = FinanceCalculator.shiftMonthKey(selectedExpenseMonthKey, 1)
          },
        )
      }

      // Selisih Target Harian Bulanan (akumulasi target pada hari produktif saja)
      item {
        CleanCard(
          containerColor = EmeraldContainer.copy(alpha = 0.5f),
          borderColor = EmeraldPrimary.copy(alpha = 0.16f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Selisih Target Harian Bulanan",
              style = MaterialTheme.typography.titleSmall,
              color = OnEmeraldContainer,
              fontWeight = FontWeight.SemiBold,
            )
            StatusPill(
              text = "${uiState.monthlyProductiveDays} hari produktif",
              backgroundColor = EmeraldPrimary,
              textColor = Color.White,
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = FinanceCalculator.formatSignedRupiah(uiState.monthlyDailyTargetDiff, hide),
            style = MaterialTheme.typography.headlineSmall,
            color = if (uiState.monthlyDailyTargetDiff >= 0L) IncomeGreen else ExpenseRed,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              "Total pendapatan (${FinanceCalculator.formatRupiah(uiState.monthlyTotalIncome, hide)}) − akumulasi target harian pada ${uiState.monthlyProductiveDays} hari produktif (${FinanceCalculator.formatRupiah(uiState.monthlyDailyTargetAccumulation, hide)}).",
            style = MaterialTheme.typography.bodySmall,
            color = OnEmeraldContainer,
          )
        }
      }

      // Rincian Saldo Awal, Pemasukan, Pengeluaran, Transfer, Perubahan Saldo & Saldo Akhir tiap lokasi
      item {
        SectionHeader(
          title = "Rincian per Lokasi Dana",
          actionLabel = "Cek Transaksi",
          onActionClick = { onNavigate(AppDestination.TRANSAKSI) },
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Setiap perubahan saldo dapat ditelusuri dari pemasukan, pengeluaran, transfer, dan koreksi saldo awal pembentuknya.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
      }

      items(uiState.locationBalances, key = { it.location.id }) { detail ->
        LocationBreakdownCard(
          detail = detail,
          hideAmounts = hide,
          onTraceTransactions = { onNavigate(AppDestination.TRANSAKSI) },
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onNavigate(AppDestination.STATISTIK) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("ringkasan_to_statistik"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Buka Statistik")
          }
          OutlinedButton(
            onClick = { onNavigate(AppDestination.TAGIHAN_KEWAJIBAN) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("ringkasan_to_tagihan"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Cek Tagihan", color = EmeraldPrimary)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun LocationBreakdownCard(
  detail: LocationBalanceDetail,
  hideAmounts: Boolean,
  onTraceTransactions: () -> Unit,
) {
  CleanCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = detail.location.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
      StatusPill(
        text = FinanceCalculator.formatSignedRupiah(detail.monthlyChange, hideAmounts),
        backgroundColor = if (detail.monthlyChange >= 0L) IncomeGreenBg else ExpenseRedBg,
        textColor = if (detail.monthlyChange >= 0L) IncomeGreen else ExpenseRed,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SummaryPairRow(
      label = "Saldo Awal (+ koreksi)",
      value = FinanceCalculator.formatRupiah(detail.effectiveOpeningBalance, hideAmounts),
    )
    SummaryPairRow(
      label = "Pemasukan",
      value = "+${FinanceCalculator.formatRupiah(detail.incomeIn, hideAmounts)}",
      valueColor = IncomeGreen,
    )
    SummaryPairRow(
      label = "Pengeluaran",
      value = "-${FinanceCalculator.formatRupiah(detail.expenseOut, hideAmounts)}",
      valueColor = ExpenseRed,
    )
    SummaryPairRow(
      label = "Transfer (Masuk / Keluar)",
      value =
        "+${FinanceCalculator.formatRupiah(detail.transferIn, hideAmounts)} / -${FinanceCalculator.formatRupiah(detail.transferOut, hideAmounts)}",
      valueColor = InfoBlue,
    )
    SummaryPairRow(
      label = "Perubahan Saldo",
      value = FinanceCalculator.formatSignedRupiah(detail.monthlyChange, hideAmounts),
    )
    HorizontalDivider(
      modifier = Modifier.padding(vertical = 8.dp),
      thickness = 0.75.dp,
      color = OutlineSubtle.copy(alpha = 0.55f),
    )
    SummaryPairRow(
      label = "Saldo Akhir (Berjalan)",
      value = FinanceCalculator.formatRupiah(detail.runningBalance, hideAmounts),
      bold = true,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
      onClick = onTraceTransactions,
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
    ) {
      Text("Telusuri Transaksi Pembentuk", color = EmeraldPrimary)
    }
  }
}

@Composable
private fun SummaryPairRow(
  label: String,
  value: String,
  valueColor: Color = InkPrimary,
  bold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
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
      style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
      color = valueColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun MonthlyExpenseSummaryCard(
  summary: MonthlyExpenseSummary,
  availableMonths: List<String>,
  hideAmounts: Boolean,
  onSelectMonth: (String) -> Unit,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
) {
  CleanCard(modifier = Modifier.testTag("ringkasan_pengeluaran_bulanan_card")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Ringkasan Pengeluaran Bulanan",
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Gabungan GoPay & Cash • Transfer tidak dihitung",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
      }
      StatusPill(
        text = "${summary.categories.size} kategori",
        backgroundColor = ExpenseRedBg,
        textColor = ExpenseRed,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Navigasi pindah bulan (< Bulan Tahun >) + chip bulan cepat
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
      color = SurfaceTinted,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onPrevMonth,
          modifier = Modifier.testTag("expense_summary_prev_month"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Bulan sebelumnya",
            tint = EmeraldPrimary,
          )
        }
        Text(
          text = summary.monthLabel,
          style = MaterialTheme.typography.titleSmall,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag("expense_summary_month_label"),
        )
        IconButton(
          onClick = onNextMonth,
          modifier = Modifier.testTag("expense_summary_next_month"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Bulan berikutnya",
            tint = EmeraldPrimary,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(availableMonths, key = { it }) { mKey ->
        FilterChip(
          selected = mKey == summary.monthKey,
          onClick = { onSelectMonth(mKey) },
          label = { Text(FinanceCalculator.formatMonthIndo(mKey)) },
          modifier = Modifier.testTag("expense_summary_month_chip_$mKey"),
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = EmeraldContainer,
              selectedLabelColor = OnEmeraldContainer,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "Total Pengeluaran (${summary.monthLabel})",
      style = MaterialTheme.typography.bodySmall,
      color = InkMuted,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = FinanceCalculator.formatRupiah(summary.totalExpense, hideAmounts),
      style = MaterialTheme.typography.headlineMedium,
      color = if (summary.totalExpense > 0L) ExpenseRed else InkPrimary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.testTag("expense_summary_total_amount"),
    )

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 12.dp),
      thickness = 0.75.dp,
      color = OutlineSubtle.copy(alpha = 0.55f),
    )

    if (summary.categories.isEmpty()) {
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("expense_summary_empty_state"),
        shape = MaterialTheme.shapes.small,
        color = SurfaceTinted,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.ReceiptLong,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Belum ada pengeluaran pada ${summary.monthLabel}",
              style = MaterialTheme.typography.titleSmall,
              color = InkPrimary,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text =
                "Transaksi pengeluaran GoPay maupun Cash di bulan ini akan otomatis dirangkum di sini.",
              style = MaterialTheme.typography.bodySmall,
              color = InkSecondary,
            )
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        summary.categories.forEachIndexed { index, item ->
          val progress = (item.percentage / 100.0).toFloat().coerceIn(0f, 1f)
          Column(modifier = Modifier.fillMaxWidth().testTag("expense_summary_cat_$index")) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${index + 1}. ${item.categoryName}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = FinanceCalculator.formatRupiah(item.totalAmount, hideAmounts),
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.width(8.dp))
              StatusPill(
                text = item.percentageLabel,
                backgroundColor = ExpenseRedBg,
                textColor = ExpenseRed,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
              color = ExpenseRed,
              trackColor = SurfaceVariantSoft,
            )
          }
        }
      }
    }
  }
}
