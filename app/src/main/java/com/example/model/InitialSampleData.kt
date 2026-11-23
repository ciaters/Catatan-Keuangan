package com.example.model

object InitialSampleData {
  const val TODAY_DATE = "2026-11-21" // Sabtu, 21 Nov 2026
  const val ACTIVE_MONTH_KEY = "2026-11"
  const val ACTIVE_MONTH_LABEL = "November 2026"
  const val PREVIOUS_MONTH_KEY = "2026-10"
  const val PREVIOUS_MONTH_LABEL = "Oktober 2026"

  // Target awal: harian Rp250.000, bulanan Rp6.000.000 + riwayat perubahan
  val initialTargetHistory =
    listOf(
      IncomeTargetRecord(
        id = "tgt1",
        effectiveDate = "2026-10-01",
        dailyTarget = 225_000L,
        monthlyTarget = 5_500_000L,
        note = "Target awal bulan Oktober",
      ),
      IncomeTargetRecord(
        id = "tgt2",
        effectiveDate = "2026-11-01",
        dailyTarget = 250_000L,
        monthlyTarget = 6_000_000L,
        note = "Target standar harian & bulanan November",
      ),
    )

  // Kategori pengeluaran sepenuhnya milik pengguna (tanpa kategori BBM karena motor listrik)
  val initialCategories =
    listOf(
      ExpenseCategory("cat1", "Swap & Cas Baterai"),
      ExpenseCategory("cat2", "Makan & Minum"),
      ExpenseCategory("cat3", "Kuota Internet"),
      ExpenseCategory("cat4", "Servis Motor Listrik"),
      ExpenseCategory("cat5", "Tagihan & Cicilan"),
      ExpenseCategory("cat6", "Kebutuhan Pribadi"),
    )

  // Daftar wilayah ternormalisasi
  val initialAreas =
    listOf(
      "Kemang",
      "Kebayoran Baru",
      "Sudirman",
      "Kuningan",
      "Tebet",
      "Senopati",
      "Blok M",
      "Cilandak",
      "Pancoran",
    )

  // Saldo akhir Oktober 2026 -> otomatis menjadi saldo awal November 2026
  val initialPreviousClosingBalances: Map<FundLocation, Long> =
    mapOf(
      FundLocation.GOPAY to 420_000L,
      FundLocation.CASH to 185_000L,
      FundLocation.JAGO_CICILAN to 1_600_000L,
      FundLocation.JAGO_PRIBADI to 2_450_000L,
    )

  val initialBalanceCorrections =
    listOf(
      BalanceCorrection(
        id = "cor1",
        location = FundLocation.CASH,
        date = "2026-11-01",
        deltaAmount = 15_000L,
        note = "Penyesuaian uang receh fisik di dompet awal bulan",
      )
    )

