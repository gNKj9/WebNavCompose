---
date: "2026-10-21"
tags: [pdm, android]
status: maduro
---

# MyNews — base da aula, desenvolvida

Projeto da cadeira de PDM construído **em cima da base do professor** (`PDM2627/MyNews`):
a lista de notícias da NewsAPI, o `Article`, o `ArticleViewRow` e o WebView do detalhe são
dele; o que faltava do enunciado — navegação multi-ecrã, top bar com ações, os quatro
estados de UI e o `enqueue` do OkHttp — está implementado por cima, sem mudar a estrutura
nem o estilo do código original.

## Como abrir e correr

Pasta do projeto: **`WebNavCompose`** (é a raiz Gradle; `rootProject.name = "MyNews"`).

1. Android Studio → **Open** → escolher esta pasta (a que tem `settings.gradle.kts`).
2. Esperar o *Gradle Sync* — AGP 9.4.1, Kotlin 2.4.10, Compose BOM 2026.02.01, Gradle 9.6,
   OkHttp 5.5, Coil 3.6.3 (as versões que já vinham na base).
3. Correr num emulador/telefone com internet.

A app pede `https://newsapi.org/v2/top-headlines?country=us` com a **chave que vinha na base**
(plano free: ~100 pedidos por dia). Se a chave estiver esgotada, a app mostra o estado de erro
com a mensagem da própria API — é o comportamento esperado, não uma falha da app.

## Ecrãs

| Ecrã | O que mostra |
|---|---|
| **Artigos** | `LazyColumn` de `ArticleViewRow` (imagem, título, descrição, data); ⟳ na top bar |
| **Detalhe** | dados da notícia escolhida + página completa no WebView; top bar com voltar, guardar, partilhar |
| **Guardados** | a mesma lista filtrada pelo coração; reutiliza `ArticleViewRow` |
| **Sobre** | o que a app faz, stack e origem da base |

## Ficheiros

| Ficheiro | Responsabilidade |
|---|---|
| `MainActivity.kt` | Activity mínima: tema + `MyNewsApp()` |
| `MyNewsApp.kt` | `Scaffold` (top bar, bottom bar) + `NavHost`; um ViewModel para a app |
| `Screen.kt` | `sealed class Screen`, `isBaseScreen`, `fromRoute`, itens da bottom bar |
| `ArticlesViewModel.kt` | `ArticlesViewState`, `StateFlow`, pedido OkHttp com `enqueue`, guardados |
| `Article.kt` | modelo de domínio + `fromJson` |
| `Helpers.kt` | datas UTC (`toServerDate`, `toShortTimeDate`) |
| `ArticlesView.kt` | ecrã de lista + `when` sobre os quatro estados |
| `ArticleViewRow.kt` | a linha da lista (reutilizada nos guardados) |
| `ArticleDetailView.kt` | ecrã de detalhe + `ArticleWebView` |
| `BookmarksView.kt` | lista filtrada pelos guardados |
| `AboutView.kt` | ecrã estático |
| `StateViews.kt` | `LoadingView`, `ErrorView`, `EmptyView` |

## Requisitos → onde está

### Lists and Details

| Requisito | Onde |
|---|---|
| `LazyColumn` + itens reutilizáveis | `ArticlesView.kt` (`itemsIndexed`), `ArticleViewRow.kt` |
| Modelar `Article` a partir de JSON | `Article.kt` — `Article.fromJson(JSONObject)` |
| Lista → detalhe com o item escolhido | `ArticlesView` navega com a URL; `ArticleDetailView` procura a notícia no estado |
| Estados loading / erro / vazio / sucesso | `ArticlesViewContent` (`when` com 4 ramos) + `ArticlesViewState.isEmpty` |
| Linha e detalhe em Composables distintos | `ArticleViewRow` vs `ArticleDetailView` |

### Navigation

| Requisito | Onde |
|---|---|
| `sealed class Screen` | `Screen.kt` (Articles, Bookmarks, About, ArticleDetail) |
| `NavHost` / `NavController` | `MyNewsApp.kt` |
| `Scaffold` com `topBar`, `bottomBar`, conteúdo | `MyNewsApp.kt` |
| `NavigationBar` + `NavigationBarItem` | `AppBottomBar` |
| Top bar com título, voltar e ações | `AppTopBar` (partilhar, guardar, atualizar) |
| Base vs detalhe (`isBaseScreen`, `popBackStack`) | `Screen.isBaseScreen` e `onBack = { navController.popBackStack() }` |
| Seleção da bottom bar com `rememberSaveable` | `AppBottomBar`: `selectedTab` + `LaunchedEffect(currentRoute)` |

