package com.monu.songhunter

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale

data class Track(
    val title: String,
    val creator: String,
    val source: String,
    val detailUrl: String,
    val downloadUrl: String?,
    val fileName: String?,
    val format: String,
    val size: String,
    val license: String,
    val canDownload: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SongHunterApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongHunterApp(context: Context) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Track>()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank() || loading) return
        loading = true
        searched = true
        error = ""
        scope.launch {
            try {
                results = searchAll(query.trim())
            } catch (e: Exception) {
                error = e.message ?: "Search failed. Please try again."
                results = emptyList()
            } finally {
                loading = false
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFB69CFF),
            secondary = Color(0xFF64D8CB),
            background = Color(0xFF0B0D14),
            surface = Color(0xFF171A25)
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF0B0D14),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SONG HUNTER", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text("Search music archives", fontSize = 11.sp, color = Color(0xFF9AA0B3))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0B0D14),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(18.dp))
                Text("Find the original recording.", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("Search Internet Archive and Wikimedia Commons in one place.", color = Color(0xFF9AA0B3), fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Song name, singer or album") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9A7BFF),
                        unfocusedBorderColor = Color(0xFF34394A),
                        focusedPlaceholderColor = Color(0xFF777E92),
                        unfocusedPlaceholderColor = Color(0xFF777E92),
                        cursorColor = Color(0xFFB69CFF)
                    )
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { search() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8062E8)),
                    enabled = !loading
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SEARCH ALL SOURCES", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RESULTS", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${results.size}", color = Color(0xFF9AA0B3), fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("2 SOURCES", color = Color(0xFF64D8CB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFB69CFF))
                            Spacer(Modifier.height(12.dp))
                            Text("Searching music archives…", color = Color(0xFF9AA0B3))
                        }
                    }
                    error.isNotBlank() -> Text(error, color = Color(0xFFFF8A8A), modifier = Modifier.padding(8.dp))
                    results.isNotEmpty() -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results) { track -> TrackCard(track, context) { startDownload(context, track) } }
                    }
                    searched -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching audio files found. Try another spelling.", color = Color(0xFF9AA0B3))
                    }
                    else -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF5B527B), modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Your next favourite track is out there.", color = Color(0xFF9AA0B3))
                            Text("Search to explore downloadable archive audio.", color = Color(0xFF70788D), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackCard(track: Track, context: Context, onDownload: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A25)),
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(color = Color(0xFF29243D), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFB69CFF), modifier = Modifier.padding(10.dp).size(23.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (track.creator.isNotBlank()) Text(track.creator, color = Color(0xFF9AA0B3), fontSize = 12.sp)
                    Text(track.source, color = Color(0xFF64D8CB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("${track.format}  •  ${track.size}", color = Color(0xFFB4B9C8), fontSize = 11.sp)
            Text("Licence: ${track.license}", color = Color(0xFF9AA0B3), fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            if (track.canDownload) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D806E))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("DOWNLOAD")
                }
            } else {
                Text("Download not enabled: licence/rights not confirmed. Open the archive item to review.", color = Color(0xFFFFC978), fontSize = 11.sp)
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.detailUrl)))
                    }.onFailure {
                        Toast.makeText(context, "Unable to open source page", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("OPEN SOURCE PAGE") }
            }
        }
    }
}

fun startDownload(context: Context, track: Track) {
    val url = track.downloadUrl ?: return
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(track.title)
            .setDescription("Downloading from ${track.source}")
            .setMimeType("audio/*")
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "SongHunter/${safeName(track.fileName ?: track.title)}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download queued. Check the notification shade.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not start download: ${e.message ?: "check connection"}", Toast.LENGTH_LONG).show()
    }
}

fun safeName(value: String): String = value.replace(Regex("[^a-zA-Z0-9._-]"), "_")

