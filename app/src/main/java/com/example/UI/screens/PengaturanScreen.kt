package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.DriveBackupFile
import com.example.data.remote.GoogleAuthOutcome
import com.example.data.remote.GoogleDriveAuthManager
import com.example.model.BonusRuleRecord
import com.example.model.FinanceCalculator
import com.example.model.IncomeTargetRecord
import com.example.state.BackupPendingAuthAction
import com.example.state.FinanceUiState
import com.example.state.FinanceViewModel
import com.example.state.PendingRestoreConfirmation
import com.example.ui.components.CleanCard
import com.example.ui.components.IconBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer
import kotlinx.coroutines.launch

@Composable
fun PengaturanScreen(
  uiState: FinanceUiState,
  viewModel: FinanceViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val backupUiState by viewModel.backupUiState.collectAsStateWithLifecycle()
  val hide = uiState.raw.hideAmounts
  var showTargetDialog by remember { mutableStateOf(false) }
  var showBonusRuleDialog by remember { mutableStateOf(false) }
  var showAreaManager by remember { mutableStateOf(false) }
  var showCategoryManager by remember { mutableStateOf(false) }
  var pendingActionRef by remember { mutableStateOf<BackupPendingAuthAction?>(null) }

  val dispatchAuthorizedToken: (String, BackupPendingAuthAction?) -> Unit = { token, action ->
    when (action) {
      BackupPendingAuthAction.BACKUP -> viewModel.startBackupWithToken(token)
      BackupPendingAuthAction.LIST_FOR_RESTORE -> viewModel.loadBackupFilesWithToken(token)
      null -> Unit
    }
  }

  val authResolutionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
      val action = pendingActionRef ?: backupUiState.pendingAuthAction
      pendingActionRef = null
      if (activityResult.resultCode != Activity.RESULT_OK && activityResult.data == null) {
        viewModel.reportBackupAuthError("Otorisasi akun Google dibatalkan.")
        return@rememberLauncherForActivityResult
      }
      when (
        val outcome =
          GoogleDriveAuthManager.extractAuthorizationResultFromIntent(context, activityResult.data)
      ) {
        is GoogleAuthOutcome.Authorized -> dispatchAuthorizedToken(outcome.accessToken, action)
        is GoogleAuthOutcome.NeedsResolution ->
          viewModel.reportBackupAuthError(
            "Otorisasi tambahan masih diperlukan. Silakan coba tekan tombol kembali."
          )
        is GoogleAuthOutcome.Failed -> viewModel.reportBackupAuthError(outcome.message)
      }
    }

  val launchGoogleBackupOrRestore: (BackupPendingAuthAction) -> Unit = { action ->
    pendingActionRef = action
    viewModel.setPendingAuthAction(action)
    coroutineScope.launch {
      when (val outcome = GoogleDriveAuthManager.requestDriveAndSheetsAuthorization(context)) {
        is GoogleAuthOutcome.Authorized -> {
          pendingActionRef = null
          dispatchAuthorizedToken(outcome.accessToken, action)
        }
        is GoogleAuthOutcome.NeedsResolution -> {
          authResolutionLauncher.launch(
            IntentSenderRequest.Builder(outcome.pendingIntent).build()
          )
        }
        is GoogleAuthOutcome.Failed -> {
          pendingActionRef = null
          viewModel.reportBackupAuthError(outcome.message)
        }
      }
    }
  }

  if (showTargetDialog) {
    TargetManagementDialog(
      defaultEffectiveDate = uiState.todayDate,
      currentDaily = uiState.todayDailyTarget,
      currentMonthly = uiState.currentMonthlyTarget,
      history = uiState.raw.targetHistory,
      onDismiss = { showTargetDialog = false },
      onAddTarget = { effDate, daily, monthly, note ->
        viewModel.addTargetChange(effDate, daily, monthly, note)
        showTargetDialog = false
      },
    )
  }

  if (showBonusRuleDialog) {
    BonusRuleManagementDialog(
      defaultEffectiveDate = uiState.todayDate,
      currentRule = uiState.activeBonusRule,
      history = uiState.raw.bonusRuleHistory,
      onDismiss = { showBonusRuleDialog = false },
      onAddBonusRule = { effDate, r1, r2, r3, r4, note ->
        viewModel.addBonusRuleChange(effDate, r1, r2, r3, r4, note)
        showBonusRuleDialog = false
      },
    )
  }

  if (showAreaManager) {
    ManageAreasDialog(
      areas = uiState.raw.areas,
      onAdd = viewModel::addArea,
      onRename = viewModel::renameArea,
      onDelete = viewModel::deleteArea,
      onMerge = viewModel::mergeAreas,
      onDismiss = { showAreaManager = false },
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

  val availableFiles = backupUiState.availableBackupFiles
  if (availableFiles != null) {
    RestoreFilePickerDialog(
      files = availableFiles,
      isBusy = backupUiState.isBusy,
      onSelectFile = { file -> viewModel.selectBackupFileForConfirmation(file) },
      onDismiss = { viewModel.dismissRestoreDialogs() },
    )
  }

  val pendingConfirmation = backupUiState.pendingRestoreConfirmation
  if (pendingConfirmation != null) {
    RestoreConfirmationDialog(
      pending = pendingConfirmation,
      isBusy = backupUiState.isBusy,
      onConfirm = { viewModel.confirmAndExecuteRestore() },
      onDismiss = { viewModel.dismissRestoreDialogs() },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("pengaturan_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Profil Lokal & Tampilan
      item {
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Pengguna Lokal",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Tema Terang • Bahasa Indonesia • Data Lokal Utama",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
            StatusPill(
              text = "Database Lokal",
              backgroundColor = EmeraldContainer,
              textColor = OnEmeraldContainer,
            )
          }
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.75.dp,
            color = OutlineSubtle.copy(alpha = 0.55f),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.VisibilityOff,
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              iconColor = InkSecondary,
              size = 38,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Sembunyikan Nominal Saldo",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Menyamarkan nominal di seluruh 8 halaman",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
              )
            }
            Switch(
              checked = hide,
              onCheckedChange = { viewModel.toggleHideAmounts() },
              modifier = Modifier.testTag("pengaturan_switch_hide_amounts"),
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = EmeraldPrimary,
                ),
            )
          }
        }
      }

      // A. Pengaturan Target Penghasilan (dengan tanggal berlaku & riwayat)
      item {
        SectionHeader(title = "Target Penghasilan")
        Spacer(modifier = Modifier.height(8.dp))
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.Flag,
              containerColor = EmeraldContainer,
              iconColor = EmeraldPrimary,
              size = 42,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  "Harian: ${FinanceCalculator.formatRupiah(uiState.todayDailyTarget, hide)} • Bulanan: ${FinanceCalculator.formatRupiah(uiState.currentMonthlyTarget, hide)}",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "${uiState.raw.targetHistory.size} riwayat target tersimpan dengan tanggal mulai berlaku.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { showTargetDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_manage_target"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Ubah Target & Lihat Riwayat", color = EmeraldPrimary)
          }
        }
      }

      // Aturan Bonus Paket (dengan tanggal berlaku & riwayat)
      item {
        SectionHeader(title = "Aturan Bonus Paket")
        Spacer(modifier = Modifier.height(8.dp))
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.CardGiftcard,
              containerColor = EmeraldContainer,
              iconColor = EmeraldPrimary,
              size = 42,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              val rule = uiState.activeBonusRule
              Text(
                text =
                  "1–${rule.tier1MaxPackages}: ${FinanceCalculator.formatRupiah(rule.tier1Rate, hide)} • 10–${rule.tier2MaxPackages}: ${FinanceCalculator.formatRupiah(rule.tier2Rate, hide)} • 15–${rule.tier3MaxPackages}: ${FinanceCalculator.formatRupiah(rule.tier3Rate, hide)} • >${rule.tier3MaxPackages}: ${FinanceCalculator.formatRupiah(rule.tier4Rate, hide)}",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "${uiState.raw.bonusRuleHistory.size} riwayat aturan bonus per paket tersimpan di database.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { showBonusRuleDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_manage_bonus_rules"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Ubah Aturan Bonus & Riwayat", color = EmeraldPrimary)
          }
        }
      }

      // F. Pengaturan Wilayah (Tambah, Ubah, Hapus, Gabungkan, Normalisasi)
      item {
        SectionHeader(title = "Pengaturan Wilayah")
        Spacer(modifier = Modifier.height(8.dp))
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.LocationOn,
              containerColor = InfoBlueBg,
              iconColor = InfoBlue,
              size = 42,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${uiState.raw.areas.size} Wilayah Tersimpan",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "Penamaan dinormalisasi otomatis. Perubahan nama tidak mengubah catatan historis.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { showAreaManager = true },
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_manage_areas"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Kelola & Gabungkan Wilayah", color = EmeraldPrimary)
          }
        }
      }

      // Kelola Kategori Pengeluaran
      item {
        SectionHeader(title = "Kategori Pengeluaran")
        Spacer(modifier = Modifier.height(8.dp))
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconBadge(
              icon = Icons.Outlined.Category,
              containerColor = EmeraldContainer,
              iconColor = EmeraldPrimary,
              size = 42,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${uiState.raw.categories.size} Kategori Aktif",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Sepenuhnya dikelola pengguna (tambah, ubah, hapus tanpa kategori bawaan paksa).",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { showCategoryManager = true },
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_manage_categories"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Kelola Kategori Pengeluaran", color = EmeraldPrimary)
          }
        }
      }

      // Data dan Cadangan Manual melalui Google Sheets (Tanpa Sinkronisasi Otomatis)
      item(key = "data_dan_cadangan") {
        SectionHeader(title = "Data & Cadangan (Google Sheets Manual)")
        Spacer(modifier = Modifier.height(8.dp))
        CleanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Cadangan Manual Google Sheets",
              style = MaterialTheme.typography.titleSmall,
              color = InkPrimary,
              fontWeight = FontWeight.SemiBold,
            )
            StatusPill(
              text =
                if (uiState.raw.backupInfo.isGoogleAccountLinkedPreview) "Terhubung"
                else "Manual",
              backgroundColor =
                if (uiState.raw.backupInfo.isGoogleAccountLinkedPreview) EmeraldContainer
                else InfoBlueBg,
              textColor =
                if (uiState.raw.backupInfo.isGoogleAccountLinkedPreview) OnEmeraldContainer
                else InfoBlue,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              "Data lokal tetap sumber utama. Anda memilih sendiri kapan membuat cadangan ke Google Sheets atau memulihkannya. Tidak ada sinkronisasi otomatis.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
          Spacer(modifier = Modifier.height(10.dp))
          val linkedEmail = uiState.raw.backupInfo.linkedGoogleEmail
          Text(
            text =
              if (linkedEmail.isNotBlank()) "Akun Google: $linkedEmail"
              else "Akun Google: Dipilih saat mencadangkan atau memulihkan",
            style = MaterialTheme.typography.labelMedium,
            color = InkPrimary,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Nama file Sheets: ${uiState.raw.backupInfo.sheetNamePreview}",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Text(
            text = "Terakhir dicadangkan: ${uiState.raw.backupInfo.lastBackupTimestamp}",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Text(
            text = "Terakhir dipulihkan: ${uiState.raw.backupInfo.lastRestoreTimestamp}",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = uiState.raw.backupInfo.lastBackupSummary,
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldPrimary,
          )

          if (backupUiState.isBusy) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth().testTag("backup_busy_indicator"),
              color = EmeraldPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = backupUiState.busyMessage ?: "Memproses...",
              style = MaterialTheme.typography.bodySmall,
              color = InkSecondary,
            )
          }

          val statusMsg = backupUiState.statusMessage
          if (!statusMsg.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = if (backupUiState.isError) ExpenseRedBg else EmeraldContainer,
              shape = MaterialTheme.shapes.small,
              border =
                BorderStroke(
                  0.75.dp,
                  if (backupUiState.isError) ExpenseRed.copy(alpha = 0.4f)
                  else EmeraldPrimary.copy(alpha = 0.35f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("backup_status_banner"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector =
                    if (backupUiState.isError) Icons.Outlined.ErrorOutline
                    else Icons.Outlined.CloudDone,
                  contentDescription = null,
                  tint = if (backupUiState.isError) ExpenseRed else OnEmeraldContainer,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = statusMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (backupUiState.isError) ExpenseRed else OnEmeraldContainer,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = { viewModel.clearBackupStatusMessage() },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup pesan status",
                    tint = if (backupUiState.isError) ExpenseRed else OnEmeraldContainer,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { launchGoogleBackupOrRestore(BackupPendingAuthAction.BACKUP) },
            enabled = !backupUiState.isBusy,
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_backup_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
              ),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buat Cadangan Sekarang")
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = { launchGoogleBackupOrRestore(BackupPendingAuthAction.LIST_FOR_RESTORE) },
            enabled = !backupUiState.isBusy,
            modifier = Modifier.fillMaxWidth().testTag("pengaturan_restore_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pulihkan Cadangan", color = InkPrimary)
          }

          if (uiState.raw.backupInfo.isGoogleAccountLinkedPreview) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
              onClick = {
                coroutineScope.launch {
                  GoogleDriveAuthManager.clearCredentialSession(context)
                }
                viewModel.disconnectLinkedAccountDisplay()
              },
              enabled = !backupUiState.isBusy,
              modifier = Modifier.align(Alignment.End).testTag("pengaturan_unlink_google"),
            ) {
              Text("Putuskan Tampilan Akun", color = InkSecondary)
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun RestoreFilePickerDialog(
  files: List<DriveBackupFile>,
  isBusy: Boolean,
  onSelectFile: (DriveBackupFile) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("restore_file_picker_dialog"),
    title = {
      Text(
        text = "Pilih File Cadangan Google Drive",
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
          text =
            "Pilih file Google Sheets cadangan untuk diperiksa keutuhannya sebelum memulihkan data:",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        if (isBusy) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldPrimary)
        }
        files.forEachIndexed { idx, file ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(0.75.dp, OutlineSubtle),
            modifier =
              Modifier.fillMaxWidth()
                .testTag("restore_file_item_$idx")
                .clickable(enabled = !isBusy) { onSelectFile(file) },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = file.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = InkPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "Diubah: ${formatBackupDisplayTimestamp(file.modifiedTimeIso)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text("Tutup", color = InkSecondary)
      }
    },
  )
}

