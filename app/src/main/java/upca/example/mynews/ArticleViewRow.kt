package upca.example.mynews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import upca.example.mynews.ui.theme.MyNewsTheme
import java.util.Date

/**
 * Uma linha da lista — o `RowArticle` da base, tal como estava, mais dois pormenores:
 * é um Composable à parte (reciclado pelo LazyColumn e reutilizado nos guardados) e mostra
 * um coração quando a notícia está guardada.
 */
@Composable
fun ArticleViewRow(
    modifier: Modifier = Modifier,
    article: Article,
    isBookmarked: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text(
            text = article.title ?: "",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            article.description ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onTertiary)
                    .height(200.dp),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                model = article.urlToImage ?: "no image",
                contentDescription = "image article",
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .padding(2.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text((article.publishedAt ?: Date()).toShortTimeDate())
                if (isBookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArticleViewRowPreview() {
    MyNewsTheme {
        ArticleViewRow(
            article = Article(
                title = "As aulas mobile são as melhores jhsdgfuhjwegfjsegdfjsdgfjsdg jsg fu yaegfsiuqasf auwfg iquawfg qiuwfgqwuifg quiwfyg ",
                description = "sdjwdkjw kjfhwse",
                urlToImage = "ddd"
            )
        )
    }
}
