package upca.example.mynews

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * "2026-10-13T14:15:23Z" -> Date.
 *
 * A NewsAPI devolve sempre UTC: o formato inclui o `Z` e o timezone é fixado, senão o
 * SimpleDateFormat interpretava a hora como local e as notícias apareciam desfasadas.
 */
fun String.toServerDate(): Date? {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return runCatching { format.parse(this) }.getOrNull()
}

fun Date.toShortTimeDate(): String {
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return format.format(this)
}
