package com.example.model

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class FundLocation(
  val id: String,
  val displayName: String,
  val shortName: String,
  val subtitle: String,
  val isDirectSpendAllowed: Boolean,
  val isJagoPocket: Boolean,
) {
  GOPAY(
    id = "gopay",
    displayName = "GoPay",
    shortName = "GoPay",
    subtitle = "Menerima Argo, Bonus & Tip GoPay • Bisa pengeluaran langsung",
    isDirectSpendAllowed = true,
    isJagoPocket = false,
  ),
  CASH(
    id = "cash",
    displayName = "Cash",
    shortName = "Cash",
    subtitle = "Menerima Tip Cash • Bisa pengeluaran langsung",
    isDirectSpendAllowed = true,
    isJagoPocket = false,
  ),
  JAGO_CICILAN(
    id = "jago_cicilan",
    displayName = "Jago Tagihan",
    shortName = "Jago Tagihan",
    subtitle = "Bank Jago • Dana khusus tagihan & cicilan (transfer dulu sebelum bayar)",
    isDirectSpendAllowed = false,
    isJagoPocket = true,
  ),
  JAGO_PRIBADI(
    id = "jago_pribadi",
    displayName = "Jago Tabungan",
    shortName = "Jago Tabungan",
    subtitle = "Bank Jago • Dana tabungan pribadi (transfer dulu sebelum bayar)",
    isDirectSpendAllowed = false,
    isJagoPocket = true,
  );

  companion object {
    val directSpendLocations = listOf(GOPAY, CASH)
    val jagoLocations = listOf(JAGO_CICILAN, JAGO_PRIBADI)
  }
}

data class IncomeTargetRecord(
  val id: String,
  val effectiveDate: String, // YYYY-MM-DD
  val dailyTarget: Long,
  val monthlyTarget: Long,
  val note: String,
)

data class BonusRuleRecord(
  val id: String,
  val effectiveDate: String, // YYYY-MM-DD
  val tier1MaxPackages: Int, // 9 -> 1..9 pkt
  val tier1Rate: Long, // 1.250
  val tier2MaxPackages: Int, // 14 -> 10..14 pkt
  val tier2Rate: Long, // 2.250
  val tier3MaxPackages: Int, // 20 -> 15..20 pkt
  val tier3Rate: Long, // 3.000
  val tier4Rate: Long, // >20 pkt -> 3.500
  val note: String,
) {
  companion object {
    val DEFAULT =
      BonusRuleRecord(
        id = "bonus_default",
        effectiveDate = "2026-01-01",
        tier1MaxPackages = 9,
        tier1Rate = 1_250L,
        tier2MaxPackages = 14,
        tier2Rate = 2_250L,
        tier3MaxPackages = 20,
        tier3Rate = 3_000L,
        tier4Rate = 3_500L,
        note = "Aturan bonus paket standar",
      )
  }
}

data class WorkOrder(
  val id: String,
  val date: String, // YYYY-MM-DD (tanggal mulai order, termasuk lintas tengah malam)
  val startTime: String, // HH:mm
  val endTime: String, // HH:mm
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
  val note: String = "",
)

data class ComputedWorkOrder(
  val order: WorkOrder,
  val orderNumberOnDate: Int,
  val durationMinutes: Int,
  val crossesMidnight: Boolean,
  val batteryConsumed: Int,
  val totalDistanceKm: Double,
  val cumulativePackagesOnDate: Int,
  val cumulativeBonusOnDate: Long,
  val bonusTierRate: Long,
  val totalCashTipOnOrder: Long,
  val cumulativeDailyIncomeOnDate: Long,
  val dailyTargetOnDate: Long,
)

data class BlankSession(
  val id: String,
  val date: String, // YYYY-MM-DD
  val startTime: String, // HH:mm
  val endTime: String, // HH:mm (berakhir pada waktu mulai order berikutnya)
  val nextOrderId: String,
  val area: String,
  val batteryStart: Int,
  val batteryEnd: Int,
  val distanceKm: Double, // termasuk 0.0
  val note: String = "",
)

data class ComputedBlankSession(
  val blank: BlankSession,
  val durationMinutes: Int,
  val batteryConsumed: Int,
)

sealed interface IncomeActivityItem {
  val id: String
  val date: String
  val startTime: String

  data class OrderEntry(val data: ComputedWorkOrder) : IncomeActivityItem {
    override val id: String = data.order.id
    override val date: String = data.order.date
    override val startTime: String = data.order.startTime
  }

