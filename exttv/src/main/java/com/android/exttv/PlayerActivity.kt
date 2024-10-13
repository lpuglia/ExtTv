package com.android.exttv

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okio.GzipSource
import okio.buffer


@UnstableApi
class PlayerActivity : AppCompatActivity() {

    @Serializable
    data class License(
        val headers: Map<String, String> = emptyMap(),
        val licenseType: String = "",
        val licenseKey: String = ""
    )

    @Serializable
    data class ExtTvMediaSource(
        val headers: Map<String, String> = emptyMap(),
        val source: String,
        val streamType: String,
        val license: License = License(),
        val label: String,
        val label2: String,
        val plot: String,
        val art: Map<String, String>
    )

    private var doubleBackToExitPressedOnce = false
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
                return true
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, backPressInterval)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Security.insertProviderAt(Conscrypt.newProvider(), 1) //without this I get handshake error

        // disable strict mode because ScraperManager.postfinished may need to scrape a proxy when onDemand is called
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val data = intent.data
        data?.let {
            val uriString = data.toString()
            if (uriString.startsWith("exttv://")) {
                val mediaSource = data.getQueryParameter("media_source")
                    ?.let { Json.decodeFromString<ExtTvMediaSource>(it) }

                setContent {
                    PlayerViewComposable(mediaSource)
                }
            }
        }
    }

    private fun preparePlayer(mediaSource: ExtTvMediaSource): MediaSource {

        val mediaItem = MediaItem.fromUri(Uri.parse(mediaSource.source))

        val mediaDataSourceFactory = clientFactory(mediaSource.headers)

        val mediaSourceFactory = when (mediaSource.streamType) {
            "application/dash+xml" -> {
                val manifestDataSourceFactory = clientFactory(mediaSource.license.headers)
                val playreadyCallback = HttpMediaDrmCallback(mediaSource.license.licenseKey, manifestDataSourceFactory)

                val drmManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        if (mediaSource.license.licenseType == "com.widevine.alpha") C.WIDEVINE_UUID else C.CLEARKEY_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(playreadyCallback)

                DashMediaSource.Factory(mediaDataSourceFactory).setDrmSessionManagerProvider { drmManager }
            }
            "application/x-mpegURL" -> HlsMediaSource.Factory(mediaDataSourceFactory)
            "extractor" -> ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            "mp4", "mkv", "" -> DefaultMediaSourceFactory(mediaDataSourceFactory)
            else -> {
                throw IllegalArgumentException("Unsupported media source type: ${mediaSource.streamType}")
            }
        }
        return mediaSourceFactory.createMediaSource(mediaItem)
    }

    private fun clientFactory(headers: Map<String, String>): OkHttpDataSource.Factory {
        val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val newRequestBuilder = chain.request().newBuilder()
                for ((key, value) in headers) {
                    newRequestBuilder.header(key, value)
                }
                chain.proceed(newRequestBuilder.build())
            }
            .addInterceptor { chain ->
                val request = chain.request()
                val originalResponse = chain.proceed(request)
                val contentEncoding = originalResponse.header("Content-Encoding")

                if (contentEncoding != null && contentEncoding.equals("gzip", ignoreCase = true)) {
                    val responseBody = originalResponse.body
                    val gzipSource = GzipSource(responseBody!!.source())
                    val decompressedBody = ResponseBody.create(responseBody.contentType(), -1, gzipSource.buffer())

                    originalResponse.newBuilder()
                        .header("Content-Encoding", "identity")
                        .removeHeader("Content-Length")
                        .body(decompressedBody)
                        .build()
                } else {
                    originalResponse
                }

            }
//        if (requiresProxy) initClientProxy(clientBuilder)
//        val cookieManager = CookieManager()
//        val cookieJar = object : CookieJar {override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
//            for (cookie in cookies) {
//                cookieManager.cookieStore.add(url.toUri(), cookie)
//            }
//        }
//        }
//        clientBuilder.cookieJar(cookieJar)
        return OkHttpDataSource.Factory(clientBuilder.build())
    }

    @Composable
    fun PlayerViewComposable(extTvMediaSource: ExtTvMediaSource?) {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                // Set up your player (media source, etc.) here
            }
        }
        exoPlayer.playWhenReady = true
        exoPlayer.stop()
        val mediaSource = preparePlayer(extTvMediaSource!!)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        // Proper usage of AndroidView and DisposableEffect
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                }
            }
        )

        DisposableEffect(exoPlayer) {
            onDispose {
                exoPlayer.release()
            }
        }
    }

}