package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.dp
import com.example.model.FinanceCalculator
import com.example.model.FundLocation
import com.example.model.LocationBalanceDetail
import com.example.state.FinanceUiState
import com.example.state.FinanceViewModel
import com.example.ui.components.CleanCard
import com.example.ui.components.IconBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer

@Composable
fun PosisiSaldoScreen(
  uiState: FinanceUiState,
  viewModel: FinanceViewModel,
  onNavigateToRingkasan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hide = uiState.raw.hideAmounts
  var correctingLocation by remember { mutableStateOf<LocationBalanceDetail?>(null) }
  var showFirstTimeSetupDialog by remember { mutableStateOf(false) }

  correctingLocation?.let { detail ->
    CorrectionOpeningBalanceDialog(
      detail = detail,
      onDismiss = { correctingLocation = null },
      onSave = { delta, note ->
        viewModel.addBalanceCorrection(detail.location, delta, note)
        correctingLocation = null
      },
    )
  }

  if (showFirstTimeSetupDialog) {
    InitialBalancesSetupDialog(
      currentOpening = uiState.raw.previousClosingBalances,
      onDismiss = { showFirstTimeSetupDialog = false },
      onSave = { updated ->
        viewModel.setInitialActualBalances(updated)
        showFirstTimeSetupDialog = false
      },
    )
  }

  val totalSafe = uiState.totalActualBalance.coerceAtLeast(1L)
  val directRatio = (uiState.totalDirectSpendBalance.toFloat() / totalSafe.toFloat()).coerceIn(0.05f, 0.95f)
  val jagoRatio = (1f - directRatio).coerceIn(0.05f, 0.95f)

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Empat Kotak Saldo Terpisah (2 kolom x 2 baris)
      item {
        SectionHeader(title = "Empat Lokasi Dana")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          uiState.locationBalances.chunked(2).forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              rowItems.forEach { detail ->
                BalanceGridBox(
                  detail = detail,
                  hideAmounts = hide,
                  modifier = Modifier.weight(1f),
                )
              }
              if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }

      // Kartu Total Seluruh Saldo (tetap tersedia tanpa menggabung saldo terpisah)
      item {
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Column {
              Text(
                text = "Total Seluruh Saldo Aktual",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = FinanceCalculator.formatRupiah(uiState.totalActualBalance, hide),
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            IconBadge(
              icon = Icons.Outlined.AccountBalanceWallet,
              containerColor = EmeraldContainer,
              iconColor = EmeraldPrimary,
              size = 44,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Box(modifier = Modifier.weight(directRatio).height(10.dp).background(IncomeGreen))
            Box(modifier = Modifier.weight(jagoRatio).height(10.dp).background(InfoBlue))
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            LegendChip(
              label = "Siap Pakai (GoPay+Cash): ${FinanceCalculator.formatRupiah(uiState.totalDirectSpendBalance, hide)}",
              color = IncomeGreen,
            )
            LegendChip(
              label = "Bank Jago: ${FinanceCalculator.formatRupiah(uiState.totalJagoBalance, hide)}",
              color = InfoBlue,
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { showFirstTimeSetupDialog = true },
              modifier = Modifier.weight(1f).testTag("saldo_setup_awal_button"),
              shape = MaterialTheme.shapes.small,
            ) {
              Text("Atur Saldo Awal", color = EmeraldPrimary)
            }
            OutlinedButton(
              onClick = onNavigateToRingkasan,
              modifier = Modifier.weight(1f).testTag("saldo_to_ringkasan_button"),
              shape = MaterialTheme.shapes.small,
            ) {
              Text("Ringkasan", color = InkPrimary)
            }
          }
        }
      }

      item {
        SectionHeader(title = "Rincian & Koreksi Saldo")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text =
            "Saldo awal bulan otomatis dibawa dari saldo akhir bulan sebelumnya. Koreksi saldo awal dengan catatan akan memperbarui saldo berjalan.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
      }

      items(uiState.locationBalances, key = { it.location.id }) { detail ->
        LocationBalanceCard(
          detail = detail,
          hideAmounts = hide,
          onCorrectOpening = { correctingLocation = detail },
        )
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun BalanceGridBox(
  detail: LocationBalanceDetail,
  hideAmounts: Boolean,
  modifier: Modifier = Modifier,
) {
  val loc = detail.location
  val icon =
    when (loc) {
      FundLocation.GOPAY -> Icons.Outlined.PhoneAndroid
      FundLocation.CASH -> Icons.Outlined.Payments
      FundLocation.JAGO_CICILAN, FundLocation.JAGO_PRIBADI -> Icons.Outlined.AccountBalance
    }
  val accentColor =
    when (loc) {
      FundLocation.GOPAY -> IncomeGreen
      FundLocation.CASH -> WarmAmber
      FundLocation.JAGO_CICILAN -> InfoBlue
      FundLocation.JAGO_PRIBADI -> EmeraldPrimary
    }

  val tintBg =
    when (loc) {
      FundLocation.GOPAY -> IncomeGreenBg.copy(alpha = 0.45f)
      FundLocation.CASH -> WarmAmberContainer.copy(alpha = 0.45f)
      FundLocation.JAGO_CICILAN -> InfoBlueBg.copy(alpha = 0.45f)
      FundLocation.JAGO_PRIBADI -> EmeraldContainer.copy(alpha = 0.45f)
    }

  CleanCard(
    modifier = modifier.testTag("saldo_grid_box_${loc.id}"),
    containerColor = tintBg,
    borderColor = accentColor.copy(alpha = 0.16f),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(
        icon = icon,
        containerColor = accentColor.copy(alpha = 0.14f),
        iconColor = accentColor,
        size = 34,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = loc.displayName,
        style = MaterialTheme.typography.titleSmall,
        color = InkPrimary,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = FinanceCalculator.formatRupiah(detail.runningBalance, hideAmounts),
      style = MaterialTheme.typography.titleLarge,
      color = InkPrimary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (loc.isDirectSpendAllowed) "Bisa bayar langsung"
        else "Bank Jago • Transfer dulu",
      style = MaterialTheme.typography.labelSmall,
      color = if (loc.isDirectSpendAllowed) IncomeGreen else InfoBlue,
      maxLines = 1,
    )
  }
}

@Composable
private fun LegendChip(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Spacer(modifier = Modifier.width(5.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
  }
}

@Composable
private fun LocationBalanceCard(
  detail: LocationBalanceDetail,
  hideAmounts: Boolean,
  onCorrectOpening: () -> Unit,
) {
  val loc = detail.location
  val icon =
    when (loc) {
      FundLocation.GOPAY -> Icons.Outlined.PhoneAndroid
      FundLocation.CASH -> Icons.Outlined.Payments
      FundLocation.JAGO_CICILAN, FundLocation.JAGO_PRIBADI -> Icons.Outlined.AccountBalance
    }
  val accentColor =
    when (loc) {
      FundLocation.GOPAY -> IncomeGreen
      FundLocation.CASH -> WarmAmber
      FundLocation.JAGO_CICILAN -> InfoBlue
      FundLocation.JAGO_PRIBADI -> EmeraldPrimary
    }

  CleanCard(modifier = Modifier.testTag("saldo_card_${loc.id}")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconBadge(
        icon = icon,
        containerColor = accentColor.copy(alpha = 0.14f),
        iconColor = accentColor,
        size = 44,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = loc.displayName,
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = loc.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    StatusPill(
      text =
        if (loc.isDirectSpendAllowed) "Bisa untuk pengeluaran langsung"
        else "Bank Jago • Transfer dulu sebelum dipakai bayar",
      backgroundColor = if (loc.isDirectSpendAllowed) IncomeGreenBg else InfoBlueBg,
      textColor = if (loc.isDirectSpendAllowed) IncomeGreen else InfoBlue,
    )

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 12.dp),
      thickness = 0.75.dp,
      color = OutlineSubtle.copy(alpha = 0.55f),
    )

    BalanceRow("Saldo Berjalan", FinanceCalculator.formatRupiah(detail.runningBalance, hideAmounts), bold = true)
    BalanceRow(
      "Saldo Awal (bawaan Okt)",
      FinanceCalculator.formatRupiah(detail.previousMonthClosing, hideAmounts),
    )
    if (detail.openingCorrectionDelta != 0L) {
      BalanceRow(
        "Koreksi Saldo Awal",
        FinanceCalculator.formatSignedRupiah(detail.openingCorrectionDelta, hideAmounts),
        valueColor = InfoBlue,
      )
    }
    BalanceRow(
      "Perubahan Saldo Bulanan",
      FinanceCalculator.formatSignedRupiah(detail.monthlyChange, hideAmounts),
      valueColor = if (detail.monthlyChange >= 0L) IncomeGreen else ExpenseRed,
    )

    if (detail.corrections.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      detail.corrections.forEach { cor ->
        Text(
          text = "• Catatan koreksi (${FinanceCalculator.formatDateIndo(cor.date)}): ${cor.note}",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    OutlinedButton(
      onClick = onCorrectOpening,
      modifier = Modifier.fillMaxWidth().testTag("saldo_koreksi_${loc.id}"),
      shape = MaterialTheme.shapes.small,
    ) {
      Icon(
        Icons.Outlined.EditNote,
        contentDescription = null,
        tint = EmeraldPrimary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text("Koreksi Saldo Awal", color = EmeraldPrimary)
    }
  }
}

@Composable
private fun BalanceRow(
  label: String,
  value: String,
  bold: Boolean = false,
  valueColor: Color = InkPrimary,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = if (bold) InkPrimary else InkSecondary,
      fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    )
    Text(
      text = value,
      style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
      color = valueColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun CorrectionOpeningBalanceDialog(
  detail: LocationBalanceDetail,
  onDismiss: () -> Unit,
  onSave: (deltaAmount: Long, note: String) -> Unit,
) {
  var desiredOpeningText by remember { mutableStateOf(detail.effectiveOpeningBalance.toString()) }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Koreksi Saldo Awal ${detail.location.shortName}",
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text =
            "Saldo awal aktif saat ini: ${FinanceCalculator.formatRupiah(detail.effectiveOpeningBalance)}. Koreksi akan memperbarui saldo berjalan.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = desiredOpeningText,
          onValueChange = { desiredOpeningText = it.filter(Char::isDigit) },
          label = { Text("Saldo Awal Baru (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan Koreksi (wajib)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      val desired = desiredOpeningText.toLongOrNull() ?: detail.effectiveOpeningBalance
      val delta = desired - detail.effectiveOpeningBalance
      Button(
        enabled = delta != 0L && note.trim().isNotEmpty(),
        onClick = { onSave(delta, note) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Koreksi")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun InitialBalancesSetupDialog(
  currentOpening: Map<FundLocation, Long>,
  onDismiss: () -> Unit,
  onSave: (Map<FundLocation, Long>) -> Unit,
) {
  val textMap = remember {
    mutableStateMapOf<FundLocation, String>().apply {
      FundLocation.entries.forEach { loc -> put(loc, (currentOpening[loc] ?: 0L).toString()) }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Saldo Aktual Awal (4 Lokasi Dana)",
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
          text = "Masukkan saldo aktual awal untuk keempat lokasi dana pada penggunaan pertama.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        FundLocation.entries.forEach { loc ->
          OutlinedTextField(
            value = textMap[loc].orEmpty(),
            onValueChange = { textMap[loc] = it.filter(Char::isDigit) },
            label = { Text(loc.displayName) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val map =
            FundLocation.entries.associateWith { loc -> textMap[loc]?.toLongOrNull() ?: 0L }
          onSave(map)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Saldo Awal")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}
