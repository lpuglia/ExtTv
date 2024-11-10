import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.data.ExtTvMediaSource
import com.android.exttv.model.manager.MediaSourceManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Modifier.dbgMode(color: Color = Color.Red): Modifier =
    if (true) this.border(2.dp, color) else this

@OptIn(UnstableApi::class)
@Composable
fun PlayerView(card: CardItem, extTvMediaSource: ExtTvMediaSource) {

    var isProgressBarVisible by remember { mutableStateOf(false) }
    var lastKeyPressedTime by remember { mutableStateOf(System.currentTimeMillis()) }
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

    // Launch a coroutine that checks if enough time has passed to hide the box
    LaunchedEffect(lastKeyPressedTime) {
        // Keep the ProgressBar visible for 3 seconds after the last key press
        delay(3000)
        if (System.currentTimeMillis() - lastKeyPressedTime >= 3000) {
            isProgressBarVisible = !exoPlayer.isPlaying
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // AndroidView to render the PlayerView
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Hide default controls
                }
            }
        )

        Row(
            modifier = Modifier
                .dbgMode(Color.Blue)
                .alpha(if (isProgressBarVisible) 1f else 0f)
                .align(Alignment.TopCenter)
                .height(200.dp)
                .fillMaxWidth()
//                .background(
//                    brush = Brush.verticalGradient(
//                        colors = listOf(Color.Black, Color.Transparent),
//                        endY = Float.POSITIVE_INFINITY,
//                        startY = 0f
//                    )
//                )
        ){
            Box(Modifier.dbgMode(Color.Green).width(200.dp)){
                AsyncImage(model = card.primaryArt, contentDescription = card.label)
            }
            Box(Modifier.dbgMode(Color.Green).width(200.dp)){
                Text("Title: ${card.label} + ${card.plot}")
            }
        }

        Column(
            modifier = Modifier
                .dbgMode(Color.Blue)
                .alpha(if (isProgressBarVisible) 1f else 0f)
                .align(Alignment.BottomCenter)
                .height(200.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        ) {
            CustomProgressBar(
                progress = progress,
                modifier = Modifier
                    .dbgMode(Color.Green)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        println("Key event: $keyEvent, progress")
                        if (keyEvent.key != Key.Back) {
                            isProgressBarVisible = true
                            lastKeyPressedTime = System.currentTimeMillis()
                        }
                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp) {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            true
                        } else {
                            handleKeyEvent(keyEvent, exoPlayer)
                            false
                        }
                    } // Call the separate handler
                    .padding(vertical = 10.dp),
                player = exoPlayer
            )
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
}


@Composable
fun CustomProgressBar(progress: Float, modifier: Modifier = Modifier, player: ExoPlayer) {
    val currentPosition = player.currentPosition
    val duration = player.duration
    val shadow = Shadow(
        color = Color.Black, // Outline color
        offset = Offset(0f, 0f),
        blurRadius = 3f // Optional, for a softer outline
    )

    fun formatTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Row(modifier = modifier
        .dbgMode(Color.Yellow)
        .fillMaxWidth()
        .height(80.dp)
        .padding(horizontal = 16.dp)
    ) {
        Column {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Gray, shape = RoundedCornerShape(50))
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(50),
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color.Red, shape = RoundedCornerShape(50))
                )
            }
            if(player.isCurrentMediaItemLive){
                val currentTimeOfDay = System.currentTimeMillis()
                val playbackStartTime = currentTimeOfDay - (duration - currentPosition)
                Row(modifier = Modifier.dbgMode()) {
                    LiveIndicator()
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(playbackStartTime)),
                        style = TextStyle.Default.copy(
                            fontSize = 16.sp,
                            color = Color.White, // Set base text color to white
                            shadow = shadow
                        )
                    )
                }
            }else {
                Text(formatTime(currentPosition) + " / " + formatTime(duration))
            }
        }
    }
}

@Composable
fun LiveIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
    ) {
        // Red Circle
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.Red, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // "LIVE" Text
        Text(
            text = "LIVE",
            fontWeight = FontWeight.Bold,
            style = TextStyle.Default.copy(
                fontSize = 16.sp,
                color = Color.Red, // Set base text color to white
                shadow = Shadow(
                    color = Color.Black, // Outline color
                    offset = Offset(0f, 0f),
                    blurRadius = 3f // Optional, for a softer outline
                )
            )

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