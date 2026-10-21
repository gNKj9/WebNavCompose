package upca.example.mynews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estado do ecrã — o da base, com uma propriedade derivada.
 *
 * `isEmpty` distingue "sucesso sem artigos" de "ainda a carregar": sem ela, uma resposta
 * vazia desenhava uma lista em branco indistinguível de um erro.
 */
data class ArticlesViewState(
    val articles: List<Article> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean
        get() = articles.isEmpty() && !isLoading && error == null
}

/**
 * Uma instância para a app toda (criada em MyNewsApp): a lista, os guardados e o detalhe
 * leem o mesmo estado, por isso abrir uma notícia não repete o pedido à API.
 */
class ArticlesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArticlesViewState())
    val uiState: StateFlow<ArticlesViewState> = _uiState.asStateFlow()

    /** Guardados por URL — a NewsAPI não tem ids. Só em memória, chega para o exercício. */
    private val _bookmarks = MutableStateFlow<Set<String>>(emptySet())
    val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        fetchArticles()
    }

    fun fetchArticles() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val articles = requestArticles()
                _uiState.value = ArticlesViewState(articles = articles, error = null, isLoading = false)
            } catch (e: Exception) {
                // Rede em baixo, timeout, HTTP 4xx/5xx, chave inválida ou JSON inesperado.
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Falha de rede"
                )
            }
        }
    }

    fun toggleBookmark(url: String) {
        val current = _bookmarks.value
        _bookmarks.value = if (url in current) current - url else current + url
    }

    /**
     * GET assíncrono com `enqueue`: o pedido corre no dispatcher do OkHttp e o resultado chega
     * por callback — nunca há socket a bloquear a Main Thread nem uma corrotina presa ao IO.
     * `suspendCancellableCoroutine` transforma esse callback numa função `suspend`.
     */
    private suspend fun requestArticles(): List<Article> = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL?country=us&apiKey=$API_KEY")
            .get()
            .build()

        val call: Call = client.newCall(request)

        // Se o ViewModel for destruído a meio, cancela o pedido HTTP.
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { result ->
                    // OkHttp 5: o corpo é não-nulo; lê-se no callback (nunca na Main Thread).
                    val payload = result.body.string()

                    if (!result.isSuccessful) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException("HTTP ${result.code}"))
                        }
                        return
                    }

                    try {
                        val json = JSONObject(payload)

                        // A NewsAPI responde 200 mesmo quando recusa o pedido: o erro vem no corpo.
                        if (json.optString("status") != "ok") {
                            val message = json.optString("message")
                                .ifBlank { "Pedido recusado pela API" }
                            if (continuation.isActive) continuation.resumeWithException(IOException(message))
                            return
                        }

                        val articlesJson = json.getJSONArray("articles")
                        val articles = ArrayList<Article>(articlesJson.length())
                        for (index in 0 until articlesJson.length()) {
                            articles.add(Article.fromJson(articlesJson.getJSONObject(index)))
                        }
                        if (continuation.isActive) continuation.resume(articles)
                    } catch (e: JSONException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException("Resposta JSON inválida", e))
                        }
                    }
                }
            }
        })
    }

    companion object {
        private const val BASE_URL = "https://newsapi.org/v2/top-headlines"

        // Chave da base da aula (NewsAPI, plano free). Trocar pela própria antes de entregar.
        private const val API_KEY = "1765f87e4ebc40229e80fd0f75b6416c"
    }
}
