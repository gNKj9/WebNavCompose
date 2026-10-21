package upca.example.mynews

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Um único Scaffold para toda a app — topBar e bottomBar mudam com a rota atual.
 *
 * - Ecrãs base  -> bottom bar visível, sem botão voltar.
 * - Detalhe     -> botão voltar + ações (guardar, partilhar), sem bottom bar.
 */
@Composable
fun MyNewsApp(viewModel: ArticlesViewModel = viewModel()) {
    val navController = rememberNavController()

    val uiState by viewModel.uiState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)
    // No primeiro frame o back stack está vazio: assume base (o destino inicial é a lista).
    val isBaseScreen = currentScreen?.isBaseScreen ?: true

    // Notícia aberta no detalhe (o top bar precisa dela para partilhar/guardar).
    val detailUrl = if (currentScreen == Screen.ArticleDetail) {
        backStackEntry?.arguments?.getString(Screen.ArticleDetail.ARG_URL).orEmpty()
    } else {
        null
    }
    val detailArticle: Article? = detailUrl?.let { url ->
        uiState.articles.firstOrNull { it.url == url }
    }

    val context = LocalContext.current

    val title = when (currentScreen) {
        Screen.Bookmarks -> stringResource(R.string.titulo_guardados)
        Screen.About -> stringResource(R.string.titulo_sobre)
        Screen.ArticleDetail -> detailArticle?.title?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.titulo_artigo)

        else -> stringResource(R.string.titulo_artigos)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                showBack = !isBaseScreen,
                showRefresh = currentScreen == Screen.Articles,
                article = detailArticle,
                bookmarked = detailArticle?.url?.let { it in bookmarks } == true,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::fetchArticles,
                onShare = { detailArticle?.let { shareArticle(context, it) } },
                onBookmark = { detailArticle?.url?.let { viewModel.toggleBookmark(it) } }
            )
        },
        bottomBar = {
            if (isBaseScreen) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { screen -> navController.navigateToTab(screen) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Articles.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Articles.route) {
                ArticlesView(
                    uiState = uiState,
                    bookmarks = bookmarks,
                    navController = navController,
                    onRetry = viewModel::fetchArticles
                )
            }

            composable(Screen.Bookmarks.route) {
                BookmarksView(
                    uiState = uiState,
                    bookmarks = bookmarks,
                    navController = navController,
                    onRetry = viewModel::fetchArticles,
                    onGoToArticles = { navController.navigateToTab(Screen.Articles) }
                )
            }

            composable(Screen.About.route) {
                AboutView()
            }

            composable(
                route = Screen.ArticleDetail.route,
                arguments = listOf(
                    navArgument(Screen.ArticleDetail.ARG_URL) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                ArticleDetailView(
                    url = entry.arguments?.getString(Screen.ArticleDetail.ARG_URL).orEmpty(),
                    uiState = uiState,
                    onRetry = viewModel::fetchArticles,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Navegação entre ecrãs base: não empilha, reutiliza o estado de cada separador.
 * `popUpTo` no início limpa a pilha; `saveState`/`restoreState` devolvem o scroll e a
 * posição quando se volta ao separador.
 */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    showBack: Boolean,
    showRefresh: Boolean,
    article: Article?,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.voltar)
                    )
                }
            }
        },
        actions = {
            if (showRefresh) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.atualizar)
                    )
                }
            }
            if (article != null) {
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(
                            if (bookmarked) R.string.remover_guardado else R.string.guardar
                        )
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.partilhar)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun AppBottomBar(currentRoute: String?, onSelect: (Screen) -> Unit) {
    // rememberSaveable: a seleção sobrevive a rotação e à morte do processo.
    var selectedTab by rememberSaveable { mutableStateOf(Screen.Articles.route) }

    // Se a rota mudar por outra via (ex.: voltar do detalhe), a barra acompanha.
    LaunchedEffect(currentRoute) {
        if (Screen.baseScreens.any { it.route == currentRoute }) {
            selectedTab = currentRoute ?: Screen.Articles.route
        }
    }

    NavigationBar {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab.screen.route,
                onClick = {
                    selectedTab = tab.screen.route
                    onSelect(tab.screen)
                },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) }
            )
        }
    }
}

/** Partilha nativa: entrega título e link a qualquer app que aceite texto. */
private fun shareArticle(context: Context, article: Article) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.url}")
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.partilhar)))
}
