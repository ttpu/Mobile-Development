# Laboratory 10 - Retrofit

GitHub users ro'yxatini Retrofit bilan yuklaydigan oddiy Compose ilova. Bitta ekran, barcha kod `MainActivity.kt` ichida.

## Sozlash

`app/build.gradle.kts`:

```kotlin
android {
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

`AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Demo video (max 40 soniya)

<details>
<summary>Videoni ochish / yopish</summary>

<!-- Videoni shu yerga sudrab tashlang (GitHub README tahrirlashda) va hosil bo'lgan havolani qo'ying -->
VIDEO_HAVOLASI

</details>

## Video senariysi (qadamma-qadam)

**1. Happy path**
1. Ilovani oching: aylanuvchi progress ko'rinadi (Loading).
2. Ro'yxatda 5 ta login chiqadi (`per_page = 5`).
3. Biror loginni bosing: uning ostida `id` va `avatarUrl` chiqadi.

**2. Failure path (n tugaguncha takrorlanadi)**
1. Airplane mode ni yoqing, ilovani qayta oching.
2. "No network" va **Retry** tugmasi chiqadi.
3. Retry ni bosing, yana xato: n = 1, n = 2, n = 3.
4. n = 3 bo'lganda Retry yo'qoladi, **Open Wi-Fi settings** tugmasi chiqadi.
5. Debug tugma (`Debug: call 404`) bosilsa: "Server error: 404".

**3. Recovery path**
1. Airplane mode ni o'chiring.
2. Retry bosing: ro'yxat yuklanadi, n qayta 0 bo'ladi.

**4. Ixtiyoriy qism**
- Qilinmagan (AuthInterceptor / Moshi).

## Qolgan bandlar bo'yicha tekshiruv

| Band | Nima ko'rsatildi |
|---|---|
| 1. Dependencies, permission | Gradle sync o'tdi, ikkala fayl videoda ko'rsatildi |
| 2. Model | `avatar_url` -> `avatarUrl`; `avatarUrll` xatosi -> `null` ekranda |
| 3. Interface | `@GET("/users")`, `@GET("/users/{login}")` + `@Path` |
| 4. Builder | trailing slash yo'q bo'lsa `IllegalArgumentException` (Logcat) |
| 5. ViewModel | INTERNET olib tashlanganda xato: `TEKSHIRIB_YOZING` |
| 6. Query / Path | `per_page = 5` -> 5 ta element |
| 7. UiState | Loading / Success / Error |
| 8. ApiInstance | Find in Files: `Retrofit.Builder` - 1 ta natija |
| 9. Logging | debug: `GET /users?per_page=5`, 200, vaqt; release: log yo'q |
| 10. Errors and retry | NetworkError, HttpError(404), n = 3 |

## Chaqiruv serverga qanday boradi (Retrofit -> converter -> OkHttp)

```text
SHU YERGA O'ZINGIZNING SO'ZLARINGIZ BILAN YOZING.
Repo bo'yicha: GitHubService.users() -> ApiInstance dagi Retrofit ->
GsonConverterFactory -> OkHttpClient (HttpLoggingInterceptor) -> api.github.com
va javob qaytib User ro'yxatiga aylanishi.
```
