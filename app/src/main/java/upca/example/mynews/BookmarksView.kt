package upca.example.mynews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * Ecrã de guardados: a mesma lista, filtrada pelas notícias marcadas na top bar.
 * Reutiliza o `ArticleViewRow` — a única diferença é a origem dos dados.
 */
@Composable
fun BookmarksView(
    uiState: ArticlesViewState,
    bookmarks: Set<String>,
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    onRetry: () -> Unit = {},
    onGoToArticles: () -> Unit = {}
) {
    val saved = uiState.articles.filter { article ->
        article.url?.let { it in bookmarks } == true
    }

    when {
        uiState.isLoading -> LoadingView(modifier = modifier)

        uiState.error != null -> ErrorView(
            message = uiState.error,
            onRetry = onRetry,
            modifier = modifier
        )

        saved.isEmpty() -> EmptyView(
            title = stringResource(R.string.guardados_vazio_titulo),
            description = stringResource(R.string.guardados_vazio_texto),
            actionLabel = stringResource(R.string.ver_artigos),
            onAction = onGoToArticles,
            modifier = modifier
        )

        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            itemsIndexed(items = saved) { _, article ->
                ArticleViewRow(
                    modifier = Modifier.clickable {
                        article.url?.let { navController.navigate(Screen.ArticleDetail.createRoute(it)) }
                    },
                    article = article,
                    isBookmarked = true
                )
            }
        }
    }
}
