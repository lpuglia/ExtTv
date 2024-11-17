import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.manager.PlayerManager
import com.android.exttv.model.manager.PlayerManager as Player
import com.android.exttv.ui.SectionView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Modifier.dbgMode(color: Color = Color.Red): Modifier =
    if (false) this.border(2.dp, color) else this

@OptIn(UnstableApi::class)
@Composable
fun PlayerView() {
    var progress by remember { mutableFloatStateOf(0f) }

    // Launch a coroutine to update progress periodically
    LaunchedEffect(Player.player) {
        while (true) {
            progress = (Player.player.currentPosition.toFloat() / Player.player.duration).coerceIn(0f, 1f)
            delay(500) // Update every 500ms
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
                    player = Player.player
                    useController = false // Hide default controls
                }
            }
        )

        Column( modifier = Modifier.align(Alignment.TopCenter)){
            Player.currentCard?.let { TopHeader(card = it) }
        }

        Column( modifier = Modifier.align(Alignment.BottomCenter)) {
            Player.currentCard?.let {
                CustomProgressBar(
                    card = it,
                    progress = progress,
                )
            }
        }
    }

    if (PlayerManager.isLoading || PlayerManager.playerState < 3) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(58.dp)
            )
        }
    }

    DisposableEffect(Player.player) {
        onDispose {
            Player.player.release()
        }
    }
}

@Composable
fun TopHeader(card : CardItem){
    val shadow = Shadow(
        color = Color.Black, // Outline color
        offset = Offset(0f, 0f),
        blurRadius = 3f // Optional, for a softer outline
    )
    Box(
        modifier = Modifier
            .dbgMode(Color.Blue)
            .alpha(if (Player.isProgressBarVisible && !Player.isLoading) 1f else 0f)
            .height(200.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    endY = Float.POSITIVE_INFINITY,
                    startY = 0f
                )
            )
    ){
        Row(modifier = Modifier
            .dbgMode()
            .align(Alignment.Center)) {
            Box(
                Modifier
                    .dbgMode(Color.Green)
                    .padding(end = 20.dp)
                    .width(200.dp)) {
                AsyncImage(model = card.primaryArt, contentDescription = card.label)
            }
            Box(
                Modifier
                    .dbgMode(Color.Green)
                    .padding(top = 20.dp)
                    .width(600.dp)) {
                Text(
                    "Title: ${card.label}\nPlot:  ${card.plot}",
                    style = TextStyle.Default.copy(
                        fontSize = 16.sp,
                        color = Color.White, // Set base text color to white
                        shadow = shadow
                    )
                )
            }
        }
    }
}