### Web API — OkHttp

| Requisito | Onde |
|---|---|
| Dependência OkHttp + permissão `INTERNET` | `app/build.gradle.kts` (já tinha) e `AndroidManifest.xml` (acrescentada) |
| GET assíncrono com `enqueue` | `ArticlesViewModel.requestArticles()` → `call.enqueue(...)` |
| JSON → modelos Kotlin | `JSONObject`/`JSONArray` + `Article.fromJson` |
| Resultado na UI por ViewModel + `StateFlow` | `uiState`/`bookmarks` + `collectAsState()` em `MyNewsApp` |
| Falhas de rede e HTTP inesperado | `onFailure`, `if (!result.isSuccessful)`, e `status != "ok"` no corpo da NewsAPI |
| Sem rede na Main Thread | `enqueue` corre no dispatcher do OkHttp; a corrotina só espera o callback |

## O que mudou em relação à base (e porquê)

| Na base | Aqui | Porquê |
|---|---|---|
| `getString("description")` no `fromJson` | `optString(...)` | A NewsAPI devolve `null` em `description`/`urlToImage`; o `getString` lançava `JSONException` e a lista ficava vazia por causa de uma imagem em falta |
| `execute()` dentro de `Dispatchers.IO` | `enqueue` + `suspendCancellableCoroutine` | É o modo assíncrono pedido no enunciado: nenhuma thread/ corrotina presa ao socket e o pedido é cancelável |
| só tratava `status == "ok"` | 4xx/5xx, timeout, `status: "error"` da API e JSON inválido | Na base, uma resposta de erro deixava o spinner para sempre |
| sem `response.use` | `response.use { }` | Fecha o corpo da resposta (fuga de recursos) |
| `OkHttpClient()` criado em cada pedido | um cliente com timeouts no ViewModel | Reutilizar o cliente é o que permite pool de ligações e timeouts |
| `Date` sem timezone | formato com `'Z'` e `TimeZone.UTC` | A NewsAPI devolve UTC; as horas apareciam desfasadas |
| sem `<uses-permission INTERNET>` | acrescentada | Sem ela, OkHttp e Coil falham sempre |
| `Screen` dentro do `MainActivity` | `Screen.kt` com 3 ecrãs base + detalhe | Permite bottom bar, `isBaseScreen` e rotas tipadas |
| rota `{title}/{url}` na path | `article_detail?url={url}` com `Uri.encode` | URLs da NewsAPI têm `/`, `?` e `&`; na path partiam o matching |
| `lifecycle-viewmodel-*:2.4.0` | `2.11.0` | Estava desalinhado do `lifecycle-runtime-ktx` 2.11 do catálogo |
| sem `material-icons-core` | dependência explícita `1.7.8` no catálogo | O Compose BOM 2026 já não traz os ícones com o `material3`; sem ela, `Icons.Filled.*` não compila |

Ficou tudo o resto como estava: package `upca.example.mynews`, nome MyNews, NewsAPI, OkHttp 5,
Coil, tema Material 3, desenho das linhas e o WebView do detalhe.

## Testar os quatro estados

1. **Sucesso** — abrir com internet.
2. **Loading** — tocar em ⟳ na top bar.
3. **Erro** — modo avião e ⟳; "Tentar novamente" recupera. Chave esgotada dá erro da API (HTTP 401), também visível.
4. **Lista vazia** — trocar `country=us` por um país sem notícias, ou apontar `BASE_URL` para um endpoint que devolva `{"status":"ok","articles":[]}`.

O detalhe também espera: se abrir antes de a lista carregar, mostra loading e depois a notícia.

## Verificação

Corrido neste projeto com a toolchain da base (Gradle 9.6, AGP 9.4.1, Kotlin 2.4.10, SDK 37):

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` | `BUILD SUCCESSFUL` — `app/build/outputs/apk/debug/app-debug.apk` (13,3 MB) |
| `./gradlew lintDebug` | `0 errors, 19 warnings` — versões mais recentes disponíveis, cores do template sem uso, `setJavaScriptEnabled` (o WebView já era da base) e 7 `UseTomlInstead` nas dependências que a base declara fora do catálogo |
| `aapt2 dump badging app-debug.apk` | `package upca.example.mynews`, `minSdk 24`, `targetSdk 37`, `uses-permission INTERNET`, `launchable-activity .MainActivity` |

Não há emulador neste ambiente: o que fica validado é compilação, lint e manifesto do APK.
Percorrer os ecrãs antes de entregar — o `app-debug.apk` que está na pasta serve para instalar
sem passar pelo Android Studio.