  data class BlankEntry(val data: ComputedBlankSession) : IncomeActivityItem {
    override val id: String = data.blank.id
    override val date: String = data.blank.date
    override val startTime: String = data.blank.startTime
  }
}

data class DailyIncomeSummary(
  val date: String,
  val orderCount: Int,
  val totalPackages: Int,
  val totalArgo: Long,
  val totalBonus: Long, // Nilai kumulatif terakhir (bukan penjumlahan tiap order)
  val totalTipGoPay: Long,
  val totalTipCash: Long,
  val totalIncome: Long,
  val orderDurationMinutes: Int,
  val blankDurationMinutes: Int,
  val totalActivityDurationMinutes: Int, // order + blank
  val orderDistanceKm: Double,
  val blankDistanceKm: Double,
  val totalDistanceKm: Double,
  val orderBatteryConsumed: Int,
  val blankBatteryConsumed: Int,
  val totalBatteryConsumed: Int,
)

data class ExpenseCategory(
  val id: String,
  val name: String,
)

data class ExpenseRecord(
  val id: String,
  val date: String, // YYYY-MM-DD (tanpa jam)
  val categoryName: String,
  val amount: Long,
  val source: FundLocation, // Hanya GOPAY atau CASH
  val note: String = "",
  val insertOrder: Long,
  val linkedBillId: String? = null,
)

data class TransferRecord(
  val id: String,
  val date: String, // YYYY-MM-DD (tanpa jam)
  val from: FundLocation,
  val to: FundLocation,
  val amount: Long,
  val note: String = "",
  val insertOrder: Long,
)

data class GroupedExpenseDisplay(
  val groupKey: String,
  val date: String,
  val categoryName: String,
  val totalAmount: Long,
  val firstInsertOrder: Long,
  val items: List<ExpenseRecord>,
)

sealed interface TransactionListEntry {
  val date: String
  val sortPriority: Int
  val orderKey: Long

  data class DailyIncomeRow(
    override val date: String,
    val summary: DailyIncomeSummary,
  ) : TransactionListEntry {
    override val sortPriority: Int = 0
    override val orderKey: Long = 0L
  }

  data class GroupedExpenseRow(
    val group: GroupedExpenseDisplay,
  ) : TransactionListEntry {
    override val date: String = group.date
    override val sortPriority: Int = 1
    override val orderKey: Long = group.firstInsertOrder
  }

  data class TransferRow(
    val transfer: TransferRecord,
  ) : TransactionListEntry {
    override val date: String = transfer.date
    override val sortPriority: Int = 2
    override val orderKey: Long = transfer.insertOrder
  }
}

data class DailyTransactionSummary(
  val date: String,
  val totalIncome: Long,
  val totalExpense: Long,
  val difference: Long,
  val totalTransfer: Long,
)

data class BalanceCorrection(
  val id: String,
  val location: FundLocation,
  val date: String,
  val deltaAmount: Long,
  val note: String,
)

data class LocationBalanceDetail(
  val location: FundLocation,
  val previousMonthClosing: Long,
  val openingCorrectionDelta: Long,
  val effectiveOpeningBalance: Long,
  val incomeIn: Long,
  val expenseOut: Long,
  val transferIn: Long,
  val transferOut: Long,
  val netTransfer: Long,
  val runningBalance: Long,
  val monthlyChange: Long,
  val corrections: List<BalanceCorrection>,
)

enum class BillStatus(val label: String) {
  BELUM_DIBAYAR("Belum dibayar"),
  SUDAH_DIBAYAR("Sudah dibayar"),
  TERLAMBAT("Terlambat"),
}

data class BillMonthEntry(
  val monthKey: String, // YYYY-MM
  val monthLabel: String,
  val amount: Long,
  val status: BillStatus,
  val dueDateLabel: String,
  val paidSource: FundLocation? = null,
  val paidDate: String? = null,
  val linkedExpenseId: String? = null,
)

data class BillItem(
  val id: String,
  val name: String,
  val categoryNote: String,
  val history: List<BillMonthEntry>,
)

data class DayStatPoint(
  val dayIndexMondayFirst: Int, // 0 = Senin .. 6 = Minggu
  val dayShortLabel: String,
  val dayFullLabel: String,
  val date: String,
  val isSunday: Boolean,
  val income: Long,
  val orderCount: Int,
  val durationMinutes: Int,
  val distanceKm: Double,
  val batteryConsumed: Int,
)

data class WeeklyTrendPoint(
  val weekLabel: String,
  val dateRangeLabel: String,
  val income: Long,
  val orderCount: Int,
)

