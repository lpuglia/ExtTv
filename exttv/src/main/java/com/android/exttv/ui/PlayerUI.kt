import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.data.ExtTvMediaSource
import com.android.exttv.model.manager.MediaSourceManager
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerView(card: CardItem, extTvMediaSource: ExtTvMediaSource) {
    var progress by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setMediaSource(MediaSourceManager.preparePlayer(extTvMediaSource))
            prepare()
        }
    }

    // Launch a coroutine to update progress periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            progress = (exoPlayer.currentPosition.toFloat() / exoPlayer.duration).coerceIn(0f, 1f)
            delay(500) // Update every 500ms
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // AndroidView to render the PlayerView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Hide default controls if you want a fully custom experience
                }
            }
        )

        // Custom progress bar at the bottom
        CustomProgressBar(
            progress = progress,
            modifier = Modifier
                .focusable()
                .onKeyEvent { keyEvent -> handleKeyEvent(keyEvent, exoPlayer) } // Call the separate handler
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 30.dp)
        )
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
}


@Composable
fun CustomProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.Gray, shape = RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Color.Red, shape = RoundedCornerShape(50))
        )
    }
}

var seekMultiplier = 1.0 // Initial multiplier for seek increment
val resetSeekTimeoutMs = 1000L // Time in milliseconds to reset seek increment after inactivity
var handler: Handler? = null
var resetRunnable: Runnable? = null

fun handleKeyEvent(keyEvent: KeyEvent, exoPlayer: ExoPlayer, seekIncrementMs: Long = 5000L): Boolean {
    // Reset the multiplier if no key is pressed for a while
    resetSeekMultiplierIfNeeded()

    return if (keyEvent.type == KeyEventType.KeyDown) {
        when (keyEvent.key) {
            Key.DirectionLeft -> {
                // Calculate the seek increment with the multiplier
                var seekAmount = seekIncrementMs * seekMultiplier
                // Limit the seek amount to a maximum of 10% of the video duration
                val maxSeekAmount = exoPlayer.duration / 10
                seekAmount = seekAmount.coerceAtMost(maxSeekAmount.toDouble())

                // Rewind by the calculated seekAmount
                exoPlayer.seekTo((exoPlayer.currentPosition - seekAmount).coerceAtLeast(0.0).toLong())

                // Increase seek increment by 50% after each press
                seekMultiplier *= 1.5
                // Restart the reset timeout
                resetSeekMultiplierIfNeeded()
                true
            }
            Key.DirectionRight -> {
                // Calculate the seek increment with the multiplier
                var seekAmount = seekIncrementMs * seekMultiplier
                // Limit the seek amount to a maximum of 10% of the video duration
                val maxSeekAmount = exoPlayer.duration / 10
                seekAmount = seekAmount.coerceAtMost(maxSeekAmount.toDouble())

                // Fast-forward by the calculated seekAmount
                exoPlayer.seekTo((exoPlayer.currentPosition + seekAmount).coerceAtMost(exoPlayer.duration.toDouble()).toLong())

                // Increase seek increment by 50% after each press
                seekMultiplier *= 1.5
                // Restart the reset timeout
                resetSeekMultiplierIfNeeded()
                true
            }
            Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                // Play/pause
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                true
            }
            else -> false
        }
    } else {
        false
    }
}

fun resetSeekMultiplierIfNeeded() {
    // Cancel any existing reset task
    handler?.removeCallbacks(resetRunnable ?: return)

    // Create a new reset task to reset seekMultiplier after the timeout
    resetRunnable = Runnable {
        seekMultiplier = 1.0 // Reset multiplier to initial value
    }

    // Set up the timeout to reset the seek multiplier
    handler = Handler(Looper.getMainLooper())
    handler?.postDelayed(resetRunnable!!, resetSeekTimeoutMs)
}