package com.example.data.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface GoogleAuthOutcome {
  data class Authorized(val accessToken: String) : GoogleAuthOutcome

  data class NeedsResolution(val pendingIntent: PendingIntent) : GoogleAuthOutcome

  data class Failed(val message: String) : GoogleAuthOutcome
}

object GoogleDriveAuthManager {
  const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
  const val SCOPE_SPREADSHEETS = "https://www.googleapis.com/auth/spreadsheets"
  const val SCOPE_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email"

  private val requiredScopes: List<Scope> =
    listOf(
      Scope(SCOPE_DRIVE_FILE),
      Scope(SCOPE_SPREADSHEETS),
      Scope(SCOPE_USER_EMAIL),
    )

  suspend fun requestDriveAndSheetsAuthorization(context: Context): GoogleAuthOutcome {
    return try {
      val request =
        AuthorizationRequest.builder()
          .setRequestedScopes(requiredScopes)
          .build()
      val result = Identity.getAuthorizationClient(context).authorize(request).awaitResult()
      mapAuthorizationResult(result)
    } catch (e: ApiException) {
      GoogleAuthOutcome.Failed(mapApiExceptionMessage(e))
    } catch (e: Exception) {
      GoogleAuthOutcome.Failed(
        "Gagal membuka otorisasi akun Google (${e.message ?: "kesalahan tidak diketahui"})."
      )
    }
  }

  fun extractAuthorizationResultFromIntent(context: Context, intent: Intent?): GoogleAuthOutcome {
    if (intent == null) {
      return GoogleAuthOutcome.Failed("Otorisasi akun Google dibatalkan.")
    }
    return try {
      val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
      mapAuthorizationResult(result)
    } catch (e: ApiException) {
      GoogleAuthOutcome.Failed(mapApiExceptionMessage(e))
    } catch (e: Exception) {
      GoogleAuthOutcome.Failed(
        "Gagal membaca hasil otorisasi Google (${e.message ?: "kesalahan tidak diketahui"})."
      )
    }
  }

  suspend fun clearCredentialSession(context: Context) {
    runCatching {
      CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
  }

  private fun mapAuthorizationResult(result: AuthorizationResult): GoogleAuthOutcome {
    if (result.hasResolution()) {
      val pendingIntent = result.pendingIntent
      if (pendingIntent != null) {
        return GoogleAuthOutcome.NeedsResolution(pendingIntent)
      }
    }
    val token = result.accessToken
    return if (!token.isNullOrBlank()) {
      GoogleAuthOutcome.Authorized(token)
    } else {
      GoogleAuthOutcome.Failed(
        "Token akses Google Drive/Sheets tidak diterima. Silakan coba otorisasi ulang."
      )
    }
  }

  private fun mapApiExceptionMessage(e: ApiException): String {
    return when (e.statusCode) {
      CommonStatusCodes.CANCELED,
      16,
      12501 -> "Izin akses Google Drive dan Sheets dibatalkan oleh pengguna."
      CommonStatusCodes.NETWORK_ERROR ->
        "Koneksi internet tidak tersedia saat menghubungkan akun Google."
      CommonStatusCodes.DEVELOPER_ERROR ->
        "Klien OAuth Android belum dikonfigurasi di Google Cloud Console untuk paket dan sertifikat penandatanganan aplikasi ini."
      CommonStatusCodes.SIGN_IN_REQUIRED ->
        "Silakan pilih dan izinkan akun Google terlebih dahulu."
      else ->
        "Otorisasi Google gagal (kode ${e.statusCode}). Pastikan Google Drive API, Google Sheets API, dan OAuth Android Client sudah aktif."
    }
  }

  private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
      addOnSuccessListener { value ->
        if (cont.isActive) cont.resume(value)
      }
      addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
      }
      addOnCanceledListener {
        if (cont.isActive) cont.cancel()
      }
    }
}
