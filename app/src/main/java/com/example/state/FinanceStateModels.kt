package com.example.state

import com.example.model.AreaStatItem
import com.example.model.BalanceCorrection
import com.example.model.BillItem
import com.example.model.BlankSession
import com.example.model.BonusRuleRecord
import com.example.model.CategoryComparisonItem
import com.example.model.ComputedBlankSession
import com.example.model.ComputedWorkOrder
import com.example.model.DailyIncomeSummary
import com.example.model.DayStatPoint
import com.example.model.ExpenseCategory
import com.example.model.ExpenseRecord
import com.example.model.FinanceCalculator
import com.example.model.FundLocation
import com.example.model.IncomeActivityItem
import com.example.model.IncomeTargetRecord
import com.example.model.LocationBalanceDetail
import com.example.model.TransactionListEntry
import com.example.model.TransferRecord
import com.example.model.WeeklyTrendPoint
import com.example.model.WorkOrder

import com.example.data.backup.BackupMetadataInfo
import com.example.data.remote.DriveBackupFile

data class BackupPreviewInfo(
  val lastBackupTimestamp: String = "Belum pernah dicadangkan",
  val lastRestoreTimestamp: String = "-",
  val isGoogleAccountLinkedPreview: Boolean = false,
  val sheetNamePreview: String = "Catatan_Keuangan_Backup",
  val lastBackupSummary: String =
    "Data lokal di database Room adalah sumber utama. Tidak ada sinkronisasi otomatis.",
  val backupSpreadsheetId: String = "",
  val linkedGoogleEmail: String = "",
)

enum class BackupPendingAuthAction {
  BACKUP,
  LIST_FOR_RESTORE,
}

data class PendingRestoreConfirmation(
  val file: DriveBackupFile,
  val metadata: BackupMetadataInfo,
  val restoredState: FinanceRawState,
)

data class BackupUiState(
  val isBusy: Boolean = false,
  val busyMessage: String? = null,
  val pendingAuthAction: BackupPendingAuthAction? = null,
  val availableBackupFiles: List<DriveBackupFile>? = null,
  val pendingRestoreConfirmation: PendingRestoreConfirmation? = null,
  val statusMessage: String? = null,
  val isError: Boolean = false,
)

data class FinanceRawState(
  val hideAmounts: Boolean = false,
  val isFirstTimeBalanceConfigured: Boolean = true,
  val orders: List<WorkOrder> = emptyList(),
  val blanks: List<BlankSession> = emptyList(),
  val bonusRuleHistory: List<BonusRuleRecord> = listOf(BonusRuleRecord.DEFAULT),
  val expenses: List<ExpenseRecord> = emptyList(),
  val transfers: List<TransferRecord> = emptyList(),
  val categories: List<ExpenseCategory> = emptyList(),
  val areas: List<String> = emptyList(),
  val previousClosingBalances: Map<FundLocation, Long> =
    FundLocation.entries.associateWith { 0L },
  val balanceCorrections: List<BalanceCorrection> = emptyList(),
  val bills: List<BillItem> = emptyList(),
  val targetHistory: List<IncomeTargetRecord> = emptyList(),
  val activeMonthKey: String = FinanceCalculator.currentSystemIsoDate().take(7),
  val todayDate: String = FinanceCalculator.currentSystemIsoDate(),
  val backupInfo: BackupPreviewInfo = BackupPreviewInfo(),
  val nextSequenceId: Long = 100L,
)

data class FinanceUiState(
  val isDatabaseLoaded: Boolean,
  val raw: FinanceRawState,
  val todayDate: String,
  val activeMonthKey: String,
  val activeMonthLabel: String,
  val activeBonusRule: BonusRuleRecord,
  val computedOrdersByDate: Map<String, List<ComputedWorkOrder>>,
  val computedBlanksByDate: Map<String, List<ComputedBlankSession>>,
  val activityTimelineByDate: Map<String, List<IncomeActivityItem>>,
  val dailySummariesByDate: Map<String, DailyIncomeSummary>,
  val todaySummary: DailyIncomeSummary,
  val todayDailyTarget: Long,
  val currentMonthlyTarget: Long,
  val monthlyRemainingTarget: Long,
  val monthlyTotalIncome: Long,
  val monthlyTotalArgo: Long,
  val monthlyTotalBonus: Long,
  val monthlyTotalTipGoPay: Long,
  val monthlyTotalTipCash: Long,
  val monthlyTotalExpense: Long,
  val monthlyTotalTransfer: Long,
  val monthlyProductiveDays: Int,
  val monthlyDailyTargetAccumulation: Long,
  val monthlyDailyTargetDiff: Long,
  val monthlyTotalOrders: Int,
  val monthlyTotalPackages: Int,
  val monthlyTotalOrderDurationMinutes: Int,
  val monthlyTotalBlankDurationMinutes: Int,
  val monthlyTotalDistanceKm: Double,
  val monthlyTotalBatteryConsumed: Int,
  val monthlyAvgIncomePerProductiveDay: Long,
  val monthlyEstimated26DaysIncome: Long,
  val monthlyAvgBaseIncomePerOrder: Long,
  val locationBalances: List<LocationBalanceDetail>,
  val totalActualBalance: Long,
  val totalDirectSpendBalance: Long,
  val totalJagoBalance: Long,
  val monthlyNetCashflow: Long,
  val transactionEntries: List<TransactionListEntry>,
  val weeklyDaysMondayToSunday: List<DayStatPoint>,
  val weeklyTrendsInMonth: List<WeeklyTrendPoint>,
  val categoryComparisons: List<CategoryComparisonItem>,
  val areaStats: List<AreaStatItem>,
  val unpaidBillsTotal: Long,
  val overdueBillsTotal: Long,
  val paidBillsTotal: Long,
)
