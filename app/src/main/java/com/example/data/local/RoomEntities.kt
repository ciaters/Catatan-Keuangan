package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.BalanceCorrection
import com.example.model.BillItem
import com.example.model.BillMonthEntry
import com.example.model.BillStatus
import com.example.model.BlankSession
import com.example.model.BonusRuleRecord
import com.example.model.ExpenseCategory
import com.example.model.ExpenseRecord
import com.example.model.FundLocation
import com.example.model.IncomeTargetRecord
import com.example.model.TransferRecord
import com.example.model.WorkOrder

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
  @PrimaryKey val id: String,
  val date: String,
  val startTime: String,
  val endTime: String,
  val argo: Long,
  val packageCount: Int,
  val tipGoPay: Long,
  val tipCash: Long,
  val pickupDistanceKm: Double,
  val deliveryDistanceKm: Double,
  val originArea: String,
  val destinationArea: String,
  val batteryStart: Int,
  val batteryEnd: Int,
  val note: String,
)

@Entity(tableName = "blank_sessions")
data class BlankSessionEntity(
  @PrimaryKey val id: String,
  val date: String,
  val startTime: String,
  val endTime: String,
  val nextOrderId: String,
  val area: String,
  val batteryStart: Int,
  val batteryEnd: Int,
  val distanceKm: Double,
  val note: String,
)

@Entity(tableName = "bonus_rules")
data class BonusRuleEntity(
  @PrimaryKey val id: String,
  val effectiveDate: String,
  val tier1MaxPackages: Int,
  val tier1Rate: Long,
  val tier2MaxPackages: Int,
  val tier2Rate: Long,
  val tier3MaxPackages: Int,
  val tier3Rate: Long,
  val tier4Rate: Long,
  val note: String,
)

@Entity(tableName = "income_targets")
data class IncomeTargetEntity(
  @PrimaryKey val id: String,
  val effectiveDate: String,
  val dailyTarget: Long,
  val monthlyTarget: Long,
  val note: String,
)

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey val id: String,
  val date: String,
  val categoryName: String,
  val amount: Long,
  val sourceId: String,
  val note: String,
  val insertOrder: Long,
  val linkedBillId: String?,
)

@Entity(tableName = "transfers")
data class TransferEntity(
  @PrimaryKey val id: String,
  val date: String,
  val fromLocationId: String,
  val toLocationId: String,
  val amount: Long,
  val note: String,
  val insertOrder: Long,
)

@Entity(tableName = "areas")
data class AreaEntity(
  @PrimaryKey val name: String,
)

@Entity(tableName = "wallet_openings")
data class WalletOpeningEntity(
  @PrimaryKey val locationId: String,
  val previousMonthClosing: Long,
)

@Entity(tableName = "balance_corrections")
data class BalanceCorrectionEntity(
  @PrimaryKey val id: String,
  val locationId: String,
  val date: String,
  val deltaAmount: Long,
  val note: String,
)

@Entity(tableName = "bills")
data class BillEntity(
  @PrimaryKey val id: String,
  val name: String,
  val categoryNote: String,
)

@Entity(tableName = "bill_months", primaryKeys = ["billId", "monthKey"])
data class BillMonthEntity(
  val billId: String,
  val monthKey: String,
  val monthLabel: String,
  val amount: Long,
  val statusName: String,
  val dueDateLabel: String,
  val paidSourceId: String?,
  val paidDate: String?,
  val linkedExpenseId: String?,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val hideAmounts: Boolean,
  val isFirstTimeBalanceConfigured: Boolean,
  val activeMonthKey: String,
  val nextSequenceId: Long,
  val lastBackupTimestamp: String,
  val lastRestoreTimestamp: String,
  val isGoogleAccountLinkedPreview: Boolean,
  val sheetNamePreview: String,
  val lastBackupSummary: String,
  val backupSpreadsheetId: String = "",
  val linkedGoogleEmail: String = "",
)

// --- Domain <-> Room Mappers ---

fun WorkOrderEntity.toDomain(): WorkOrder =
  WorkOrder(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    argo = argo,
    packageCount = packageCount,
    tipGoPay = tipGoPay,
    tipCash = tipCash,
    pickupDistanceKm = pickupDistanceKm,
    deliveryDistanceKm = deliveryDistanceKm,
    originArea = originArea,
    destinationArea = destinationArea,
    batteryStart = batteryStart,
    batteryEnd = batteryEnd,
    note = note,
  )

fun WorkOrder.toEntity(): WorkOrderEntity =
  WorkOrderEntity(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    argo = argo,
    packageCount = packageCount,
    tipGoPay = tipGoPay,
    tipCash = tipCash,
    pickupDistanceKm = pickupDistanceKm,
    deliveryDistanceKm = deliveryDistanceKm,
    originArea = originArea,
    destinationArea = destinationArea,
    batteryStart = batteryStart,
    batteryEnd = batteryEnd,
    note = note,
  )

fun BlankSessionEntity.toDomain(): BlankSession =
  BlankSession(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    nextOrderId = nextOrderId,
    area = area,
    batteryStart = batteryStart,
    batteryEnd = batteryEnd,
    distanceKm = distanceKm,
    note = note,
  )