  // Data contoh Order Kerja bulan November 2026 (Senin 16 Nov s/d Sabtu 21 Nov + awal bulan)
  val initialOrders =
    listOf(
      // Hari ini: Sabtu, 21 Nov 2026
      WorkOrder(
        id = "ord_21_1",
        date = "2026-11-21",
        startTime = "07:45",
        endTime = "08:30",
        argo = 48_000L,
        packageCount = 4,
        tipGoPay = 5_000L,
        tipCash = 0L,
        pickupDistanceKm = 1.4,
        deliveryDistanceKm = 6.8,
        originArea = "Kemang",
        destinationArea = "Sudirman",
        batteryStart = 98,
        batteryEnd = 86,
        note = "Paket pagi perkantoran",
      ),
      WorkOrder(
        id = "ord_21_2",
        date = "2026-11-21",
        startTime = "09:10",
        endTime = "10:05",
        argo = 62_000L,
        packageCount = 6, // Akumulasi 10 paket -> masuk tier 10-14 (Rp2.250/paket)
        tipGoPay = 10_000L,
        tipCash = 5_000L,
        pickupDistanceKm = 0.9,
        deliveryDistanceKm = 8.4,
        originArea = "Sudirman",
        destinationArea = "Kuningan",
        batteryStart = 84,
        batteryEnd = 69,
        note = "",
      ),
      WorkOrder(
        id = "ord_21_3",
        date = "2026-11-21",
        startTime = "10:40",
        endTime = "11:35",
        argo = 74_000L,
        packageCount = 5, // Akumulasi 15 paket -> masuk tier 15-20 (Rp3.000/paket) -> 45.000
        tipGoPay = 0L,
        tipCash = 10_000L,
        pickupDistanceKm = 1.8,
        deliveryDistanceKm = 9.2,
        originArea = "Kuningan",
        destinationArea = "Tebet",
        batteryStart = 66,
        batteryEnd = 51,
        note = "Multi drop 5 titik lancar",
      ),
      // Jumat, 20 Nov 2026 (termasuk order lintas tengah malam)
      WorkOrder(
        id = "ord_20_1",
        date = "2026-11-20",
        startTime = "08:15",
        endTime = "09:10",
        argo = 54_000L,
        packageCount = 5,
        tipGoPay = 5_000L,
        tipCash = 0L,
        pickupDistanceKm = 1.2,
        deliveryDistanceKm = 7.1,
        originArea = "Kebayoran Baru",
        destinationArea = "Senopati",
        batteryStart = 95,
        batteryEnd = 82,
        note = "",
      ),
      WorkOrder(
        id = "ord_20_2",
        date = "2026-11-20",
        startTime = "13:20",
        endTime = "14:25",
        argo = 68_000L,
        packageCount = 7,
        tipGoPay = 0L,
        tipCash = 10_000L,
        pickupDistanceKm = 1.5,
        deliveryDistanceKm = 8.8,
        originArea = "Blok M",
        destinationArea = "Kemang",
        batteryStart = 79,
        batteryEnd = 64,
        note = "Tukar baterai di stasiun Blok M",
      ),
      WorkOrder(
        id = "ord_20_3",
        date = "2026-11-20",
        startTime = "23:25",
        endTime = "00:20", // Melewati tengah malam -> tetap tanggal 2026-11-20
        argo = 58_000L,
        packageCount = 4, // Akumulasi 16 paket
        tipGoPay = 10_000L,
        tipCash = 0L,
        pickupDistanceKm = 2.0,
        deliveryDistanceKm = 7.6,
        originArea = "Cilandak",
        destinationArea = "Kebayoran Baru",
        batteryStart = 62,
        batteryEnd = 48,
        note = "Order malam lintas tengah malam",
      ),
      // Kamis, 19 Nov 2026
      WorkOrder(
        id = "ord_19_1",
        date = "2026-11-19",
        startTime = "08:00",
        endTime = "09:05",
        argo = 72_000L,
        packageCount = 6,
        tipGoPay = 5_000L,
        tipCash = 5_000L,
        pickupDistanceKm = 1.1,
        deliveryDistanceKm = 8.5,
        originArea = "Tebet",
        destinationArea = "Kuningan",
        batteryStart = 99,
        batteryEnd = 84,
        note = "",
      ),
      WorkOrder(
        id = "ord_19_2",
        date = "2026-11-19",
        startTime = "10:15",
        endTime = "11:40",
        argo = 115_000L,
        packageCount = 10, // Akumulasi 16 paket
        tipGoPay = 15_000L,
        tipCash = 0L,
        pickupDistanceKm = 2.1,
        deliveryDistanceKm = 12.4,
        originArea = "Kuningan",
        destinationArea = "Sudirman",
        batteryStart = 81,
        batteryEnd = 61,
        note = "Borongan korporat",
      ),
      // Rabu, 18 Nov 2026 -> sengaja tanpa kerja (Rp0 di grafik mingguan)
      // Selasa, 17 Nov 2026
      WorkOrder(
        id = "ord_17_1",
        date = "2026-11-17",
        startTime = "08:30",
        endTime = "09:50",
        argo = 86_000L,
        packageCount = 8,
        tipGoPay = 10_000L,
        tipCash = 0L,
        pickupDistanceKm = 1.3,
        deliveryDistanceKm = 9.4,
        originArea = "Kemang",
        destinationArea = "Cilandak",
        batteryStart = 96,
        batteryEnd = 80,
        note = "",
      ),
      WorkOrder(
        id = "ord_17_2",
        date = "2026-11-17",
        startTime = "11:00",
        endTime = "12:15",
        argo = 92_000L,
        packageCount = 9, // Akumulasi 17 paket
        tipGoPay = 5_000L,
        tipCash = 10_000L,
        pickupDistanceKm = 1.7,
        deliveryDistanceKm = 10.2,
        originArea = "Cilandak",
        destinationArea = "Blok M",
        batteryStart = 78,
        batteryEnd = 60,
        note = "",
      ),
      // Senin, 16 Nov 2026
      WorkOrder(
        id = "ord_16_1",
        date = "2026-11-16",
        startTime = "08:10",
        endTime = "09:20",
        argo = 79_000L,
        packageCount = 7,
        tipGoPay = 0L,
        tipCash = 5_000L,
        pickupDistanceKm = 1.0,
        deliveryDistanceKm = 8.9,
        originArea = "Pancoran",
        destinationArea = "Tebet",
        batteryStart = 97,
        batteryEnd = 83,
        note = "",
      ),
      WorkOrder(
        id = "ord_16_2",
        date = "2026-11-16",
        startTime = "10:05",
        endTime = "11:25",
        argo = 110_000L,
        packageCount = 14, // Akumulasi 21 paket -> >20 paket (Rp3.500/paket)
        tipGoPay = 10_000L,
        tipCash = 5_000L,
        pickupDistanceKm = 1.6,
        deliveryDistanceKm = 11.8,
        originArea = "Tebet",
        destinationArea = "Sudirman",
        batteryStart = 81,
        batteryEnd = 60,
        note = "Tembus tier tertinggi >20 paket",
      ),
      // Minggu awal bulan (Minggu 1 & 2 November 2026 untuk tren bulanan)
      WorkOrder(
        id = "ord_12_1",
        date = "2026-11-12",
        startTime = "08:20",
        endTime = "10:30",
        argo = 195_000L,
        packageCount = 16,
        tipGoPay = 15_000L,
        tipCash = 10_000L,
        pickupDistanceKm = 2.4,
        deliveryDistanceKm = 18.0,
        originArea = "Kemang",
        destinationArea = "Kuningan",
        batteryStart = 98,
        batteryEnd = 66,
        note = "Rute penuh tengah bulan",
      ),
      WorkOrder(
        id = "ord_09_1",
        date = "2026-11-09",
        startTime = "08:00",
        endTime = "10:10",
        argo = 182_000L,
        packageCount = 15,
        tipGoPay = 10_000L,
        tipCash = 5_000L,
        pickupDistanceKm = 1.9,
        deliveryDistanceKm = 16.5,
        originArea = "Sudirman",
        destinationArea = "Blok M",
        batteryStart = 97,
        batteryEnd = 68,
        note = "",
      ),
      WorkOrder(
        id = "ord_05_1",
        date = "2026-11-05",
        startTime = "08:30",
        endTime = "10:45",
        argo = 175_000L,
        packageCount = 14,
        tipGoPay = 10_000L,
        tipCash = 5_000L,
        pickupDistanceKm = 2.0,
        deliveryDistanceKm = 15.8,
        originArea = "Senopati",
        destinationArea = "Kemang",
        batteryStart = 95,
        batteryEnd = 67,
        note = "",
      ),
    )