@Composable
private fun RestoreConfirmationDialog(
  pending: PendingRestoreConfirmation,
  isBusy: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("restore_confirm_dialog"),
    title = {
      Text(
        text = "Konfirmasi Pemulihan Data",
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
        Surface(
          color = EmeraldContainer.copy(alpha = 0.6f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "File: ${pending.file.name}",
              style = MaterialTheme.typography.titleSmall,
              color = InkPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Tanggal cadangan: ${formatBackupDisplayTimestamp(pending.metadata.createdAtIso)}",
              style = MaterialTheme.typography.bodySmall,
              color = InkSecondary,
            )
            Text(
              text = "Ringkasan isi: ${pending.metadata.summaryText}",
              style = MaterialTheme.typography.bodySmall,
              color = OnEmeraldContainer,
            )
          }
        }

        Surface(
          color = WarmAmberContainer,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(0.75.dp, WarmAmber.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.WarningAmber,
              contentDescription = null,
              tint = WarmAmber,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                "Peringatan: Seluruh data lokal di perangkat saat ini (order, blank, pengeluaran, transfer, saldo, kategori, wilayah, aturan bonus, target & tagihan) akan digantikan sepenuhnya oleh data cadangan ini.",
              style = MaterialTheme.typography.bodySmall,
              color = InkPrimary,
            )
          }
        }

        if (isBusy) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldPrimary)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isBusy,
        modifier = Modifier.testTag("restore_confirm_button"),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Ya, Pulihkan Data")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isBusy,
        modifier = Modifier.testTag("restore_cancel_button"),
      ) {
        Text("Batal", color = InkSecondary)
      }
    },
  )
}

