package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class AppDestination(
  val route: String,
  @StringRes val titleRes: Int,
  val label: String,
  val shortLabel: String,
  val subtitle: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val isPrimaryBottomTab: Boolean,
) {
  BERANDA(
    route = "beranda",
    titleRes = R.string.page_beranda,
    label = "Beranda",
    shortLabel = "Beranda",
    subtitle = "Target harian, aktivitas kerja & saldo",
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    isPrimaryBottomTab = true,
  ),
  INCOME(
    route = "income",
    titleRes = R.string.page_income,
    label = "Income",
    shortLabel = "Income",
    subtitle = "Kronologi order kerja & waktu Blank",
    selectedIcon = Icons.Filled.TrendingUp,
    unselectedIcon = Icons.Outlined.TrendingUp,
    isPrimaryBottomTab = true,
  ),
  TRANSAKSI(
    route = "transaksi",
    titleRes = R.string.page_transaksi,
    label = "Transaksi",
    shortLabel = "Transaksi",
    subtitle = "Pemasukan harian, pengeluaran & transfer",
    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    isPrimaryBottomTab = true,
  ),
  STATISTIK(
    route = "statistik",
    titleRes = R.string.page_statistik,
    label = "Statistik",
    shortLabel = "Statistik",
    subtitle = "Performa mingguan, bulanan & wilayah",
    selectedIcon = Icons.Filled.BarChart,
    unselectedIcon = Icons.Outlined.BarChart,
    isPrimaryBottomTab = true,
  ),
  POSISI_SALDO(
    route = "posisi_saldo",
    titleRes = R.string.page_posisi_saldo,
    label = "Posisi Saldo",
    shortLabel = "Saldo",
    subtitle = "GoPay, Cash & dua kantong Bank Jago",
    selectedIcon = Icons.Filled.AccountBalanceWallet,
    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    isPrimaryBottomTab = false,
  ),
  RINGKASAN_KEUANGAN(
    route = "ringkasan_keuangan",
    titleRes = R.string.page_ringkasan_keuangan,
    label = "Ringkasan Keuangan",
    shortLabel = "Ringkasan",
    subtitle = "Mutasi 4 lokasi dana & selisih target",
    selectedIcon = Icons.Filled.Assessment,
    unselectedIcon = Icons.Outlined.Assessment,
    isPrimaryBottomTab = false,
  ),
  TAGIHAN_KEWAJIBAN(
    route = "tagihan_kewajiban",
    titleRes = R.string.page_tagihan_kewajiban,
    label = "Tagihan dan Kewajiban",
    shortLabel = "Tagihan",
    subtitle = "Tagihan bulanan manual & pembayaran",
    selectedIcon = Icons.Filled.EventNote,
    unselectedIcon = Icons.Outlined.EventNote,
    isPrimaryBottomTab = false,
  ),
  PENGATURAN(
    route = "pengaturan",
    titleRes = R.string.page_pengaturan,
    label = "Pengaturan",
    shortLabel = "Pengaturan",
    subtitle = "Target, wilayah, kategori & cadangan",
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    isPrimaryBottomTab = false,
  );

  companion object {
    val primaryTabs = entries.filter { it.isPrimaryBottomTab }
    val secondaryPages = entries.filterNot { it.isPrimaryBottomTab }

    fun fromRoute(route: String?): AppDestination =
      entries.firstOrNull { it.route == route } ?: BERANDA
  }
}
