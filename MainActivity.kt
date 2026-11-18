package com.example.retrofitlab

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.annotations.SerializedName
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class User(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
)

interface GitHubService {
    @GET("/users")
    suspend fun users(@Query("per_page") perPage: Int? = null): List<User>

    @GET("/users/{login}")
    suspend fun user(@Path("login") login: String): User
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class HttpError(val code: Int) : ApiResult<Nothing>
    data class NetworkError(val cause: IOException) : ApiResult<Nothing>
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(call())
    } catch (e: HttpException) {
        ApiResult.HttpError(e.code())
    } catch (e: IOException) {
        ApiResult.NetworkError(e)
    }

object ApiInstance {
    private const val BASE_URL = "https://api.github.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().addInterceptor(logging).build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(service: Class<T>): T = retrofit.create(service)
}

sealed interface UiState {
    data object Loading : UiState
    data class Success(val users: List<User>) : UiState
    data class Error(val message: String, val tries: Int) : UiState
}

class UsersViewModel : ViewModel() {
    private val service = ApiInstance.createService(GitHubService::class.java)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private val _details = MutableStateFlow<Map<String, User>>(emptyMap())
    val details: StateFlow<Map<String, User>> = _details

    private var tries = 0

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = safeApiCall { service.users(PER_PAGE) }) {
                is ApiResult.Success -> {
                    tries = 0
                    _state.value = UiState.Success(r.data)
                }
                is ApiResult.HttpError -> _state.value = UiState.Error("Server error: ${r.code}", tries)
                is ApiResult.NetworkError -> _state.value = UiState.Error("No network", tries)
            }
        }
    }

    fun retry() {
        tries++
        load()
    }

    fun loadUser(login: String) {
        viewModelScope.launch {
            when (val r = safeApiCall { service.user(login) }) {
                is ApiResult.Success -> _details.value = _details.value + (login to r.data)
                is ApiResult.HttpError -> _state.value = UiState.Error("Server error: ${r.code}", tries)
                is ApiResult.NetworkError -> _state.value = UiState.Error("No network", tries)
            }
        }
    }

    companion object {
        const val PER_PAGE = 5
        const val MAX_TRIES = 3
    }
}

@Composable
fun UsersScreen(vm: UsersViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (BuildConfig.DEBUG) {
            Button(onClick = { vm.loadUser("no-such-login-xyz") }) { Text("Debug: call 404") }
        }

        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is UiState.Success -> LazyColumn {
                items(s.users, key = { it.id }) { user ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.loadUser(user.login) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(user.login, style = MaterialTheme.typography.titleMedium)
                        details[user.login]?.let { d ->
                            Text("id: ${d.id}")
                            Text("avatarUrl: ${d.avatarUrl}")
                        }
                    }
                }
            }

            is UiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                if (s.tries < UsersViewModel.MAX_TRIES) {
                    Button(onClick = { vm.retry() }) { Text("Retry") }
                } else {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    }) { Text("Open Wi-Fi settings") }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { UsersScreen() } }
    }
}
