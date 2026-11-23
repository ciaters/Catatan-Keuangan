package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AreaStatItem
import com.example.model.CategoryComparisonItem
import com.example.model.DayStatPoint
import com.example.model.FinanceCalculator
import com.example.state.FinanceUiState
import com.example.ui.components.CleanCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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
import com.example.ui.theme.SurfaceVariantSoft
import com.example.ui.theme.WarmAmber

private enum class WeeklyMetric(val label: String) {
  PENDAPATAN("Pendapatan"),
  ORDER("Order"),
  DURASI("Durasi"),
  JARAK("Jarak"),
  BATERAI("Baterai"),
}

@Composable
fun StatistikScreen(
  uiState: FinanceUiState,
  onNavigateToRingkasan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts
  var selectedTab by remember { mutableStateOf("Mingguan") } // Mingguan, Bulanan, Wilayah
  var selectedWeeklyMetric by remember { mutableStateOf(WeeklyMetric.PENDAPATAN) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Pemilih Bagian Statistik (Tanpa analisis jam tertentu)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("Mingguan", "Bulanan", "Wilayah").forEach { tab ->
            FilterChip(
              selected = selectedTab == tab,
              onClick = { selectedTab = tab },
              label = { Text(tab) },
              modifier = Modifier.testTag("statistik_tab_$tab"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldContainer,
                  selectedLabelColor = OnEmeraldContainer,
                ),
            )
          }
        }
      }

      if (selectedTab == "Mingguan") {
        // Statistik Mingguan: selalu Senin s/d Minggu (7 hari penuh, Minggu merah, hari kosong Rp0)
        item {
          CleanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Statistik Mingguan (Senin–Minggu)",
                  style = MaterialTheme.typography.titleMedium,
                  color = InkPrimary,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "16–22 Nov 2026 • Hari Minggu berwarna merah",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
              }
              StatusPill(
                text = "7 Hari",
                backgroundColor = EmeraldContainer,
                textColor = OnEmeraldContainer,
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(WeeklyMetric.entries) { metric ->
                FilterChip(
                  selected = selectedWeeklyMetric == metric,
                  onClick = { selectedWeeklyMetric = metric },
                  label = { Text(metric.label) },
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            WeeklyMondayToSundayChart(
              days = uiState.weeklyDaysMondayToSunday,
              metric = selectedWeeklyMetric,
            )
          }
        }

        // Perbandingan Antarhari Senin–Minggu (lengkap)
        item {
          SectionHeader(title = "Perbandingan Antarhari (Senin–Minggu)")
          Spacer(modifier = Modifier.height(8.dp))
          CleanCard {
            uiState.weeklyDaysMondayToSunday.forEachIndexed { idx, day ->
              DayComparisonRow(day = day, hideAmounts = hide)
              if (idx < uiState.weeklyDaysMondayToSunday.lastIndex) {
                HorizontalDivider(
                  modifier = Modifier.padding(vertical = 8.dp),
                  thickness = 0.75.dp,
                  color = OutlineSubtle.copy(alpha = 0.5f),
                )
              }
            }
          }
        }
      }

      if (selectedTab == "Bulanan") {
        // Statistik Bulanan lengkap
        item {
          CleanCard {
            Text(
              text = "Statistik Bulanan (November 2026)",
              style = MaterialTheme.typography.titleMedium,
              color = InkPrimary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatKeyValueRow(
              label = "Total Pendapatan Bulan Ini",
              value = FinanceCalculator.formatRupiah(uiState.monthlyTotalIncome, hide),
              valueColor = IncomeGreen,
            )
            StatKeyValueRow(
              label = "Target Bulanan",
              value = FinanceCalculator.formatRupiah(uiState.currentMonthlyTarget, hide),
            )
            StatKeyValueRow(
              label = "Jumlah Hari Produktif",
              value = "${uiState.monthlyProductiveDays} hari",
            )
            StatKeyValueRow(
              label = "Rata-rata per Hari Produktif",
              value = FinanceCalculator.formatRupiah(uiState.monthlyAvgIncomePerProductiveDay, hide),
              valueColor = EmeraldPrimary,
            )
            StatKeyValueRow(
              label = "Estimasi Penghasilan 26 Hari",
              value = FinanceCalculator.formatRupiah(uiState.monthlyEstimated26DaysIncome, hide),
              valueColor = InfoBlue,
            )
            HorizontalDivider(
              modifier = Modifier.padding(vertical = 10.dp),
              thickness = 0.75.dp,
              color = OutlineSubtle.copy(alpha = 0.5f),
            )
            StatKeyValueRow(
              label = "Rata-rata Dasar/Order (tanpa bonus & tip)",
              value = FinanceCalculator.formatRupiah(uiState.monthlyAvgBaseIncomePerOrder, hide),
            )
            StatKeyValueRow(
              label = "Total Jarak (Order + Blank)",
              value = FinanceCalculator.formatDistance(uiState.monthlyTotalDistanceKm),
            )
            StatKeyValueRow(
              label = "Total Baterai (Order + Blank)",
              value = "${uiState.monthlyTotalBatteryConsumed}%",
            )
            StatKeyValueRow(
              label = "Durasi Order / Durasi Blank",
              value =
                "${FinanceCalculator.formatDuration(uiState.monthlyTotalOrderDurationMinutes)} / ${FinanceCalculator.formatDuration(uiState.monthlyTotalBlankDurationMinutes)}",
            )
          }
        }

        // Tren Pendapatan Mingguan dalam 1 Bulan
        item {
          SectionHeader(
            title = "Tren Pendapatan Mingguan dalam Bulan",
            actionLabel = "Ringkasan",
            onActionClick = onNavigateToRingkasan,
          )
          Spacer(modifier = Modifier.height(8.dp))
          CleanCard {
            val maxTrend = (uiState.weeklyTrendsInMonth.maxOfOrNull { it.income } ?: 1L).coerceAtLeast(1L)
            uiState.weeklyTrendsInMonth.forEachIndexed { idx, wk ->
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "${wk.weekLabel} (${wk.dateRangeLabel}) • ${wk.orderCount} order",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                  )
                  Text(
                    text = FinanceCalculator.formatRupiah(wk.income, hide),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { (wk.income.toFloat() / maxTrend.toFloat()).coerceIn(0f, 1f) },
                  modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                  color = EmeraldPrimary,
                  trackColor = SurfaceVariantSoft,
                )
              }
              if (idx < uiState.weeklyTrendsInMonth.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
              }
            }
          }
        }

        // Perbandingan Kategori Pengeluaran vs Bulan Sebelumnya (Rp & %, '-' jika bulan lalu 0)
        item {
          SectionHeader(title = "Perbandingan Pengeluaran vs Oktober 2026")
          Spacer(modifier = Modifier.height(8.dp))
          CleanCard {
            uiState.categoryComparisons.forEachIndexed { idx, item ->
              CategoryComparisonRow(item = item, hideAmounts = hide)
              if (idx < uiState.categoryComparisons.lastIndex) {
                HorizontalDivider(
                  modifier = Modifier.padding(vertical = 10.dp),
                  thickness = 0.75.dp,
                  color = OutlineSubtle.copy(alpha = 0.5f),
                )
              }
            }
          }
        }
      }

      if (selectedTab == "Wilayah") {
        item {
          SectionHeader(title = "Analisis Kinerja per Wilayah")
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Mencakup jumlah order, pendapatan, rata-rata/order, waktu tunggu (Blank), jarak, dan konsumsi baterai.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
        }
        items(uiState.areaStats, key = { it.areaName }) { stat ->
          AreaAnalysisCard(stat = stat, hideAmounts = hide)
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun WeeklyMondayToSundayChart(
  days: List<DayStatPoint>,
  metric: WeeklyMetric,
) {
  val values =
    days.map { day ->
      when (metric) {
        WeeklyMetric.PENDAPATAN -> day.income.toFloat()
        WeeklyMetric.ORDER -> day.orderCount.toFloat()
        WeeklyMetric.DURASI -> day.durationMinutes.toFloat()
        WeeklyMetric.JARAK -> day.distanceKm.toFloat()
        WeeklyMetric.BATERAI -> day.batteryConsumed.toFloat()
      }
    }
  val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

  Column(modifier = Modifier.fillMaxWidth()) {
    Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
      val slotW = size.width / days.size.coerceAtLeast(1)
      val barW = (slotW * 0.48f).coerceAtMost(36f)
      val chartH = size.height - 6f

      drawLine(
        color = OutlineSubtle,
        start = Offset(0f, chartH),
        end = Offset(size.width, chartH),
        strokeWidth = 1.5f,
      )

      days.forEachIndexed { index, day ->
        val ratio = (values[index] / maxVal).coerceIn(0f, 1f)
        val barH = if (values[index] > 0f) (ratio * (chartH - 8f)).coerceAtLeast(8f) else 3f
        val left = slotW * index + (slotW - barW) / 2f
        val barColor =
          when {
            day.isSunday -> ExpenseRed
            values[index] == 0f -> OutlineSubtle
            metric == WeeklyMetric.BATERAI -> WarmAmber
            else -> EmeraldPrimary
          }
        drawRoundRect(
          color = barColor,
          topLeft = Offset(left, chartH - barH),
          size = Size(barW, barH),
          cornerRadius = CornerRadius(6f, 6f),
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
      days.forEach { day ->
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = day.dayShortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isSunday) ExpenseRed else InkSecondary,
            fontWeight = if (day.isSunday) FontWeight.Bold else FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun DayComparisonRow(
  day: DayStatPoint,
  hideAmounts: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "${day.dayFullLabel} (${FinanceCalculator.formatDateIndo(day.date)})",
        style = MaterialTheme.typography.titleSmall,
        color = if (day.isSunday) ExpenseRed else InkPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text =
          "${day.orderCount} order • ${FinanceCalculator.formatDuration(day.durationMinutes)} • ${FinanceCalculator.formatDistance(day.distanceKm)} • ${day.batteryConsumed}%",
        style = MaterialTheme.typography.bodySmall,
        color = InkMuted,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = FinanceCalculator.formatRupiah(day.income, hideAmounts),
      style = MaterialTheme.typography.titleSmall,
      color =
        when {
          day.isSunday -> ExpenseRed
          day.income > 0L -> IncomeGreen
          else -> InkMuted
        },
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun StatKeyValueRow(
  label: String,
  value: String,
  valueColor: Color = InkPrimary,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = InkSecondary,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      color = valueColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun CategoryComparisonRow(
  item: CategoryComparisonItem,
  hideAmounts: Boolean,
) {
  val diffPositive = item.diffRupiah > 0L
  val pillBg =
    when {
      item.diffPercentLabel == "-" -> EmeraldContainer
      diffPositive -> ExpenseRedBg
      else -> IncomeGreenBg
    }
  val pillText =
    when {
      item.diffPercentLabel == "-" -> OnEmeraldContainer
      diffPositive -> ExpenseRed
      else -> IncomeGreen
    }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.categoryName,
        style = MaterialTheme.typography.titleSmall,
        color = InkPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text =
          "Nov: ${FinanceCalculator.formatRupiah(item.currentAmount, hideAmounts)} • Okt: ${FinanceCalculator.formatRupiah(item.previousAmount, hideAmounts)}",
        style = MaterialTheme.typography.bodySmall,
        color = InkMuted,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = FinanceCalculator.formatSignedRupiah(item.diffRupiah, hideAmounts),
        style = MaterialTheme.typography.labelLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      StatusPill(
        text = item.diffPercentLabel,
        backgroundColor = pillBg,
        textColor = pillText,
      )
    }
  }
}

@Composable
private fun AreaAnalysisCard(
  stat: AreaStatItem,
  hideAmounts: Boolean,
) {
  CleanCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stat.areaName,
        style = MaterialTheme.typography.titleMedium,
        color = InkPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      StatusPill(
        text = "${stat.orderCount} order",
        backgroundColor = EmeraldContainer,
        textColor = OnEmeraldContainer,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    StatKeyValueRow(
      label = "Total Pendapatan",
      value = FinanceCalculator.formatRupiah(stat.totalIncome, hideAmounts),
      valueColor = IncomeGreen,
    )
    StatKeyValueRow(
      label = "Rata-rata Pendapatan / Order",
      value = FinanceCalculator.formatRupiah(stat.avgIncomePerOrder, hideAmounts),
    )
    StatKeyValueRow(
      label = "Waktu Tunggu (Blank)",
      value = FinanceCalculator.formatDuration(stat.waitBlankMinutes),
    )
    StatKeyValueRow(
      label = "Jarak Tempuh (Order + Blank)",
      value = FinanceCalculator.formatDistance(stat.distanceKm),
    )
    StatKeyValueRow(
      label = "Pemakaian Baterai",
      value = "${stat.batteryConsumed}%",
    )
  }
}
