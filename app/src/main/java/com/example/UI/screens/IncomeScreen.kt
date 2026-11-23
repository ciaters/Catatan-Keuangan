package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.model.ComputedBlankSession
import com.example.model.ComputedWorkOrder
import com.example.model.FinanceCalculator
import com.example.model.IncomeActivityItem
import com.example.model.WorkOrder
import com.example.state.FinanceUiState
import com.example.state.FinanceViewModel
import com.example.ui.components.CleanCard
import com.example.ui.components.IconBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.BlankCardBg
import com.example.ui.theme.BlankCardBorder
import com.example.ui.theme.BlankInkPrimary
import com.example.ui.theme.BlankInkSecondary
import com.example.ui.theme.BlankPillBg
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OrderCardBg
import com.example.ui.theme.OrderCardBorder
import com.example.ui.theme.OrderInnerSurface
import com.example.ui.theme.OrderProgressTrack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted
import com.example.ui.theme.SurfaceVariantSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
  uiState: FinanceUiState,
  viewModel: FinanceViewModel,
  onNavigateToRingkasan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts

  LaunchedEffect(Unit) {
    viewModel.refreshTodayDate()
  }

  // Default selalu tanggal hari ini; otomatis mengikuti tanggal baru bila hari berganti
  var selectedDate by remember(uiState.todayDate) { mutableStateOf(uiState.todayDate) }
  var showDatePicker by remember { mutableStateOf(false) }

  var searchQuery by remember { mutableStateOf("") }
  var activityFilter by remember { mutableStateOf("Semua") } // Semua, Order, Blank
  var originFilter by remember { mutableStateOf("Semua") }
  var destFilter by remember { mutableStateOf("Semua") }
  var distanceFilter by remember { mutableStateOf("Semua") } // Semua, < 8 km, 8-12 km, > 12 km
  var durationFilter by remember { mutableStateOf("Semua") } // Semua, <= 45 mnt, 46-90 mnt, > 90 mnt
  var batteryFilter by remember { mutableStateOf("Semua") } // Semua, Hemat (<=15%), Sedang (16-25%), Tinggi (>25%)
  var showAdvancedFilters by remember { mutableStateOf(false) }

  var showAddOrderDialog by remember { mutableStateOf(false) }
  var editingOrder by remember { mutableStateOf<WorkOrder?>(null) }
  var showAddBlankDialog by remember { mutableStateOf(false) }

  val areaOptions = remember(uiState.raw.areas) { listOf("Semua") + uiState.raw.areas }

  val entriesOnSelectedDate =
    remember(uiState.activityTimelineByDate, selectedDate) {
      uiState.activityTimelineByDate[selectedDate].orEmpty()
    }

  val selectedDailySummary =
    remember(uiState.dailySummariesByDate, selectedDate) {
      uiState.dailySummariesByDate[selectedDate]
        ?: FinanceCalculator.computeDailySummary(selectedDate, emptyList(), emptyList())
    }

  val selectedDailyTarget =
    remember(uiState.raw.targetHistory, selectedDate) {
      FinanceCalculator.dailyTargetForDate(selectedDate, uiState.raw.targetHistory).coerceAtLeast(1L)
    }

  val dailyTargetFraction =
    (selectedDailySummary.totalIncome.toFloat() / selectedDailyTarget.toFloat()).coerceIn(0f, 1f)
  val dailyTargetPercent =
    ((selectedDailySummary.totalIncome.toDouble() / selectedDailyTarget.toDouble()) * 100.0)
      .roundToInt()

  val filteredEntries =
    remember(
      entriesOnSelectedDate,
      searchQuery,
      activityFilter,
      originFilter,
      destFilter,
      distanceFilter,
      durationFilter,
      batteryFilter,
    ) {
      entriesOnSelectedDate.filter { entry ->
        val typeOk =
          when (activityFilter) {
            "Order" -> entry is IncomeActivityItem.OrderEntry
            "Blank" -> entry is IncomeActivityItem.BlankEntry
            else -> true
          }
        if (!typeOk) return@filter false

        when (entry) {
          is IncomeActivityItem.OrderEntry -> {
            val o = entry.data.order
            val dist = entry.data.totalDistanceKm
            val dur = entry.data.durationMinutes
            val batt = entry.data.batteryConsumed
            val qOk =
              searchQuery.isBlank() ||
                o.originArea.contains(searchQuery, ignoreCase = true) ||
                o.destinationArea.contains(searchQuery, ignoreCase = true) ||
                o.note.contains(searchQuery, ignoreCase = true) ||
                o.date.contains(searchQuery, ignoreCase = true)
            val origOk = originFilter == "Semua" || o.originArea.equals(originFilter, true)
            val destOk = destFilter == "Semua" || o.destinationArea.equals(destFilter, true)
            val distOk =
              when (distanceFilter) {
                "< 8 km" -> dist < 8.0
                "8–12 km" -> dist in 8.0..12.0
                "> 12 km" -> dist > 12.0
                else -> true
              }
            val durOk =
              when (durationFilter) {
                "≤ 45 mnt" -> dur <= 45
                "46–90 mnt" -> dur in 46..90
                "> 90 mnt" -> dur > 90
                else -> true
              }
            val battOk =
              when (batteryFilter) {
                "Hemat (≤15%)" -> batt <= 15
                "Sedang (16–25%)" -> batt in 16..25
                "Tinggi (>25%)" -> batt > 25
                else -> true
              }
            qOk && origOk && destOk && distOk && durOk && battOk
          }
          is IncomeActivityItem.BlankEntry -> {
            val b = entry.data.blank
            val dist = b.distanceKm
            val dur = entry.data.durationMinutes
            val batt = entry.data.batteryConsumed
            val qOk =
              searchQuery.isBlank() ||
                b.area.contains(searchQuery, ignoreCase = true) ||
                b.note.contains(searchQuery, ignoreCase = true) ||
                b.date.contains(searchQuery, ignoreCase = true)
            val origOk = originFilter == "Semua" || b.area.equals(originFilter, true)
            val destOk = destFilter == "Semua"
            val distOk =
              when (distanceFilter) {
                "< 8 km" -> dist < 8.0
                "8–12 km" -> dist in 8.0..12.0
                "> 12 km" -> dist > 12.0
                else -> true
              }
            val durOk =
              when (durationFilter) {
                "≤ 45 mnt" -> dur <= 45
                "46–90 mnt" -> dur in 46..90
                "> 90 mnt" -> dur > 90
                else -> true
              }
            val battOk =
              when (batteryFilter) {
                "Hemat (≤15%)" -> batt <= 15
                "Sedang (16–25%)" -> batt in 16..25
                "Tinggi (>25%)" -> batt > 25
                else -> true
              }
            qOk && origOk && destOk && distOk && durOk && battOk
          }
        }
      }
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
          modifier = Modifier.testTag("income_date_picker_confirm"),
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

  if (showAddOrderDialog || editingOrder != null) {
    OrderFormDialog(
      defaultDate = selectedDate,
      existingOrder = editingOrder,
      availableAreas = uiState.raw.areas,
      onDismiss = {
        showAddOrderDialog = false
        editingOrder = null
      },
      onSave = {
        date,
        startTime,
        endTime,
        argo,
        pkgCount,
        tipGoPay,
        tipCash,
        pickupKm,
        deliveryKm,
        origin,
        dest,
        batStart,
        batEnd,
        note ->
        val existing = editingOrder
        if (existing == null) {
          viewModel.addOrder(
            date,
            startTime,
            endTime,
            argo,
            pkgCount,
            tipGoPay,
            tipCash,
            pickupKm,
            deliveryKm,
            origin,
            dest,
            batStart,
            batEnd,
            note,
          )
        } else {
          viewModel.updateOrder(
            existing.copy(
              date = date,
              startTime = startTime,
              endTime = endTime,
              argo = argo,
              packageCount = pkgCount,
              tipGoPay = tipGoPay,
              tipCash = tipCash,
              pickupDistanceKm = pickupKm,
              deliveryDistanceKm = deliveryKm,
              originArea = origin,
              destinationArea = dest,
              batteryStart = batStart,
              batteryEnd = batEnd,
              note = note,
            )
          )
        }
        showAddOrderDialog = false
        editingOrder = null
      },
    )
  }

  if (showAddBlankDialog) {
    BlankFormDialog(
      defaultDate = selectedDate,
      orders = uiState.raw.orders,
      availableAreas = uiState.raw.areas,
      onDismiss = { showAddBlankDialog = false },
      onSave = { date, startTime, nextOrderId, area, batStart, batEnd, distKm, note ->
        viewModel.addBlank(date, startTime, nextOrderId, area, batStart, batEnd, distKm, note)
        showAddBlankDialog = false
      },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("income_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Navigasi Tanggal (< Tanggal > + Pemilih Tanggal) — konsisten dengan Transaksi
      item {
        CleanCard(modifier = Modifier.testTag("income_date_navigator")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { selectedDate = FinanceCalculator.shiftIsoDate(selectedDate, -1) },
              modifier = Modifier.testTag("income_prev_date"),
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
                  .testTag("income_date_picker_button"),
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
                modifier = Modifier.testTag("income_selected_date_label"),
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
              modifier = Modifier.testTag("income_next_date"),
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
                    .testTag("income_jump_today"),
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

      // Ringkasan Penghasilan Harian (mengikuti tanggal yang sedang dipilih)
      item {
        CleanCard(modifier = Modifier.testTag("income_daily_summary_card")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ringkasan Penghasilan Harian",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = FinanceCalculator.formatRupiah(selectedDailySummary.totalIncome, hide),
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("income_daily_total_value"),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
              text = "${selectedDailySummary.orderCount} Order",
              backgroundColor =
                if (selectedDailySummary.orderCount > 0) EmeraldContainer else SurfaceVariantSoft,
              textColor =
                if (selectedDailySummary.orderCount > 0) OnEmeraldContainer else InkSecondary,
              modifier = Modifier.testTag("income_daily_order_count"),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Progres Target Harian + Progress bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                "Progres Target Harian (${FinanceCalculator.formatRupiah(selectedDailyTarget, hide)})",
              style = MaterialTheme.typography.labelMedium,
              color = InkSecondary,
            )
            Text(
              text = "$dailyTargetPercent%",
              style = MaterialTheme.typography.labelLarge,
              color = if (dailyTargetPercent >= 100) IncomeGreen else EmeraldPrimary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("income_daily_target_percent"),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { dailyTargetFraction },
            modifier =
              Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .testTag("income_daily_progress_bar"),
            color = if (dailyTargetPercent >= 100) IncomeGreen else EmeraldPrimary,
            trackColor = EmeraldContainer.copy(alpha = 0.7f),
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MiniBreakdownPill(
              label = "Argo (→GoPay)",
              value = FinanceCalculator.formatRupiah(selectedDailySummary.totalArgo, hide),
              modifier = Modifier.weight(1f).testTag("income_daily_argo_value"),
            )
            MiniBreakdownPill(
              label = "Bonus",
              value = FinanceCalculator.formatRupiah(selectedDailySummary.totalBonus, hide),
              modifier = Modifier.weight(1f).testTag("income_daily_bonus_value"),
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MiniBreakdownPill(
              label = "Tip GoPay (→GoPay)",
              value = FinanceCalculator.formatRupiah(selectedDailySummary.totalTipGoPay, hide),
              modifier = Modifier.weight(1f).testTag("income_daily_tip_gopay_value"),
            )
            MiniBreakdownPill(
              label = "Tip Cash (→Cash)",
              value = FinanceCalculator.formatRupiah(selectedDailySummary.totalTipCash, hide),
              modifier = Modifier.weight(1f).testTag("income_daily_tip_cash_value"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text =
              if (selectedDailySummary.orderCount == 0) {
                "Belum ada order selesai pada ${FinanceCalculator.formatDateIndo(selectedDate)}."
              } else {
                "${selectedDailySummary.totalPackages} paket selesai • Bonus harian mengikuti akumulasi paket terakhir pada tanggal ini."
              },
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
          )
        }
      }

      // Tombol Aksi Order & Blank
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { showAddOrderDialog = true },
            modifier = Modifier.weight(1f).height(46.dp).testTag("income_add_order_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
              ),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Catat Order")
          }

          OutlinedButton(
            onClick = { showAddBlankDialog = true },
            modifier = Modifier.weight(1f).height(46.dp).testTag("income_add_blank_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              Icons.Outlined.HourglassEmpty,
              contentDescription = null,
              tint = WarmAmber,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Catat Blank", color = InkPrimary)
          }
        }
      }

      // Pencarian & Filter
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari wilayah, catatan, atau tanggal...") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showAdvancedFilters = !showAdvancedFilters },
              modifier = Modifier.testTag("income_toggle_filters"),
            ) {
              Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filter lanjutan",
                tint = if (showAdvancedFilters) EmeraldPrimary else InkSecondary,
              )
            }
          },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth().testTag("income_search_field"),
        )

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(listOf("Semua", "Order", "Blank")) { itemType ->
            FilterChip(
              selected = activityFilter == itemType,
              onClick = { activityFilter = itemType },
              label = { Text(itemType) },
              modifier = Modifier.testTag("income_type_filter_$itemType"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldContainer,
                  selectedLabelColor = OnEmeraldContainer,
                ),
            )
          }
        }

        AnimatedVisibility(visible = showAdvancedFilters) {
          CleanCard(modifier = Modifier.padding(top = 10.dp), containerColor = SurfaceTinted) {
            Text(
              text = "Filter Lanjutan",
              style = MaterialTheme.typography.titleSmall,
              color = InkPrimary,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Wilayah Awal", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(areaOptions) { area ->
                FilterChip(
                  selected = originFilter == area,
                  onClick = { originFilter = area },
                  label = { Text(area) },
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Wilayah Tujuan", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(areaOptions) { area ->
                FilterChip(
                  selected = destFilter == area,
                  onClick = { destFilter = area },
                  label = { Text(area) },
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Rentang Jarak", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("Semua", "< 8 km", "8–12 km", "> 12 km")) { d ->
                FilterChip(
                  selected = distanceFilter == d,
                  onClick = { distanceFilter = d },
                  label = { Text(d) },
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Durasi Aktivitas", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("Semua", "≤ 45 mnt", "46–90 mnt", "> 90 mnt")) { dur ->
                FilterChip(
                  selected = durationFilter == dur,
                  onClick = { durationFilter = dur },
                  label = { Text(dur) },
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Efisiensi Baterai", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("Semua", "Hemat (≤15%)", "Sedang (16–25%)", "Tinggi (>25%)")) { b ->
                FilterChip(
                  selected = batteryFilter == b,
                  onClick = { batteryFilter = b },
                  label = { Text(b) },
                )
              }
            }

            TextButton(
              onClick = {
                searchQuery = ""
                activityFilter = "Semua"
                originFilter = "Semua"
                destFilter = "Semua"
                distanceFilter = "Semua"
                durationFilter = "Semua"
                batteryFilter = "Semua"
              }
            ) {
              Text("Reset Semua Filter", color = EmeraldPrimary)
            }
          }
        }
      }

      // Kronologi Aktivitas Harian pada Tanggal Terpilih (Order & Blank saling berselang-seling)
      if (entriesOnSelectedDate.isEmpty()) {
        item(key = "income_empty_state") {
          CleanCard(modifier = Modifier.testTag("income_empty_state")) {
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
                      "Belum ada aktivitas Order atau Blank hari ini"
                    } else {
                      "Belum ada aktivitas pada ${FinanceCalculator.formatDateIndo(selectedDate)}"
                    },
                  style = MaterialTheme.typography.titleSmall,
                  color = InkPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text =
                    if (selectedDate == uiState.todayDate) {
                      "Jika hari ini libur atau belum ada Order/Blank, aktivitas hari sebelumnya tetap tersimpan dan tidak dicampur ke hari ini. Gunakan panah tanggal atau pemilih tanggal untuk melihat hari sebelumnya."
                    } else {
                      "Tidak ada catatan Order maupun Blank pada tanggal ini. Gunakan tombol panah atau pemilih tanggal untuk melihat hari lainnya."
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
          CleanCard(modifier = Modifier.testTag("income_filter_empty_state")) {
            Text(
              text =
                "Tidak ada aktivitas yang cocok dengan filter pada ${FinanceCalculator.formatDateIndo(selectedDate)}.",
              style = MaterialTheme.typography.bodyMedium,
              color = InkSecondary,
            )
          }
        }
      } else {
        val daySummary = uiState.dailySummariesByDate[selectedDate]
        item(key = "day_header_$selectedDate") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = FinanceCalculator.formatDateWithDayIndo(selectedDate),
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              if (daySummary != null) {
                Text(
                  text =
                    buildAnnotatedString {
                      append("${daySummary.orderCount} order • ${daySummary.totalPackages} pkt • Total bonus: ")
                      withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = InkPrimary)) {
                        append(FinanceCalculator.formatRupiah(daySummary.totalBonus, hide))
                      }
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
              }
            }
            if (daySummary != null) {
              StatusPill(
                text = FinanceCalculator.formatRupiah(daySummary.totalIncome, hide),
                backgroundColor = IncomeGreenBg,
                textColor = IncomeGreen,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        items(filteredEntries, key = { it.id }) { item ->
          when (item) {
            is IncomeActivityItem.OrderEntry ->
              OrderActivityCard(
                computed = item.data,
                hideAmounts = hide,
                onEdit = { editingOrder = item.data.order },
                onDelete = { viewModel.deleteOrder(item.data.order.id) },
              )
            is IncomeActivityItem.BlankEntry ->
              BlankActivityCard(
                computed = item.data,
                onDelete = { viewModel.deleteBlank(item.data.blank.id) },
              )
          }
        }
      }

      item {
        OutlinedButton(
          onClick = onNavigateToRingkasan,
          modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
          Text("Lihat Ringkasan Keuangan", color = EmeraldPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun MiniBreakdownPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.semantics(mergeDescendants = true) {},
    shape = MaterialTheme.shapes.small,
    color = SurfaceTinted,
  ) {
    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun OrderActivityCard(
  computed: ComputedWorkOrder,
  hideAmounts: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val order = computed.order
  val targetAmount = computed.dailyTargetOnDate.coerceAtLeast(1L)
  val rawProgress = computed.cumulativeDailyIncomeOnDate.toFloat() / targetAmount.toFloat()
  val progressFraction = rawProgress.coerceIn(0f, 1f)
  val progressPercent = (rawProgress * 100f).roundToInt()
  val targetDiff = computed.cumulativeDailyIncomeOnDate - computed.dailyTargetOnDate
  val targetReached = targetDiff >= 0L
  val batteryEfficiencyText =
    FinanceCalculator.formatBatteryEfficiency(computed.totalDistanceKm, computed.batteryConsumed)

  CleanCard(
    containerColor = OrderCardBg,
    borderColor = OrderCardBorder,
    borderWidth = 1.dp,
    modifier = Modifier.testTag("order_card_${order.id}"),
  ) {
    // 1. Kepala kartu: Nomor order & jumlah barang di kiri, jam mulai–selesai kecil di kanan atas
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(
          text = "Order #${computed.orderNumberOnDate} • ${order.packageCount} paket",
          backgroundColor = EmeraldPrimary,
          textColor = Color.White,
        )
        if (computed.crossesMidnight) {
          StatusPill(
            text = "Lintas Tengah Malam",
            backgroundColor = InfoBlueBg,
            textColor = InfoBlue,
          )
        }
      }
      Text(
        text = "${order.startTime} – ${order.endTime}",
        style = MaterialTheme.typography.labelSmall,
        color = OnEmeraldContainer.copy(alpha = 0.85f),
        fontWeight = FontWeight.Medium,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Bagian penghasilan: Argo besar di kiri, Progres harian (akumulasi & %) di kanan,
    //    Progress bar, lalu Bonus kumulatif, Tip GoPay, dan Tip Cash di bawahnya
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Argo Order",
          style = MaterialTheme.typography.labelSmall,
          color = OnEmeraldContainer.copy(alpha = 0.82f),
        )
        Text(
          text = FinanceCalculator.formatRupiah(order.argo, hideAmounts),
          style = MaterialTheme.typography.headlineMedium,
          color = OnEmeraldContainer,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "Progres Harian",
          style = MaterialTheme.typography.labelSmall,
          color = OnEmeraldContainer.copy(alpha = 0.82f),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = FinanceCalculator.formatRupiah(computed.cumulativeDailyIncomeOnDate, hideAmounts),
            style = MaterialTheme.typography.titleMedium,
            color = if (targetReached) IncomeGreen else EmeraldPrimaryDark,
            fontWeight = FontWeight.Bold,
          )
          StatusPill(
            text = "$progressPercent%",
            backgroundColor = if (targetReached) IncomeGreen else OrderInnerSurface,
            textColor = if (targetReached) Color.White else OnEmeraldContainer,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { progressFraction },
      modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
      color = if (targetReached) IncomeGreen else EmeraldPrimary,
      trackColor = OrderProgressTrack,
    )

    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text =
          buildAnnotatedString {
            append("Target: ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = OnEmeraldContainer)) {
              append(FinanceCalculator.formatRupiah(computed.dailyTargetOnDate, hideAmounts))
            }
          },
        style = MaterialTheme.typography.labelSmall,
        color = OnEmeraldContainer.copy(alpha = 0.82f),
      )
      Text(
        text =
          buildAnnotatedString {
            if (targetReached) {
              append("Kelebihan: ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("+${FinanceCalculator.formatRupiah(targetDiff, hideAmounts)}")
              }
            } else {
              append("Sisa: ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(FinanceCalculator.formatRupiah(-targetDiff, hideAmounts))
              }
            }
          },
        style = MaterialTheme.typography.labelSmall,
        color = if (targetReached) IncomeGreen else WarmAmber,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
      color = OrderInnerSurface,
      border = BorderStroke(0.75.dp, OrderCardBorder.copy(alpha = 0.7f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OrderMetricCell(
          label = "Bonus Kumulatif (${computed.cumulativePackagesOnDate} pkt)",
          value = FinanceCalculator.formatRupiah(computed.cumulativeBonusOnDate, hideAmounts),
          valueColor = IncomeGreen,
          modifier = Modifier.weight(1.15f),
        )
        OrderMetricCell(
          label = "Tip GoPay",
          value = FinanceCalculator.formatRupiah(order.tipGoPay, hideAmounts),
          valueColor = if (order.tipGoPay > 0L) InfoBlue else InkPrimary,
          modifier = Modifier.weight(0.9f),
        )
        OrderMetricCell(
          label = "Tip Cash",
          value = FinanceCalculator.formatRupiah(order.tipCash, hideAmounts),
          valueColor = if (order.tipCash > 0L) WarmAmber else InkPrimary,
          modifier = Modifier.weight(0.9f),
        )
      }
    }

    // 3. Bagian lokasi: Awal / Pick up & Tujuan masing-masing dengan ikon pin/titik lokasi
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.MyLocation,
          contentDescription = "Lokasi awal / Pick up",
          tint = EmeraldPrimary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Pick up: ${order.originArea}",
          style = MaterialTheme.typography.bodyMedium,
          color = OnEmeraldContainer,
          fontWeight = FontWeight.Medium,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.LocationOn,
          contentDescription = "Lokasi tujuan",
          tint = WarmAmber,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Tujuan: ${order.destinationArea}",
          style = MaterialTheme.typography.bodyMedium,
          color = OnEmeraldContainer,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // 4. Bagian jarak dan durasi (Pick up, Pengantaran, Total jarak, Durasi order)
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(
      thickness = 0.75.dp,
      color = OrderCardBorder.copy(alpha = 0.75f),
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.Route,
          contentDescription = null,
          tint = OnEmeraldContainer.copy(alpha = 0.8f),
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text =
            "Pick up ${FinanceCalculator.formatDistance(order.pickupDistanceKm)} • Antar ${FinanceCalculator.formatDistance(order.deliveryDistanceKm)} • Total ${FinanceCalculator.formatDistance(computed.totalDistanceKm)}",
          style = MaterialTheme.typography.bodySmall,
          color = OnEmeraldContainer.copy(alpha = 0.88f),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Durasi ${FinanceCalculator.formatDuration(computed.durationMinutes)}",
        style = MaterialTheme.typography.labelSmall,
        color = OnEmeraldContainer,
        fontWeight = FontWeight.SemiBold,
      )
    }

    // 5. Bagian baterai (awal, akhir, total pemakaian, efisiensi km/%)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.BatteryChargingFull,
          contentDescription = null,
          tint = OnEmeraldContainer.copy(alpha = 0.8f),
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
          Text(
            text =
              "Baterai ${order.batteryStart}% → ${order.batteryEnd}% (-${computed.batteryConsumed}%)",
            style = MaterialTheme.typography.bodySmall,
            color = OnEmeraldContainer.copy(alpha = 0.88f),
          )
          Text(
            text = "Efisiensi baterai: $batteryEfficiencyText",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldPrimaryDark,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Row {
        IconButton(onClick = onEdit) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit order",
            tint = OnEmeraldContainer.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Hapus order",
            tint = ExpenseRed,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // 6. Catatan HANYA ditampilkan jika tersedia
    if (order.note.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = OrderInnerSurface,
        border = BorderStroke(0.75.dp, OrderCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Catatan: ${order.note}",
          style = MaterialTheme.typography.bodySmall,
          color = OnEmeraldContainer.copy(alpha = 0.88f),
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
      }
    }
  }
}

@Composable
private fun BlankActivityCard(
  computed: ComputedBlankSession,
  onDelete: () -> Unit,
) {
  val blank = computed.blank
  CleanCard(
    containerColor = BlankCardBg,
    borderColor = BlankCardBorder,
    borderWidth = 1.dp,
    modifier = Modifier.testTag("blank_card_${blank.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusPill(
        text = "Blank • Bukan Order (Rp0)",
        backgroundColor = BlankPillBg,
        textColor = BlankInkPrimary,
      )
      Text(
        text = "${blank.startTime} – ${blank.endTime}",
        style = MaterialTheme.typography.labelSmall,
        color = BlankInkSecondary,
        fontWeight = FontWeight.Medium,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.LocationOn,
          contentDescription = "Wilayah Blank",
          tint = BlankInkSecondary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Wilayah ${blank.area}",
          style = MaterialTheme.typography.titleSmall,
          color = BlankInkPrimary,
          fontWeight = FontWeight.Medium,
        )
      }
      Text(
        text =
          buildAnnotatedString {
            append("Durasi ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append(FinanceCalculator.formatDuration(computed.durationMinutes))
            }
          },
        style = MaterialTheme.typography.labelMedium,
        color = BlankInkPrimary,
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text =
          "Jarak: ${FinanceCalculator.formatDistance(blank.distanceKm)} • Baterai ${blank.batteryStart}% → ${blank.batteryEnd}% (-${computed.batteryConsumed}%)",
        style = MaterialTheme.typography.bodySmall,
        color = BlankInkSecondary,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = "Hapus Blank",
          tint = BlankInkSecondary,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    if (blank.note.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Catatan: ${blank.note}",
        style = MaterialTheme.typography.bodySmall,
        color = BlankInkSecondary,
      )
    }
  }
}

@Composable
private fun OrderMetricCell(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: Color = InkPrimary,
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = InkSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = valueColor,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