  // Waktu Kosong (Blank) selalu berada di antara order & berakhir di jam mulai order berikutnya
  // Tidak ada Blank setelah order terakhir hari tersebut
  val initialBlanks =
    listOf(
      BlankSession(
        id = "blk_21_1",
        date = "2026-11-21",
        startTime = "08:30",
        endTime = "09:10",
        nextOrderId = "ord_21_2",
        area = "Sudirman",
        batteryStart = 86,
        batteryEnd = 84,
        distanceKm = 0.8,
        note = "Geser titik tunggu ke area lobi",
      ),
      BlankSession(
        id = "blk_21_2",
        date = "2026-11-21",
        startTime = "10:05",
        endTime = "10:40",
        nextOrderId = "ord_21_3",
        area = "Kuningan",
        batteryStart = 69,
        batteryEnd = 66,
        distanceKm = 0.0, // Jarak 0 tetap dicatat
        note = "Menunggu di tempat tanpa berpindah",
      ),
      BlankSession(
        id = "blk_20_1",
        date = "2026-11-20",
        startTime = "09:10",
        endTime = "13:20",
        nextOrderId = "ord_20_2",
        area = "Senopati",
        batteryStart = 82,
        batteryEnd = 79,
        distanceKm = 1.4,
        note = "Tunggu order siang + geser ke Blok M",
      ),
      BlankSession(
        id = "blk_19_1",
        date = "2026-11-19",
        startTime = "09:05",
        endTime = "10:15",
        nextOrderId = "ord_19_2",
        area = "Kuningan",
        batteryStart = 84,
        batteryEnd = 81,
        distanceKm = 0.6,
        note = "",
      ),
      BlankSession(
        id = "blk_17_1",
        date = "2026-11-17",
        startTime = "09:50",
        endTime = "11:00",
        nextOrderId = "ord_17_2",
        area = "Cilandak",
        batteryStart = 80,
        batteryEnd = 78,
        distanceKm = 0.0,
        note = "Istirahat tunggu order berikutnya",
      ),
      BlankSession(
        id = "blk_16_1",
        date = "2026-11-16",
        startTime = "09:20",
        endTime = "10:05",
        nextOrderId = "ord_16_2",
        area = "Tebet",
        batteryStart = 83,
        batteryEnd = 81,
        distanceKm = 0.5,
        note = "",
      ),
    )

