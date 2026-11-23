package com.example.data.backup

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
import com.example.state.BackupPreviewInfo
import com.example.state.FinanceRawState

class BackupValidationException(message: String) : Exception(message)

data class BackupMetadataInfo(
  val formatVersion: Int,
  val appVersion: String,
  val createdAtIso: String,
  val summaryText: String,
)

data class BackupRestorePayload(
  val metadata: BackupMetadataInfo,
  val state: FinanceRawState,
)

object BackupSchema {
  const val BACKUP_FORMAT_VERSION = 1
  const val DEFAULT_SPREADSHEET_TITLE = "Catatan_Keuangan_Backup"

  const val TAB_METADATA = "Metadata"
  const val TAB_ORDERS = "Orders"
  const val TAB_BLANKS = "Blanks"
  const val TAB_BONUS_RULES = "BonusRules"
  const val TAB_TARGETS = "Targets"
  const val TAB_WALLETS = "Wallets"
  const val TAB_BALANCE_CORRECTIONS = "BalanceCorrections"
  const val TAB_CATEGORIES = "Categories"
  const val TAB_EXPENSES = "Expenses"
  const val TAB_TRANSFERS = "Transfers"
  const val TAB_BILLS = "Bills"
  const val TAB_BILL_MONTHS = "BillMonths"
  const val TAB_AREAS = "Areas"
  const val TAB_APP_SETTINGS = "AppSettings"

  val allTabs: List<String> =
    listOf(
      TAB_METADATA,
      TAB_ORDERS,
      TAB_BLANKS,
      TAB_BONUS_RULES,
      TAB_TARGETS,
      TAB_WALLETS,
      TAB_BALANCE_CORRECTIONS,
      TAB_CATEGORIES,
      TAB_EXPENSES,
      TAB_TRANSFERS,
      TAB_BILLS,
      TAB_BILL_MONTHS,
      TAB_AREAS,
      TAB_APP_SETTINGS,
    )

  val headersByTab: Map<String, List<String>> =
    mapOf(
      TAB_METADATA to listOf("key", "value"),
      TAB_ORDERS to
        listOf(
          "id",
          "date",
          "startTime",
          "endTime",
          "argo",
          "packageCount",
          "tipGoPay",
          "tipCash",
          "pickupDistanceKm",
          "deliveryDistanceKm",
          "originArea",
          "destinationArea",
          "batteryStart",
          "batteryEnd",
          "note",
        ),
      TAB_BLANKS to
        listOf(
          "id",
          "date",
          "startTime",
          "endTime",
          "nextOrderId",
          "area",
          "batteryStart",
          "batteryEnd",
          "distanceKm",
          "note",
        ),
      TAB_BONUS_RULES to
        listOf(
          "id",
          "effectiveDate",
          "tier1MaxPackages",
          "tier1Rate",
          "tier2MaxPackages",
          "tier2Rate",
          "tier3MaxPackages",
          "tier3Rate",
          "tier4Rate",
          "note",
        ),
      TAB_TARGETS to listOf("id", "effectiveDate", "dailyTarget", "monthlyTarget", "note"),
      TAB_WALLETS to listOf("locationId", "previousMonthClosing"),
      TAB_BALANCE_CORRECTIONS to listOf("id", "locationId", "date", "deltaAmount", "note"),
      TAB_CATEGORIES to listOf("id", "name"),
      TAB_EXPENSES to
        listOf(
          "id",
          "date",
          "categoryName",
          "amount",
          "sourceId",
          "note",
          "insertOrder",
          "linkedBillId",
        ),
      TAB_TRANSFERS to
        listOf("id", "date", "fromLocationId", "toLocationId", "amount", "note", "insertOrder"),
      TAB_BILLS to listOf("id", "name", "categoryNote"),
      TAB_BILL_MONTHS to
        listOf(
          "billId",
          "monthKey",
          "monthLabel",
          "amount",
          "statusName",
          "dueDateLabel",
          "paidSourceId",
          "paidDate",
          "linkedExpenseId",
        ),
      TAB_AREAS to listOf("name"),
      TAB_APP_SETTINGS to
        listOf("hideAmounts", "isFirstTimeBalanceConfigured", "activeMonthKey", "nextSequenceId"),
    )
}

object BackupSerializer {
  private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
  private val monthKeyRegex = Regex("""^\d{4}-\d{2}$""")
  private val timeRegex = Regex("""^\d{2}:\d{2}$""")