@Composable
fun CustomProgressBar(
    card: CardItem,
    progress: Float,
) {
    var lastKeyPressedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val focusRequesters = FocusRequester()

    // Launch a coroutine that checks if enough time has passed to hide the box
    LaunchedEffect(lastKeyPressedTime, Player.player.isPlaying) {
        // Keep the ProgressBar visible for 3 seconds after the last key press
        delay(3000)
        if(!Player.isVisibleCardList) {
            if (System.currentTimeMillis() - lastKeyPressedTime >= 3000) {
                Player.isProgressBarVisible = !Player.player.isPlaying
            }
        }
    }

    val currentPosition = Player.player.currentPosition
    val duration = Player.player.duration
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

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .dbgMode(Color.Yellow)
            .focusRequester(focusRequesters)
            .alpha(if (Player.isProgressBarVisible && !Player.isLoading) 1f else 0f)
            .fillMaxWidth()
            .height(300.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = Float.POSITIVE_INFINITY,
                    endY = 0f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .dbgMode(color = Color.Green)
                .focusable()
                .onKeyEvent { keyEvent ->
                    Player.isProgressBarVisible = true
                    lastKeyPressedTime = System.currentTimeMillis()
                    handleKeyEvent(keyEvent)
                    false
                }
        ) {
            Row(
                modifier = Modifier
                    .dbgMode()
                    .padding(horizontal = 40.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
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
            }
            Row(
                modifier = Modifier
                    .dbgMode()
                    .padding(end = 40.dp, bottom = 20.dp)
                    .align(Alignment.End)
            ) {
                if (Player.player.isCurrentMediaItemLive) {
                    val currentTimeOfDay = System.currentTimeMillis()
                    if (duration != C.TIME_UNSET) {
                        val playbackStartTime = currentTimeOfDay - (duration - currentPosition)
                        Text(
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                                Date(
                                    playbackStartTime
                                )
                            ),
                            style = TextStyle.Default.copy(
                                fontSize = 16.sp,
                                color = Color.White, // Set base text color to white
                                shadow = shadow
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    LiveIndicator()
                } else {
                    Text(
                        formatTime(currentPosition) + " / " + if (duration != C.TIME_UNSET) formatTime(duration) else "Loading...",
                        style = TextStyle.Default.copy(
                            fontSize = 16.sp,
                            color = Color.White, // Set base text color to white
                            shadow = shadow
                        )
                    )
                }
            }
        }

        Row(modifier = Modifier
            .dbgMode()
            .height(if (Player.isVisibleCardList) 200.dp else 0.dp)
            .onKeyEvent { keyEvent ->
                Player.isProgressBarVisible = true
                lastKeyPressedTime = System.currentTimeMillis()
                if (keyEvent.key == Key.DirectionUp) {
                    Player.isVisibleCardList = false
                    true
                } else {
                    false
                }
            }
        ) {
            SectionView(cardList = Player.cardList, sectionIndex = 0, isNotPlayer = false)
        }
    }

    LaunchedEffect(Player.isVisibleCardList) {
        if (!Player.isVisibleCardList) {
            focusRequesters.requestFocus()
        }
    }
}

@Composable
fun LiveIndicator() {
    // Define animation state for pulsing
    val pulseAnimation = rememberInfiniteTransition(label = "")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
    ) {
        // Pulsing Red Circle
        Box(modifier = Modifier.size(15.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(10.dp * pulseScale)  // Only the circle pulses in size
                    .background(Color.Red, shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Stable "LIVE" Text with shadow
        Text(
            text = "LIVE",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Red,
            style = TextStyle.Default.copy(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(0f, 0f),
                    blurRadius = 3f
                )
            )
        )
    }
}

var seekMultiplier = 1.0 // Initial multiplier for seek increment
val resetSeekTimeoutMs = 1000L // Time in milliseconds to reset seek increment after inactivity
var handler: Handler? = null
var resetRunnable: Runnable? = null

fun handleKeyEvent(keyEvent: KeyEvent, seekIncrementMs: Long = 5000L): Boolean {
    // Reset the multiplier if no key is pressed for a while
    resetSeekMultiplierIfNeeded()
    if (keyEvent.type == KeyEventType.KeyUp) {
        when (keyEvent.key) {
            Key.DirectionLeft -> {
                // Calculate the seek increment with the multiplier
                var seekAmount = seekIncrementMs * seekMultiplier
                // Limit the seek amount to a maximum of 10% of the video duration
                val maxSeekAmount = Player.player.duration / 10
                seekAmount = seekAmount.coerceAtMost(maxSeekAmount.toDouble())

                // Rewind by the calculated seekAmount
                Player.player.seekTo(
                    (Player.player.currentPosition - seekAmount).coerceAtLeast(0.0).toLong()
                )

                // Increase seek increment by 50% after each press
                seekMultiplier *= 1.5
                // Restart the reset timeout
                resetSeekMultiplierIfNeeded()
                return true
            }

            Key.DirectionRight -> {
                // Calculate the seek increment with the multiplier
                var seekAmount = seekIncrementMs * seekMultiplier
                // Limit the seek amount to a maximum of 10% of the video duration
                val maxSeekAmount = Player.player.duration / 10
                seekAmount = seekAmount.coerceAtMost(maxSeekAmount.toDouble())

                // Fast-forward by the calculated seekAmount
                Player.player.seekTo(
                    (Player.player.currentPosition + seekAmount).coerceAtMost(
                        Player.player.duration.toDouble()
                    ).toLong()
                )

                // Increase seek increment by 50% after each press
                seekMultiplier *= 1.5
                // Restart the reset timeout
                resetSeekMultiplierIfNeeded()
                return true
            }

            Key.DirectionDown -> {
                Player.isVisibleCardList = true
                return true
            }

            Key.DirectionUp -> {
                // Show card list
                Player.isVisibleCardList = false
                return true
            }

            Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                // Play/pause
                Player.player.playWhenReady = !Player.player.playWhenReady
                return true
            }
        }
    }
    return false
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