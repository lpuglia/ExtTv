import android.app.Activity
import android.view.KeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import com.android.exttv.model.CardView
import com.android.exttv.model.Section
import com.android.exttv.manager.PyManager
import com.android.exttv.util.GithubDialog
import com.android.exttv.util.RepositoryDialog
import com.android.exttv.util.cleanText
import com.android.exttv.util.parseText

@Composable
fun CatalogBrowser(
    context: Activity,
) {
    PyManager.init(context)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val items = PyManager.installedAddons.map { it to Icons.Default.Star } + listOf(
        "Add from Repository" to Icons.Default.Add,
        "Add from GitHub" to Icons.Default.Add,
        "Settings" to Icons.Default.Settings,
    )

    // Create a DrawerState instance to manage the drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val showGithubDialog = remember { mutableStateOf(false) }
    val showRepositoryDialog = remember { mutableStateOf(false) }
    val backgroundImageState = remember { mutableStateOf("") }
    val drawerWidth by animateDpAsState(
        targetValue = if (drawerState.currentValue == DrawerValue.Open) 280.dp else 60.dp,
        label = ""
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                Modifier
                    .background(Color(0xA30F2B31))
                    .width(drawerWidth) // Use animated width
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val (text, icon) = item
                    NavigationDrawerItem(
                        selected = selectedIndex == index,
                        onClick = {
                            if (!text.startsWith("Add from")) {
                                selectedIndex = index
                                if(text!="Settings"){
                                    drawerState.setValue(DrawerValue.Closed)
                                    PyManager.selectPlugin(text)
                                }
                            }

                            if(text=="Add from Repository"){
                                showRepositoryDialog.value = true
                            }
                            if(text=="Add from GitHub"){
                                showGithubDialog.value = true
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            containerColor = Color(0xCB1D2E31),
                            focusedContainerColor = Color(0xCB2B474D),
                            pressedContentColor = Color(0xCB426C75),
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selectedIndex == index) Color.White else Color.LightGray
                            )
                        }
                    ) {
                        Text(
                            text,
                            color = if (selectedIndex == index) Color.White else Color.LightGray
                            )
                    }
                }
            }
        }
    ) {
        Content(showGithubDialog, showRepositoryDialog, backgroundImageState, drawerState)
    }
    val view = LocalView.current
    // Move right when the first section is loaded
    LaunchedEffect(PyManager.isLoadingAddon) {
        if (!PyManager.isLoadingAddon) {
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        }
    }
    // Move down when the next section is loaded
    LaunchedEffect(PyManager.isLoadingSection) {
        if (!PyManager.isLoadingSection && PyManager.sectionList.isNotEmpty()) {
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
        }
    }

}

@Composable
fun Content(
    showGithubDialog: MutableState<Boolean>,
    showRepositoryDialog: MutableState<Boolean>,
    backgroundImageState: MutableState<String>,
    drawerState: DrawerState
) {

    if (showGithubDialog.value) {
        GithubDialog(showGithubDialog, drawerState);
    }

    if (showRepositoryDialog.value) {
        RepositoryDialog(showRepositoryDialog, drawerState);
    }

//    val listState = rememberTvLazyListState()
    // Scroll to the last item when the list is first composed
//    val sections = PyManager.sectionList
//    LaunchedEffect(sections) {
//        if (sections.isNotEmpty()) {
//            listState.scrollToItem(sections.size - 1)
//        }
//    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x88167c6c), Color(0x0004282d)),
                    center = Offset.Infinite,
                    radius = 1200f
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x88167c6c), Color(0x0004282d)),
                    center = Offset.Zero,
                    radius = 1200f
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x8801080a), Color(0x880b465f), Color(0xFF01080a)),
                    start = Offset.Zero,
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            ).padding(start=60.dp)
    ) {
        AsyncImage(
            model = backgroundImageState.value,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.3f),
            contentScale = ContentScale.Crop
        )
        TvLazyColumn(
//            state = listState,
        ) {
            itemsIndexed(PyManager.sectionList) { index, section ->
                Section(
                    section = section,
                    sectionIndex = index,
                    backgroundImageState = backgroundImageState
                )
            }
        }
        // Show a loading indicator if the data is still loading
        if (PyManager.isLoadingSection) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Section(
    section: Section,
    sectionIndex: Int,
    backgroundImageState: MutableState<String>,
) {
    val listState = rememberTvLazyListState()

    LaunchedEffect(section.movieList) {
        listState.scrollToItem(0)
    }

    Text(
        text = parseText(cleanText(section.title)),
        color = Color.White,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(start = 40.dp, top = 10.dp, bottom = 10.dp) // Add padding below the title
    )
    TvLazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp)
    ) {
        itemsIndexed(section.movieList) { cardIndex, card ->
            Card(
                card = card,
                isSelected = PyManager.manager.getSelectedIndexForSection(sectionIndex)==cardIndex,
                onClick = {
                    if(!PyManager.isLoadingSection) {
                        PyManager.setSection(card.id, sectionIndex, cardIndex)
                    }
                },
                backgroundImageState = backgroundImageState
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Card(
    card: CardView,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    backgroundImageState: MutableState<String>,
) {

    val bgModifier = if (isSelected) {
        Modifier.background(Color(0x44BB0000))
    } else {
        Modifier.background(Color(0x00000000))
    }

    val border = if (isSelected) {
        Border(border = BorderStroke(width = 2.dp, color = Color(0xFFBB0000)))
    } else {
        Border(border = BorderStroke(width = 0.dp, color = Color(0x00000000)))
    }

    Column(
        modifier = Modifier
            .width(200.dp)
    ){
        var isFocused by remember { mutableStateOf(false) }

        Card(
            modifier = modifier
                .padding(start = 10.dp)
                .height(120.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
                    backgroundImageState.value = card.fanartUrl
                },
            onClick = onClick,
            colors = CardDefaults.colors(containerColor = Color(0x00000000)),
            border = CardDefaults.border(
//                focusedBorder = Border(border = BorderStroke(width = 3.dp, color = Color(0xFFFF0000))),
//                pressedBorder = Border(border = BorderStroke(width = 10.dp, color = Color(0xFFFF0000))),
            )

        ) {
            Box() {
                AsyncImage(
                    model = card.thumbnailUrl,
                    contentDescription = card.label,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
//                        .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent overlay
                        .then(bgModifier)
                )
            }
        }
        Text(
            text = parseText(cleanText(card.label)),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(start=10.dp, top=20.dp, end=10.dp)
                               .width(200.dp)
                               .basicMarquee(iterations = if (isFocused) 100 else 0),
            overflow = TextOverflow.Ellipsis,
        )
        if(card.plot.isNotEmpty()){
            Text(
                text = parseText(cleanText(card.plot)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.alpha(if (isFocused) 1f else 0f)
                    .padding(start=10.dp, top=5.dp, end=10.dp)
                    .width(200.dp)
                    .basicMarquee(iterations = if (isFocused) 100 else 0),
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
    }

}
