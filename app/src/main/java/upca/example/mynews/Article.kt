package upca.example.mynews

import org.json.JSONObject
import java.util.Date

/**
 * Modelo de domínio — o da base da aula.
 *
 * A única mudança é o parsing: a NewsAPI devolve `null` em `description`, `urlToImage` ou
 * `publishedAt` quando não há esse dado, e o `getString` original lançava JSONException a
 * meio da lista (bastava uma notícia sem imagem para ficar sem nenhuma). `optString`
 * devolve "" nesses casos.
 */
class Article(
    var title: String? = null,
    var description: String? = null,
    var url: String? = null,
    var urlToImage: String? = null,
    var publishedAt: Date? = null,
) {
    companion object {
        fun fromJson(articleJson: JSONObject): Article {
            return Article(
                title = articleJson.optString("title"),
                description = articleJson.optString("description"),
                url = articleJson.optString("url"),
                urlToImage = articleJson.optString("urlToImage"),
                publishedAt = articleJson.optString("publishedAt").toServerDate()
            )
        }
    }
}
