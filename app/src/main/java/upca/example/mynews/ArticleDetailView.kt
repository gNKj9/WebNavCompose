package upca.example.mynews

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import upca.example.mynews.ui.theme.MyNewsTheme
import java.util.Date

/**
 * Ecrã de detalhe. Recebe a URL da rota e procura a notícia no estado que já está em
 * memória — não repete o pedido à API, por isso a abertura é instantânea.
 *
 * Mostra os dados do item selecionado (imagem, título, data e descrição) e, por baixo, a
 * página completa no WebView que já existia na base. Se a lista ainda não carregou, mostra
 * o estado correspondente em vez de um ecrã em branco.
 */
@Composable
fun ArticleDetailView(
    url: String,
    uiState: ArticlesViewState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val article = uiState.articles.firstOrNull { it.url == url }

    when {
        article != null -> ArticleDetailContent(article = article, modifier = modifier)

        uiState.isLoading -> LoadingView(modifier = modifier)

        uiState.error != null -> ErrorView(
            message = uiState.error,
            onRetry = onRetry,
            modifier = modifier
        )

        else -> EmptyView(
            title = stringResource(R.string.artigo_indisponivel),
            description = stringResource(R.string.artigo_indisponivel_texto),
            actionLabel = stringResource(R.string.voltar),
            onAction = onBack,
            modifier = modifier
        )
    }
}

@Composable
private fun ArticleDetailContent(article: Article, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {

        if (!article.urlToImage.isNullOrBlank()) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                model = article.urlToImage,
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = article.title,
                contentScale = ContentScale.Crop
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = article.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            article.publishedAt?.toShortTimeDate()?.let { date ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            article.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider()

        // A página completa — o WebView da base da aula, agora com cabeçalho por cima.
        ArticleWebView(
            url = article.url.orEmpty(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ArticleWebView(url: String, modifier: Modifier = Modifier) {
    if (url.isBlank()) return

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.setSupportZoom(true)
            }
        },
        update = { webView ->
            // A tag guarda a última URL carregada: sem isto, cada recomposição (ex.: ao
            // tocar no coração da top bar) recarregava a página.
            if (webView.tag != url) {
                webView.tag = url
                webView.loadUrl(url)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ArticleDetailViewPreview() {
    MyNewsTheme {
        ArticleDetailView(
            url = "https://exemplo.pt/noticia",
            uiState = ArticlesViewState(
                articles = arrayListOf(
                    Article(
                        title = "As aulas mobile são as melhores",
                        description = "Uma descrição curta da notícia para a pré-visualização.",
                        url = "https://exemplo.pt/noticia",
                        publishedAt = Date()
                    )
                )
            ),
            onRetry = {},
            onBack = {}
        )
    }
}