data class AreaStatItem(
  val areaName: String,
  val orderCount: Int,
  val totalIncome: Long,
  val avgIncomePerOrder: Long,
  val waitBlankMinutes: Int,
  val distanceKm: Double,
  val batteryConsumed: Int,
)

data class CategoryComparisonItem(
  val categoryName: String,
  val currentAmount: Long,
  val previousAmount: Long,
  val diffRupiah: Long,
  val diffPercentLabel: String, // "-" jika previousAmount == 0
)

data class MonthlyCategoryExpenseItem(
  val categoryName: String,
  val totalAmount: Long,
  val percentage: Double,
  val percentageLabel: String,
)

data class MonthlyExpenseSummary(
  val monthKey: String,
  val monthLabel: String,
  val totalExpense: Long,
  val categories: List<MonthlyCategoryExpenseItem>,
)

data class MonthlyIncomeSummary(
  val monthKey: String,
  val monthLabel: String,
  val totalIncome: Long,
  val totalArgo: Long,
  val totalBonus: Long,
  val totalTipGoPay: Long,
  val totalTipCash: Long,
  val productiveDays: Int,
  val avgIncomePerProductiveDay: Long,
  val estimated26DaysIncome: Long,
  val dailyTargetAccumulation: Long,
  val dailyTargetDiff: Long,
  val monthlyTarget: Long,
  val remainingTarget: Long,
  val targetProgressFraction: Float,
  val targetProgressPercent: Int,
)

