package com.resu.sumo.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean
)

object GitHubUpdateChecker {

    // Official GitHub API endpoint for Sumo Work Log App releases
    private const val GITHUB_REPO_URL = "https://api.github.com/repos/Yeremi804/Sumo-Work-Log-App/releases/latest"
    const val CURRENT_APP_VERSION = "1.1.0"

    suspend fun checkForUpdates(): GitHubReleaseInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_REPO_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "SumoApp-Android")
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val code = connection.responseCode
                Log.i("SumoUpdate", "HTTP Response Code: $code")

                if (code == 200) {
                    val stream = connection.inputStream
                    val jsonText = stream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonText)

                    val tagName = json.optString("tag_name", "v1.1.0").replace("v", "").trim()
                    val releaseNotes = json.optString("body", "Nuevas mejoras y correcciones")

                    var downloadUrl = json.optString("html_url", "")
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        val firstAsset = assets.getJSONObject(0)
                        downloadUrl = firstAsset.optString("browser_download_url", downloadUrl)
                    }

                    val isUpdate = isVersionHigher(tagName, CURRENT_APP_VERSION)
                    Log.i("SumoUpdate", "Latest Tag: $tagName, Current: $CURRENT_APP_VERSION, IsUpdate: $isUpdate")

                    GitHubReleaseInfo(
                        tagName = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                        isUpdateAvailable = isUpdate
                    )
                } else {
                    Log.e("SumoUpdate", "Error response code: $code")
                    null
                }
            } catch (e: Exception) {
                Log.e("SumoUpdate", "Exception checking update: ${e.message}", e)
                null
            }
        }
    }

    private fun isVersionHigher(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(latestParts.size, currentParts.size)

            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
