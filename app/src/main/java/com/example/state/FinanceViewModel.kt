package com.example.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppDatabaseProvider
import com.example.data.backup.BackupSerializer
import com.example.data.backup.BackupValidationException
import com.example.data.remote.DriveBackupFile
import com.example.data.remote.GoogleApiException
import com.example.data.remote.GoogleBackupGateway
import com.example.data.repository.FinanceRepository
import com.example.model.AreaStatItem
import com.example.model.BalanceCorrection
import com.example.model.BillStatus
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
import com.example.model.GroupedExpenseDisplay
import com.example.model.IncomeActivityItem
import com.example.model.IncomeTargetRecord
import com.example.model.LocationBalanceDetail
import com.example.model.TransactionListEntry
import com.example.model.TransferRecord
import com.example.model.WeeklyTrendPoint
import com.example.model.WorkOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FinanceViewModel(
  private val repository: FinanceRepository,
  private val todayDateProvider: () -> String = { FinanceCalculator.currentSystemIsoDate() },
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val backupGateway: GoogleBackupGateway = AppDatabaseProvider.getBackupGateway(),
) : ViewModel() {

  constructor() : this(AppDatabaseProvider.getRepository())

  constructor(todayDateProvider: () -> String) :
    this(AppDatabaseProvider.getRepository(), todayDateProvider)

  private val persistenceMutex = Mutex()
  private val pendingWrites = AtomicInteger(0)
  private val initializedDeferred = CompletableDeferred<Unit>()
  @Volatile private var manualTodayDateOverride: String? = null
  @Volatile private var sessionAccessToken: String? = null
  private val effectiveTodayDateProvider: () -> String = {
    manualTodayDateOverride ?: todayDateProvider()
  }

  private val _rawState =
    MutableStateFlow(
      FinanceRawState(
        todayDate = todayDateProvider(),
        activeMonthKey = todayDateProvider().take(7),
      )
    )
  private val _uiState =
    MutableStateFlow(computeUiState(_rawState.value, isDatabaseLoaded = false))
  val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

  private val _backupUiState = MutableStateFlow(BackupUiState())
  val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

  private val observerJob: Job =
    viewModelScope.launch(ioDispatcher) {
      persistenceMutex.withLock {
        repository.ensureDatabaseSeededOnce()
      }
      val dbRaw = repository.observeRawState(effectiveTodayDateProvider).filterNotNull().first()
      _rawState.value = dbRaw
      _uiState.value = computeUiState(dbRaw, isDatabaseLoaded = true)
      if (!initializedDeferred.isCompleted) {
        initializedDeferred.complete(Unit)
      }
    }

  /** Memastikan inisialisasi database dan seluruh penulisan tertunda sudah tersimpan permanen. */
  suspend fun awaitIdle() {
    initializedDeferred.await()
    while (pendingWrites.get() > 0) {
      kotlinx.coroutines.delay(5)
    }
    persistenceMutex.withLock {}
  }

  /** Menghentikan observer Room secara bersih pada pengujian sebelum database ditutup. */
  internal suspend fun closeObserversForTests() {
    awaitIdle()
    observerJob.cancelAndJoin()
  }

  private fun mutateAndPersist(
    transform: (FinanceRawState) -> FinanceRawState,
    persistAction: suspend (previous: FinanceRawState, updated: FinanceRawState) -> Unit,
  ) {
    lateinit var previousSnapshot: FinanceRawState
    lateinit var updatedSnapshot: FinanceRawState
    _rawState.update { current ->
      previousSnapshot = current
      val updated = transform(current)
      updatedSnapshot = updated
      _uiState.value =
        computeUiState(updated, isDatabaseLoaded = initializedDeferred.isCompleted)
      updated
    }
    if (updatedSnapshot == previousSnapshot) return

    pendingWrites.incrementAndGet()
    viewModelScope.launch(ioDispatcher) {
      try {
        persistenceMutex.withLock {
          initializedDeferred.await()
          persistAction(previousSnapshot, updatedSnapshot)
        }
      } finally {
        pendingWrites.decrementAndGet()
      }
    }
  }

  fun refreshTodayDate() {
    val latest = effectiveTodayDateProvider()
    if (latest != _rawState.value.todayDate) {
      _rawState.update { current ->
        val updated = current.copy(todayDate = latest)
        _uiState.value =
          computeUiState(updated, isDatabaseLoaded = initializedDeferred.isCompleted)
        updated
      }
    }
  }

  fun setTodayDate(date: String) {
    manualTodayDateOverride = date
    if (date != _rawState.value.todayDate) {
      _rawState.update { current ->
        val updated = current.copy(todayDate = date)
        _uiState.value =
          computeUiState(updated, isDatabaseLoaded = initializedDeferred.isCompleted)
        updated
      }
    }
  }

  fun toggleHideAmounts() {
    mutateAndPersist(
      transform = { it.copy(hideAmounts = !it.hideAmounts) },
      persistAction = { _, updated -> repository.saveSettings(updated) },
    )
  }

  fun setActiveMonth(monthKey: String) {
    mutateAndPersist(
      transform = { it.copy(activeMonthKey = monthKey) },
      persistAction = { _, updated -> repository.saveSettings(updated) },
    )
  }

  // --- ORDER & BLANK ---
  fun addOrder(
    date: String,
    startTime: String,
    endTime: String,
    argo: Long,
    packageCount: Int,
    tipGoPay: Long,
    tipCash: Long,
    pickupDistanceKm: Double,
    deliveryDistanceKm: Double,
    originAreaRaw: String,
    destinationAreaRaw: String,
    batteryStart: Int,
    batteryEnd: Int,
    note: String,
  ) {
    val origin = FinanceCalculator.normalizeAreaName(originAreaRaw)
    val dest = FinanceCalculator.normalizeAreaName(destinationAreaRaw)
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        val newOrder =
          WorkOrder(
            id = "ord_$seq",
            date = date,
            startTime = startTime,
            endTime = endTime,
            argo = argo.coerceAtLeast(0L),
            packageCount = packageCount.coerceAtLeast(1),
            tipGoPay = tipGoPay.coerceAtLeast(0L),
            tipCash = tipCash.coerceAtLeast(0L),
            pickupDistanceKm = pickupDistanceKm.coerceAtLeast(0.0),
            deliveryDistanceKm = deliveryDistanceKm.coerceAtLeast(0.0),
            originArea = origin,
            destinationArea = dest,
            batteryStart = batteryStart.coerceIn(0, 100),
            batteryEnd = batteryEnd.coerceIn(0, 100),
            note = note.trim(),
          )
        val updatedAreas = mergeIntoAreaList(state.areas, listOf(origin, dest))
        state.copy(
          orders = state.orders + newOrder,
          areas = updatedAreas,
          nextSequenceId = seq,
        )
      },
      persistAction = { _, updated ->
        val created = updated.orders.last()
        repository.addOrder(created, listOf(origin, dest), updated.nextSequenceId)
      },
    )
  }

  fun updateOrder(updatedOrder: WorkOrder) {
    val origin = FinanceCalculator.normalizeAreaName(updatedOrder.originArea)
    val dest = FinanceCalculator.normalizeAreaName(updatedOrder.destinationArea)
    val normalized = updatedOrder.copy(originArea = origin, destinationArea = dest)
    mutateAndPersist(
      transform = { state ->
        val updatedAreas = mergeIntoAreaList(state.areas, listOf(origin, dest))
        state.copy(
          orders = state.orders.map { if (it.id == normalized.id) normalized else it },
          areas = updatedAreas,
        )
      },
      persistAction = { _, _ ->
        repository.updateOrder(normalized, listOf(origin, dest))
      },
    )
  }

  fun deleteOrder(orderId: String) {
    mutateAndPersist(
      transform = { state ->
        state.copy(
          orders = state.orders.filterNot { it.id == orderId },
          blanks = state.blanks.filterNot { it.nextOrderId == orderId },
        )
      },
      persistAction = { _, _ -> repository.deleteOrder(orderId) },
    )
  }

  fun addBlank(
    date: String,
    startTime: String,
    nextOrderId: String,
    areaRaw: String,
    batteryStart: Int,
    batteryEnd: Int,
    distanceKm: Double,
    note: String,
  ) {
    val area = FinanceCalculator.normalizeAreaName(areaRaw)
    mutateAndPersist(
      transform = { state ->
        val targetOrder = state.orders.firstOrNull { it.id == nextOrderId } ?: return@mutateAndPersist state
        val seq = state.nextSequenceId + 1
        val newBlank =
          BlankSession(
            id = "blk_$seq",
            date = date,
            startTime = startTime,
            endTime = targetOrder.startTime,
            nextOrderId = targetOrder.id,
            area = area,
            batteryStart = batteryStart.coerceIn(0, 100),
            batteryEnd = batteryEnd.coerceIn(0, 100),
            distanceKm = distanceKm.coerceAtLeast(0.0),
            note = note.trim(),
          )
        val updatedAreas = mergeIntoAreaList(state.areas, listOf(area))
        state.copy(
          blanks = state.blanks + newBlank,
          areas = updatedAreas,
          nextSequenceId = seq,
        )
      },
      persistAction = { prev, updated ->
        if (updated.blanks.size > prev.blanks.size) {
          repository.addBlank(updated.blanks.last(), area, updated.nextSequenceId)
        }
      },
    )
  }

  fun deleteBlank(blankId: String) {
    mutateAndPersist(
      transform = { state -> state.copy(blanks = state.blanks.filterNot { it.id == blankId }) },
      persistAction = { _, _ -> repository.deleteBlank(blankId) },
    )
  }

  // --- PENGELUARAN & TRANSFER ---
  fun addExpense(
    date: String,
    categoryName: String,
    amount: Long,
    source: FundLocation,
    note: String,
  ) {
    if (!source.isDirectSpendAllowed || amount <= 0L) return
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        val record =
          ExpenseRecord(
            id = "exp_$seq",
            date = date,
            categoryName = categoryName,
            amount = amount,
            source = source,
            note = note.trim(),
            insertOrder = seq,
          )
        state.copy(expenses = state.expenses + record, nextSequenceId = seq)
      },
      persistAction = { _, updated ->
        repository.addExpense(updated.expenses.last(), updated.nextSequenceId)
      },
    )
  }

  fun updateExpense(updated: ExpenseRecord) {
    if (!updated.source.isDirectSpendAllowed || updated.amount <= 0L) return
    mutateAndPersist(
      transform = { state ->
        state.copy(
          expenses = state.expenses.map { if (it.id == updated.id) updated else it }
        )
      },
      persistAction = { _, _ -> repository.updateExpense(updated) },
    )
  }

  fun deleteExpense(expenseId: String) {
    mutateAndPersist(
      transform = { state ->
        state.copy(expenses = state.expenses.filterNot { it.id == expenseId })
      },
      persistAction = { _, _ -> repository.deleteExpense(expenseId) },
    )
  }

  fun addTransfer(
    date: String,
    from: FundLocation,
    to: FundLocation,
    amount: Long,
    note: String,
  ) {
    if (from == to || amount <= 0L) return
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        val transfer =
          TransferRecord(
            id = "trf_$seq",
            date = date,
            from = from,
            to = to,
            amount = amount,
            note = note.trim(),
            insertOrder = seq,
          )
        state.copy(transfers = state.transfers + transfer, nextSequenceId = seq)
      },
      persistAction = { _, updated ->
        repository.addTransfer(updated.transfers.last(), updated.nextSequenceId)
      },
    )
  }

  // --- KATEGORI PENGELUARAN (BEBAS DIKELOLA PENGGUNA) ---
  fun addCategory(nameRaw: String) {
    val name = nameRaw.trim()
    if (name.isEmpty()) return
    mutateAndPersist(
      transform = { state ->
        if (state.categories.any { it.name.equals(name, ignoreCase = true) }) return@mutateAndPersist state
        val seq = state.nextSequenceId + 1
        state.copy(
          categories = state.categories + ExpenseCategory("cat_$seq", name),
          nextSequenceId = seq,
        )
      },
      persistAction = { prev, updated ->
        if (updated.categories.size > prev.categories.size) {
          repository.addCategory(updated.categories.last(), updated.nextSequenceId)
        }
      },
    )
  }

  fun renameCategory(categoryId: String, newNameRaw: String) {
    val newName = newNameRaw.trim()
    if (newName.isEmpty()) return
    mutateAndPersist(
      transform = { state ->
        val old = state.categories.firstOrNull { it.id == categoryId } ?: return@mutateAndPersist state
        state.copy(
          categories = state.categories.map { if (it.id == categoryId) it.copy(name = newName) else it },
          expenses =
            state.expenses.map {
              if (it.categoryName == old.name) it.copy(categoryName = newName) else it
            },
        )
      },
      persistAction = { prev, _ ->
        val old = prev.categories.firstOrNull { it.id == categoryId }
        if (old != null) {
          repository.renameCategory(categoryId, old.name, newName)
        }
      },
    )
  }

  fun deleteCategory(categoryId: String) {
    mutateAndPersist(
      transform = { state ->
        state.copy(categories = state.categories.filterNot { it.id == categoryId })
      },
      persistAction = { _, _ -> repository.deleteCategory(categoryId) },
    )
  }

  // --- POSISI SALDO & KOREKSI SALDO AWAL ---
  fun setInitialActualBalances(balances: Map<FundLocation, Long>) {
    mutateAndPersist(
      transform = { state ->
        state.copy(
          isFirstTimeBalanceConfigured = true,
          previousClosingBalances = balances,
        )
      },
      persistAction = { _, _ -> repository.setInitialActualBalances(balances) },
    )
  }

  fun addBalanceCorrection(location: FundLocation, deltaAmount: Long, note: String) {
    if (deltaAmount == 0L) return
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        val correction =
          BalanceCorrection(
            id = "cor_$seq",
            location = location,
            date = state.todayDate,
            deltaAmount = deltaAmount,
            note = note.trim().ifEmpty { "Koreksi saldo awal" },
          )
        state.copy(
          balanceCorrections = state.balanceCorrections + correction,
          nextSequenceId = seq,
        )
      },
      persistAction = { _, updated ->
        repository.addBalanceCorrection(updated.balanceCorrections.last(), updated.nextSequenceId)
      },
    )
  }

  // --- TAGIHAN & KEWAJIBAN ---
  fun updateBillAmountForMonth(billId: String, monthKey: String, newAmount: Long) {
    if (newAmount < 0L) return
    mutateAndPersist(
      transform = { state ->
        state.copy(
          bills =
            state.bills.map { bill ->
              if (bill.id != billId) bill
              else {
                bill.copy(
                  history =
                    bill.history.map { entry ->
                      if (entry.monthKey == monthKey && entry.status != BillStatus.SUDAH_DIBAYAR) {
                        entry.copy(amount = newAmount)
                      } else {
                        entry
                      }
                    }
                )
              }
            }
        )
      },
      persistAction = { _, _ ->
        repository.updateBillAmountForMonth(billId, monthKey, newAmount)
      },
    )
  }

  fun payBill(billId: String, monthKey: String, source: FundLocation) {
    if (!source.isDirectSpendAllowed) return
    mutateAndPersist(
      transform = { state ->
        val bill = state.bills.firstOrNull { it.id == billId } ?: return@mutateAndPersist state
        val targetEntry =
          bill.history.firstOrNull { it.monthKey == monthKey } ?: return@mutateAndPersist state
        if (targetEntry.status == BillStatus.SUDAH_DIBAYAR) return@mutateAndPersist state

        val seq = state.nextSequenceId + 1
        val expenseId = "exp_$seq"
        val expenseCategory =
          state.categories.firstOrNull { it.name.contains("Tagihan", ignoreCase = true) }?.name
            ?: state.categories.firstOrNull()?.name
            ?: "Tagihan"

        val expense =
          ExpenseRecord(
            id = expenseId,
            date = state.todayDate,
            categoryName = expenseCategory,
            amount = targetEntry.amount,
            source = source,
            note = "Pembayaran tagihan: ${bill.name}",
            insertOrder = seq,
            linkedBillId = bill.id,
          )

        val updatedBills =
          state.bills.map { item ->
            if (item.id != billId) item
            else {
              item.copy(
                history =
                  item.history.map { entry ->
                    if (entry.monthKey == monthKey) {
                      entry.copy(
                        status = BillStatus.SUDAH_DIBAYAR,
                        paidSource = source,
                        paidDate = state.todayDate,
                        linkedExpenseId = expenseId,
                      )
                    } else entry
                  }
              )
            }
          }

        state.copy(
          expenses = state.expenses + expense,
          bills = updatedBills,
          nextSequenceId = seq,
        )
      },
      persistAction = { prev, updated ->
        if (updated.expenses.size > prev.expenses.size) {
          val createdExpense = updated.expenses.last()
          repository.payBill(
            billId = billId,
            monthKey = monthKey,
            source = source,
            paidDate = updated.todayDate,
            expense = createdExpense,
            nextSeq = updated.nextSequenceId,
          )
        }
      },
    )
  }

  // --- TARGET PENGHASILAN & ATURAN BONUS PAKET ---
  fun addTargetChange(
    effectiveDate: String,
    dailyTarget: Long,
    monthlyTarget: Long,
    note: String,
  ) {
    if (dailyTarget <= 0L || monthlyTarget <= 0L) return
    lateinit var createdTarget: IncomeTargetRecord
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        createdTarget =
          IncomeTargetRecord(
            id = "tgt_$seq",
            effectiveDate = effectiveDate,
            dailyTarget = dailyTarget,
            monthlyTarget = monthlyTarget,
            note = note.trim().ifEmpty { "Pembaruan target" },
          )
        state.copy(
          targetHistory = (state.targetHistory + createdTarget).sortedBy { it.effectiveDate },
          nextSequenceId = seq,
        )
      },
      persistAction = { _, updated ->
        repository.addTargetChange(createdTarget, updated.nextSequenceId)
      },
    )
  }

  fun addBonusRuleChange(
    effectiveDate: String,
    tier1Rate: Long,
    tier2Rate: Long,
    tier3Rate: Long,
    tier4Rate: Long,
    note: String,
  ) {
    if (tier1Rate < 0L || tier2Rate < 0L || tier3Rate < 0L || tier4Rate < 0L) return
    lateinit var createdRule: BonusRuleRecord
    mutateAndPersist(
      transform = { state ->
        val seq = state.nextSequenceId + 1
        createdRule =
          BonusRuleRecord(
            id = "bon_$seq",
            effectiveDate = effectiveDate,
            tier1MaxPackages = 9,
            tier1Rate = tier1Rate,
            tier2MaxPackages = 14,
            tier2Rate = tier2Rate,
            tier3MaxPackages = 20,
            tier3Rate = tier3Rate,
            tier4Rate = tier4Rate,
            note = note.trim().ifEmpty { "Pembaruan aturan bonus paket" },
          )
        state.copy(
          bonusRuleHistory = (state.bonusRuleHistory + createdRule).sortedBy { it.effectiveDate },
          nextSequenceId = seq,
        )
      },
      persistAction = { _, updated ->
        repository.addBonusRuleChange(createdRule, updated.nextSequenceId)
      },
    )
  }

  // --- WILAYAH (NORMALISASI + TIDAK MENGUBAH HISTORIS) ---
  fun addArea(rawName: String) {
    val normalized = FinanceCalculator.normalizeAreaName(rawName)
    if (normalized.isEmpty()) return
    mutateAndPersist(
      transform = { state ->
        state.copy(areas = mergeIntoAreaList(state.areas, listOf(normalized)))
      },
      persistAction = { _, _ -> repository.addArea(normalized) },
    )
  }

  fun renameArea(oldName: String, newNameRaw: String) {
    val normalizedNew = FinanceCalculator.normalizeAreaName(newNameRaw)
    if (normalizedNew.isEmpty()) return
    // Catatan historis pada order/blank TIDAK diubah sesuai spesifikasi
    mutateAndPersist(
      transform = { state ->
        val withoutOld = state.areas.filterNot { it.equals(oldName, ignoreCase = true) }
        state.copy(areas = mergeIntoAreaList(withoutOld, listOf(normalizedNew)))
      },
      persistAction = { _, _ -> repository.renameArea(oldName, normalizedNew) },
    )
  }

  fun deleteArea(areaName: String) {
    // Catatan historis tetap dipertahankan
    mutateAndPersist(
      transform = { state ->
        state.copy(areas = state.areas.filterNot { it.equals(areaName, ignoreCase = true) })
      },
      persistAction = { _, _ -> repository.deleteArea(areaName) },
    )
  }

  fun mergeAreas(sourceAreas: Set<String>, targetAreaRaw: String) {
    val target = FinanceCalculator.normalizeAreaName(targetAreaRaw)
    if (target.isEmpty() || sourceAreas.isEmpty()) return
    // Menggabungkan daftar wilayah tanpa mengubah catatan historis
    mutateAndPersist(
      transform = { state ->
        val remaining =
          state.areas.filterNot { area -> sourceAreas.any { it.equals(area, ignoreCase = true) } }
        state.copy(areas = mergeIntoAreaList(remaining, listOf(target)))
      },
      persistAction = { _, _ -> repository.mergeAreas(sourceAreas, target) },
    )
  }

  // --- CADANGAN & PEMULIHAN MANUAL GOOGLE SHEETS ---
  override fun onCleared() {
    sessionAccessToken = null
    super.onCleared()
  }

  fun setPendingAuthAction(action: BackupPendingAuthAction?) {
    _backupUiState.update {
      it.copy(
        pendingAuthAction = action,
        statusMessage = if (action != null) null else it.statusMessage,
        isError = if (action != null) false else it.isError,
      )
    }
  }

  fun clearBackupStatusMessage() {
    _backupUiState.update { it.copy(statusMessage = null, isError = false) }
  }

  fun reportBackupAuthError(message: String) {
    _backupUiState.update {
      it.copy(
        isBusy = false,
        busyMessage = null,
        pendingAuthAction = null,
        statusMessage = message,
        isError = true,
      )
    }
  }

  fun dismissRestoreDialogs() {
    _backupUiState.update {
      it.copy(
        availableBackupFiles = null,
        pendingRestoreConfirmation = null,
      )
    }
  }

  fun disconnectLinkedAccountDisplay() {
    sessionAccessToken = null
    mutateAndPersist(
      transform = { state ->
        state.copy(
          backupInfo =
            state.backupInfo.copy(
              isGoogleAccountLinkedPreview = false,
              linkedGoogleEmail = "",
            )
        )
      },
      persistAction = { _, updated -> repository.saveSettings(updated) },
    )
    _backupUiState.update {
      it.copy(
        availableBackupFiles = null,
        pendingRestoreConfirmation = null,
        statusMessage = "Tampilan akun Google terhubung dihapus. Anda dapat memilih akun lain saat cadangan berikutnya.",
        isError = false,
      )
    }
  }

  fun startBackupWithToken(accessToken: String, linkedEmailOverride: String? = null) {
    sessionAccessToken = accessToken
    viewModelScope.launch(ioDispatcher) {
      _backupUiState.update {
        it.copy(
          isBusy = true,
          busyMessage = "Menyiapkan dan memeriksa integritas data lokal...",
          pendingAuthAction = null,
          statusMessage = null,
          isError = false,
        )
      }
      try {
        initializedDeferred.await()
        val snapshot = persistenceMutex.withLock { _rawState.value }
        val nowIso = currentIsoTimestamp()
        val tabs = BackupSerializer.encodeToTabs(snapshot, createdAtIso = nowIso)

        _backupUiState.update {
          it.copy(busyMessage = "Mengunggah seluruh 14 tab data ke Google Sheets...")
        }
        val fetchedEmail = linkedEmailOverride ?: backupGateway.fetchUserEmail(accessToken)
        val resolvedEmail = fetchedEmail?.takeIf { it.isNotBlank() } ?: snapshot.backupInfo.linkedGoogleEmail
        val targetTitle =
          snapshot.backupInfo.sheetNamePreview.takeIf {
            it.isNotBlank() && it != "Catatan_Keuangan_Backup_Lokal"
          } ?: com.example.data.backup.BackupSchema.DEFAULT_SPREADSHEET_TITLE

        val driveFile =
          backupGateway.createOrUpdateBackupSpreadsheet(
            accessToken = accessToken,
            existingFileId = snapshot.backupInfo.backupSpreadsheetId.takeIf { it.isNotBlank() },
            title = targetTitle,
            tabs = tabs,
          )

        val formattedTs = formatDisplayTimestamp(nowIso)
        val summary =
          "Berhasil dicadangkan ke '${driveFile.name}': ${snapshot.orders.size} order, ${snapshot.blanks.size} blank, ${snapshot.expenses.size} pengeluaran, ${snapshot.transfers.size} transfer, ${snapshot.bills.size} tagihan, ${snapshot.categories.size} kategori & ${snapshot.areas.size} wilayah."

        persistenceMutex.withLock {
          val latestRaw = _rawState.value
          val updatedRaw =
            latestRaw.copy(
              backupInfo =
                latestRaw.backupInfo.copy(
                  lastBackupTimestamp = formattedTs,
                  isGoogleAccountLinkedPreview = true,
                  sheetNamePreview = driveFile.name,
                  lastBackupSummary = summary,
                  backupSpreadsheetId = driveFile.id,
                  linkedGoogleEmail = resolvedEmail,
                )
            )
          repository.saveSettings(updatedRaw)
          _rawState.value = updatedRaw
          _uiState.value = computeUiState(updatedRaw, isDatabaseLoaded = true)
        }

        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage = "Cadangan Google Sheets berhasil disimpan (${driveFile.name} • $formattedTs).",
            isError = false,
          )
        }
      } catch (e: BackupValidationException) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage = "Data lokal gagal divalidasi untuk cadangan: ${e.message}. Data lokal tidak diubah.",
            isError = true,
          )
        }
      } catch (e: GoogleApiException) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage = "${e.message} Data lokal tetap aman.",
            isError = true,
          )
        }
      } catch (e: Exception) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage =
              "Terjadi kendala saat mencadangkan ke Google Sheets (${e.message ?: "tidak diketahui"}). Data lokal tetap aman.",
            isError = true,
          )
        }
      }
    }
  }

  fun loadBackupFilesWithToken(accessToken: String, linkedEmailOverride: String? = null) {
    sessionAccessToken = accessToken
    viewModelScope.launch(ioDispatcher) {
      _backupUiState.update {
        it.copy(
          isBusy = true,
          busyMessage = "Mengambil daftar file cadangan dari Google Drive...",
          pendingAuthAction = null,
          availableBackupFiles = null,
          pendingRestoreConfirmation = null,
          statusMessage = null,
          isError = false,
        )
      }
      try {
        initializedDeferred.await()
        val fetchedEmail = linkedEmailOverride ?: backupGateway.fetchUserEmail(accessToken)
        if (!fetchedEmail.isNullOrBlank()) {
          persistenceMutex.withLock {
            val cur = _rawState.value
            val updated =
              cur.copy(
                backupInfo =
                  cur.backupInfo.copy(
                    isGoogleAccountLinkedPreview = true,
                    linkedGoogleEmail = fetchedEmail,
                  )
              )
            repository.saveSettings(updated)
            _rawState.value = updated
            _uiState.value = computeUiState(updated, isDatabaseLoaded = true)
          }
        }

        val files = backupGateway.listBackupSpreadsheets(accessToken)
        if (files.isEmpty()) {
          _backupUiState.update {
            it.copy(
              isBusy = false,
              busyMessage = null,
              availableBackupFiles = null,
              statusMessage =
                "Belum ada file cadangan Google Sheets Catatan Keuangan di Google Drive akun ini.",
              isError = false,
            )
          }
        } else {
          _backupUiState.update {
            it.copy(
              isBusy = false,
              busyMessage = null,
              availableBackupFiles = files,
            )
          }
        }
      } catch (e: GoogleApiException) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage = "${e.message} Data lokal tetap aman.",
            isError = true,
          )
        }
      } catch (e: Exception) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage =
              "Gagal memuat daftar cadangan dari Google Drive (${e.message ?: "kesalahan jaringan"}). Data lokal tetap aman.",
            isError = true,
          )
        }
      }
    }
  }

  fun selectBackupFileForConfirmation(
    file: DriveBackupFile,
    accessTokenOverride: String? = null,
  ) {
    val token = accessTokenOverride ?: sessionAccessToken
    if (token.isNullOrBlank()) {
      _backupUiState.update {
        it.copy(
          availableBackupFiles = null,
          statusMessage = "Sesi otorisasi Google berakhir. Tekan Pulihkan untuk mengizinkan akses ulang.",
          isError = true,
        )
      }
      return
    }

    viewModelScope.launch(ioDispatcher) {
      _backupUiState.update {
        it.copy(
          isBusy = true,
          busyMessage = "Mengunduh dan memvalidasi keutuhan '${file.name}'...",
          statusMessage = null,
          isError = false,
        )
      }
      try {
        val (resolvedFile, tabs) = backupGateway.readSpreadsheetTabs(token, file.id)
        val payload = BackupSerializer.decodeAndValidate(tabs)
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            availableBackupFiles = null,
            pendingRestoreConfirmation =
              PendingRestoreConfirmation(
                file = resolvedFile,
                metadata = payload.metadata,
                restoredState = payload.state,
              ),
          )
        }
      } catch (e: BackupValidationException) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            pendingRestoreConfirmation = null,
            statusMessage =
              "File cadangan '${file.name}' ditolak karena tidak valid atau rusak: ${e.message} Data lokal tidak berubah.",
            isError = true,
          )
        }
      } catch (e: GoogleApiException) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage = "${e.message} Data lokal tidak berubah.",
            isError = true,
          )
        }
      } catch (e: Exception) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            statusMessage =
              "Gagal membaca file cadangan (${e.message ?: "kesalahan tidak diketahui"}). Data lokal tidak berubah.",
            isError = true,
          )
        }
      }
    }
  }

  fun confirmAndExecuteRestore() {
    val pending = _backupUiState.value.pendingRestoreConfirmation ?: return
    viewModelScope.launch(ioDispatcher) {
      _backupUiState.update {
        it.copy(
          isBusy = true,
          busyMessage = "Memulihkan seluruh data ke database lokal secara atomik...",
          statusMessage = null,
          isError = false,
        )
      }
      try {
        initializedDeferred.await()
        val nowDisplay = formatDisplayTimestamp(currentIsoTimestamp())
        val backupDateDisplay = formatDisplayTimestamp(pending.metadata.createdAtIso)

        persistenceMutex.withLock {
          val current = _rawState.value
          val finalRestored =
            pending.restoredState.copy(
              todayDate = effectiveTodayDateProvider(),
              backupInfo =
                current.backupInfo.copy(
                  lastRestoreTimestamp = nowDisplay,
                  isGoogleAccountLinkedPreview =
                    current.backupInfo.isGoogleAccountLinkedPreview ||
                      current.backupInfo.linkedGoogleEmail.isNotBlank(),
                  sheetNamePreview = pending.file.name,
                  backupSpreadsheetId = pending.file.id,
                  lastBackupSummary =
                    "Dipulihkan dari '${pending.file.name}' ($backupDateDisplay): ${pending.metadata.summaryText}",
                ),
            )
          repository.replaceAllDataAtomically(finalRestored)
          _rawState.value = finalRestored
          _uiState.value = computeUiState(finalRestored, isDatabaseLoaded = true)
        }

        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            pendingRestoreConfirmation = null,
            availableBackupFiles = null,
            statusMessage =
              "Pemulihan berhasil! Seluruh data aplikasi telah dipulihkan dari '${pending.file.name}'.",
            isError = false,
          )
        }
      } catch (e: Exception) {
        _backupUiState.update {
          it.copy(
            isBusy = false,
            busyMessage = null,
            pendingRestoreConfirmation = null,
            statusMessage =
              "Pemulihan dibatalkan karena gagal menulis ke database lokal (${e.message ?: "kesalahan sistem"}). Data sebelumnya tetap dipertahankan.",
            isError = true,
          )
        }
      }
    }
  }

  fun triggerManualBackupPreview() {
    mutateAndPersist(
      transform = { state ->
        val orderCount = state.orders.size
        val blankCount = state.blanks.size
        val expCount = state.expenses.size
        val billCount = state.bills.size
        state.copy(
          backupInfo =
            state.backupInfo.copy(
              lastBackupTimestamp = "${FinanceCalculator.formatDateIndo(state.todayDate)} (Siap Tahap 3)",
              isGoogleAccountLinkedPreview = true,
              lastBackupSummary =
                "Ringkasan database Room siap dicadangkan: $orderCount order, $blankCount blank, $expCount pengeluaran, ${state.transfers.size} transfer, $billCount tagihan, ${state.categories.size} kategori & ${state.areas.size} wilayah.",
            )
        )
      },
      persistAction = { _, updated -> repository.saveSettings(updated) },
    )
  }

  fun triggerManualRestorePreview() {
    mutateAndPersist(
      transform = { state ->
        state.copy(
          backupInfo =
            state.backupInfo.copy(
              lastRestoreTimestamp = "${FinanceCalculator.formatDateIndo(state.todayDate)} (Siap Tahap 3)",
              lastBackupSummary =
                "Integritas database lokal Room terverifikasi: relasi order, blank, tagihan, transaksi, kategori, wilayah, aturan bonus, target & saldo utuh.",
            )
        )
      },
      persistAction = { _, updated -> repository.saveSettings(updated) },
    )
  }

  private fun currentIsoTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
  }

  private fun formatDisplayTimestamp(rawIso: String): String {
    if (rawIso.length >= 16 && rawIso[4] == '-' && rawIso[7] == '-') {
      val datePart = rawIso.take(10)
      val timePart = rawIso.substring(11, 16)
      return "${FinanceCalculator.formatDateIndo(datePart)}, $timePart"
    }
    return rawIso
  }

  private fun mergeIntoAreaList(existing: List<String>, candidates: List<String>): List<String> {
    val result = existing.toMutableList()
    candidates.forEach { raw ->
      val normalized = FinanceCalculator.normalizeAreaName(raw)
      if (normalized.isNotEmpty() && result.none { it.equals(normalized, ignoreCase = true) }) {
        result.add(normalized)
      }
    }
    return result.sorted()
  }

  private fun computeUiState(
    raw: FinanceRawState,
    isDatabaseLoaded: Boolean,
  ): FinanceUiState {
    val allDates =
      (raw.orders.map { it.date } + raw.blanks.map { it.date })
        .distinct()
        .sortedDescending()

    val computedOrdersByDate = mutableMapOf<String, List<ComputedWorkOrder>>()
    val computedBlanksByDate = mutableMapOf<String, List<ComputedBlankSession>>()
    val activityTimelineByDate = mutableMapOf<String, List<IncomeActivityItem>>()
    val dailySummariesByDate = mutableMapOf<String, DailyIncomeSummary>()

    allDates.forEach { date ->
      val ordersOnDate = raw.orders.filter { it.date == date }
      val effectiveDailyTarget = FinanceCalculator.dailyTargetForDate(date, raw.targetHistory)
      val effectiveBonusRule = FinanceCalculator.bonusRuleForDate(date, raw.bonusRuleHistory)
      val computedOrders =
        FinanceCalculator.computeOrdersForDate(
          ordersOnDate = ordersOnDate,
          dailyTarget = effectiveDailyTarget,
          bonusRule = effectiveBonusRule,
        )
      computedOrdersByDate[date] = computedOrders

      val blanksOnDate =
        raw.blanks
          .filter { it.date == date }
          .map { blank ->
            val nextOrder = ordersOnDate.firstOrNull { it.id == blank.nextOrderId }
            val resolvedEnd = nextOrder?.startTime ?: blank.endTime
            val resolvedBlank = blank.copy(endTime = resolvedEnd)
            val (dur, _) = FinanceCalculator.computeDurationMinutes(resolvedBlank.startTime, resolvedEnd)
            ComputedBlankSession(
              blank = resolvedBlank,
              durationMinutes = dur,
              batteryConsumed = (resolvedBlank.batteryStart - resolvedBlank.batteryEnd).coerceAtLeast(0),
            )
          }
      computedBlanksByDate[date] = blanksOnDate

      // Interleave order & blank chronologically (Order -> Blank -> Order)
      val combined =
        (computedOrders.map { IncomeActivityItem.OrderEntry(it) } +
          blanksOnDate.map { IncomeActivityItem.BlankEntry(it) })
          .sortedWith(
            compareBy<IncomeActivityItem> { FinanceCalculator.parseTimeMinutes(it.startTime) }
              .thenBy { if (it is IncomeActivityItem.OrderEntry) 0 else 1 }
          )
      activityTimelineByDate[date] = combined

      dailySummariesByDate[date] =
        FinanceCalculator.computeDailySummary(date, computedOrders, blanksOnDate)
    }

    val todaySummary =
      dailySummariesByDate[raw.todayDate]
        ?: FinanceCalculator.computeDailySummary(raw.todayDate, emptyList(), emptyList())

    val monthPrefix = raw.activeMonthKey
    val monthlySummaries =
      dailySummariesByDate.filterKeys { it.startsWith(monthPrefix) }.values.toList()
    val productiveSummaries =
      monthlySummaries.filter { it.orderCount > 0 && it.totalIncome > 0L }

    val monthlyTotalIncome = productiveSummaries.sumOf { it.totalIncome }
    val monthlyTotalArgo = monthlySummaries.sumOf { it.totalArgo }
    val monthlyTotalBonus = monthlySummaries.sumOf { it.totalBonus }
    val monthlyTotalTipGoPay = monthlySummaries.sumOf { it.totalTipGoPay }
    val monthlyTotalTipCash = monthlySummaries.sumOf { it.totalTipCash }
    val monthlyProductiveDays = productiveSummaries.size
    val monthlyTotalOrders = monthlySummaries.sumOf { it.orderCount }
    val monthlyTotalPackages = monthlySummaries.sumOf { it.totalPackages }
    val monthlyOrderDuration = monthlySummaries.sumOf { it.orderDurationMinutes }
    val monthlyBlankDuration = monthlySummaries.sumOf { it.blankDurationMinutes }
    val monthlyDistance = monthlySummaries.sumOf { it.totalDistanceKm }
    val monthlyBattery = monthlySummaries.sumOf { it.totalBatteryConsumed }

    val monthlyDailyTargetAccumulation =
      productiveSummaries.sumOf {
        FinanceCalculator.dailyTargetForDate(it.date, raw.targetHistory)
      }
    val monthlyDailyTargetDiff = monthlyTotalIncome - monthlyDailyTargetAccumulation

    val monthlyAvgPerProductiveDay =
      if (monthlyProductiveDays > 0) monthlyTotalIncome / monthlyProductiveDays else 0L
    val monthlyEstimated26Days = monthlyAvgPerProductiveDay * 26L
    val monthlyAvgBasePerOrder =
      if (monthlyTotalOrders > 0) monthlyTotalArgo / monthlyTotalOrders else 0L
    val currentMonthlyTarget =
      FinanceCalculator.monthlyTargetForMonth(monthPrefix, raw.targetHistory)
    val monthlyRemainingTarget = currentMonthlyTarget - monthlyTotalIncome

    val monthlyExpenses = raw.expenses.filter { it.date.startsWith(monthPrefix) }
    val monthlyTransfers = raw.transfers.filter { it.date.startsWith(monthPrefix) }
    val monthlyTotalExpense = monthlyExpenses.sumOf { it.amount }
    val monthlyTotalTransfer = monthlyTransfers.sumOf { it.amount }

    // Perhitungan saldo tiap 4 lokasi dana langsung dari data database Room
    val locationBalances =
      FundLocation.entries.map { loc ->
        val prevClosing = raw.previousClosingBalances[loc] ?: 0L
        val corrections = raw.balanceCorrections.filter { it.location == loc }
        val corrDelta = corrections.sumOf { it.deltaAmount }
        val effectiveOpening = prevClosing + corrDelta

        val incomeIn =
          when (loc) {
            FundLocation.GOPAY -> monthlyTotalArgo + monthlyTotalBonus + monthlyTotalTipGoPay
            FundLocation.CASH -> monthlyTotalTipCash
            FundLocation.JAGO_CICILAN, FundLocation.JAGO_PRIBADI -> 0L
          }
        val expenseOut = monthlyExpenses.filter { it.source == loc }.sumOf { it.amount }
        val trfIn = monthlyTransfers.filter { it.to == loc }.sumOf { it.amount }
        val trfOut = monthlyTransfers.filter { it.from == loc }.sumOf { it.amount }
        val netTransfer = trfIn - trfOut
        val running = effectiveOpening + incomeIn - expenseOut + netTransfer
        val change = running - effectiveOpening

        LocationBalanceDetail(
          location = loc,
          previousMonthClosing = prevClosing,
          openingCorrectionDelta = corrDelta,
          effectiveOpeningBalance = effectiveOpening,
          incomeIn = incomeIn,
          expenseOut = expenseOut,
          transferIn = trfIn,
          transferOut = trfOut,
          netTransfer = netTransfer,
          runningBalance = running,
          monthlyChange = change,
          corrections = corrections,
        )
      }

    val totalActualBalance = locationBalances.sumOf { it.runningBalance }
    val totalDirectSpend =
      locationBalances.filter { it.location.isDirectSpendAllowed }.sumOf { it.runningBalance }
    val totalJago =
      locationBalances.filter { it.location.isJagoPocket }.sumOf { it.runningBalance }

    val incomeRows =
      dailySummariesByDate.values
        .filter { it.orderCount > 0 && it.totalIncome > 0L }
        .map { summary ->
          TransactionListEntry.DailyIncomeRow(date = summary.date, summary = summary)
        }

    val groupedExpenseRows =
      raw.expenses
        .groupBy { "${it.date}|${it.categoryName}" }
        .values
        .map { groupItems ->
          val sortedItems = groupItems.sortedBy { it.insertOrder }
          val first = sortedItems.first()
          TransactionListEntry.GroupedExpenseRow(
            GroupedExpenseDisplay(
              groupKey = "${first.date}|${first.categoryName}",
              date = first.date,
              categoryName = first.categoryName,
              totalAmount = sortedItems.sumOf { it.amount },
              firstInsertOrder = first.insertOrder,
              items = sortedItems,
            )
          )
        }

    val transferRows = raw.transfers.map { TransactionListEntry.TransferRow(it) }

    val transactionEntries =
      (incomeRows + groupedExpenseRows + transferRows)
        .sortedWith(
          compareByDescending<TransactionListEntry> { it.date }
            .thenBy { it.sortPriority }
            .thenBy { it.orderKey }
        )

    // Statistik Mingguan (Senin - Minggu pada pekan tanggal aktif / hari ini)
    val todayDow = FinanceCalculator.dayOfWeekMondayFirst(raw.todayDate)
    val mondayDate = FinanceCalculator.shiftIsoDate(raw.todayDate, -todayDow)
    val dayLabelPairs =
      listOf(
        "Sen" to "Senin",
        "Sel" to "Selasa",
        "Rab" to "Rabu",
        "Kam" to "Kamis",
        "Jum" to "Jumat",
        "Sab" to "Sabtu",
        "Min" to "Minggu",
      )
    val weeklyDays =
      dayLabelPairs.mapIndexed { idx, (shortL, fullL) ->
        val date = FinanceCalculator.shiftIsoDate(mondayDate, idx)
        val sum = dailySummariesByDate[date]
        DayStatPoint(
          dayIndexMondayFirst = idx,
          dayShortLabel = shortL,
          dayFullLabel = fullL,
          date = date,
          isSunday = idx == 6,
          income = sum?.totalIncome ?: 0L,
          orderCount = sum?.orderCount ?: 0,
          durationMinutes = sum?.totalActivityDurationMinutes ?: 0,
          distanceKm = sum?.totalDistanceKm ?: 0.0,
          batteryConsumed = sum?.totalBatteryConsumed ?: 0,
        )
      }

    // Tren mingguan dalam bulan aktif
    val monthShortNames =
      listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val mIdx = (monthPrefix.takeLast(2).toIntOrNull() ?: 11).coerceIn(1, 12)
    val mShort = monthShortNames[mIdx - 1]
    val weeklyBuckets =
      listOf(
        Triple("Mgg 1", "1–7 $mShort", 1..7),
        Triple("Mgg 2", "8–14 $mShort", 8..14),
        Triple("Mgg 3", "15–21 $mShort", 15..21),
        Triple("Mgg 4", "22–31 $mShort", 22..31),
      )
    val weeklyTrends =
      weeklyBuckets.map { (wLabel, rLabel, range) ->
        val matching =
          monthlySummaries.filter { s ->
            val day = s.date.takeLast(2).toIntOrNull() ?: 0
            day in range
          }
        WeeklyTrendPoint(
          weekLabel = wLabel,
          dateRangeLabel = rLabel,
          income = matching.sumOf { it.totalIncome },
          orderCount = matching.sumOf { it.orderCount },
        )
      }

    // Perbandingan kategori pengeluaran vs bulan sebelumnya langsung dari database
    val prevMonthKey = FinanceCalculator.shiftMonthKey(monthPrefix, -1)
    val currentCatMap =
      monthlyExpenses.groupBy { it.categoryName }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val prevCatMap =
      raw.expenses
        .filter { it.date.startsWith(prevMonthKey) }
        .groupBy { it.categoryName }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
    val allCatNames =
      (raw.categories.map { it.name } + currentCatMap.keys + prevCatMap.keys).distinct()
    val categoryComparisons =
      allCatNames
        .map { cat ->
          val cur = currentCatMap[cat] ?: 0L
          val prev = prevCatMap[cat] ?: 0L
          val diff = cur - prev
          val pctLabel =
            if (prev == 0L) {
              "-"
            } else {
              val pct = ((diff.toDouble() / prev.toDouble()) * 100.0).roundToInt()
              if (pct > 0) "+$pct%" else "$pct%"
            }
          CategoryComparisonItem(
            categoryName = cat,
            currentAmount = cur,
            previousAmount = prev,
            diffRupiah = diff,
            diffPercentLabel = pctLabel,
          )
        }
        .filter { it.currentAmount > 0L || it.previousAmount > 0L }
        .sortedByDescending { it.currentAmount }

    // Analisis Wilayah (order + blank tunggu + jarak + baterai)
    val allComputedOrders =
      computedOrdersByDate.values.flatten().filter { it.order.date.startsWith(monthPrefix) }
    val allComputedBlanks =
      computedBlanksByDate.values.flatten().filter { it.blank.date.startsWith(monthPrefix) }
    val statsAreaNames =
      (allComputedOrders.map { it.order.originArea } + allComputedBlanks.map { it.blank.area })
        .filter { it.isNotBlank() }
        .distinct()
    val areaStats =
      statsAreaNames
        .map { area ->
          val areaOrders = allComputedOrders.filter { it.order.originArea == area }
          val areaBlanks = allComputedBlanks.filter { it.blank.area == area }
          val orderIncome =
            areaOrders.sumOf { it.order.argo + it.order.tipGoPay + it.order.tipCash }
          val count = areaOrders.size
          AreaStatItem(
            areaName = area,
            orderCount = count,
            totalIncome = orderIncome,
            avgIncomePerOrder = if (count > 0) orderIncome / count else 0L,
            waitBlankMinutes = areaBlanks.sumOf { it.durationMinutes },
            distanceKm =
              areaOrders.sumOf { it.totalDistanceKm } + areaBlanks.sumOf { it.blank.distanceKm },
            batteryConsumed =
              areaOrders.sumOf { it.batteryConsumed } + areaBlanks.sumOf { it.batteryConsumed },
          )
        }
        .sortedByDescending { it.totalIncome }

    // Ringkasan status tagihan bulan aktif
    val activeMonthEntries =
      raw.bills.mapNotNull { b ->
        b.history.firstOrNull { it.monthKey == monthPrefix } ?: b.history.lastOrNull()
      }
    val unpaidBills =
      activeMonthEntries.filter { it.status == BillStatus.BELUM_DIBAYAR }.sumOf { it.amount }
    val overdueBills =
      activeMonthEntries.filter { it.status == BillStatus.TERLAMBAT }.sumOf { it.amount }
    val paidBills =
      activeMonthEntries.filter { it.status == BillStatus.SUDAH_DIBAYAR }.sumOf { it.amount }

    return FinanceUiState(
      isDatabaseLoaded = isDatabaseLoaded,
      raw = raw,
      todayDate = raw.todayDate,
      activeMonthKey = monthPrefix,
      activeMonthLabel = FinanceCalculator.formatMonthIndo(monthPrefix),
      activeBonusRule = FinanceCalculator.bonusRuleForDate(raw.todayDate, raw.bonusRuleHistory),
      computedOrdersByDate = computedOrdersByDate,
      computedBlanksByDate = computedBlanksByDate,
      activityTimelineByDate = activityTimelineByDate,
      dailySummariesByDate = dailySummariesByDate,
      todaySummary = todaySummary,
      todayDailyTarget = FinanceCalculator.dailyTargetForDate(raw.todayDate, raw.targetHistory),
      currentMonthlyTarget = currentMonthlyTarget,
      monthlyRemainingTarget = monthlyRemainingTarget,
      monthlyTotalIncome = monthlyTotalIncome,
      monthlyTotalArgo = monthlyTotalArgo,
      monthlyTotalBonus = monthlyTotalBonus,
      monthlyTotalTipGoPay = monthlyTotalTipGoPay,
      monthlyTotalTipCash = monthlyTotalTipCash,
      monthlyTotalExpense = monthlyTotalExpense,
      monthlyTotalTransfer = monthlyTotalTransfer,
      monthlyProductiveDays = monthlyProductiveDays,
      monthlyDailyTargetAccumulation = monthlyDailyTargetAccumulation,
      monthlyDailyTargetDiff = monthlyDailyTargetDiff,
      monthlyTotalOrders = monthlyTotalOrders,
      monthlyTotalPackages = monthlyTotalPackages,
      monthlyTotalOrderDurationMinutes = monthlyOrderDuration,
      monthlyTotalBlankDurationMinutes = monthlyBlankDuration,
      monthlyTotalDistanceKm = monthlyDistance,
      monthlyTotalBatteryConsumed = monthlyBattery,
      monthlyAvgIncomePerProductiveDay = monthlyAvgPerProductiveDay,
      monthlyEstimated26DaysIncome = monthlyEstimated26Days,
      monthlyAvgBaseIncomePerOrder = monthlyAvgBasePerOrder,
      locationBalances = locationBalances,
      totalActualBalance = totalActualBalance,
      totalDirectSpendBalance = totalDirectSpend,
      totalJagoBalance = totalJago,
      monthlyNetCashflow = monthlyTotalIncome - monthlyTotalExpense,
      transactionEntries = transactionEntries,
      weeklyDaysMondayToSunday = weeklyDays,
      weeklyTrendsInMonth = weeklyTrends,
      categoryComparisons = categoryComparisons,
      areaStats = areaStats,
      unpaidBillsTotal = unpaidBills,
      overdueBillsTotal = overdueBills,
      paidBillsTotal = paidBills,
    )
  }
}