object FinanceCalculator {
  private val rupiahFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))

  fun formatRupiah(amount: Long, hideAmounts: Boolean = false, withPrefix: Boolean = true): String {
    if (hideAmounts) return if (withPrefix) "Rp •••••••" else "•••••••"
    val absFormatted = rupiahFormat.format(kotlin.math.abs(amount))
    val sign = if (amount < 0) "-" else ""
    return if (withPrefix) "${sign}Rp$absFormatted" else "$sign$absFormatted"
  }

  fun formatSignedRupiah(amount: Long, hideAmounts: Boolean = false): String {
    if (hideAmounts) return "Rp •••••••"
    val prefix = if (amount > 0) "+" else if (amount < 0) "-" else ""
    val absFormatted = rupiahFormat.format(kotlin.math.abs(amount))
    return "${prefix}Rp$absFormatted"
  }

  fun formatDifferenceStatus(diff: Long, hideAmounts: Boolean = false): String {
    if (hideAmounts) return "Rp •••••••"
    return when {
      diff > 0L -> "Plus ${formatRupiah(diff, false)}"
      diff < 0L -> "Minus ${formatRupiah(-diff, false)}"
      else -> "Impas ${formatRupiah(0L, false)}"
    }
  }

  fun formatDistance(km: Double): String {
    val rounded = kotlin.math.round(km * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
      "${rounded.toInt()} km"
    } else {
      String.format(Locale.forLanguageTag("id-ID"), "%.1f km", rounded)
    }
  }

  fun formatBatteryEfficiency(distanceKm: Double, batteryUsedPercent: Int): String {
    if (batteryUsedPercent <= 0) return "-"
    val eff = distanceKm / batteryUsedPercent.toDouble()
    val rounded2 = kotlin.math.round(eff * 100.0) / 100.0
    val rounded1 = kotlin.math.round(eff * 10.0) / 10.0
    return when {
      rounded2 % 1.0 == 0.0 -> "${rounded2.toInt()} km/%"
      kotlin.math.abs(rounded2 - rounded1) < 0.001 ->
        String.format(Locale.forLanguageTag("id-ID"), "%.1f km/%%", rounded1)
      else -> String.format(Locale.forLanguageTag("id-ID"), "%.2f km/%%", rounded2)
    }
  }

  fun formatDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
      h > 0 && m > 0 -> "${h}j ${m}m"
      h > 0 -> "${h} jam"
      else -> "${m} mnt"
    }
  }

  fun parseTimeMinutes(hhmm: String): Int {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: 0
    val m = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h * 60 + m
  }

  fun computeDurationMinutes(startTime: String, endTime: String): Pair<Int, Boolean> {
    val start = parseTimeMinutes(startTime)
    val end = parseTimeMinutes(endTime)
    return if (end < start) {
      ((24 * 60 - start) + end) to true
    } else {
      (end - start) to false
    }
  }

  fun bonusRuleForDate(date: String, history: List<BonusRuleRecord>): BonusRuleRecord {
    if (history.isEmpty()) return BonusRuleRecord.DEFAULT
    val sorted = history.sortedBy { it.effectiveDate }
    return sorted.lastOrNull { it.effectiveDate <= date } ?: sorted.first()
  }

  fun bonusRatePerPackage(
    cumulativePackages: Int,
    rule: BonusRuleRecord = BonusRuleRecord.DEFAULT,
  ): Long =
    when {
      cumulativePackages <= 0 -> 0L
      cumulativePackages <= rule.tier1MaxPackages -> rule.tier1Rate
      cumulativePackages <= rule.tier2MaxPackages -> rule.tier2Rate
      cumulativePackages <= rule.tier3MaxPackages -> rule.tier3Rate
      else -> rule.tier4Rate
    }

  fun calculateCumulativeBonus(
    cumulativePackages: Int,
    rule: BonusRuleRecord = BonusRuleRecord.DEFAULT,
  ): Long =
    cumulativePackages.coerceAtLeast(0) * bonusRatePerPackage(cumulativePackages, rule)

  fun normalizeAreaName(raw: String): String {
    val collapsed = raw.trim().replace(Regex("\\s+"), " ")
    if (collapsed.isEmpty()) return ""
    return collapsed.split(" ").joinToString(" ") { token ->
      token.lowercase(Locale.forLanguageTag("id-ID")).replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.forLanguageTag("id-ID")) else ch.toString()
      }
    }
  }

  fun formatDateIndo(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    val monthNames =
      listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
      )
    val mLabel = monthNames.getOrElse(month - 1) { parts[1] }
    return "$day $mLabel $year"
  }

  fun formatMonthIndo(monthKey: String): String {
    val parts = monthKey.split("-")
    if (parts.size != 2) return monthKey
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return monthKey
    val fullMonthNames =
      listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
      )
    val mLabel = fullMonthNames.getOrElse(month - 1) { parts[1] }
    return "$mLabel $year"
  }

  fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
    val parts = monthKey.split("-")
    if (parts.size != 2) return monthKey
    val year = parts[0].toIntOrNull() ?: return monthKey
    val month = parts[1].toIntOrNull() ?: return monthKey
    val totalMonths = (year * 12 + (month - 1)) + deltaMonths
    val targetYear = Math.floorDiv(totalMonths, 12)
    val targetMonth = Math.floorMod(totalMonths, 12) + 1
    return String.format(Locale.US, "%04d-%02d", targetYear, targetMonth)
  }

  fun formatDateWithDayIndo(isoDate: String): String {
    val cal = parseCalendar(isoDate) ?: return formatDateIndo(isoDate)
    val dayName =
      when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Sen"
        Calendar.TUESDAY -> "Sel"
        Calendar.WEDNESDAY -> "Rab"
        Calendar.THURSDAY -> "Kam"
        Calendar.FRIDAY -> "Jum"
        Calendar.SATURDAY -> "Sab"
        Calendar.SUNDAY -> "Min"
        else -> ""
      }
    return "$dayName, ${formatDateIndo(isoDate)}"
  }

  fun dayOfWeekMondayFirst(isoDate: String): Int {
    val cal = parseCalendar(isoDate) ?: return 0
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> 0
      Calendar.TUESDAY -> 1
      Calendar.WEDNESDAY -> 2
      Calendar.THURSDAY -> 3
      Calendar.FRIDAY -> 4
      Calendar.SATURDAY -> 5
      Calendar.SUNDAY -> 6
      else -> 0
    }
  }

  fun parseCalendar(isoDate: String): Calendar? {
    val parts = isoDate.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance().apply {
      clear()
      set(Calendar.YEAR, y)
      set(Calendar.MONTH, m - 1)
      set(Calendar.DAY_OF_MONTH, d)
    }
  }

  fun currentSystemIsoDate(): String {
    val cal = Calendar.getInstance()
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
    val cal = parseCalendar(isoDate) ?: return isoDate
    cal.add(Calendar.DAY_OF_MONTH, deltaDays)
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun isoDateToUtcMillis(isoDate: String): Long? {
    val parts = isoDate.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(Calendar.YEAR, y)
      set(Calendar.MONTH, m - 1)
      set(Calendar.DAY_OF_MONTH, d)
    }.timeInMillis
  }

  fun utcMillisToIsoDate(utcMillis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun computeDailyTransactionSummary(
    date: String,
    dailySummariesByDate: Map<String, DailyIncomeSummary>,
    expenses: List<ExpenseRecord>,
    transfers: List<TransferRecord>,
  ): DailyTransactionSummary {
    val totalIncome = dailySummariesByDate[date]?.totalIncome ?: 0L
    val totalExpense = expenses.filter { it.date == date }.sumOf { it.amount }
    val totalTransfer = transfers.filter { it.date == date }.sumOf { it.amount }
    return DailyTransactionSummary(
      date = date,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      difference = totalIncome - totalExpense,
      totalTransfer = totalTransfer,
    )
  }

  fun computeOrdersForDate(
    ordersOnDate: List<WorkOrder>,
    dailyTarget: Long = 250_000L,
    bonusRule: BonusRuleRecord = BonusRuleRecord.DEFAULT,
  ): List<ComputedWorkOrder> {
    val sorted = ordersOnDate.sortedBy { parseTimeMinutes(it.startTime) }
    var runningPackages = 0
    var runningArgo = 0L
    var runningTipGoPay = 0L
    var runningTipCash = 0L
    return sorted.mapIndexed { index, order ->
      runningPackages += order.packageCount.coerceAtLeast(0)
      val rate = bonusRatePerPackage(runningPackages, bonusRule)
      val cumBonus = runningPackages * rate

      runningArgo += order.argo
      runningTipGoPay += order.tipGoPay
      runningTipCash += order.tipCash

      val cumDailyIncome = runningArgo + cumBonus + runningTipGoPay + runningTipCash
      val (dur, crosses) = computeDurationMinutes(order.startTime, order.endTime)
      ComputedWorkOrder(
        order = order,
        orderNumberOnDate = index + 1,
        durationMinutes = dur,
        crossesMidnight = crosses,
        batteryConsumed = (order.batteryStart - order.batteryEnd).coerceAtLeast(0),
        totalDistanceKm = order.pickupDistanceKm + order.deliveryDistanceKm,
        cumulativePackagesOnDate = runningPackages,
        cumulativeBonusOnDate = cumBonus,
        bonusTierRate = rate,
        totalCashTipOnOrder = order.tipCash,
        cumulativeDailyIncomeOnDate = cumDailyIncome,
        dailyTargetOnDate = dailyTarget,
      )
    }
  }

  fun computeDailySummary(
    date: String,
    computedOrders: List<ComputedWorkOrder>,
    computedBlanks: List<ComputedBlankSession>,
  ): DailyIncomeSummary {
    val totalArgo = computedOrders.sumOf { it.order.argo }
    val totalBonus = computedOrders.lastOrNull()?.cumulativeBonusOnDate ?: 0L
    val totalTipGoPay = computedOrders.sumOf { it.order.tipGoPay }
    val totalTipCash = computedOrders.sumOf { it.order.tipCash }
    val orderDur = computedOrders.sumOf { it.durationMinutes }
    val blankDur = computedBlanks.sumOf { it.durationMinutes }
    val orderDist = computedOrders.sumOf { it.totalDistanceKm }
    val blankDist = computedBlanks.sumOf { it.blank.distanceKm }
    val orderBatt = computedOrders.sumOf { it.batteryConsumed }
    val blankBatt = computedBlanks.sumOf { it.batteryConsumed }
    return DailyIncomeSummary(
      date = date,
      orderCount = computedOrders.size,
      totalPackages = computedOrders.sumOf { it.order.packageCount },
      totalArgo = totalArgo,
      totalBonus = totalBonus,
      totalTipGoPay = totalTipGoPay,
      totalTipCash = totalTipCash,
      totalIncome = totalArgo + totalBonus + totalTipGoPay + totalTipCash,
      orderDurationMinutes = orderDur,
      blankDurationMinutes = blankDur,
      totalActivityDurationMinutes = orderDur + blankDur,
      orderDistanceKm = orderDist,
      blankDistanceKm = blankDist,
      totalDistanceKm = orderDist + blankDist,
      orderBatteryConsumed = orderBatt,
      blankBatteryConsumed = blankBatt,
      totalBatteryConsumed = orderBatt + blankBatt,
    )
  }

  fun dailyTargetForDate(date: String, history: List<IncomeTargetRecord>): Long {
    val sorted = history.sortedBy { it.effectiveDate }
    return sorted.lastOrNull { it.effectiveDate <= date }?.dailyTarget
      ?: sorted.firstOrNull()?.dailyTarget
      ?: 250_000L
  }

  fun monthlyTargetForMonth(monthPrefix: String, history: List<IncomeTargetRecord>): Long {
    val sorted = history.sortedBy { it.effectiveDate }
    val endOfMonth = "$monthPrefix-31"
    return sorted.lastOrNull { it.effectiveDate <= endOfMonth }?.monthlyTarget
      ?: sorted.firstOrNull()?.monthlyTarget
      ?: 6_000_000L
  }

  fun computeMonthlyIncomeSummary(
    monthKey: String,
    dailySummariesByDate: Map<String, DailyIncomeSummary>,
    targetHistory: List<IncomeTargetRecord>,
  ): MonthlyIncomeSummary {
    val monthSummaries = dailySummariesByDate.filterKeys { it.startsWith(monthKey) }.values.toList()
    val productiveSummaries = monthSummaries.filter { it.orderCount > 0 && it.totalIncome > 0L }

    val totalIncome = productiveSummaries.sumOf { it.totalIncome }
    val totalArgo = monthSummaries.sumOf { it.totalArgo }
    val totalBonus = monthSummaries.sumOf { it.totalBonus }
    val totalTipGoPay = monthSummaries.sumOf { it.totalTipGoPay }
    val totalTipCash = monthSummaries.sumOf { it.totalTipCash }
    val productiveDays = productiveSummaries.size
    val avgPerDay = if (productiveDays > 0) totalIncome / productiveDays else 0L
    val estimated26 = avgPerDay * 26L
    val dailyTargetAccum =
      productiveSummaries.sumOf { dailyTargetForDate(it.date, targetHistory) }
    val dailyTargetDiff = totalIncome - dailyTargetAccum
    val monthlyTarget = monthlyTargetForMonth(monthKey, targetHistory)
    val remainingTarget = monthlyTarget - totalIncome
    val progressFraction =
      if (monthlyTarget > 0L) (totalIncome.toFloat() / monthlyTarget.toFloat()).coerceIn(0f, 1f)
      else 0f
    val progressPercent =
      if (monthlyTarget > 0L) {
        kotlin.math.round((totalIncome.toDouble() / monthlyTarget.toDouble()) * 100.0).toInt()
      } else {
        0
      }

    return MonthlyIncomeSummary(
      monthKey = monthKey,
      monthLabel = formatMonthIndo(monthKey),
      totalIncome = totalIncome,
      totalArgo = totalArgo,
      totalBonus = totalBonus,
      totalTipGoPay = totalTipGoPay,
      totalTipCash = totalTipCash,
      productiveDays = productiveDays,
      avgIncomePerProductiveDay = avgPerDay,
      estimated26DaysIncome = estimated26,
      dailyTargetAccumulation = dailyTargetAccum,
      dailyTargetDiff = dailyTargetDiff,
      monthlyTarget = monthlyTarget,
      remainingTarget = remainingTarget,
      targetProgressFraction = progressFraction,
      targetProgressPercent = progressPercent,
    )
  }

  fun computeMonthlyExpenseSummary(
    monthKey: String,
    expenses: List<ExpenseRecord>,
    userCategories: List<ExpenseCategory>,
  ): MonthlyExpenseSummary {
    val monthExpenses = expenses.filter { it.date.startsWith(monthKey) }
    val totalExpense = monthExpenses.sumOf { it.amount }
    val monthLabel = formatMonthIndo(monthKey)
    if (totalExpense <= 0L) {
      return MonthlyExpenseSummary(
        monthKey = monthKey,
        monthLabel = monthLabel,
        totalExpense = 0L,
        categories = emptyList(),
      )
    }

    val groupedTotals =
      monthExpenses.groupBy { it.categoryName }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val allCategoryNames = (userCategories.map { it.name } + groupedTotals.keys).distinct()
    val items =
      allCategoryNames
        .map { categoryName ->
          val amount = groupedTotals[categoryName] ?: 0L
          val pct = (amount.toDouble() / totalExpense.toDouble()) * 100.0
          val pctLabel =
            if (amount == 0L) {
              "0%"
            } else {
              String.format(Locale.forLanguageTag("id-ID"), "%.1f%%", pct)
            }
          MonthlyCategoryExpenseItem(
            categoryName = categoryName,
            totalAmount = amount,
            percentage = pct,
            percentageLabel = pctLabel,
          )
        }
        .sortedWith(
          compareByDescending<MonthlyCategoryExpenseItem> { it.totalAmount }
            .thenBy { it.categoryName }
        )

    return MonthlyExpenseSummary(
      monthKey = monthKey,
      monthLabel = monthLabel,
      totalExpense = totalExpense,
      categories = items,
    )
  }
}