  fun validateRawStateForBackup(raw: FinanceRawState) {
    if (raw.bonusRuleHistory.isEmpty()) {
      throw BackupValidationException("Aturan bonus paket kosong dan tidak dapat dicadangkan.")
    }
    if (raw.targetHistory.isEmpty()) {
      throw BackupValidationException("Riwayat target penghasilan kosong dan tidak dapat dicadangkan.")
    }
    val orderIds = raw.orders.map { it.id }
    if (orderIds.size != orderIds.toSet().size) {
      throw BackupValidationException("Terdapat ID order ganda pada data lokal.")
    }
    val orderIdSet = orderIds.toSet()
    raw.blanks.forEach { blank ->
      if (!orderIdSet.contains(blank.nextOrderId)) {
        throw BackupValidationException(
          "Blank ${blank.id} mengacu ke order yang tidak ditemukan (${blank.nextOrderId})."
        )
      }
    }
    val billIdSet = raw.bills.map { it.id }.toSet()
    raw.expenses.forEach { exp ->
      val linked = exp.linkedBillId
      if (!linked.isNullOrBlank() && !billIdSet.contains(linked)) {
        throw BackupValidationException(
          "Pengeluaran ${exp.id} mengacu ke tagihan yang tidak ditemukan ($linked)."
        )
      }
    }
    FundLocation.entries.forEach { loc ->
      if (!raw.previousClosingBalances.containsKey(loc)) {
        throw BackupValidationException("Saldo awal dompet ${loc.displayName} belum lengkap.")
      }
    }
  }

  fun encodeToTabs(
    raw: FinanceRawState,
    createdAtIso: String,
    appVersion: String = "1.0",
  ): Map<String, List<List<String>>> {
    validateRawStateForBackup(raw)

    val summary =
      "${raw.orders.size} order, ${raw.blanks.size} blank, ${raw.expenses.size} pengeluaran, ${raw.transfers.size} transfer, ${raw.bills.size} tagihan, ${raw.categories.size} kategori, ${raw.areas.size} wilayah"

    val result = linkedMapOf<String, List<List<String>>>()

    result[BackupSchema.TAB_METADATA] =
      listOf(
        BackupSchema.headersByTab.getValue(BackupSchema.TAB_METADATA),
        listOf("formatVersion", BackupSchema.BACKUP_FORMAT_VERSION.toString()),
        listOf("appVersion", appVersion),
        listOf("createdAtIso", createdAtIso),
        listOf("orderCount", raw.orders.size.toString()),
        listOf("blankCount", raw.blanks.size.toString()),
        listOf("bonusRuleCount", raw.bonusRuleHistory.size.toString()),
        listOf("targetCount", raw.targetHistory.size.toString()),
        listOf("categoryCount", raw.categories.size.toString()),
        listOf("expenseCount", raw.expenses.size.toString()),
        listOf("transferCount", raw.transfers.size.toString()),
        listOf("billCount", raw.bills.size.toString()),
        listOf("areaCount", raw.areas.size.toString()),
        listOf("summaryText", summary),
      )

    result[BackupSchema.TAB_ORDERS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_ORDERS)) +
        raw.orders.sortedWith(compareBy<WorkOrder> { it.date }.thenBy { it.startTime }.thenBy { it.id }).map { o ->
          listOf(
            o.id,
            o.date,
            o.startTime,
            o.endTime,
            o.argo.toString(),
            o.packageCount.toString(),
            o.tipGoPay.toString(),
            o.tipCash.toString(),
            o.pickupDistanceKm.toString(),
            o.deliveryDistanceKm.toString(),
            o.originArea,
            o.destinationArea,
            o.batteryStart.toString(),
            o.batteryEnd.toString(),
            o.note,
          )
        }

