package com.example.utils

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Headers
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object GoogleDriveBackup {
    const val PREFS = "app_prefs"
    const val KEY_API_ENABLED = "backup_drive_api_enabled"
    const val KEY_FOLDER_ID = "backup_drive_folder_id"
    private const val SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val FOLDER_NAME = "محطة العسل"
    const val KEY_LAST_ERROR = "backup_drive_last_error"

    private fun saveError(context: Context, message: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_ERROR, message.take(500)).apply()
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS).build()

    fun authorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    suspend fun authorizationResult(context: Context) =
        Identity.getAuthorizationClient(context).authorize(authorizationRequest()).await()

    suspend fun uploadBackup(context: Context, file: File, fileName: String): Boolean {
        return try {
            val result = authorizationResult(context)
            if (result.hasResolution()) false
            else {
                val token = result.accessToken
                !token.isNullOrBlank() && uploadWithToken(context, token, file, fileName)
            }
        } catch (_: Exception) { false }
    }

    suspend fun uploadWithToken(context: Context, token: String, file: File, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() <= 0L) return@withContext false
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val folderId = ensureFolder(token, prefs.getString(KEY_FOLDER_ID, null)) ?: return@withContext false
        prefs.edit().putString(KEY_FOLDER_ID, folderId).apply()
        val existingId = findFile(token, folderId, fileName)
        if (existingId != null) {
            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media")
                .header("Authorization", "Bearer $token")
                .put(file.asRequestBody("application/zip".toMediaType())).build()
            client.newCall(req).execute().use { response ->
                android.util.Log.i("GoogleDriveBackup", "Upload existing file: HTTP ${response.code}")
                if (!response.isSuccessful) saveError(context, "HTTP ${response.code}: ${response.peekBody(2048).string()}") else saveError(context, "")
                response.isSuccessful
            }
        } else {
            val metadata = JSONObject().put("name", fileName).put("mimeType", "application/zip")
                .put("parents", org.json.JSONArray().put(folderId)).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            // Drive API multipart uploads require multipart/related, not multipart/form-data.
            val body = MultipartBody.Builder()
                .setType(MultipartBody.MIXED)
                .addPart(
                    Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                    metadata
                )
                .addPart(
                    Headers.headersOf("Content-Type", "application/zip"),
                    file.asRequestBody("application/zip".toMediaType())
                )
                .build()
            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "multipart/related; boundary=${body.boundary}")
                .post(body).build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.peekBody(2048).string()
                android.util.Log.i("GoogleDriveBackup", "Create backup: HTTP ${response.code}; body=$responseBody")
                if (!response.isSuccessful) saveError(context, "HTTP ${response.code}: $responseBody") else saveError(context, "")
                response.isSuccessful
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Drive upload failed", e)
            saveError(context, "${e.javaClass.simpleName}: ${e.message ?: "unknown error"} | ${e.stackTrace.firstOrNull()?.toString().orEmpty()}")
            false
        }
    }

    private fun ensureFolder(token: String, savedId: String?): String? {
        if (!savedId.isNullOrBlank()) {
            val check = Request.Builder().url("https://www.googleapis.com/drive/v3/files/$savedId?fields=id,mimeType")
                .header("Authorization", "Bearer $token").get().build()
            client.newCall(check).execute().use { response ->
                if (response.isSuccessful && JSONObject(response.body?.string().orEmpty())
                        .optString("mimeType") == "application/vnd.google-apps.folder") return savedId
            }
        }
        val body = JSONObject().put("name", FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder").toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url("https://www.googleapis.com/drive/v3/files?fields=id,name")
            .header("Authorization", "Bearer $token").post(body).build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return null
            return JSONObject(response.body?.string().orEmpty()).optString("id").takeIf { it.isNotBlank() }
        }
    }

    private fun findFile(token: String, folderId: String, fileName: String): String? {
        val escaped = fileName.replace("\\", "\\\\").replace("'", "\\'")
        val q = "name = '$escaped' and '$folderId' in parents and trashed = false"
        val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name())
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$encoded&fields=files(id,name)&pageSize=10")
            .header("Authorization", "Bearer $token").get().build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return null
            val files = JSONObject(response.body?.string().orEmpty()).optJSONArray("files") ?: return null
            return if (files.length() > 0) files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() } else null
        }
    }
}