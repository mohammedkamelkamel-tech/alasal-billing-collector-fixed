package com.example.utils

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    fun uploadWithToken(context: Context, token: String, file: File, fileName: String): Boolean {
        return try {
            if (!file.exists() || file.length() <= 0L) return false
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val folderId = ensureFolder(token, prefs.getString(KEY_FOLDER_ID, null)) ?: return false
        prefs.edit().putString(KEY_FOLDER_ID, folderId).apply()
        val existingId = findFile(token, folderId, fileName)
        if (existingId != null) {
            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media")
                .header("Authorization", "Bearer $token")
                .put(file.asRequestBody("application/zip".toMediaType())).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } else {
            val metadata = JSONObject().put("name", fileName).put("mimeType", "application/zip")
                .put("parents", org.json.JSONArray().put(folderId)).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadata)
                .addFormDataPart("file", fileName, file.asRequestBody("application/zip".toMediaType())).build()
            val req = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
                .header("Authorization", "Bearer $token").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        }
        } catch (_: Exception) { false }
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