package upca.example.mynews

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rotas da app — a `sealed class` da base, agora completa.
 *
 * `isBaseScreen` separa os ecrãs que vivem na bottom bar (lista, guardados, sobre) do detalhe,
 * que se abre por cima e tem botão voltar. O detalhe leva a URL do artigo como *query
 * parameter*: ao contrário do `{title}/{url}` da base, sobrevive a "/", "?" e "&" dentro do
 * link, que é o que as URLs da NewsAPI trazem.
 */
sealed class Screen(val route: String, val isBaseScreen: Boolean) {

    data object Articles : Screen("articles", isBaseScreen = true)

    data object Bookmarks : Screen("bookmarks", isBaseScreen = true)

    data object About : Screen("about", isBaseScreen = true)

    data object ArticleDetail : Screen("article_detail?url={url}", isBaseScreen = false) {
        const val ARG_URL = "url"

        fun createRoute(url: String): String = "article_detail?url=${Uri.encode(url)}"
    }

    companion object {
        /** Ordem dos itens da bottom bar. */
        val baseScreens: List<Screen> = listOf(Articles, Bookmarks, About)

        /** Rota atual -> ecrã. Serve para decidir título, botão voltar e bottom bar. */
        fun fromRoute(route: String?): Screen? =
            (baseScreens + ArticleDetail).firstOrNull { it.route == route }
    }
}

/** Um item da bottom bar: ecrã + etiqueta + ícone. */
data class BottomTab(
    val screen: Screen,
    @StringRes val label: Int,
    val icon: ImageVector
)

val bottomTabs: List<BottomTab> = listOf(
    BottomTab(Screen.Articles, R.string.titulo_artigos, Icons.AutoMirrored.Filled.List),
    BottomTab(Screen.Bookmarks, R.string.titulo_guardados, Icons.Filled.Favorite),
    BottomTab(Screen.About, R.string.titulo_sobre, Icons.Filled.Info)
)
