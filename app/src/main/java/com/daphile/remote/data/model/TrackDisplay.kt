package com.daphile.remote.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * 曲名显示层。
 *
 * 乱码的典型成因是：原始文本按 UTF-8（或 GBK）编码，却被 LMS/文件标签按 Latin-1、
 * Windows-1252 等单字节编码解读，再作为 UTF-8 字符串传给客户端。
 * 这里用「严格往返编码 + Decoder REPORT」验证候选解码，只有结果明显更可读时才替换，
 * 避免把正常的法文、西班牙文重音字符改坏。
 */
private val WINDOWS_1252: Charset = Charset.forName("Windows-1252")
private val GB18030: Charset = Charset.forName("GB18030")
private val BIG5: Charset = Charset.forName("Big5")
private val SHIFT_JIS: Charset = Charset.forName("Shift_JIS")

/** (被误用的源编码, 真实的目标编码) */
private val REPAIR_PAIRS: List<Pair<Charset, Charset>> = listOf(
    StandardCharsets.ISO_8859_1 to StandardCharsets.UTF_8,
    WINDOWS_1252 to StandardCharsets.UTF_8,
    GB18030 to StandardCharsets.UTF_8,
    BIG5 to StandardCharsets.UTF_8,
    SHIFT_JIS to StandardCharsets.UTF_8,
    StandardCharsets.ISO_8859_1 to GB18030,
    WINDOWS_1252 to GB18030
)

private fun encodeStrict(value: String, charset: Charset): ByteArray? = try {
    val buffer = charset.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .encode(CharBuffer.wrap(value))
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    bytes
} catch (_: Throwable) {
    null
}

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? = try {
    charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
} catch (_: Throwable) {
    null
}

private fun looksMojibake(value: String): Boolean {
    if (value.length < 2) return false
    if (value.any { it == '\uFFFD' }) return true
    var markers = 0
    for (ch in value) {
        if (ch.code in 0x80..0x9F || ch.code in 0xA1..0xFF) markers++
        if (markers >= 2) return true
    }
    return false
}

/** 粗略的可读性打分：中日韩字符和常规字母加分，乱码特征与替换字符大幅扣分。 */
private fun readability(value: String): Int {
    var score = 0
    for (ch in value) {
        val code = ch.code
        when {
            ch == '\uFFFD' -> score -= 8
            ch.isISOControl() -> score -= 6
            code in 0x80..0x9F -> score -= 5
            code in 0xA1..0xFF -> score -= 3
            code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF ||
                code in 0x3040..0x30FF || code in 0xAC00..0xD7AF -> score += 6
            ch.isLetterOrDigit() || ch == ' ' || ch in "·-_()[]" -> score += 1
        }
    }
    return score
}

/** 修复 UTF-8/GBK 被单字节编码误读产生的乱码；无法确认时原样返回。 */
fun repairMojibake(value: String): String {
    if (!looksMojibake(value)) return value
    val baseScore = readability(value)
    var best = value
    var bestScore = baseScore
    for ((source, target) in REPAIR_PAIRS) {
        val bytes = encodeStrict(value, source) ?: continue
        val decoded = decodeStrict(bytes, target) ?: continue
        if (decoded.isBlank() || decoded == value) continue
        // UTF-8 是最常见的原始编码；两个候选都合法时优先它，避免把「é」误判成 GBK 汉字。
        val score = readability(decoded) + if (target == StandardCharsets.UTF_8) 10 else 0
        if (score >= bestScore + 6) {
            best = decoded
            bestScore = score
        }
    }
    return best
}

/** 从 LMS 返回的 url 中取出不带扩展名的文件名，用作备用曲名。 */
fun Track.fileNameTitle(): String {
    if (url.isBlank()) return ""
    val raw = url.substringAfterLast('/', "").substringAfterLast('\\', "")
    if (raw.isBlank()) return ""
    val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    val withoutQuery = decoded.substringBefore('?').substringBefore('#')
    return withoutQuery.substringBeforeLast('.').trim().ifBlank { withoutQuery.trim() }
}

/** 曲名显示设置与工具，供所有列表、播放页、歌词页共用。 */
object TrackDisplay {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun configure(value: AppSettings) {
        _settings.value = value
    }

    fun text(value: String, setting: AppSettings = _settings.value): String =
        if (setting.fixGarbledText) repairMojibake(value) else value

    fun title(track: Track, setting: AppSettings = _settings.value): String {
        val base = if (setting.useFileNameAsTitle) {
            track.fileNameTitle().ifBlank { track.title }
        } else {
            track.title
        }
        return text(base, setting)
    }

    fun artist(value: String, setting: AppSettings = _settings.value): String = text(value, setting)
    fun album(value: String, setting: AppSettings = _settings.value): String = text(value, setting)
}
