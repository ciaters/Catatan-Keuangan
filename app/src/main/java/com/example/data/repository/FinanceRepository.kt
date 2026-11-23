package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.AreaEntity
import com.example.data.local.BalanceCorrectionEntity
import com.example.data.local.BillEntity
import com.example.data.local.CatatanKeuanganDao
import com.example.data.local.ExpenseCategoryEntity
import com.example.data.local.WalletOpeningEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.BalanceCorrection
import com.example.model.BonusRuleRecord
import com.example.model.ExpenseCategory
import com.example.model.ExpenseRecord
import com.example.model.FundLocation
import com.example.model.IncomeTargetRecord
import com.example.model.InitialSampleData
import com.example.model.TransferRecord
import com.example.model.WorkOrder
import com.example.state.BackupPreviewInfo
import com.example.state.FinanceRawState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class FinanceRepository(private val dao: CatatanKeuanganDao) {

  suspend fun ensureDatabaseSeededOnce() {
    if (dao.getSettingsOnce() != null) return

    val defaultSettings =
      AppSettingsEntity(
        id = 1,
        hideAmounts = false,
        isFirstTimeBalanceConfigured = true,
        activeMonthKey = InitialSampleData.ACTIVE_MONTH_KEY,
        nextSequenceId = 100L,
        lastBackupTimestamp = "Belum pernah dicadangkan",
        lastRestoreTimestamp = "-",
        isGoogleAccountLinkedPreview = false,
        sheetNamePreview = "Catatan_Keuangan_Backup_Lokal",
        lastBackupSummary =
          "Data lokal di database Room adalah sumber utama. Tidak ada sinkronisasi otomatis.",
      )

    val walletOpenings =
      FundLocation.entries.map { loc ->
        WalletOpeningEntity(
          locationId = loc.id,
          previousMonthClosing = InitialSampleData.initialPreviousClosingBalances[loc] ?: 0L,
        )
      }

    val billEntities =
      InitialSampleData.initialBills.map { bill ->
        BillEntity(id = bill.id, name = bill.name, categoryNote = bill.categoryNote)
      }
    val billMonths =
      InitialSampleData.initialBills.flatMap { bill ->
        bill.history.map { entry -> entry.toEntity(bill.id) }
      }

    dao.seedInitialDatabase(
      settings = defaultSettings,
      bonusRules = listOf(BonusRuleRecord.DEFAULT.toEntity()),
      targets = InitialSampleData.initialTargetHistory.map { it.toEntity() },
      categories = InitialSampleData.initialCategories.map { it.toEntity() },
      areas = InitialSampleData.initialAreas.map { AreaEntity(it) },
      walletOpenings = walletOpenings,
      corrections = InitialSampleData.initialBalanceCorrections.map { it.toEntity() },
      orders = InitialSampleData.initialOrders.map { it.toEntity() },
      blanks = InitialSampleData.initialBlanks.map { it.toEntity() },
      expenses = InitialSampleData.initialExpenses.map { it.toEntity() },
      transfers = InitialSampleData.initialTransfers.map { it.toEntity() },
      bills = billEntities,
      billMonths = billMonths,
    )
  }

  fun observeRawState(todayDateProvider: () -> String): Flow<FinanceRawState?> {
    val activityGroup =
      combine(
        dao.observeOrders(),
        dao.observeBlanks(),
        dao.observeBonusRules(),
        dao.observeTargets(),
      ) { orders, blanks, bonusRules, targets ->
        ActivityTuple(orders, blanks, bonusRules, targets)
      }

    val ledgerGroup =
      combine(
        dao.observeExpenses(),
        dao.observeTransfers(),
        dao.observeCategories(),
        dao.observeAreas(),
      ) { expenses, transfers, categories, areas ->
        LedgerTuple(expenses, transfers, categories, areas)
      }

    val walletAndBillGroup =
      combine(
        dao.observeWalletOpenings(),
        dao.observeBalanceCorrections(),
        dao.observeBills(),
        dao.observeBillMonths(),
        dao.observeSettings(),
      ) { openings, corrections, bills, billMonths, settings ->
        WalletBillTuple(openings, corrections, bills, billMonths, settings)
      }

    return combine(activityGroup, ledgerGroup, walletAndBillGroup) { act, ledger, wb ->
      val settings = wb.settings ?: return@combine null
      val openingsMap =
        FundLocation.entries.associateWith { loc ->
          wb.openings.firstOrNull { it.locationId == loc.id }?.previousMonthClosing ?: 0L
        }
      val billMonthsByBillId = wb.billMonths.groupBy { it.billId }
      val billsDomain =
        wb.bills.map { billEntity ->
          billEntity.toDomain(billMonthsByBillId[billEntity.id].orEmpty())
        }

      FinanceRawState(
        hideAmounts = settings.hideAmounts,
        isFirstTimeBalanceConfigured = settings.isFirstTimeBalanceConfigured,
        orders = act.orders.map { it.toDomain() },
        blanks = act.blanks.map { it.toDomain() },
        bonusRuleHistory =
          act.bonusRules.map { it.toDomain() }.ifEmpty { listOf(BonusRuleRecord.DEFAULT) },
        expenses = ledger.expenses.map { it.toDomain() },
        transfers = ledger.transfers.map { it.toDomain() },
        categories = ledger.categories.map { it.toDomain() },
        areas = ledger.areas,
        previousClosingBalances = openingsMap,
        balanceCorrections = wb.corrections.map { it.toDomain() },
        bills = billsDomain,
        targetHistory = act.targets.map { it.toDomain() },
        activeMonthKey = settings.activeMonthKey,
        todayDate = todayDateProvider(),
        backupInfo =
          BackupPreviewInfo(
            lastBackupTimestamp = settings.lastBackupTimestamp,
            lastRestoreTimestamp = settings.lastRestoreTimestamp,
            isGoogleAccountLinkedPreview = settings.isGoogleAccountLinkedPreview,
            sheetNamePreview = settings.sheetNamePreview,
            lastBackupSummary = settings.lastBackupSummary,
            backupSpreadsheetId = settings.backupSpreadsheetId,
            linkedGoogleEmail = settings.linkedGoogleEmail,
          ),
        nextSequenceId = settings.nextSequenceId,
      )
    }.distinctUntilChanged()
  }

  suspend fun saveSettings(raw: FinanceRawState) {
    dao.upsertSettings(
      AppSettingsEntity(
        id = 1,
        hideAmounts = raw.hideAmounts,
        isFirstTimeBalanceConfigured = raw.isFirstTimeBalanceConfigured,
        activeMonthKey = raw.activeMonthKey,
        nextSequenceId = raw.nextSequenceId,
        lastBackupTimestamp = raw.backupInfo.lastBackupTimestamp,
        lastRestoreTimestamp = raw.backupInfo.lastRestoreTimestamp,
        isGoogleAccountLinkedPreview = raw.backupInfo.isGoogleAccountLinkedPreview,
        sheetNamePreview = raw.backupInfo.sheetNamePreview,
        lastBackupSummary = raw.backupInfo.lastBackupSummary,
        backupSpreadsheetId = raw.backupInfo.backupSpreadsheetId,
        linkedGoogleEmail = raw.backupInfo.linkedGoogleEmail,
      )
    )
  }

  suspend fun replaceAllDataAtomically(restored: FinanceRawState) {
    val billEntities =
      restored.bills.map { bill ->
        BillEntity(id = bill.id, name = bill.name, categoryNote = bill.categoryNote)
      }
    val billMonths =
      restored.bills.flatMap { bill ->
        bill.history.map { entry -> entry.toEntity(bill.id) }
      }
    val walletOpenings =
      FundLocation.entries.map { loc ->
        WalletOpeningEntity(
          locationId = loc.id,
          previousMonthClosing = restored.previousClosingBalances[loc] ?: 0L,
        )
      }
    val settings =
      AppSettingsEntity(
        id = 1,
        hideAmounts = restored.hideAmounts,
        isFirstTimeBalanceConfigured = restored.isFirstTimeBalanceConfigured,
        activeMonthKey = restored.activeMonthKey,
        nextSequenceId = restored.nextSequenceId,
        lastBackupTimestamp = restored.backupInfo.lastBackupTimestamp,
        lastRestoreTimestamp = restored.backupInfo.lastRestoreTimestamp,
        isGoogleAccountLinkedPreview = restored.backupInfo.isGoogleAccountLinkedPreview,
        sheetNamePreview = restored.backupInfo.sheetNamePreview,
        lastBackupSummary = restored.backupInfo.lastBackupSummary,
        backupSpreadsheetId = restored.backupInfo.backupSpreadsheetId,
        linkedGoogleEmail = restored.backupInfo.linkedGoogleEmail,
      )

    dao.replaceAllDataAtomically(
      settings = settings,
      bonusRules = restored.bonusRuleHistory.map { it.toEntity() },
      targets = restored.targetHistory.map { it.toEntity() },
      categories = restored.categories.map { it.toEntity() },
      areas = restored.areas.map { AreaEntity(it) },
      walletOpenings = walletOpenings,
      corrections = restored.balanceCorrections.map { it.toEntity() },
      orders = restored.orders.map { it.toEntity() },
      blanks = restored.blanks.map { it.toEntity() },
      expenses = restored.expenses.map { it.toEntity() },
      transfers = restored.transfers.map { it.toEntity() },
      bills = billEntities,
      billMonths = billMonths,
    )
  }

  suspend fun addOrder(order: WorkOrder, newAreas: List<String>, nextSeq: Long) {
    dao.saveOrderAndAreas(
      order = order.toEntity(),
      newAreas = newAreas.filter { it.isNotBlank() }.map { AreaEntity(it) },
      nextSequenceId = nextSeq,
    )
  }

  suspend fun updateOrder(order: WorkOrder, newAreas: List<String>) {
    dao.saveOrderAndAreas(
      order = order.toEntity(),
      newAreas = newAreas.filter { it.isNotBlank() }.map { AreaEntity(it) },
      nextSequenceId = null,
    )
  }

  suspend fun deleteOrder(orderId: String) {
    dao.deleteOrderCascade(orderId)
  }

  suspend fun addBlank(
    blank: com.example.model.BlankSession,
    newArea: String,
    nextSeq: Long,
  ) {
    dao.saveBlankAndArea(
      blank = blank.toEntity(),
      newAreas = if (newArea.isNotBlank()) listOf(AreaEntity(newArea)) else emptyList(),
      nextSequenceId = nextSeq,
    )
  }

  suspend fun deleteBlank(blankId: String) {
    dao.deleteBlankById(blankId)
  }

  suspend fun addExpense(expense: ExpenseRecord, nextSeq: Long) {
    dao.saveExpenseAndSeq(expense.toEntity(), nextSeq)
  }

  suspend fun updateExpense(expense: ExpenseRecord) {
    dao.upsertExpense(expense.toEntity())
  }

  suspend fun deleteExpense(expenseId: String) {
    dao.deleteExpenseById(expenseId)
  }

  suspend fun addTransfer(transfer: TransferRecord, nextSeq: Long) {
    dao.saveTransferAndSeq(transfer.toEntity(), nextSeq)
  }

  suspend fun addCategory(category: ExpenseCategory, nextSeq: Long) {
    dao.upsertCategory(category.toEntity())
    dao.updateNextSequenceId(nextSeq)
  }

  suspend fun renameCategory(categoryId: String, oldName: String, newName: String) {
    dao.renameCategoryAndExpenses(categoryId, oldName, newName)
  }

  suspend fun deleteCategory(categoryId: String) {
    dao.deleteCategoryById(categoryId)
  }

  suspend fun setInitialActualBalances(balances: Map<FundLocation, Long>) {
    val openings =
      FundLocation.entries.map { loc ->
        WalletOpeningEntity(loc.id, balances[loc] ?: 0L)
      }
    dao.saveWalletOpeningBalances(openings)
  }

  suspend fun addBalanceCorrection(correction: BalanceCorrection, nextSeq: Long) {
    dao.saveBalanceCorrectionAndSeq(correction.toEntity(), nextSeq)
  }

  suspend fun updateBillAmountForMonth(billId: String, monthKey: String, newAmount: Long) {
    dao.updateUnpaidBillMonthAmount(billId, monthKey, newAmount)
  }

  suspend fun payBill(
    billId: String,
    monthKey: String,
    source: FundLocation,
    paidDate: String,
    expense: ExpenseRecord,
    nextSeq: Long,
  ) {
    dao.payBillAndCreateExpense(
      billId = billId,
      monthKey = monthKey,
      sourceId = source.id,
      paidDate = paidDate,
      expense = expense.toEntity(),
      nextSequenceId = nextSeq,
    )
  }

  suspend fun addTargetChange(target: IncomeTargetRecord, nextSeq: Long) {
    dao.saveTargetAndSeq(target.toEntity(), nextSeq)
  }

  suspend fun addBonusRuleChange(rule: BonusRuleRecord, nextSeq: Long) {
    dao.saveBonusRuleAndSeq(rule.toEntity(), nextSeq)
  }

  suspend fun addArea(name: String) {
    dao.insertArea(AreaEntity(name))
  }

  suspend fun renameArea(oldName: String, newName: String) {
    dao.replaceAreaNames(listOf(oldName), newName)
  }

  suspend fun deleteArea(areaName: String) {
    dao.deleteAreaIgnoreCase(areaName)
  }

  suspend fun mergeAreas(sourceAreas: Set<String>, targetArea: String) {
    dao.replaceAreaNames(sourceAreas.toList(), targetArea)
  }

  private data class ActivityTuple(
    val orders: List<com.example.data.local.WorkOrderEntity>,
    val blanks: List<com.example.data.local.BlankSessionEntity>,
    val bonusRules: List<com.example.data.local.BonusRuleEntity>,
    val targets: List<com.example.data.local.IncomeTargetEntity>,
  )

  private data class LedgerTuple(
    val expenses: List<com.example.data.local.ExpenseEntity>,
    val transfers: List<com.example.data.local.TransferEntity>,
    val categories: List<com.example.data.local.ExpenseCategoryEntity>,
    val areas: List<String>,
  )

  private data class WalletBillTuple(
    val openings: List<WalletOpeningEntity>,
    val corrections: List<BalanceCorrectionEntity>,
    val bills: List<BillEntity>,
    val billMonths: List<com.example.data.local.BillMonthEntity>,
    val settings: AppSettingsEntity?,
  )
}