  // Pengeluaran November 2026 (tanpa jam, hanya dari GoPay atau Cash, tanpa kategori BBM)
  val initialExpenses =
    listOf(
      ExpenseRecord(
        id = "exp1",
        date = "2026-11-21",
        categoryName = "Makan & Minum",
        amount = 24_000L,
        source = FundLocation.CASH,
        note = "Nasi warteg siang",
        insertOrder = 1L,
      ),
      ExpenseRecord(
        id = "exp2",
        date = "2026-11-21",
        categoryName = "Makan & Minum",
        amount = 10_000L,
        source = FundLocation.GOPAY,
        note = "Es teh & air mineral",
        insertOrder = 2L,
      ),
      ExpenseRecord(
        id = "exp3",
        date = "2026-11-21",
        categoryName = "Swap & Cas Baterai",
        amount = 18_000L,
        source = FundLocation.GOPAY,
        note = "Tukar baterai motor listrik 2x",
        insertOrder = 3L,
      ),
      ExpenseRecord(
        id = "exp4",
        date = "2026-11-20",
        categoryName = "Swap & Cas Baterai",
        amount = 18_000L,
        source = FundLocation.GOPAY,
        note = "Tukar baterai di Blok M",
        insertOrder = 4L,
      ),
      ExpenseRecord(
        id = "exp5",
        date = "2026-11-19",
        categoryName = "Kuota Internet",
        amount = 75_000L,
        source = FundLocation.GOPAY,
        note = "Paket data kerja 30 hari",
        insertOrder = 5L,
      ),
      ExpenseRecord(
        id = "exp6",
        date = "2026-11-17",
        categoryName = "Servis Motor Listrik",
        amount = 135_000L,
        source = FundLocation.CASH,
        note = "Cek kampas rem & tekanan ban",
        insertOrder = 6L,
      ),
      ExpenseRecord(
        id = "exp7",
        date = "2026-11-15",
        categoryName = "Tagihan & Cicilan",
        amount = 650_000L,
        source = FundLocation.GOPAY,
        note = "Pembayaran tagihan: Cicilan Motor Listrik",
        insertOrder = 7L,
        linkedBillId = "bill1",
      ),
      ExpenseRecord(
        id = "exp8",
        date = "2026-11-10",
        categoryName = "Kebutuhan Pribadi",
        amount = 140_000L,
        source = FundLocation.CASH,
        note = "Perlengkapan jas hujan & sarung tangan",
        insertOrder = 8L,
      ),
    )

  // Data pengeluaran bulan Oktober 2026 untuk perbandingan kategori (kategori "Kebutuhan Pribadi" = 0 agar contoh strip '-' terlihat)
  val previousMonthExpenseByCategory: Map<String, Long> =
    mapOf(
      "Swap & Cas Baterai" to 48_000L,
      "Makan & Minum" to 52_000L,
      "Kuota Internet" to 75_000L,
      "Servis Motor Listrik" to 90_000L,
      "Tagihan & Cicilan" to 650_000L,
      "Kebutuhan Pribadi" to 0L, // Sengaja 0 -> tampilkan "-" untuk persentase
    )

