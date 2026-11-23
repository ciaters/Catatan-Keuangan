package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities =
    [
      WorkOrderEntity::class,
      BlankSessionEntity::class,
      BonusRuleEntity::class,
      IncomeTargetEntity::class,
      ExpenseCategoryEntity::class,
      ExpenseEntity::class,
      TransferEntity::class,
      AreaEntity::class,
      WalletOpeningEntity::class,
      BalanceCorrectionEntity::class,
      BillEntity::class,
      BillMonthEntity::class,
      AppSettingsEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
abstract class CatatanKeuanganDatabase : RoomDatabase() {
  abstract fun dao(): CatatanKeuanganDao

  companion object {
    const val DATABASE_NAME = "catatan_keuangan.db"

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE app_settings ADD COLUMN backupSpreadsheetId TEXT NOT NULL DEFAULT ''"
          )
          db.execSQL(
            "ALTER TABLE app_settings ADD COLUMN linkedGoogleEmail TEXT NOT NULL DEFAULT ''"
          )
        }
      }

    @Volatile
    private var INSTANCE: CatatanKeuanganDatabase? = null

    fun getInstance(context: Context): CatatanKeuanganDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                CatatanKeuanganDatabase::class.java,
                DATABASE_NAME,
              )
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { INSTANCE = it }
        }
    }

    internal fun resetInstanceForTests() {
      synchronized(this) {
        INSTANCE?.close()
        INSTANCE = null
      }
    }
  }
}
