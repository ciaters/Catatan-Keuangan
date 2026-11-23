package com.example.data.remote

import com.example.data.backup.BackupSchema
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class DriveBackupFile(
  val id: String,
  val name: String,
  val modifiedTimeIso: String,
)

class GoogleApiException(
  val statusCode: Int?,
  override val message: String,
  override val cause: Throwable? = null,
) : Exception(message, cause)

interface GoogleBackupGateway {
  suspend fun fetchUserEmail(accessToken: String): String?

  suspend fun listBackupSpreadsheets(accessToken: String): List<DriveBackupFile>

  suspend fun createOrUpdateBackupSpreadsheet(
    accessToken: String,
    existingFileId: String?,
    title: String,
    tabs: Map<String, List<List<String>>>,
  ): DriveBackupFile

  suspend fun readSpreadsheetTabs(
    accessToken: String,
    spreadsheetId: String,
  ): Pair<DriveBackupFile, Map<String, List<List<String>>>>
}

class OkHttpGoogleBackupGateway(
  private val client: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build(),
  private val driveBaseUrl: String = "https://www.googleapis.com/drive/v3",
  private val sheetsBaseUrl: String = "https://sheets.googleapis.com/v4",
  private val userInfoUrl: String = "https://www.googleapis.com/oauth2/v3/userinfo",
) : GoogleBackupGateway {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  override suspend fun fetchUserEmail(accessToken: String): String? =
    withContext(Dispatchers.IO) {
      val request =
        Request.Builder()
          .url(userInfoUrl)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()
      try {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@withContext null
          val body = response.body?.string().orEmpty()
          if (body.isBlank()) return@withContext null
          JSONObject(body).optString("email").takeIf { it.isNotBlank() }
        }
      } catch (_: Exception) {
        null
      }
    }

  override suspend fun listBackupSpreadsheets(accessToken: String): List<DriveBackupFile> =
    withContext(Dispatchers.IO) {
      val query =
        "mimeType='application/vnd.google-apps.spreadsheet' and trashed=false and (name contains 'Catatan_Keuangan' or appProperties has { key='catatanKeuanganBackup' and value='true' })"
      val url =
        "$driveBaseUrl/files"
          .toHttpUrl()
          .newBuilder()
          .addQueryParameter("q", query)
          .addQueryParameter("fields", "files(id,name,modifiedTime)")
          .addQueryParameter("orderBy", "modifiedTime desc")
          .addQueryParameter("pageSize", "30")
          .build()

      val json = executeJson(Request.Builder().url(url).get().build(), accessToken)
      val filesArr = json.optJSONArray("files") ?: JSONArray()
      buildList {
        for (i in 0 until filesArr.length()) {
          val obj = filesArr.getJSONObject(i)
          add(
            DriveBackupFile(
              id = obj.getString("id"),
              name = obj.optString("name", BackupSchema.DEFAULT_SPREADSHEET_TITLE),
              modifiedTimeIso = obj.optString("modifiedTime", ""),
            )
          )
        }
      }
    }

  override suspend fun createOrUpdateBackupSpreadsheet(
    accessToken: String,
    existingFileId: String?,
    title: String,
    tabs: Map<String, List<List<String>>>,
  ): DriveBackupFile =
    withContext(Dispatchers.IO) {
      val targetSpreadsheetId =
        if (!existingFileId.isNullOrBlank() && isFileActiveOnDrive(accessToken, existingFileId)) {
          ensureSheetTabsExist(accessToken, existingFileId, tabs.keys.toList())
          clearSheetTabs(accessToken, existingFileId, tabs.keys.toList())
          existingFileId
        } else {
          val createdId = createNewSpreadsheet(accessToken, title, tabs.keys.toList())
          tagDriveBackupProperty(accessToken, createdId)
          createdId
        }

      writeBatchValues(accessToken, targetSpreadsheetId, tabs)
      fetchDriveFileMetadata(accessToken, targetSpreadsheetId, fallbackTitle = title)
    }

  override suspend fun readSpreadsheetTabs(
    accessToken: String,
    spreadsheetId: String,
  ): Pair<DriveBackupFile, Map<String, List<List<String>>>> =
    withContext(Dispatchers.IO) {
      val metaUrl =
        "$sheetsBaseUrl/spreadsheets/$spreadsheetId"
          .toHttpUrl()
          .newBuilder()
          .addQueryParameter("fields", "properties.title,sheets.properties.title")
          .build()
      val metaJson = executeJson(Request.Builder().url(metaUrl).get().build(), accessToken)
      val spreadsheetTitle =
        metaJson.optJSONObject("properties")?.optString("title")
          ?: BackupSchema.DEFAULT_SPREADSHEET_TITLE
      val sheetTitles = mutableListOf<String>()
      val sheetsArr = metaJson.optJSONArray("sheets") ?: JSONArray()
      for (i in 0 until sheetsArr.length()) {
        val t = sheetsArr.getJSONObject(i).optJSONObject("properties")?.optString("title").orEmpty()
        if (t.isNotBlank()) sheetTitles.add(t)
      }

      if (sheetTitles.isEmpty()) {
        throw GoogleApiException(400, "File Google Sheets tidak memiliki tab data yang dapat dibaca.")
      }

      val batchUrlBuilder =
        "$sheetsBaseUrl/spreadsheets/$spreadsheetId/values:batchGet"
          .toHttpUrl()
          .newBuilder()
          .addQueryParameter("valueRenderOption", "FORMATTED_VALUE")
      sheetTitles.forEach { tab ->
        batchUrlBuilder.addQueryParameter("ranges", "'$tab'")
      }

      val batchJson =
        executeJson(Request.Builder().url(batchUrlBuilder.build()).get().build(), accessToken)
      val valueRanges = batchJson.optJSONArray("valueRanges") ?: JSONArray()
      val tabMap = linkedMapOf<String, List<List<String>>>()
      for (i in 0 until valueRanges.length()) {
        val vr = valueRanges.getJSONObject(i)
        val rawRange = vr.optString("range", "")
        val parsedTitle = parseTabNameFromA1Range(rawRange) ?: sheetTitles.getOrNull(i).orEmpty()
        if (parsedTitle.isBlank()) continue
        val valuesArr = vr.optJSONArray("values") ?: JSONArray()
        val rows = mutableListOf<List<String>>()
        for (r in 0 until valuesArr.length()) {
          val rowArr = valuesArr.optJSONArray(r) ?: JSONArray()
          val rowCells = mutableListOf<String>()
          for (c in 0 until rowArr.length()) {
            rowCells.add(rowArr.optString(c, ""))
          }
          rows.add(rowCells)
        }
        tabMap[parsedTitle] = rows
      }

      val driveFile =
        fetchDriveFileMetadata(accessToken, spreadsheetId, fallbackTitle = spreadsheetTitle)
      driveFile to tabMap
    }

  private fun isFileActiveOnDrive(accessToken: String, fileId: String): Boolean {
    val url =
      "$driveBaseUrl/files/$fileId"
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("fields", "id,trashed,mimeType")
        .build()
    return try {
      val json = executeJson(Request.Builder().url(url).get().build(), accessToken)
      val trashed = json.optBoolean("trashed", false)
      val mime = json.optString("mimeType", "")
      !trashed && mime == "application/vnd.google-apps.spreadsheet"
    } catch (e: GoogleApiException) {
      if (e.statusCode == 404) false else throw e
    }
  }

  private fun createNewSpreadsheet(
    accessToken: String,
    title: String,
    tabNames: List<String>,
  ): String {
    val sheetsJson = JSONArray()
    tabNames.forEach { tabName ->
      sheetsJson.put(JSONObject().put("properties", JSONObject().put("title", tabName)))
    }
    val payload =
      JSONObject()
        .put("properties", JSONObject().put("title", title))
        .put("sheets", sheetsJson)

    val req =
      Request.Builder()
        .url("$sheetsBaseUrl/spreadsheets")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()
    val respJson = executeJson(req, accessToken)
    return respJson.getString("spreadsheetId")
  }

  private fun tagDriveBackupProperty(accessToken: String, fileId: String) {
    val body =
      JSONObject()
        .put("appProperties", JSONObject().put("catatanKeuanganBackup", "true"))
        .toString()
        .toRequestBody(jsonMediaType)
    val req = Request.Builder().url("$driveBaseUrl/files/$fileId").patch(body).build()
    try {
      executeJson(req, accessToken)
    } catch (_: Exception) {
      // Penandaan appProperties bersifat tambahan; pencadangan utama tetap berhasil.
    }
  }

  private fun ensureSheetTabsExist(
    accessToken: String,
    spreadsheetId: String,
    requiredTabs: List<String>,
  ) {
    val url =
      "$sheetsBaseUrl/spreadsheets/$spreadsheetId"
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("fields", "sheets.properties.title")
        .build()
    val meta = executeJson(Request.Builder().url(url).get().build(), accessToken)
    val existing = mutableSetOf<String>()
    val sheetsArr = meta.optJSONArray("sheets") ?: JSONArray()
    for (i in 0 until sheetsArr.length()) {
      val title = sheetsArr.getJSONObject(i).optJSONObject("properties")?.optString("title").orEmpty()
      if (title.isNotBlank()) existing.add(title)
    }
    val missing = requiredTabs.filterNot { existing.contains(it) }
    if (missing.isEmpty()) return

    val requests = JSONArray()
    missing.forEach { tab ->
      requests.put(
        JSONObject()
          .put("addSheet", JSONObject().put("properties", JSONObject().put("title", tab)))
      )
    }
    val body = JSONObject().put("requests", requests).toString().toRequestBody(jsonMediaType)
    executeJson(
      Request.Builder().url("$sheetsBaseUrl/spreadsheets/$spreadsheetId:batchUpdate").post(body).build(),
      accessToken,
    )
  }

  private fun clearSheetTabs(
    accessToken: String,
    spreadsheetId: String,
    tabNames: List<String>,
  ) {
    val rangesArr = JSONArray()
    tabNames.forEach { rangesArr.put("'$it'") }
    val body = JSONObject().put("ranges", rangesArr).toString().toRequestBody(jsonMediaType)
    executeJson(
      Request.Builder()
        .url("$sheetsBaseUrl/spreadsheets/$spreadsheetId/values:batchClear")
        .post(body)
        .build(),
      accessToken,
    )
  }

  private fun writeBatchValues(
    accessToken: String,
    spreadsheetId: String,
    tabs: Map<String, List<List<String>>>,
  ) {
    val dataArr = JSONArray()
    tabs.forEach { (tabName, rows) ->
      val valuesArr = JSONArray()
      rows.forEach { row ->
        val rowArr = JSONArray()
        row.forEach { cell -> rowArr.put(cell) }
        valuesArr.put(rowArr)
      }
      dataArr.put(
        JSONObject()
          .put("range", "'$tabName'!A1")
          .put("majorDimension", "ROWS")
          .put("values", valuesArr)
      )
    }
    val body =
      JSONObject()
        .put("valueInputOption", "RAW")
        .put("data", dataArr)
        .toString()
        .toRequestBody(jsonMediaType)

    executeJson(
      Request.Builder()
        .url("$sheetsBaseUrl/spreadsheets/$spreadsheetId/values:batchUpdate")
        .post(body)
        .build(),
      accessToken,
    )
  }

  private fun fetchDriveFileMetadata(
    accessToken: String,
    fileId: String,
    fallbackTitle: String,
  ): DriveBackupFile {
    val url =
      "$driveBaseUrl/files/$fileId"
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("fields", "id,name,modifiedTime")
        .build()
    return try {
      val json = executeJson(Request.Builder().url(url).get().build(), accessToken)
      DriveBackupFile(
        id = json.optString("id", fileId),
        name = json.optString("name", fallbackTitle),
        modifiedTimeIso = json.optString("modifiedTime", ""),
      )
    } catch (_: Exception) {
      DriveBackupFile(id = fileId, name = fallbackTitle, modifiedTimeIso = "")
    }
  }

  private fun parseTabNameFromA1Range(range: String): String? {
    val beforeBang = range.substringBefore('!', missingDelimiterValue = "").trim()
    if (beforeBang.isEmpty()) return null
    return beforeBang.removePrefix("'").removeSuffix("'")
  }

  private fun executeJson(request: Request, accessToken: String): JSONObject {
    val authorizedReq =
      request.newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
    try {
      client.newCall(authorizedReq).execute().use { response ->
        val bodyStr = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val apiMsg =
            runCatching {
              JSONObject(bodyStr).optJSONObject("error")?.optString("message")
            }.getOrNull()
          val friendlyMsg =
            when (response.code) {
              401 ->
                "Sesi akses Google berakhir atau belum terotorisasi. Silakan beri izin ulang akun Google."
              403 ->
                "Akses Google Drive / Sheets ditolak (${apiMsg ?: "pastikan Google Drive API & Google Sheets API aktif di Google Cloud"})."
              404 -> "File cadangan Google Sheets tidak ditemukan atau telah dihapus dari Google Drive."
              429 -> "Terlalu banyak permintaan ke Google API. Silakan tunggu sebentar lalu coba lagi."
              else -> "Gagal berkomunikasi dengan Google (${response.code}): ${apiMsg ?: response.message}"
            }
          throw GoogleApiException(response.code, friendlyMsg)
        }
        return if (bodyStr.isBlank()) JSONObject() else JSONObject(bodyStr)
      }
    } catch (e: GoogleApiException) {
      throw e
    } catch (e: IOException) {
      throw GoogleApiException(
        statusCode = null,
        message = "Koneksi internet terputus atau gagal menghubungi server Google. Data lokal tetap aman.",
        cause = e,
      )
    } catch (e: Exception) {
      throw GoogleApiException(
        statusCode = null,
        message = "Terjadi kesalahan saat membaca respons Google: ${e.message ?: "tidak diketahui"}",
        cause = e,
      )
    }
  }
}