  // Transfer antar 4 lokasi dana (tidak dihitung sebagai pemasukan atau pengeluaran)
  val initialTransfers =
    listOf(
      TransferRecord(
        id = "trf1",
        date = "2026-11-20",
        from = FundLocation.GOPAY,
        to = FundLocation.JAGO_CICILAN,
        amount = 400_000L,
        note = "Sisihkan dana cicilan bulan ini",
        insertOrder = 10L,
      ),
      TransferRecord(
        id = "trf2",
        date = "2026-11-18",
        from = FundLocation.GOPAY,
        to = FundLocation.JAGO_PRIBADI,
        amount = 300_000L,
        note = "Tabungan pribadi mingguan",
        insertOrder = 11L,
      ),
      TransferRecord(
        id = "trf3",
        date = "2026-11-15",
        from = FundLocation.JAGO_CICILAN,
        to = FundLocation.GOPAY,
        amount = 650_000L,
        note = "Tarik dana untuk bayar Cicilan Motor Listrik",
        insertOrder = 12L,
      ),
    )

  // Tagihan dan Kewajiban dengan tepat 3 status (Belum dibayar, Sudah dibayar, Terlambat) + riwayat bulan sebelumnya
  val initialBills =
    listOf(
      BillItem(
        id = "bill1",
        name = "Cicilan Motor Listrik",
        categoryNote = "Angsuran ke-5 dari 24 bulan",
        history =
          listOf(
            BillMonthEntry(
              monthKey = "2026-11",
              monthLabel = "November 2026",
              amount = 650_000L,
              status = BillStatus.SUDAH_DIBAYAR,
              dueDateLabel = "Jatuh tempo 15 Nov 2026",
              paidSource = FundLocation.GOPAY,
              paidDate = "2026-11-15",
              linkedExpenseId = "exp7",
            ),
            BillMonthEntry(
              monthKey = "2026-10",
              monthLabel = "Oktober 2026",
              amount = 650_000L,
              status = BillStatus.SUDAH_DIBAYAR,
              dueDateLabel = "Jatuh tempo 15 Okt 2026",
              paidSource = FundLocation.GOPAY,
              paidDate = "2026-10-15",
            ),
          ),
      ),
      BillItem(
        id = "bill2",
        name = "Listrik Rumah & Cas Motor",
        categoryNote = "Nominal bervariasi tiap bulan",
        history =
          listOf(
            BillMonthEntry(
              monthKey = "2026-11",
              monthLabel = "November 2026",
              amount = 315_000L,
              status = BillStatus.BELUM_DIBAYAR,
              dueDateLabel = "Jatuh tempo 25 Nov 2026",
            ),
            BillMonthEntry(
              monthKey = "2026-10",
              monthLabel = "Oktober 2026",
              amount = 292_000L,
              status = BillStatus.SUDAH_DIBAYAR,
              dueDateLabel = "Jatuh tempo 25 Okt 2026",
              paidSource = FundLocation.GOPAY,
              paidDate = "2026-10-24",
            ),
          ),
      ),
      BillItem(
        id = "bill3",
        name = "Iuran BPJS Kesehatan",
        categoryNote = "Tagihan rutin bulanan",
        history =
          listOf(
            BillMonthEntry(
              monthKey = "2026-11",
              monthLabel = "November 2026",
              amount = 150_000L,
              status = BillStatus.TERLAMBAT,
              dueDateLabel = "Jatuh tempo 10 Nov 2026",
            ),
            BillMonthEntry(
              monthKey = "2026-10",
              monthLabel = "Oktober 2026",
              amount = 150_000L,
              status = BillStatus.SUDAH_DIBAYAR,
              dueDateLabel = "Jatuh tempo 10 Okt 2026",
              paidSource = FundLocation.CASH,
              paidDate = "2026-10-09",
            ),
          ),
      ),
      BillItem(
        id = "bill4",
        name = "Sewa Baterai Swap Bulanan",
        categoryNote = "Paket berlangganan motor listrik",
        history =
          listOf(
            BillMonthEntry(
              monthKey = "2026-11",
              monthLabel = "November 2026",
              amount = 220_000L,
              status = BillStatus.BELUM_DIBAYAR,
              dueDateLabel = "Jatuh tempo 28 Nov 2026",
            ),
            BillMonthEntry(
              monthKey = "2026-10",
              monthLabel = "Oktober 2026",
              amount = 200_000L,
              status = BillStatus.SUDAH_DIBAYAR,
              dueDateLabel = "Jatuh tempo 28 Okt 2026",
              paidSource = FundLocation.GOPAY,
              paidDate = "2026-10-27",
            ),
          ),
      ),
    )
}
