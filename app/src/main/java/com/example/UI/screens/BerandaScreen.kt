package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FinanceCalculator
import com.example.state.FinanceUiState
import com.example.ui.components.CleanCard
import com.example.ui.components.IconBadge
import com.example.ui.components.SampleDataBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer
import kotlin.math.roundToInt

@Composable
fun BerandaScreen(
  uiState: FinanceUiState,
  onToggleHideAmounts: () -> Unit,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts
  val currentMonthKey = remember(uiState.todayDate) { uiState.todayDate.take(7) }
  var selectedMonthKey by remember(currentMonthKey) { mutableStateOf(currentMonthKey) }
  val selectedMonthSummary =
    remember(selectedMonthKey, uiState.dailySummariesByDate, uiState.raw.targetHistory) {
      FinanceCalculator.computeMonthlyIncomeSummary(
        monthKey = selectedMonthKey,
        dailySummariesByDate = uiState.dailySummariesByDate,
        targetHistory = uiState.raw.targetHistory,
      )
    }

  val today = uiState.todaySummary
  val target = uiState.todayDailyTarget.coerceAtLeast(1L)
  val progressFraction = (today.totalIncome.toFloat() / target.toFloat()).coerceIn(0f, 1f)
  val percentInt = ((today.totalIncome.toDouble() / target.toDouble()) * 100.0).roundToInt()
  val diff = today.totalIncome - target
  val diffText =
    when {
      diff >= 0 -> "Melebihi target ${FinanceCalculator.formatSignedRupiah(diff, hide)}"
      else -> "Kurang ${FinanceCalculator.formatRupiah(-diff, hide)} dari target"
    }

  val quickShortcuts =
    listOf(
      AppDestination.INCOME,
      AppDestination.TRANSAKSI,
      AppDestination.STATISTIK,
      AppDestination.POSISI_SALDO,
      AppDestination.RINGKASAN_KEUANGAN,
      AppDestination.TAGIHAN_KEWAJIBAN,
      AppDestination.PENGATURAN,
    )

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("beranda_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // 1. Kartu Utama Ringkasan Penghasilan Bulanan + Navigasi Bulan (paling atas Beranda)
      item(key = "ringkasan_penghasilan_bulanan") {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.large)
              .background(
                brush = Brush.linearGradient(listOf(EmeraldPrimary, EmeraldPrimaryDark))
              )
              .padding(18.dp)
              .testTag("beranda_monthly_hero_card")
        ) {
          Column {
            // Baris Judul + Navigasi Bulan + Tombol Sembunyikan Nominal
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Ringkasan Penghasilan Bulanan",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color.White.copy(alpha = 0.92f),
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text =
                    if (selectedMonthKey == currentMonthKey) "Bulan Berjalan"
                    else "Riwayat Bulan Terpilih",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.74f),
                )
              }
              IconButton(
                onClick = onToggleHideAmounts,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("beranda_toggle_saldo"),
              ) {
                Icon(
                  imageVector = if (hide) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = stringResource(R.string.cd_visibilitas_saldo),
                  tint = Color.White,
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigasi Bulan (< Nama Bulan Tahun >)
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = Color.White.copy(alpha = 0.14f),
              modifier = Modifier.fillMaxWidth().testTag("beranda_month_navigator"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = {
                    selectedMonthKey = FinanceCalculator.shiftMonthKey(selectedMonthKey, -1)
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("beranda_prev_month"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Bulan sebelumnya",
                    tint = Color.White,
                  )
                }

                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = selectedMonthSummary.monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("beranda_selected_month_label"),
                  )
                  if (selectedMonthKey != currentMonthKey) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Kembali ke bulan ini",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.9f),
                      fontWeight = FontWeight.SemiBold,
                      modifier =
                        Modifier
                          .clip(RoundedCornerShape(999.dp))
                          .clickable { selectedMonthKey = currentMonthKey }
                          .padding(horizontal = 8.dp, vertical = 2.dp)
                          .testTag("beranda_jump_current_month"),
                    )
                  }
                }

                IconButton(
                  onClick = {
                    selectedMonthKey = FinanceCalculator.shiftMonthKey(selectedMonthKey, 1)
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("beranda_next_month"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Bulan berikutnya",
                    tint = Color.White,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Penghasilan Bulan Terpilih
            Text(
              text = "Total Penghasilan ${selectedMonthSummary.monthLabel}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.82f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = FinanceCalculator.formatRupiah(selectedMonthSummary.totalIncome, hide),
              style = MaterialTheme.typography.displaySmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("beranda_monthly_total_income"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progres Target Bulanan + Progress Bar + Sisa Target Bulanan
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  buildAnnotatedString {
                    append("Target bulanan: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                      append(FinanceCalculator.formatRupiah(selectedMonthSummary.monthlyTarget, hide))
                    }
                  },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
              )
              Text(
                text = "${selectedMonthSummary.targetProgressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("beranda_monthly_progress_percent"),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { selectedMonthSummary.targetProgressFraction },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(9.dp)
                  .clip(RoundedCornerShape(999.dp))
                  .testTag("beranda_monthly_progress_bar"),
              color = Color.White,
              trackColor = Color.White.copy(alpha = 0.24f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            val remainingTarget = selectedMonthSummary.remainingTarget
            Text(
              text =
                if (remainingTarget > 0L) {
                  "Sisa target bulanan: ${FinanceCalculator.formatRupiah(remainingTarget, hide)}"
                } else {
                  "Target bulanan tercapai! Surplus ${FinanceCalculator.formatRupiah(-remainingTarget, hide)}"
                },
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.94f),
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag("beranda_monthly_remaining_top"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rincian Argo, Bonus, Tip GoPay, Tip Cash bulan terpilih
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              HeroMonthlyPill(
                label = "Argo",
                value = FinanceCalculator.formatRupiah(selectedMonthSummary.totalArgo, hide),
                modifier = Modifier.weight(1f).testTag("beranda_monthly_argo"),
              )
              HeroMonthlyPill(
                label = "Bonus",
                value = FinanceCalculator.formatRupiah(selectedMonthSummary.totalBonus, hide),
                modifier = Modifier.weight(1f).testTag("beranda_monthly_bonus"),
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              HeroMonthlyPill(
                label = "Tip GoPay",
                value = FinanceCalculator.formatRupiah(selectedMonthSummary.totalTipGoPay, hide),
                modifier = Modifier.weight(1f).testTag("beranda_monthly_tip_gopay"),
              )
              HeroMonthlyPill(
                label = "Tip Cash",
                value = FinanceCalculator.formatRupiah(selectedMonthSummary.totalTipCash, hide),
                modifier = Modifier.weight(1f).testTag("beranda_monthly_tip_cash"),
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              HeroMonthlyPill(
                label = "Jumlah Hari Kerja",
                value = "${selectedMonthSummary.productiveDays} hari",
                modifier = Modifier.weight(1f).testTag("beranda_monthly_days_top"),
              )
              HeroMonthlyPill(
                label = "Rata-rata / Hari",
                value =
                  FinanceCalculator.formatRupiah(
                    selectedMonthSummary.avgIncomePerProductiveDay,
                    hide,
                  ),
                modifier = Modifier.weight(1f).testTag("beranda_monthly_avg_top"),
              )
            }
          }
        }
      }

      // 2. Indikator Pendukung Ringkasan Bulanan (5 indikator berada di bawah kartu utama)
      item(key = "ringkasan_bulanan") {
        SectionHeader(
          title = "Ringkasan Bulanan Lainnya",
          actionLabel = "Statistik",
          onActionClick = { onNavigate(AppDestination.STATISTIK) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        CleanCard(modifier = Modifier.testTag("beranda_ringkasan_bulanan")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = selectedMonthSummary.monthLabel,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  buildAnnotatedString {
                    append("Total bulan ini: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = InkPrimary)) {
                      append(FinanceCalculator.formatRupiah(selectedMonthSummary.totalIncome, hide))
                    }
                  },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
            StatusPill(
              text =
                if (selectedMonthKey == currentMonthKey) "Bulan Berjalan"
                else selectedMonthSummary.monthLabel,
              backgroundColor = EmeraldContainer,
              textColor = EmeraldPrimary,
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Baris 1: Rata-rata per hari kerja & Total hari kerja
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MonthlySummaryTile(
              label = "Rata-rata / Hari Kerja",
              value =
                FinanceCalculator.formatRupiah(
                  selectedMonthSummary.avgIncomePerProductiveDay,
                  hide,
                ),
              subtitle = "Hari produktif saja",
              valueColor = EmeraldPrimary,
              modifier = Modifier.weight(1f).testTag("monthly_avg_per_day"),
            )
            MonthlySummaryTile(
              label = "Total Hari Kerja",
              value = "${selectedMonthSummary.productiveDays} hari",
              subtitle = "Hari produktif berorder",
              valueColor = InkPrimary,
              modifier = Modifier.weight(1f).testTag("monthly_productive_days"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Baris 2: Estimasi penghasilan 26 hari & Selisih dari target harian kumulatif
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MonthlySummaryTile(
              label = "Estimasi Sebulan (×26)",
              value =
                FinanceCalculator.formatRupiah(selectedMonthSummary.estimated26DaysIncome, hide),
              subtitle = "Rata-rata × 26 hari",
              valueColor = InfoBlue,
              modifier = Modifier.weight(1f).testTag("monthly_estimated_26"),
            )
            val dailyDiffPositive = selectedMonthSummary.dailyTargetDiff >= 0L
            MonthlySummaryTile(
              label = "Selisih Target Harian",
              value =
                FinanceCalculator.formatSignedRupiah(selectedMonthSummary.dailyTargetDiff, hide),
              subtitle = "vs akumulasi target harian",
              valueColor = if (dailyDiffPositive) IncomeGreen else ExpenseRed,
              modifier = Modifier.weight(1f).testTag("monthly_daily_target_diff"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Baris 3: Sisa target minimum bulanan
          val remainingMonthly = selectedMonthSummary.remainingTarget
          val monthlyTargetMet = remainingMonthly <= 0L
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("monthly_remaining_target"),
            shape = MaterialTheme.shapes.small,
            color =
              if (monthlyTargetMet) IncomeGreenBg.copy(alpha = 0.7f)
              else WarmAmberContainer.copy(alpha = 0.7f),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Sisa Target Minimum Bulanan",
                  style = MaterialTheme.typography.labelMedium,
                  color = InkPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text =
                    "Target ${FinanceCalculator.formatRupiah(selectedMonthSummary.monthlyTarget, hide)} − pendapatan bulan ini",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = FinanceCalculator.formatRupiah(remainingMonthly, hide),
                style = MaterialTheme.typography.titleMedium,
                color = if (monthlyTargetMet) IncomeGreen else WarmAmber,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // 3. Kartu Penghasilan Hari Ini (terpisah di bawah Ringkasan Bulanan)
      item(key = "pendapatan_hari_ini") {
        CleanCard(modifier = Modifier.testTag("beranda_daily_income_card")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Pendapatan Hari Ini",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = FinanceCalculator.formatDateWithDayIndo(uiState.todayDate),
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = EmeraldContainer,
              modifier =
                Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable { onNavigate(AppDestination.INCOME) }
                  .testTag("beranda_to_income_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Detail Order",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = EmeraldPrimary,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = FinanceCalculator.formatRupiah(today.totalIncome, hide),
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary,
            fontWeight = FontWeight.Bold,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                buildAnnotatedString {
                  append("Target harian: ")
                  withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = InkPrimary)) {
                    append(FinanceCalculator.formatRupiah(target, hide))
                  }
                },
              style = MaterialTheme.typography.labelMedium,
              color = InkSecondary,
            )
            Text(
              text = "$percentInt%",
              style = MaterialTheme.typography.labelLarge,
              color = if (percentInt >= 100) IncomeGreen else EmeraldPrimary,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = if (percentInt >= 100) IncomeGreen else EmeraldPrimary,
            trackColor = EmeraldContainer.copy(alpha = 0.7f),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = diffText,
            style = MaterialTheme.typography.labelMedium,
            color = if (diff >= 0L) IncomeGreen else InkSecondary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      item { SampleDataBanner() }

      // Metrik Aktivitas Hari Ini
      item(key = "aktivitas_hari_ini") {
        SectionHeader(
          title = "Aktivitas Kerja Hari Ini",
          actionLabel = "Income",
          onActionClick = { onNavigate(AppDestination.INCOME) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ActivityMetricBox(
              icon = Icons.Outlined.LocalShipping,
              label = "Jumlah Order",
              value = "${today.orderCount} order",
              subtitle = "${today.totalPackages} paket terkumpul",
              tint = EmeraldPrimary,
              bg = EmeraldContainer,
              modifier = Modifier.weight(1f),
            )
            ActivityMetricBox(
              icon = Icons.Outlined.Timer,
              label = "Durasi Order",
              value = FinanceCalculator.formatDuration(today.orderDurationMinutes),
              subtitle = "Waktu produktif order",
              tint = IncomeGreen,
              bg = IncomeGreenBg,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ActivityMetricBox(
              icon = Icons.Outlined.HourglassEmpty,
              label = "Durasi Blank",
              value = FinanceCalculator.formatDuration(today.blankDurationMinutes),
              subtitle = "Waktu tunggu di antara order",
              tint = WarmAmber,
              bg = WarmAmberContainer,
              modifier = Modifier.weight(1f),
            )
            ActivityMetricBox(
              icon = Icons.Outlined.AccessTime,
              label = "Total Waktu Aktivitas",
              value = FinanceCalculator.formatDuration(today.totalActivityDurationMinutes),
              subtitle = "Order + Blank",
              tint = InfoBlue,
              bg = InfoBlueBg,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ActivityMetricBox(
              icon = Icons.Outlined.Route,
              label = "Jarak Tempuh",
              value = FinanceCalculator.formatDistance(today.totalDistanceKm),
              subtitle = "Order + Blank",
              tint = EmeraldPrimary,
              bg = EmeraldContainer,
              modifier = Modifier.weight(1f),
            )
            ActivityMetricBox(
              icon = Icons.Outlined.BatteryChargingFull,
              label = "Pemakaian Baterai",
              value = "${today.totalBatteryConsumed}%",
              subtitle = "Motor listrik hari ini",
              tint = ExpenseRed,
              bg = ExpenseRedBg,
              modifier = Modifier.weight(1f),
            )
          }
          val dailyBatteryEfficiency =
            FinanceCalculator.formatBatteryEfficiency(
              today.totalDistanceKm,
              today.totalBatteryConsumed,
            )
          ActivityMetricBox(
            icon = Icons.Outlined.Bolt,
            label = "Efisiensi Baterai Harian",
            value = dailyBatteryEfficiency,
            subtitle =
              if (today.totalBatteryConsumed > 0) {
                "${FinanceCalculator.formatDistance(today.totalDistanceKm)} ÷ ${today.totalBatteryConsumed}% (Order + Blank)"
              } else {
                "Belum ada pemakaian baterai"
              },
            tint = EmeraldPrimary,
            bg = EmeraldContainer,
            modifier = Modifier.fillMaxWidth().testTag("beranda_efisiensi_baterai"),
          )
        }
      }

      // Metrik Aktivitas Hari Ini berada di bawah Ringkasan Bulanan & Pendapatan Hari Ini

      // Ringkasan 4 Lokasi Dana & Target Bulanan
      item {
        SectionHeader(
          title = "4 Lokasi Dana",
          actionLabel = "Posisi Saldo",
          onActionClick = { onNavigate(AppDestination.POSISI_SALDO) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        CleanCard(
          onClick = { onNavigate(AppDestination.POSISI_SALDO) },
          modifier = Modifier.testTag("beranda_card_saldo"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconBadge(
                icon = Icons.Outlined.AccountBalanceWallet,
                containerColor = EmeraldContainer,
                iconColor = EmeraldPrimary,
                size = 40,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Total Seluruh Saldo Aktual",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
                Text(
                  text = FinanceCalculator.formatRupiah(uiState.totalActualBalance, hide),
                  style = MaterialTheme.typography.headlineSmall,
                  color = InkPrimary,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            StatusPill(
              text = "4 Lokasi",
              backgroundColor = EmeraldContainer,
              textColor = EmeraldPrimary,
            )
          }
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.75.dp,
            color = OutlineSubtle.copy(alpha = 0.55f),
          )
          uiState.locationBalances.forEachIndexed { idx, detail ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = detail.location.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
              )
              Text(
                text = FinanceCalculator.formatRupiah(detail.runningBalance, hide),
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            if (idx < uiState.locationBalances.lastIndex) {
              Spacer(modifier = Modifier.height(2.dp))
            }
          }
        }
      }

      // Status Tagihan Bulan Ini
      item {
        SectionHeader(
          title = "Tagihan & Kewajiban",
          actionLabel = "Lihat semua",
          onActionClick = { onNavigate(AppDestination.TAGIHAN_KEWAJIBAN) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        CleanCard(
          onClick = { onNavigate(AppDestination.TAGIHAN_KEWAJIBAN) },
          modifier = Modifier.testTag("beranda_card_tagihan"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.NotificationsActive,
              containerColor = WarmAmberContainer,
              iconColor = WarmAmber,
              size = 42,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tagihan Belum Dibayar & Terlambat",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Tidak mengurangi saldo sampai dibayar via GoPay / Cash",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                FinanceCalculator.formatRupiah(
                  uiState.unpaidBillsTotal + uiState.overdueBillsTotal,
                  hide,
                ),
              style = MaterialTheme.typography.titleSmall,
              color = WarmAmber,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // Pintasan Halaman
      item {
        SectionHeader(
          title = "Menu Halaman",
          actionLabel = "Ringkasan",
          onActionClick = { onNavigate(AppDestination.RINGKASAN_KEUANGAN) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          quickShortcuts.chunked(2).forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              rowItems.forEach { dest ->
                ShortcutMenuTile(
                  destination = dest,
                  onClick = { onNavigate(dest) },
                  modifier = Modifier.weight(1f),
                )
              }
              if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun ActivityMetricBox(
  icon: ImageVector,
  label: String,
  value: String,
  subtitle: String,
  tint: Color,
  bg: Color,
  modifier: Modifier = Modifier,
) {
  CleanCard(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(icon = icon, containerColor = bg, iconColor = tint, size = 32)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = InkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = InkPrimary,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ShortcutMenuTile(
  destination: AppDestination,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("beranda_shortcut_${destination.route}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = destination.selectedIcon,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(19.dp),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = destination.label,
          style = MaterialTheme.typography.labelLarge,
          color = InkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Buka halaman",
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun MonthlySummaryTile(
  label: String,
  value: String,
  subtitle: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = SurfaceTinted,
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = InkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = valueColor,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HeroMonthlyPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.semantics(mergeDescendants = true) {},
    shape = MaterialTheme.shapes.small,
    color = Color.White.copy(alpha = 0.14f),
  ) {
    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.8f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