fun BlankSession.toEntity(): BlankSessionEntity =
  BlankSessionEntity(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    nextOrderId = nextOrderId,
    area = area,
    batteryStart = batteryStart,
    batteryEnd = batteryEnd,
    distanceKm = distanceKm,
    note = note,
  )

fun BonusRuleEntity.toDomain(): BonusRuleRecord =
  BonusRuleRecord(
    id = id,
    effectiveDate = effectiveDate,
    tier1MaxPackages = tier1MaxPackages,
    tier1Rate = tier1Rate,
    tier2MaxPackages = tier2MaxPackages,
    tier2Rate = tier2Rate,
    tier3MaxPackages = tier3MaxPackages,
    tier3Rate = tier3Rate,
    tier4Rate = tier4Rate,
    note = note,
  )

fun BonusRuleRecord.toEntity(): BonusRuleEntity =
  BonusRuleEntity(
    id = id,
    effectiveDate = effectiveDate,
    tier1MaxPackages = tier1MaxPackages,
    tier1Rate = tier1Rate,
    tier2MaxPackages = tier2MaxPackages,
    tier2Rate = tier2Rate,
    tier3MaxPackages = tier3MaxPackages,
    tier3Rate = tier3Rate,
    tier4Rate = tier4Rate,
    note = note,
  )

fun IncomeTargetEntity.toDomain(): IncomeTargetRecord =
  IncomeTargetRecord(
    id = id,
    effectiveDate = effectiveDate,
    dailyTarget = dailyTarget,
    monthlyTarget = monthlyTarget,
    note = note,
  )

fun IncomeTargetRecord.toEntity(): IncomeTargetEntity =
  IncomeTargetEntity(
    id = id,
    effectiveDate = effectiveDate,
    dailyTarget = dailyTarget,
    monthlyTarget = monthlyTarget,
    note = note,
  )

fun ExpenseCategoryEntity.toDomain(): ExpenseCategory = ExpenseCategory(id = id, name = name)

fun ExpenseCategory.toEntity(): ExpenseCategoryEntity = ExpenseCategoryEntity(id = id, name = name)

fun fundLocationFromId(id: String): FundLocation =
  FundLocation.entries.firstOrNull { it.id == id } ?: FundLocation.GOPAY

fun ExpenseEntity.toDomain(): ExpenseRecord =
  ExpenseRecord(
    id = id,
    date = date,
    categoryName = categoryName,
    amount = amount,
    source = fundLocationFromId(sourceId),
    note = note,
    insertOrder = insertOrder,
    linkedBillId = linkedBillId,
  )

fun ExpenseRecord.toEntity(): ExpenseEntity =
  ExpenseEntity(
    id = id,
    date = date,
    categoryName = categoryName,
    amount = amount,
    sourceId = source.id,
    note = note,
    insertOrder = insertOrder,
    linkedBillId = linkedBillId,
  )

fun TransferEntity.toDomain(): TransferRecord =
  TransferRecord(
    id = id,
    date = date,
    from = fundLocationFromId(fromLocationId),
    to = fundLocationFromId(toLocationId),
    amount = amount,
    note = note,
    insertOrder = insertOrder,
  )

fun TransferRecord.toEntity(): TransferEntity =
  TransferEntity(
    id = id,
    date = date,
    fromLocationId = from.id,
    toLocationId = to.id,
    amount = amount,
    note = note,
    insertOrder = insertOrder,
  )

fun BalanceCorrectionEntity.toDomain(): BalanceCorrection =
  BalanceCorrection(
    id = id,
    location = fundLocationFromId(locationId),
    date = date,
    deltaAmount = deltaAmount,
    note = note,
  )

fun BalanceCorrection.toEntity(): BalanceCorrectionEntity =
  BalanceCorrectionEntity(
    id = id,
    locationId = location.id,
    date = date,
    deltaAmount = deltaAmount,
    note = note,
  )

fun BillMonthEntity.toDomain(): BillMonthEntry =
  BillMonthEntry(
    monthKey = monthKey,
    monthLabel = monthLabel,
    amount = amount,
    status = BillStatus.entries.firstOrNull { it.name == statusName } ?: BillStatus.BELUM_DIBAYAR,
    dueDateLabel = dueDateLabel,
    paidSource = paidSourceId?.let(::fundLocationFromId),
    paidDate = paidDate,
    linkedExpenseId = linkedExpenseId,
  )

fun BillMonthEntry.toEntity(billId: String): BillMonthEntity =
  BillMonthEntity(
    billId = billId,
    monthKey = monthKey,
    monthLabel = monthLabel,
    amount = amount,
    statusName = status.name,
    dueDateLabel = dueDateLabel,
    paidSourceId = paidSource?.id,
    paidDate = paidDate,
    linkedExpenseId = linkedExpenseId,
  )

fun BillEntity.toDomain(months: List<BillMonthEntity>): BillItem =
  BillItem(
    id = id,
    name = name,
    categoryNote = categoryNote,
    history = months.sortedBy { it.monthKey }.map { it.toDomain() },
  )