    result[BackupSchema.TAB_BLANKS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_BLANKS)) +
        raw.blanks.sortedWith(compareBy<BlankSession> { it.date }.thenBy { it.startTime }.thenBy { it.id }).map { b ->
          listOf(
            b.id,
            b.date,
            b.startTime,
            b.endTime,
            b.nextOrderId,
            b.area,
            b.batteryStart.toString(),
            b.batteryEnd.toString(),
            b.distanceKm.toString(),
            b.note,
          )
        }

    result[BackupSchema.TAB_BONUS_RULES] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_BONUS_RULES)) +
        raw.bonusRuleHistory.sortedBy { it.effectiveDate }.map { r ->
          listOf(
            r.id,
            r.effectiveDate,
            r.tier1MaxPackages.toString(),
            r.tier1Rate.toString(),
            r.tier2MaxPackages.toString(),
            r.tier2Rate.toString(),
            r.tier3MaxPackages.toString(),
            r.tier3Rate.toString(),
            r.tier4Rate.toString(),
            r.note,
          )
        }

    result[BackupSchema.TAB_TARGETS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_TARGETS)) +
        raw.targetHistory.sortedBy { it.effectiveDate }.map { t ->
          listOf(
            t.id,
            t.effectiveDate,
            t.dailyTarget.toString(),
            t.monthlyTarget.toString(),
            t.note,
          )
        }

    result[BackupSchema.TAB_WALLETS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_WALLETS)) +
        FundLocation.entries.map { loc ->
          listOf(loc.id, (raw.previousClosingBalances[loc] ?: 0L).toString())
        }

    result[BackupSchema.TAB_BALANCE_CORRECTIONS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_BALANCE_CORRECTIONS)) +
        raw.balanceCorrections.sortedWith(compareBy<BalanceCorrection> { it.date }.thenBy { it.id }).map { c ->
          listOf(c.id, c.location.id, c.date, c.deltaAmount.toString(), c.note)
        }

    result[BackupSchema.TAB_CATEGORIES] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_CATEGORIES)) +
        raw.categories.sortedBy { it.id }.map { cat -> listOf(cat.id, cat.name) }

    result[BackupSchema.TAB_EXPENSES] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_EXPENSES)) +
        raw.expenses.sortedBy { it.insertOrder }.map { e ->
          listOf(
            e.id,
            e.date,
            e.categoryName,
            e.amount.toString(),
            e.source.id,
            e.note,
            e.insertOrder.toString(),
            e.linkedBillId.orEmpty(),
          )
        }

    result[BackupSchema.TAB_TRANSFERS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_TRANSFERS)) +
        raw.transfers.sortedBy { it.insertOrder }.map { tr ->
          listOf(
            tr.id,
            tr.date,
            tr.from.id,
            tr.to.id,
            tr.amount.toString(),
            tr.note,
            tr.insertOrder.toString(),
          )
        }

    result[BackupSchema.TAB_BILLS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_BILLS)) +
        raw.bills.sortedBy { it.id }.map { b -> listOf(b.id, b.name, b.categoryNote) }

    result[BackupSchema.TAB_BILL_MONTHS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_BILL_MONTHS)) +
        raw.bills.sortedBy { it.id }.flatMap { bill ->
          bill.history.sortedBy { it.monthKey }.map { m ->
            listOf(
              bill.id,
              m.monthKey,
              m.monthLabel,
              m.amount.toString(),
              m.status.name,
              m.dueDateLabel,
              m.paidSource?.id.orEmpty(),
              m.paidDate.orEmpty(),
              m.linkedExpenseId.orEmpty(),
            )
          }
        }

    result[BackupSchema.TAB_AREAS] =
      listOf(BackupSchema.headersByTab.getValue(BackupSchema.TAB_AREAS)) +
        raw.areas.sorted().map { listOf(it) }

    result[BackupSchema.TAB_APP_SETTINGS] =
      listOf(
        BackupSchema.headersByTab.getValue(BackupSchema.TAB_APP_SETTINGS),
        listOf(
          raw.hideAmounts.toString(),
          raw.isFirstTimeBalanceConfigured.toString(),
          raw.activeMonthKey,
          raw.nextSequenceId.toString(),
        ),
      )

    return result
  }

  fun readMetadataOnly(tabs: Map<String, List<List<String>>>): BackupMetadataInfo {
    val rows =
      tabs[BackupSchema.TAB_METADATA]
        ?: throw BackupValidationException("File cadangan tidak memiliki tab Metadata.")
    validateHeader(BackupSchema.TAB_METADATA, rows)
    val kv = mutableMapOf<String, String>()
    rows.drop(1).forEach { row ->
      val k = row.getOrNull(0)?.trim().orEmpty()
      val v = row.getOrNull(1)?.trim().orEmpty()
      if (k.isNotEmpty()) kv[k] = v
    }
    val version =
      kv["formatVersion"]?.toIntOrNull()
        ?: throw BackupValidationException("Versi format cadangan pada Metadata tidak valid.")
    if (version != BackupSchema.BACKUP_FORMAT_VERSION) {
      throw BackupValidationException(
        "Versi format cadangan ($version) tidak didukung. Versi yang didukung: ${BackupSchema.BACKUP_FORMAT_VERSION}."
      )
    }
    val createdAtIso =
      kv["createdAtIso"]?.takeIf { it.isNotBlank() }
        ?: throw BackupValidationException("Tanggal pembuatan cadangan pada Metadata tidak ditemukan.")
    val appVersion = kv["appVersion"].orEmpty().ifBlank { "1.0" }
    val summaryText =
      kv["summaryText"].orEmpty().ifBlank {
        "${kv["orderCount"] ?: "0"} order • ${kv["expenseCount"] ?: "0"} pengeluaran • ${kv["billCount"] ?: "0"} tagihan"
      }
    return BackupMetadataInfo(
      formatVersion = version,
      appVersion = appVersion,
      createdAtIso = createdAtIso,
      summaryText = summaryText,
    )
  }

  fun decodeAndValidate(tabs: Map<String, List<List<String>>>): BackupRestorePayload {
    // 1. Pastikan seluruh 14 tab wajib ada
    val missingTabs = BackupSchema.allTabs.filter { !tabs.containsKey(it) }
    if (missingTabs.isNotEmpty()) {
      throw BackupValidationException(
        "Struktur file cadangan tidak lengkap. Tab yang hilang: ${missingTabs.joinToString(", ")}."
      )
    }

    // 2. Validasi metadata & header setiap tab
    val metadata = readMetadataOnly(tabs)
    BackupSchema.allTabs.forEach { tab ->
      validateHeader(tab, tabs.getValue(tab))
    }

    // 3. Parse & validasi Orders
    val orders = mutableListOf<WorkOrder>()
    val orderIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_ORDERS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Orders", rowNum, "id")
      if (!orderIds.add(id)) {
        throw BackupValidationException("Duplikasi ID order '$id' pada tab Orders baris $rowNum.")
      }
      val date = requireDate(row, 1, "Orders", rowNum, "date")
      val startTime = requireTime(row, 2, "Orders", rowNum, "startTime")
      val endTime = requireTime(row, 3, "Orders", rowNum, "endTime")
      val argo = requireLong(row, 4, "Orders", rowNum, "argo", min = 0L)
      val pkg = requireInt(row, 5, "Orders", rowNum, "packageCount", min = 1, max = 10_000)
      val tipGoPay = requireLong(row, 6, "Orders", rowNum, "tipGoPay", min = 0L)
      val tipCash = requireLong(row, 7, "Orders", rowNum, "tipCash", min = 0L)
      val pickupKm = requireDouble(row, 8, "Orders", rowNum, "pickupDistanceKm", min = 0.0)
      val deliveryKm = requireDouble(row, 9, "Orders", rowNum, "deliveryDistanceKm", min = 0.0)
      val originArea = row.getOrNull(10)?.trim().orEmpty()
      val destArea = row.getOrNull(11)?.trim().orEmpty()
      val batStart = requireInt(row, 12, "Orders", rowNum, "batteryStart", min = 0, max = 100)
      val batEnd = requireInt(row, 13, "Orders", rowNum, "batteryEnd", min = 0, max = 100)
      val note = row.getOrNull(14)?.trim().orEmpty()
      orders.add(
        WorkOrder(
          id = id,
          date = date,
          startTime = startTime,
          endTime = endTime,
          argo = argo,
          packageCount = pkg,
          tipGoPay = tipGoPay,
          tipCash = tipCash,
          pickupDistanceKm = pickupKm,
          deliveryDistanceKm = deliveryKm,
          originArea = originArea,
          destinationArea = destArea,
          batteryStart = batStart,
          batteryEnd = batEnd,
          note = note,
        )
      )
    }

    // 4. Parse & validasi Blanks (termasuk relasi nextOrderId -> Orders)
    val blanks = mutableListOf<BlankSession>()
    val blankIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_BLANKS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Blanks", rowNum, "id")
      if (!blankIds.add(id)) {
        throw BackupValidationException("Duplikasi ID blank '$id' pada tab Blanks baris $rowNum.")
      }
      val date = requireDate(row, 1, "Blanks", rowNum, "date")
      val startTime = requireTime(row, 2, "Blanks", rowNum, "startTime")
      val endTime = requireTime(row, 3, "Blanks", rowNum, "endTime")
      val nextOrderId = requireNonBlank(row, 4, "Blanks", rowNum, "nextOrderId")
      if (!orderIds.contains(nextOrderId)) {
        throw BackupValidationException(
          "Relasi rusak pada tab Blanks baris $rowNum: order '$nextOrderId' tidak ditemukan."
        )
      }
      val area = row.getOrNull(5)?.trim().orEmpty()
      val batStart = requireInt(row, 6, "Blanks", rowNum, "batteryStart", min = 0, max = 100)
      val batEnd = requireInt(row, 7, "Blanks", rowNum, "batteryEnd", min = 0, max = 100)
      val distanceKm = requireDouble(row, 8, "Blanks", rowNum, "distanceKm", min = 0.0)
      val note = row.getOrNull(9)?.trim().orEmpty()
      blanks.add(
        BlankSession(
          id = id,
          date = date,
          startTime = startTime,
          endTime = endTime,
          nextOrderId = nextOrderId,
          area = area,
          batteryStart = batStart,
          batteryEnd = batEnd,
          distanceKm = distanceKm,
          note = note,
        )
      )
    }

    // 5. Parse & validasi BonusRules
    val bonusRules = mutableListOf<BonusRuleRecord>()
    val bonusRuleIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_BONUS_RULES).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "BonusRules", rowNum, "id")
      if (!bonusRuleIds.add(id)) {
        throw BackupValidationException("Duplikasi ID aturan bonus '$id' pada baris $rowNum.")
      }
      bonusRules.add(
        BonusRuleRecord(
          id = id,
          effectiveDate = requireDate(row, 1, "BonusRules", rowNum, "effectiveDate"),
          tier1MaxPackages = requireInt(row, 2, "BonusRules", rowNum, "tier1MaxPackages", 1, 1000),
          tier1Rate = requireLong(row, 3, "BonusRules", rowNum, "tier1Rate", 0L),
          tier2MaxPackages = requireInt(row, 4, "BonusRules", rowNum, "tier2MaxPackages", 1, 1000),
          tier2Rate = requireLong(row, 5, "BonusRules", rowNum, "tier2Rate", 0L),
          tier3MaxPackages = requireInt(row, 6, "BonusRules", rowNum, "tier3MaxPackages", 1, 1000),
          tier3Rate = requireLong(row, 7, "BonusRules", rowNum, "tier3Rate", 0L),
          tier4Rate = requireLong(row, 8, "BonusRules", rowNum, "tier4Rate", 0L),
          note = row.getOrNull(9)?.trim().orEmpty(),
        )
      )
    }
    if (bonusRules.isEmpty()) {
      throw BackupValidationException("Tab BonusRules wajib memiliki minimal 1 aturan bonus.")
    }

    // 6. Parse & validasi Targets
    val targets = mutableListOf<IncomeTargetRecord>()
    val targetIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_TARGETS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Targets", rowNum, "id")
      if (!targetIds.add(id)) {
        throw BackupValidationException("Duplikasi ID target '$id' pada baris $rowNum.")
      }
      targets.add(
        IncomeTargetRecord(
          id = id,
          effectiveDate = requireDate(row, 1, "Targets", rowNum, "effectiveDate"),
          dailyTarget = requireLong(row, 2, "Targets", rowNum, "dailyTarget", min = 1L),
          monthlyTarget = requireLong(row, 3, "Targets", rowNum, "monthlyTarget", min = 1L),
          note = row.getOrNull(4)?.trim().orEmpty(),
        )
      )
    }
    if (targets.isEmpty()) {
      throw BackupValidationException("Tab Targets wajib memiliki minimal 1 data target.")
    }

    // 7. Parse & validasi Wallets (harus lengkap 4 dompet resmi)
    val wallets = mutableMapOf<FundLocation, Long>()
    tabs.getValue(BackupSchema.TAB_WALLETS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val locId = requireNonBlank(row, 0, "Wallets", rowNum, "locationId")
      val loc = requireFundLocation(locId, "Wallets", rowNum)
      if (wallets.containsKey(loc)) {
        throw BackupValidationException("Duplikasi dompet '${loc.id}' pada tab Wallets.")
      }
      val closing = requireLong(row, 1, "Wallets", rowNum, "previousMonthClosing")
      wallets[loc] = closing
    }
    val missingWallets = FundLocation.entries.filter { !wallets.containsKey(it) }
    if (missingWallets.isNotEmpty()) {
      throw BackupValidationException(
        "Data dompet pada tab Wallets tidak lengkap (${missingWallets.joinToString { it.displayName }})."
      )
    }

    // 8. Parse & validasi BalanceCorrections
    val corrections = mutableListOf<BalanceCorrection>()
    val correctionIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_BALANCE_CORRECTIONS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "BalanceCorrections", rowNum, "id")
      if (!correctionIds.add(id)) {
        throw BackupValidationException("Duplikasi ID koreksi saldo '$id' pada baris $rowNum.")
      }
      val loc = requireFundLocation(requireNonBlank(row, 1, "BalanceCorrections", rowNum, "locationId"), "BalanceCorrections", rowNum)
      val date = requireDate(row, 2, "BalanceCorrections", rowNum, "date")
      val delta = requireLong(row, 3, "BalanceCorrections", rowNum, "deltaAmount")
      val note = row.getOrNull(4)?.trim().orEmpty()
      corrections.add(BalanceCorrection(id = id, location = loc, date = date, deltaAmount = delta, note = note))
    }

    // 9. Parse & validasi Categories
    val categories = mutableListOf<ExpenseCategory>()
    val categoryIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_CATEGORIES).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Categories", rowNum, "id")
      if (!categoryIds.add(id)) {
        throw BackupValidationException("Duplikasi ID kategori '$id' pada baris $rowNum.")
      }
      val name = requireNonBlank(row, 1, "Categories", rowNum, "name")
      categories.add(ExpenseCategory(id = id, name = name))
    }

    // 10. Parse & validasi Bills terlebih dahulu agar bisa cek linkedBillId pada Expenses
    data class RawBillDef(val id: String, val name: String, val categoryNote: String)
    val billDefs = mutableListOf<RawBillDef>()
    val billIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_BILLS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Bills", rowNum, "id")
      if (!billIds.add(id)) {
        throw BackupValidationException("Duplikasi ID tagihan '$id' pada baris $rowNum.")
      }
      val name = requireNonBlank(row, 1, "Bills", rowNum, "name")
      val catNote = row.getOrNull(2)?.trim().orEmpty()
      billDefs.add(RawBillDef(id = id, name = name, categoryNote = catNote))
    }

    // 11. Parse & validasi Expenses
    val expenses = mutableListOf<ExpenseRecord>()
    val expenseIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_EXPENSES).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Expenses", rowNum, "id")
      if (!expenseIds.add(id)) {
        throw BackupValidationException("Duplikasi ID pengeluaran '$id' pada baris $rowNum.")
      }
      val date = requireDate(row, 1, "Expenses", rowNum, "date")
      val categoryName = requireNonBlank(row, 2, "Expenses", rowNum, "categoryName")
      val amount = requireLong(row, 3, "Expenses", rowNum, "amount", min = 1L)
      val source =
        requireFundLocation(requireNonBlank(row, 4, "Expenses", rowNum, "sourceId"), "Expenses", rowNum)
      if (!source.isDirectSpendAllowed) {
        throw BackupValidationException(
          "Sumber pengeluaran tidak valid (${source.displayName}) pada Expenses baris $rowNum."
        )
      }
      val note = row.getOrNull(5)?.trim().orEmpty()
      val insertOrder = requireLong(row, 6, "Expenses", rowNum, "insertOrder", min = 0L)
      val linkedBillId = row.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() }
      if (linkedBillId != null && !billIds.contains(linkedBillId)) {
        throw BackupValidationException(
          "Relasi rusak pada Expenses baris $rowNum: tagihan '$linkedBillId' tidak ditemukan."
        )
      }
      expenses.add(
        ExpenseRecord(
          id = id,
          date = date,
          categoryName = categoryName,
          amount = amount,
          source = source,
          note = note,
          insertOrder = insertOrder,
          linkedBillId = linkedBillId,
        )
      )
    }

    // 12. Parse & validasi Transfers
    val transfers = mutableListOf<TransferRecord>()
    val transferIds = mutableSetOf<String>()
    tabs.getValue(BackupSchema.TAB_TRANSFERS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val id = requireNonBlank(row, 0, "Transfers", rowNum, "id")
      if (!transferIds.add(id)) {
        throw BackupValidationException("Duplikasi ID transfer '$id' pada baris $rowNum.")
      }
      val date = requireDate(row, 1, "Transfers", rowNum, "date")
      val from =
        requireFundLocation(requireNonBlank(row, 2, "Transfers", rowNum, "fromLocationId"), "Transfers", rowNum)
      val to =
        requireFundLocation(requireNonBlank(row, 3, "Transfers", rowNum, "toLocationId"), "Transfers", rowNum)
      if (from == to) {
        throw BackupValidationException("Dompet asal dan tujuan transfer sama pada baris $rowNum.")
      }
      val amount = requireLong(row, 4, "Transfers", rowNum, "amount", min = 1L)
      val note = row.getOrNull(5)?.trim().orEmpty()
      val insertOrder = requireLong(row, 6, "Transfers", rowNum, "insertOrder", min = 0L)
      transfers.add(
        TransferRecord(
          id = id,
          date = date,
          from = from,
          to = to,
          amount = amount,
          note = note,
          insertOrder = insertOrder,
        )
      )
    }

    // 13. Parse & validasi BillMonths beserta relasi ke Bills & Expenses
    val billMonthsByBillId = linkedMapOf<String, MutableList<BillMonthEntry>>()
    val billMonthKeys = mutableSetOf<Pair<String, String>>()
    tabs.getValue(BackupSchema.TAB_BILL_MONTHS).drop(1).forEachIndexed { idx, row ->
      val rowNum = idx + 2
      val billId = requireNonBlank(row, 0, "BillMonths", rowNum, "billId")
      if (!billIds.contains(billId)) {
        throw BackupValidationException(
          "Relasi rusak pada BillMonths baris $rowNum: tagihan '$billId' tidak ditemukan."
        )
      }
      val monthKey = row.getOrNull(1)?.trim().orEmpty()
      if (!monthKeyRegex.matches(monthKey)) {
        throw BackupValidationException("Format monthKey '$monthKey' tidak valid pada BillMonths baris $rowNum.")
      }
      if (!billMonthKeys.add(billId to monthKey)) {
        throw BackupValidationException("Duplikasi entri tagihan $billId untuk bulan $monthKey.")
      }
      val monthLabel = requireNonBlank(row, 2, "BillMonths", rowNum, "monthLabel")
      val amount = requireLong(row, 3, "BillMonths", rowNum, "amount", min = 0L)
      val statusName = requireNonBlank(row, 4, "BillMonths", rowNum, "statusName")
      val status =
        BillStatus.entries.firstOrNull { it.name == statusName }
          ?: throw BackupValidationException("Status tagihan '$statusName' tidak valid pada baris $rowNum.")
      val dueDateLabel = requireNonBlank(row, 5, "BillMonths", rowNum, "dueDateLabel")
      val paidSourceRaw = row.getOrNull(6)?.trim()?.takeIf { it.isNotEmpty() }
      val paidSource = paidSourceRaw?.let { requireFundLocation(it, "BillMonths", rowNum) }
      val paidDate = row.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() }
      if (paidDate != null && !isoDateRegex.matches(paidDate)) {
        throw BackupValidationException("Format paidDate '$paidDate' tidak valid pada BillMonths baris $rowNum.")
      }
      val linkedExpenseId = row.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() }
      if (linkedExpenseId != null && !expenseIds.contains(linkedExpenseId)) {
        throw BackupValidationException(
          "Relasi rusak pada BillMonths baris $rowNum: pengeluaran '$linkedExpenseId' tidak ditemukan."
        )
      }
      if (status == BillStatus.SUDAH_DIBAYAR && paidSource == null) {
        throw BackupValidationException(
          "Tagihan '$billId' bulan $monthKey berstatus Sudah dibayar tetapi sumber pembayaran kosong."
        )
      }
      val entry =
        BillMonthEntry(
          monthKey = monthKey,
          monthLabel = monthLabel,
          amount = amount,
          status = status,
          dueDateLabel = dueDateLabel,
          paidSource = paidSource,
          paidDate = paidDate,
          linkedExpenseId = linkedExpenseId,
        )
      billMonthsByBillId.getOrPut(billId) { mutableListOf() }.add(entry)
    }

    val bills =
      billDefs.map { def ->
        val entries = billMonthsByBillId[def.id].orEmpty().sortedBy { it.monthKey }
        BillItem(id = def.id, name = def.name, categoryNote = def.categoryNote, history = entries)
      }

    // 14. Parse Areas
    val areaSet = linkedSetOf<String>()
    tabs.getValue(BackupSchema.TAB_AREAS).drop(1).forEach { row ->
      val area = row.getOrNull(0)?.trim().orEmpty()
      if (area.isNotEmpty()) areaSet.add(area)
    }

    // 15. Parse AppSettings
    val settingsRows = tabs.getValue(BackupSchema.TAB_APP_SETTINGS).drop(1)
    if (settingsRows.isEmpty()) {
      throw BackupValidationException("Tab AppSettings kosong. Data preferensi tidak ditemukan.")
    }
    val sRow = settingsRows.first()
    val hideAmounts =
      sRow.getOrNull(0)?.trim()?.lowercase()?.toBooleanStrictOrNull()
        ?: throw BackupValidationException("Nilai hideAmounts pada AppSettings tidak valid.")
    val isFirstConfigured =
      sRow.getOrNull(1)?.trim()?.lowercase()?.toBooleanStrictOrNull()
        ?: throw BackupValidationException(
          "Nilai isFirstTimeBalanceConfigured pada AppSettings tidak valid."
        )
    val activeMonthKey = sRow.getOrNull(2)?.trim().orEmpty()
    if (!monthKeyRegex.matches(activeMonthKey)) {
      throw BackupValidationException("Format activeMonthKey '$activeMonthKey' pada AppSettings tidak valid.")
    }
    val nextSeq = requireLong(sRow, 3, "AppSettings", 2, "nextSequenceId", min = 1L)

    // Pastikan nextSequenceId minimal berada di atas seluruh akhiran ID numerik agar tidak bentrok
    val maxNumericSuffix =
      (orderIds + blankIds + bonusRuleIds + targetIds + correctionIds + categoryIds + expenseIds + transferIds)
        .mapNotNull { id -> id.substringAfterLast('_').toLongOrNull() }
        .maxOrNull() ?: 100L
    val safeNextSequenceId = maxOf(nextSeq, maxNumericSuffix, 100L)

    val rawState =
      FinanceRawState(
        hideAmounts = hideAmounts,
        isFirstTimeBalanceConfigured = isFirstConfigured,
        orders = orders,
        blanks = blanks,
        bonusRuleHistory = bonusRules.sortedBy { it.effectiveDate },
        expenses = expenses,
        transfers = transfers,
        categories = categories,
        areas = areaSet.sorted(),
        previousClosingBalances = wallets,
        balanceCorrections = corrections,
        bills = bills,
        targetHistory = targets.sortedBy { it.effectiveDate },
        activeMonthKey = activeMonthKey,
        backupInfo =
          BackupPreviewInfo(
            lastRestoreTimestamp = metadata.createdAtIso,
            lastBackupSummary = "Data dipulihkan dari cadangan (${metadata.createdAtIso}): ${metadata.summaryText}",
          ),
        nextSequenceId = safeNextSequenceId,
      )

    return BackupRestorePayload(metadata = metadata, state = rawState)
  }

  private fun validateHeader(tabName: String, rows: List<List<String>>) {
    val expected = BackupSchema.headersByTab.getValue(tabName)
    val actual = rows.firstOrNull()?.map { it.trim() }
    if (actual == null || actual.size < expected.size || actual.take(expected.size) != expected) {
      throw BackupValidationException(
        "Format kolom pada tab '$tabName' tidak sesuai atau telah berubah."
      )
    }
  }

  private fun requireNonBlank(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
  ): String {
    val v = row.getOrNull(col)?.trim().orEmpty()
    if (v.isEmpty()) {
      throw BackupValidationException("Kolom '$field' kosong pada tab $tab baris $rowNum.")
    }
    return v
  }

  private fun requireDate(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
  ): String {
    val v = requireNonBlank(row, col, tab, rowNum, field)
    if (!isoDateRegex.matches(v)) {
      throw BackupValidationException(
        "Format tanggal '$v' pada tab $tab baris $rowNum tidak valid (harus YYYY-MM-DD)."
      )
    }
    return v
  }

  private fun requireTime(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
  ): String {
    val v = requireNonBlank(row, col, tab, rowNum, field)
    if (!timeRegex.matches(v)) {
      throw BackupValidationException(
        "Format jam '$v' pada tab $tab baris $rowNum tidak valid (harus HH:mm)."
      )
    }
    return v
  }

  private fun requireLong(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
    min: Long = Long.MIN_VALUE,
  ): Long {
    val raw = requireNonBlank(row, col, tab, rowNum, field)
    val parsed =
      raw.toLongOrNull()
        ?: raw.toDoubleOrNull()?.takeIf { it % 1.0 == 0.0 }?.toLong()
        ?: throw BackupValidationException(
          "Nilai angka '$raw' untuk '$field' pada tab $tab baris $rowNum tidak valid."
        )
    if (parsed < min) {
      throw BackupValidationException(
        "Nilai '$field' ($parsed) pada tab $tab baris $rowNum kurang dari batas minimum ($min)."
      )
    }
    return parsed
  }

  private fun requireInt(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
  ): Int {
    val value = requireLong(row, col, tab, rowNum, field, min.toLong())
    if (value < min || value > max) {
      throw BackupValidationException(
        "Nilai '$field' ($value) pada tab $tab baris $rowNum di luar rentang ($min..$max)."
      )
    }
    return value.toInt()
  }

  private fun requireDouble(
    row: List<String>,
    col: Int,
    tab: String,
    rowNum: Int,
    field: String,
    min: Double = Double.NEGATIVE_INFINITY,
  ): Double {
    val raw = requireNonBlank(row, col, tab, rowNum, field).replace(',', '.')
    val parsed =
      raw.toDoubleOrNull()
        ?: throw BackupValidationException(
          "Nilai desimal '$raw' untuk '$field' pada tab $tab baris $rowNum tidak valid."
        )
    if (parsed.isNaN() || parsed < min) {
      throw BackupValidationException(
        "Nilai '$field' ($parsed) pada tab $tab baris $rowNum tidak valid."
      )
    }
    return parsed
  }

  private fun requireFundLocation(id: String, tab: String, rowNum: Int): FundLocation {
    return FundLocation.entries.firstOrNull { it.id == id }
      ?: throw BackupValidationException(
        "ID dompet '$id' tidak dikenal pada tab $tab baris $rowNum."
      )
  }
}
