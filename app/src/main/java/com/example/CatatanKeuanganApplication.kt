package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.CatatanKeuanganDatabase
import com.example.data.repository.FinanceRepository

import com.example.data.remote.GoogleBackupGateway
import com.example.data.remote.OkHttpGoogleBackupGateway

object AppDatabaseProvider {
  @Volatile
  private var repositoryInstance: FinanceRepository? = null

  @Volatile
  private var backupGatewayInstance: GoogleBackupGateway? = null

  fun init(context: Context) {
    if (repositoryInstance == null) {
      synchronized(this) {
        if (repositoryInstance == null) {
          val db = CatatanKeuanganDatabase.getInstance(context.applicationContext)
          repositoryInstance = FinanceRepository(db.dao())
        }
      }
    }
    if (backupGatewayInstance == null) {
      synchronized(this) {
        if (backupGatewayInstance == null) {
          backupGatewayInstance = OkHttpGoogleBackupGateway()
        }
      }
    }
  }

  fun getRepository(): FinanceRepository {
    return repositoryInstance
      ?: error("AppDatabaseProvider belum diinisialisasi. Pastikan CatatanKeuanganApplication terdaftar.")
  }

  fun getBackupGateway(): GoogleBackupGateway {
    return backupGatewayInstance
      ?: OkHttpGoogleBackupGateway().also { backupGatewayInstance = it }
  }

  internal fun setBackupGatewayForTests(gateway: GoogleBackupGateway?) {
    backupGatewayInstance = gateway
  }

  internal fun resetForTests() {
    synchronized(this) {
      repositoryInstance = null
      backupGatewayInstance = null
      CatatanKeuanganDatabase.resetInstanceForTests()
    }
  }
}

class CatatanKeuanganApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    AppDatabaseProvider.init(this)
  }
}
