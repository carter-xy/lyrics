/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.simpmusic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.simpmusic.models.LyricsData
import moe.rukamori.archivetune.simpmusic.models.SimpMusicApiResponse
import kotlin.math.abs

object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"
    private const val MAX_DURATION_DIFFERENCE_SECONDS = 20
    private val lineTime = Regex("""\[\d{1,3}:[0-5]\d(?:[.:]\d{1,3})?]""")
    private val wordTime = Regex("""<\d{1,3}:[0-5]\d(?:[.:]\d{1,3})?>""")

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }

            expectSuccess = false
        }
    }

    suspend fun getLyricsByVideoId(videoId: String): List<LyricsData> =
        runCatching {
            val response = client.get(BASE_URL + videoId)

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<SimpMusicApiResponse>()
                if (apiResponse.success) {
                    apiResponse.data
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            emptyList()
        }

    internal fun selectLyrics(tracks: List<LyricsData>, duration: Int): String? =
        matchingTracks(tracks, duration).firstNotNullOfOrNull(::preferredLyrics)

    internal fun selectAllLyrics(tracks: List<LyricsData>, duration: Int): List<String> {
        val seen = HashSet<String>()
        val matching = matchingTracks(tracks, duration)
        return (matching.mapNotNull { validRich(it.richSyncLyrics) } +
            matching.mapNotNull { validSynced(it.syncedLyrics) } +
            matching.mapNotNull { validPlain(it.plainLyrics) })
            .filter(seen::add)
            .take(8)
    }

    private fun matchingTracks(tracks: List<LyricsData>, duration: Int): List<LyricsData> =
        tracks.filter { track ->
            duration <= 0 || track.duration == null || track.duration <= 0 ||
                abs(track.duration - duration) <= MAX_DURATION_DIFFERENCE_SECONDS
        }.let { eligible ->
            if (duration <= 0) eligible
            else eligible.sortedBy { track ->
                track.duration?.takeIf { it > 0 }?.let { abs(it - duration) } ?: Int.MAX_VALUE
            }
        }

    private fun preferredLyrics(track: LyricsData): String? =
        validRich(track.richSyncLyrics) ?: validSynced(track.syncedLyrics) ?: validPlain(track.plainLyrics)

    private fun validRich(value: String?): String? = value?.trim()?.takeIf { lineTime.containsMatchIn(it) && wordTime.containsMatchIn(it) }

    private fun validSynced(value: String?): String? = value?.trim()?.takeIf(lineTime::containsMatchIn)

    private fun validPlain(value: String?): String? = value?.trim()?.takeIf(String::isNotBlank)

    suspend fun getLyrics(
        videoId: String,
        duration: Int = 0,
    ): Result<String> =
        runCatching {
            val tracks = getLyricsByVideoId(videoId)

            if (tracks.isEmpty()) {
                throw IllegalStateException("Lyrics unavailable")
            }

            selectLyrics(tracks, duration) ?: throw IllegalStateException("Lyrics unavailable")
        }

    suspend fun getAllLyrics(
        videoId: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        val tracks = getLyricsByVideoId(videoId)
        selectAllLyrics(tracks, duration).forEach(callback)
    }
}
