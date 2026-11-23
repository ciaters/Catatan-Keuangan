package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatatanKeuanganDao {
  // --- Reactive Queries (Flow) ---
  @Query("SELECT * FROM work_orders ORDER BY date DESC, startTime ASC")
  fun observeOrders(): Flow<List<WorkOrderEntity>>

  @Query("SELECT * FROM blank_sessions ORDER BY date DESC, startTime ASC")
  fun observeBlanks(): Flow<List<BlankSessionEntity>>

  @Query("SELECT * FROM bonus_rules ORDER BY effectiveDate ASC")
  fun observeBonusRules(): Flow<List<BonusRuleEntity>>

  @Query("SELECT * FROM income_targets ORDER BY effectiveDate ASC")
  fun observeTargets(): Flow<List<IncomeTargetEntity>>

  @Query("SELECT * FROM expense_categories ORDER BY id ASC")
  fun observeCategories(): Flow<List<ExpenseCategoryEntity>>

  @Query("SELECT * FROM expenses ORDER BY insertOrder ASC")
  fun observeExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM transfers ORDER BY insertOrder ASC")
  fun observeTransfers(): Flow<List<TransferEntity>>

  @Query("SELECT name FROM areas ORDER BY name ASC")
  fun observeAreas(): Flow<List<String>>

  @Query("SELECT * FROM wallet_openings")
  fun observeWalletOpenings(): Flow<List<WalletOpeningEntity>>

  @Query("SELECT * FROM balance_corrections ORDER BY date ASC, id ASC")
  fun observeBalanceCorrections(): Flow<List<BalanceCorrectionEntity>>

  @Query("SELECT * FROM bills ORDER BY id ASC")
  fun observeBills(): Flow<List<BillEntity>>

  @Query("SELECT * FROM bill_months ORDER BY billId ASC, monthKey ASC")
  fun observeBillMonths(): Flow<List<BillMonthEntity>>

  @Query("SELECT * FROM app_settings WHERE id = 1")
  fun observeSettings(): Flow<AppSettingsEntity?>

  // --- One-shot helpers ---
  @Query("SELECT * FROM app_settings WHERE id = 1")
  suspend fun getSettingsOnce(): AppSettingsEntity?

  @Query("SELECT * FROM work_orders WHERE id = :orderId LIMIT 1")
  suspend fun getOrderById(orderId: String): WorkOrderEntity?

  // --- Order & Blank Writes ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOrder(order: WorkOrderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOrders(orders: List<WorkOrderEntity>)

  @Query("DELETE FROM work_orders WHERE id = :orderId")
  suspend fun deleteOrderById(orderId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBlank(blank: BlankSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBlanks(blanks: List<BlankSessionEntity>)

  @Query("DELETE FROM blank_sessions WHERE id = :blankId")
  suspend fun deleteBlankById(blankId: String)

  @Query("DELETE FROM blank_sessions WHERE nextOrderId = :orderId")
  suspend fun deleteBlanksForNextOrder(orderId: String)

  // --- Bonus & Target Rules ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBonusRule(rule: BonusRuleEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBonusRules(rules: List<BonusRuleEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTarget(target: IncomeTargetEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTargets(targets: List<IncomeTargetEntity>)

  // --- Categories & Expenses ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategory(category: ExpenseCategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategories(categories: List<ExpenseCategoryEntity>)

  @Query("DELETE FROM expense_categories WHERE id = :categoryId")
  suspend fun deleteCategoryById(categoryId: String)

  @Query("UPDATE expense_categories SET name = :newName WHERE id = :categoryId")
  suspend fun updateCategoryName(categoryId: String, newName: String)

  @Query("UPDATE expenses SET categoryName = :newName WHERE categoryName = :oldName")
  suspend fun updateExpenseCategoryName(oldName: String, newName: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExpense(expense: ExpenseEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExpenses(expenses: List<ExpenseEntity>)

  @Query("DELETE FROM expenses WHERE id = :expenseId")
  suspend fun deleteExpenseById(expenseId: String)

  // --- Transfers ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTransfer(transfer: TransferEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTransfers(transfers: List<TransferEntity>)

  @Query("DELETE FROM transfers WHERE id = :transferId")
  suspend fun deleteTransferById(transferId: String)

  // --- Areas ---
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertArea(area: AreaEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAreas(areas: List<AreaEntity>)

  @Query("DELETE FROM areas WHERE LOWER(name) = LOWER(:areaName)")
  suspend fun deleteAreaIgnoreCase(areaName: String)

  // --- Wallets & Corrections ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWalletOpenings(openings: List<WalletOpeningEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBalanceCorrection(correction: BalanceCorrectionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBalanceCorrections(corrections: List<BalanceCorrectionEntity>)

  // --- Bills ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBills(bills: List<BillEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBillMonth(entry: BillMonthEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBillMonths(entries: List<BillMonthEntity>)

  @Query(
    "UPDATE bill_months SET amount = :newAmount WHERE billId = :billId AND monthKey = :monthKey AND statusName != 'SUDAH_DIBAYAR'"
  )
  suspend fun updateUnpaidBillMonthAmount(billId: String, monthKey: String, newAmount: Long)

  @Query(
    "UPDATE bill_months SET statusName = 'SUDAH_DIBAYAR', paidSourceId = :sourceId, paidDate = :paidDate, linkedExpenseId = :expenseId WHERE billId = :billId AND monthKey = :monthKey AND statusName != 'SUDAH_DIBAYAR'"
  )
  suspend fun markBillMonthPaid(
    billId: String,
    monthKey: String,
    sourceId: String,
    paidDate: String,
    expenseId: String,
  )

  // --- App Settings ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSettings(settings: AppSettingsEntity)

  @Update
  suspend fun updateSettings(settings: AppSettingsEntity)

  @Query("UPDATE app_settings SET nextSequenceId = :nextSeq WHERE id = 1")
  suspend fun updateNextSequenceId(nextSeq: Long)

  // --- Atomic Multi-Table Transactions ---
  @Transaction
  suspend fun saveOrderAndAreas(
    order: WorkOrderEntity,
    newAreas: List<AreaEntity>,
    nextSequenceId: Long?,
  ) {
    upsertOrder(order)
    if (newAreas.isNotEmpty()) insertAreas(newAreas)
    if (nextSequenceId != null) updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun deleteOrderCascade(orderId: String) {
    deleteOrderById(orderId)
    deleteBlanksForNextOrder(orderId)
  }

  @Transaction
  suspend fun saveBlankAndArea(
    blank: BlankSessionEntity,
    newAreas: List<AreaEntity>,
    nextSequenceId: Long,
  ) {
    upsertBlank(blank)
    if (newAreas.isNotEmpty()) insertAreas(newAreas)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun saveExpenseAndSeq(expense: ExpenseEntity, nextSequenceId: Long) {
    upsertExpense(expense)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun saveTransferAndSeq(transfer: TransferEntity, nextSequenceId: Long) {
    upsertTransfer(transfer)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun renameCategoryAndExpenses(categoryId: String, oldName: String, newName: String) {
    updateCategoryName(categoryId, newName)
    updateExpenseCategoryName(oldName, newName)
  }

  @Transaction
  suspend fun saveWalletOpeningBalances(openings: List<WalletOpeningEntity>) {
    upsertWalletOpenings(openings)
    val current = getSettingsOnce()
    if (current != null) {
      upsertSettings(current.copy(isFirstTimeBalanceConfigured = true))
    }
  }

  @Transaction
  suspend fun saveBalanceCorrectionAndSeq(correction: BalanceCorrectionEntity, nextSequenceId: Long) {
    upsertBalanceCorrection(correction)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun payBillAndCreateExpense(
    billId: String,
    monthKey: String,
    sourceId: String,
    paidDate: String,
    expense: ExpenseEntity,
    nextSequenceId: Long,
  ) {
    upsertExpense(expense)
    markBillMonthPaid(billId, monthKey, sourceId, paidDate, expense.id)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun saveTargetAndSeq(target: IncomeTargetEntity, nextSequenceId: Long) {
    upsertTarget(target)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun saveBonusRuleAndSeq(rule: BonusRuleEntity, nextSequenceId: Long) {
    upsertBonusRule(rule)
    updateNextSequenceId(nextSequenceId)
  }

  @Transaction
  suspend fun replaceAreaNames(removeNames: List<String>, addName: String) {
    removeNames.forEach { deleteAreaIgnoreCase(it) }
    insertArea(AreaEntity(addName))
  }

  // --- Pembersihan seluruh tabel untuk pemulihan atomik ---
  @Query("DELETE FROM work_orders")
  suspend fun deleteAllWorkOrders()

  @Query("DELETE FROM blank_sessions")
  suspend fun deleteAllBlankSessions()

  @Query("DELETE FROM bonus_rules")
  suspend fun deleteAllBonusRules()

  @Query("DELETE FROM income_targets")
  suspend fun deleteAllTargets()

  @Query("DELETE FROM expense_categories")
  suspend fun deleteAllCategories()

  @Query("DELETE FROM expenses")
  suspend fun deleteAllExpenses()

  @Query("DELETE FROM transfers")
  suspend fun deleteAllTransfers()

  @Query("DELETE FROM areas")
  suspend fun deleteAllAreas()

  @Query("DELETE FROM wallet_openings")
  suspend fun deleteAllWalletOpenings()

  @Query("DELETE FROM balance_corrections")
  suspend fun deleteAllBalanceCorrections()

  @Query("DELETE FROM bills")
  suspend fun deleteAllBills()

  @Query("DELETE FROM bill_months")
  suspend fun deleteAllBillMonths()

  @Query("DELETE FROM app_settings")
  suspend fun deleteAllSettings()

  @Transaction
  suspend fun seedInitialDatabase(
    settings: AppSettingsEntity,
    bonusRules: List<BonusRuleEntity>,
    targets: List<IncomeTargetEntity>,
    categories: List<ExpenseCategoryEntity>,
    areas: List<AreaEntity>,
    walletOpenings: List<WalletOpeningEntity>,
    corrections: List<BalanceCorrectionEntity>,
    orders: List<WorkOrderEntity>,
    blanks: List<BlankSessionEntity>,
    expenses: List<ExpenseEntity>,
    transfers: List<TransferEntity>,
    bills: List<BillEntity>,
    billMonths: List<BillMonthEntity>,
  ) {
    if (getSettingsOnce() != null) return
    upsertSettings(settings)
    upsertBonusRules(bonusRules)
    upsertTargets(targets)
    upsertCategories(categories)
    insertAreas(areas)
    upsertWalletOpenings(walletOpenings)
    upsertBalanceCorrections(corrections)
    upsertOrders(orders)
    upsertBlanks(blanks)
    upsertExpenses(expenses)
    upsertTransfers(transfers)
    upsertBills(bills)
    upsertBillMonths(billMonths)
  }

  @Transaction
  suspend fun replaceAllDataAtomically(
    settings: AppSettingsEntity,
    bonusRules: List<BonusRuleEntity>,
    targets: List<IncomeTargetEntity>,
    categories: List<ExpenseCategoryEntity>,
    areas: List<AreaEntity>,
    walletOpenings: List<WalletOpeningEntity>,
    corrections: List<BalanceCorrectionEntity>,
    orders: List<WorkOrderEntity>,
    blanks: List<BlankSessionEntity>,
    expenses: List<ExpenseEntity>,
    transfers: List<TransferEntity>,
    bills: List<BillEntity>,
    billMonths: List<BillMonthEntity>,
  ) {
    deleteAllBillMonths()
    deleteAllBills()
    deleteAllBlankSessions()
    deleteAllWorkOrders()
    deleteAllExpenses()
    deleteAllTransfers()
    deleteAllCategories()
    deleteAllAreas()
    deleteAllBalanceCorrections()
    deleteAllWalletOpenings()
    deleteAllBonusRules()
    deleteAllTargets()
    deleteAllSettings()

    upsertSettings(settings)
    if (bonusRules.isNotEmpty()) upsertBonusRules(bonusRules)
    if (targets.isNotEmpty()) upsertTargets(targets)
    if (categories.isNotEmpty()) upsertCategories(categories)
    if (areas.isNotEmpty()) insertAreas(areas)
    if (walletOpenings.isNotEmpty()) upsertWalletOpenings(walletOpenings)
    if (corrections.isNotEmpty()) upsertBalanceCorrections(corrections)
    if (orders.isNotEmpty()) upsertOrders(orders)
    if (blanks.isNotEmpty()) upsertBlanks(blanks)
    if (expenses.isNotEmpty()) upsertExpenses(expenses)
    if (transfers.isNotEmpty()) upsertTransfers(transfers)
    if (bills.isNotEmpty()) upsertBills(bills)
    if (billMonths.isNotEmpty()) upsertBillMonths(billMonths)
  }
}