private fun formatBackupDisplayTimestamp(rawIso: String): String {
  if (rawIso.length >= 16 && rawIso[4] == '-' && rawIso[7] == '-') {
    val datePart = rawIso.take(10)
    val timePart = rawIso.substring(11, 16)
    return "${FinanceCalculator.formatDateIndo(datePart)}, $timePart"
  }
  return rawIso.ifBlank { "-" }
}

@Composable
private fun TargetManagementDialog(
  defaultEffectiveDate: String,
  currentDaily: Long,
  currentMonthly: Long,
  history: List<IncomeTargetRecord>,
  onDismiss: () -> Unit,
  onAddTarget: (effectiveDate: String, daily: Long, monthly: Long, note: String) -> Unit,
) {
  var effectiveDate by remember { mutableStateOf(defaultEffectiveDate) }
  var dailyText by remember { mutableStateOf(currentDaily.toString()) }
  var monthlyText by remember { mutableStateOf(currentMonthly.toString()) }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Target Penghasilan & Riwayat",
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
          text = "Setiap perubahan memiliki tanggal mulai berlaku dan tetap mempertahankan riwayat target lama.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = effectiveDate,
          onValueChange = { effectiveDate = it },
          label = { Text("Tanggal Mulai Berlaku (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = dailyText,
          onValueChange = { dailyText = it.filter(Char::isDigit) },
          label = { Text("Target Harian (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = monthlyText,
          onValueChange = { monthlyText = it.filter(Char::isDigit) },
          label = { Text("Target Bulanan (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan perubahan") },
          modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
          text = "Riwayat Target Tersimpan",
          style = MaterialTheme.typography.labelLarge,
          color = InkPrimary,
        )
        history.sortedByDescending { it.effectiveDate }.forEach { item ->
          Text(
            text =
              "• Mulai ${FinanceCalculator.formatDateIndo(item.effectiveDate)}: Harian ${FinanceCalculator.formatRupiah(item.dailyTarget)} / Bulanan ${FinanceCalculator.formatRupiah(item.monthlyTarget)}",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onAddTarget(
            effectiveDate.trim().ifEmpty { defaultEffectiveDate },
            dailyText.toLongOrNull() ?: currentDaily,
            monthlyText.toLongOrNull() ?: currentMonthly,
            note,
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Target Baru")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
  )
}

@Composable
private fun BonusRuleManagementDialog(
  defaultEffectiveDate: String,
  currentRule: BonusRuleRecord,
  history: List<BonusRuleRecord>,
  onDismiss: () -> Unit,
  onAddBonusRule: (
    effectiveDate: String,
    tier1Rate: Long,
    tier2Rate: Long,
    tier3Rate: Long,
    tier4Rate: Long,
    note: String,
  ) -> Unit,
) {
  var effectiveDate by remember { mutableStateOf(defaultEffectiveDate) }
  var tier1Text by remember { mutableStateOf(currentRule.tier1Rate.toString()) }
  var tier2Text by remember { mutableStateOf(currentRule.tier2Rate.toString()) }
  var tier3Text by remember { mutableStateOf(currentRule.tier3Rate.toString()) }
  var tier4Text by remember { mutableStateOf(currentRule.tier4Rate.toString()) }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Aturan Bonus Paket & Riwayat",
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
          text =
            "Bonus dihitung dari total paket harian dikali tarif per paket pada tier akhir. Perubahan berlaku sejak tanggal yang ditentukan dan riwayat lama tetap tersimpan.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        OutlinedTextField(
          value = effectiveDate,
          onValueChange = { effectiveDate = it },
          label = { Text("Tanggal Mulai Berlaku (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tier1Text,
            onValueChange = { tier1Text = it.filter(Char::isDigit) },
            label = { Text("1–9 Paket (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = tier2Text,
            onValueChange = { tier2Text = it.filter(Char::isDigit) },
            label = { Text("10–14 Paket (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tier3Text,
            onValueChange = { tier3Text = it.filter(Char::isDigit) },
            label = { Text("15–20 Paket (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = tier4Text,
            onValueChange = { tier4Text = it.filter(Char::isDigit) },
            label = { Text(">20 Paket (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan perubahan") },
          modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
          text = "Riwayat Aturan Bonus Tersimpan",
          style = MaterialTheme.typography.labelLarge,
          color = InkPrimary,
        )
        history.sortedByDescending { it.effectiveDate }.forEach { item ->
          Text(
            text =
              "• Mulai ${FinanceCalculator.formatDateIndo(item.effectiveDate)}: ${FinanceCalculator.formatRupiah(item.tier1Rate)} / ${FinanceCalculator.formatRupiah(item.tier2Rate)} / ${FinanceCalculator.formatRupiah(item.tier3Rate)} / ${FinanceCalculator.formatRupiah(item.tier4Rate)}",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onAddBonusRule(
            effectiveDate.trim().ifEmpty { defaultEffectiveDate },
            tier1Text.toLongOrNull() ?: currentRule.tier1Rate,
            tier2Text.toLongOrNull() ?: currentRule.tier2Rate,
            tier3Text.toLongOrNull() ?: currentRule.tier3Rate,
            tier4Text.toLongOrNull() ?: currentRule.tier4Rate,
            note,
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
      ) {
        Text("Simpan Aturan Bonus")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
  )
}

@Composable
private fun ManageAreasDialog(
  areas: List<String>,
  onAdd: (String) -> Unit,
  onRename: (String, String) -> Unit,
  onDelete: (String) -> Unit,
  onMerge: (Set<String>, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var newAreaText by remember { mutableStateOf("") }
  var renamingArea by remember { mutableStateOf<String?>(null) }
  var renameText by remember { mutableStateOf("") }
  val selectedForMerge = remember { mutableStateListOf<String>() }
  var mergeTargetText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "Pengaturan Wilayah",
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
          text =
            "Nama dinormalisasi (huruf & spasi) agar tidak duplikat. Perubahan nama tidak mengubah catatan historis.",
          style = MaterialTheme.typography.bodySmall,
          color = InkSecondary,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedTextField(
            value = newAreaText,
            onValueChange = { newAreaText = it },
            label = { Text("Wilayah baru") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = {
              onAdd(newAreaText)
              newAreaText = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
          ) {
            Text("Tambah")
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
          text = "Centang 2+ wilayah untuk digabungkan:",
          style = MaterialTheme.typography.labelSmall,
          color = InkMuted,
        )
        areas.forEach { area ->
          if (renamingArea == area) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = renameText,
                onValueChange = { renameText = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = {
                  onRename(area, renameText)
                  renamingArea = null
                }
              ) { Text("Simpan") }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = selectedForMerge.contains(area),
                onCheckedChange = { checked ->
                  if (checked) selectedForMerge.add(area) else selectedForMerge.remove(area)
                },
              )
              Text(
                text = area,
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = {
                  renamingArea = area
                  renameText = area
                },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Ubah wilayah", modifier = Modifier.size(17.dp))
              }
              IconButton(
                onClick = { onDelete(area) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = "Hapus wilayah",
                  tint = ExpenseRed,
                  modifier = Modifier.size(17.dp),
                )
              }
            }
          }
        }

        if (selectedForMerge.size >= 2) {
          HorizontalDivider()
          OutlinedTextField(
            value = mergeTargetText,
            onValueChange = { mergeTargetText = it },
            label = { Text("Nama Wilayah Gabungan (${selectedForMerge.size} dipilih)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            enabled = mergeTargetText.trim().isNotEmpty(),
            onClick = {
              onMerge(selectedForMerge.toSet(), mergeTargetText)
              selectedForMerge.clear()
              mergeTargetText = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue, contentColor = Color.White),
          ) {
            Icon(Icons.Outlined.MergeType, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Gabungkan Wilayah Terpilih")
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai", color = EmeraldPrimary) } },
  )
}
