import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.exttv.model.MediaSourceManager

@OptIn(UnstableApi::class)
@Composable
fun PlayerView(extTvMediaSource: String?) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Set up your player (media source, etc.) here
        }
    }
    exoPlayer.playWhenReady = true
    exoPlayer.stop()

    val mediaSource = extTvMediaSource?.let {
        MediaSourceManager.preparePlayer(it)
    }
    mediaSource?.let {
        exoPlayer.setMediaSource(it)
        exoPlayer.prepare()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
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