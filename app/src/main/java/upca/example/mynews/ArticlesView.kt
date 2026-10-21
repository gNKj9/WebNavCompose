package upca.example.mynews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import upca.example.mynews.ui.theme.MyNewsTheme
import java.util.Date

/**
 * Ecrã de lista. A base desenhava a lista e o loading dentro do composable que também criava
 * o ViewModel; aqui o estado vem de fora (state down, events up), o que permite pré-visualizar
 * os quatro estados no @Preview e testar o ecrã sem rede.
 */
@Composable
fun ArticlesView(
    uiState: ArticlesViewState,
    bookmarks: Set<String>,
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    onRetry: () -> Unit = {}
) {
    ArticlesViewContent(
        modifier = modifier,
        uiState = uiState,
        bookmarks = bookmarks,
        onRetry = onRetry,
        onArticleClick = { article ->
            article.url?.takeIf { it.isNotBlank() }?.let { url ->
                navController.navigate(Screen.ArticleDetail.createRoute(url))
            }
        }
    )
}

@Composable
fun ArticlesViewContent(
    modifier: Modifier = Modifier,
    uiState: ArticlesViewState = ArticlesViewState(),
    bookmarks: Set<String> = emptySet(),
    onRetry: () -> Unit = {},
    onArticleClick: (Article) -> Unit = {}
) {
    when {
        uiState.isLoading -> LoadingView(modifier = modifier)

        uiState.error != null -> ErrorView(
            message = uiState.error,
            onRetry = onRetry,
            modifier = modifier
        )

        uiState.isEmpty -> EmptyView(
            title = stringResource(R.string.lista_vazia_titulo),
            description = stringResource(R.string.lista_vazia_texto),
            actionLabel = stringResource(R.string.tentar_novamente),
            onAction = onRetry,
            modifier = modifier
        )

        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            itemsIndexed(items = uiState.articles) { _, article ->
                ArticleViewRow(
                    modifier = Modifier.clickable { onArticleClick(article) },
                    article = article,
                    isBookmarked = article.url?.let { it in bookmarks } == true
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Lista com artigos")
@Composable
fun ArticlesViewPreview() {
    MyNewsTheme {
        ArticlesViewContent(
            uiState = ArticlesViewState(
                articles = arrayListOf(
                    Article(
                        title = "As aulas começaram na UPCA",
                        description = "sdjwdkjw kjfhwse",
                        publishedAt = Date()
                    ),
                    Article(
                        title = "As aulas mobile são as melhores",
                        description = "sdjwdkjw kjfhwse",
                        urlToImage = "ddd",
                        publishedAt = Date()
                    ),
                ),
                error = null,
                isLoading = false
            ),
            bookmarks = setOf("https://exemplo.pt/aulas-mobile")
        )
    }
}

@Preview(showBackground = true, name = "Lista vazia")
@Composable
fun ArticlesViewEmptyPreview() {
    MyNewsTheme {
        ArticlesViewContent(uiState = ArticlesViewState(articles = emptyList(), isLoading = false))
    }
}

@Preview(showBackground = true, name = "Erro")
@Composable
fun ArticlesViewErrorPreview() {
    MyNewsTheme {
        ArticlesViewContent(
            uiState = ArticlesViewState(error = "HTTP 401", isLoading = false)
        )
    }
}