fun encodePathPart(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

suspend fun searchAll(term: String): List<Track> = withContext(Dispatchers.IO) {
    val queries = buildSearchQueries(term)
    val iaResults = mutableListOf<Track>()
    val commonsResults = mutableListOf<Track>()
    for (q in queries) {
        try { iaResults.addAll(searchInternetArchive(q)) } catch (_: Exception) { }
        try { commonsResults.addAll(searchCommons(q)) } catch (_: Exception) { }
    }
    (iaResults + commonsResults)
        .distinctBy { it.detailUrl + "|" + (it.fileName ?: "") }
        .map { it to relevanceScore(term, it.title, it.creator, it.fileName.orEmpty()) }
        .filter { it.second >= 25 }
        .sortedWith(compareByDescending<Pair<Track, Int>> { it.second }
            .thenByDescending { if (it.first.canDownload) 1 else 0 }
            .thenBy { it.first.title.length })
        .map { it.first }
        .take(60)
}

fun buildSearchQueries(input: String): List<String> {
    val cleaned = input.trim().replace(Regex("\\s+"), " ")
    val noPunctuation = cleaned.replace(Regex("[\\p{Punct}]+"), " ").replace(Regex("\\s+"), " ").trim()
    val variants = linkedSetOf<String>()
    if (cleaned.isNotBlank()) variants.add(cleaned)
    if (noPunctuation.isNotBlank() && !noPunctuation.equals(cleaned, true)) variants.add(noPunctuation)
    return variants.take(2)
}

fun normalizeSearchText(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace("ё", "е")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun relevanceScore(query: String, title: String, creator: String, fileName: String = ""): Int {
    val q = normalizeSearchText(query)
    val t = normalizeSearchText(title)
    val a = normalizeSearchText(creator)
    val f = normalizeSearchText(fileName.substringBeforeLast('.', fileName))
    if (q.isBlank() || (t.isBlank() && f.isBlank())) return 0
    if (t == q) return 120
    if (t.startsWith("$q ")) return 110
    if (t.contains(q)) return 100
    if (f == q || f.contains(q)) return 95

    val words = q.split(" ").filter { it.length > 1 }.distinct()
    if (words.isEmpty()) return 0
    val titleWords = t.split(" ").toSet()
    val fileWords = f.split(" ").toSet()
    val artistWords = a.split(" ").toSet()
    val titleHits = words.count { word -> titleWords.any { it == word || (word.length >= 4 && it.startsWith(word)) } }
    val fileHits = words.count { word -> fileWords.any { it == word || (word.length >= 4 && it.startsWith(word)) } }
    val artistHits = words.count { word -> artistWords.any { it == word || (word.length >= 4 && it.startsWith(word)) } }
    val titleRatio = titleHits.toDouble() / words.size
    val fileRatio = fileHits.toDouble() / words.size

    return when {
        titleHits == words.size -> 85 + titleHits
        titleRatio >= 0.75 -> 65 + titleHits
        fileHits == words.size -> 80 + fileHits
        fileRatio >= 0.75 -> 55 + fileHits
        artistHits == words.size && titleHits > 0 -> 45 + artistHits + titleHits
        titleRatio >= 0.5 -> 30 + titleHits
        else -> 0
    }
}

fun getJson(url: String): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 20000
    connection.setRequestProperty("User-Agent", "SongHunterAndroid/1.0 (archive search app)")
    return try {
        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

fun searchInternetArchive(term: String): List<Track> {
    val escaped = term.replace("\\", " ").replace("\"", " ").trim()
    val q = "\"$escaped\" AND mediatype:audio"
    val url = "https://archive.org/advancedsearch.php?q=${URLEncoder.encode(q, "UTF-8")}" +
        "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=year&rows=30&page=1&output=json"
    val docs = getJson(url).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
    val out = mutableListOf<Track>()
    val seenIds = mutableSetOf<String>()
    for (i in 0 until docs.length()) {
        val doc = docs.optJSONObject(i) ?: continue
        val id = doc.optString("identifier")
        if (id.isBlank() || !seenIds.add(id)) continue
        try {
            val meta = getJson("https://archive.org/metadata/${URLEncoder.encode(id, "UTF-8")}")
            val metadata = meta.optJSONObject("metadata") ?: JSONObject()
            val licenseUrl = metadata.optString("licenseurl", "")
            val license = metadata.optString("license", "").ifBlank { licenseUrl }
            val files = meta.optJSONArray("files") ?: continue
            for (j in 0 until files.length()) {
                val file = files.optJSONObject(j) ?: continue
                val name = file.optString("name")
                val format = file.optString("format", "")
                val lower = name.lowercase(Locale.ROOT)
                val audio = lower.endsWith(".mp3") || lower.endsWith(".flac") ||
                    lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".wav")
                if (!audio || file.optString("source") == "metadata") continue
                val permitted = isRecognizedOpenLicense(licenseUrl, license)
                val download = "https://archive.org/download/${id}/${name.split("/").joinToString("/") { encodePathPart(it) }}"
                out.add(
                    Track(
                        title = doc.optString("title", id),
                        creator = doc.optString("creator", ""),
                        source = "Internet Archive",
                        detailUrl = "https://archive.org/details/$id",
                        downloadUrl = if (permitted) download else null,
                        fileName = name.substringAfterLast('/'),
                        format = format.ifBlank { name.substringAfterLast('.', "Audio").uppercase() },
                        size = humanSize(file.optString("size")),
                        license = license.ifBlank { "Not specified" },
                        canDownload = permitted
                    )
                )
                if (out.size >= 12) break
            }
        } catch (_: Exception) {
            // Ignore an unavailable archive item and continue with other results.
        }
    }
    return out
}

fun searchCommons(term: String): List<Track> {
    val safeTerm = term.replace("\"", " ").trim()
    val search = URLEncoder.encode("filetype:audio $safeTerm", "UTF-8")
    val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$search" +
        "&gsrnamespace=6&gsrlimit=20&prop=imageinfo&iiprop=url%7Csize%7Cextmetadata&format=json"
    val pages = getJson(url).optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
    val out = mutableListOf<Track>()
    val keys = pages.keys()
    while (keys.hasNext()) {
        val page = pages.optJSONObject(keys.next()) ?: continue
        val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
        val ext = info.optJSONObject("extmetadata") ?: JSONObject()
        val licenseName = ext.optJSONObject("LicenseShortName")?.optString("value").orEmpty()
        val licenseUrl = ext.optJSONObject("LicenseUrl")?.optString("value").orEmpty()
        val artist = ext.optJSONObject("Artist")?.optString("value").orEmpty().replace(Regex("<[^>]*>"), "")
        val title = page.optString("title").removePrefix("File:")
        val mime = info.optString("mime")
        val isAudio = mime.startsWith("audio/") ||
            title.substringAfterLast('.', "").lowercase() in listOf("mp3", "ogg", "wav", "flac")
        if (!isAudio) continue
        val permitted = isRecognizedOpenLicense(licenseUrl, licenseName)
        out.add(
            Track(
                title = title,
                creator = artist,
                source = "Wikimedia Commons",
                detailUrl = "https://commons.wikimedia.org/wiki/${URLEncoder.encode(page.optString("title").replace(' ', '_'), "UTF-8")}",
                downloadUrl = if (permitted) info.optString("url") else null,
                fileName = title,
                format = mime.substringAfter("/", "Audio").uppercase(),
                size = humanSize(info.optString("size")),
                license = licenseName.ifBlank { "Not specified" },
                canDownload = permitted && info.optString("url").isNotBlank()
            )
        )
    }
    return out
}

fun isRecognizedOpenLicense(url: String, label: String): Boolean {
    val value = (url + " " + label).lowercase(Locale.ROOT)
    return value.contains("creativecommons.org/licenses/") ||
        value.contains("creativecommons.org/publicdomain/") ||
        value.contains("public domain") || value.contains("cc0")
}

fun humanSize(raw: String): String {
    val bytes = raw.toLongOrNull() ?: return "Size unknown"
    if (bytes <= 0) return "Size unknown"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}
