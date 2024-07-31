import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import com.android.exttv.model.CardView
import com.android.exttv.model.Section
import com.android.exttv.model.SectionManager
import com.android.exttv.util.cleanText
import com.android.exttv.util.parseText
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

object PythonInitializer {
    private var kodi: PyObject? = null
    lateinit var sectionList: MutableState<List<Section>>
    lateinit var isLoading: MutableState<Boolean>
    var manager : SectionManager = SectionManager()
    private val titleMap: MutableMap<String, String> = mutableMapOf(Pair("plugin://plugin.video.kod/","Menu"))

    fun init(context: Activity, sectionList: MutableState<List<Section>>, isLoading: MutableState<Boolean>): PyObject? {
        this.sectionList = sectionList
        this.isLoading = isLoading
        if (kodi != null && sectionList.value.isNotEmpty()) {
            return kodi
        }

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val runnable = Runnable {
            val py = Python.getInstance()
            py.getModule("xbmcgui")
            kodi = py.getModule("kodi")
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()

        SetSection("plugin://plugin.video.kod/")

        return kodi
    }

    fun SetSection(argv2: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        isLoading.value = true
        val runnable = Runnable {
            val title : String = titleMap.getOrDefault(argv2, "")
            val newSection = Section(title, kodi?.callAttr("run", argv2)?.toJava(List::class.java) as List<CardView>)
            titleMap.putAll(newSection.movieList.associate { it.id to it.label })

            val lastKey = manager.getLastSectionKey()
            if(manager.removeAndAdd(sectionIndex+1, argv2, newSection)) {
                manager.updateSelectedIndex(sectionIndex, cardIndex)
            }
            try {
                Handler(Looper.getMainLooper()).post {
                    Log.d("Python", newSection.toString())
                    sectionList.value = manager.getSectionsInOrder()
                    isLoading.value = false // Stop loading indicator
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
    }
}

@Composable
fun CatalogBrowser(
    context: Activity,
) {
    PythonInitializer.init(
        context = context,
        sectionList = remember { mutableStateOf(listOf()) },
        isLoading = remember { mutableStateOf(false) },
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    val items = listOf(
        "Home" to Icons.Default.Home,
        "Favourites" to Icons.Default.Favorite,
        "Settings" to Icons.Default.Settings,
    )

    // Create a DrawerState instance to manage the drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var backgroundImageState = remember { mutableStateOf("") }
    // Animate the width of the drawer
    val drawerWidth by animateDpAsState(
        targetValue = if (drawerState.currentValue == DrawerValue.Open) 255.dp else 0.dp
    )

//    ModalNavigationDrawer(
//        drawerState = drawerState,
//        drawerContent = {
//            Column(
//                Modifier
//                    .background(Color.Gray)
//                    .width(drawerWidth) // Use animated width
//                    .fillMaxHeight()
//                    .padding(12.dp)
//                    .selectableGroup(),
//                horizontalAlignment = Alignment.Start,
//                verticalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                items.forEachIndexed { index, item ->
//                    val (text, icon) = item
//                    NavigationDrawerItem(
//                        selected = selectedIndex == index,
//                        onClick = {
//                            selectedIndex = index
//                        },
//                        leadingContent = {
//                            Icon(
//                                imageVector = icon,
//                                contentDescription = null,
//                            )
//                        }
//                    ) {
//                        Text(text)
//                    }
//                }
//            }
//        }
//    ) {
    val listState = rememberTvLazyListState()
    // Scroll to the last item when the list is first composed
    val sections = PythonInitializer.sectionList.value
    LaunchedEffect(sections) {
        if (sections.isNotEmpty()) {
            listState.scrollToItem(sections.size - 1)
        }
    }
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
            )
    ) {
        AsyncImage(
            model = backgroundImageState.value,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.3f)
        )
        TvLazyColumn(
            state = listState,
        ) {
            itemsIndexed(PythonInitializer.sectionList.value) { index, section ->
                Section(
                    section = section,
                    sectionIndex = index,
                    backgroundImageState = backgroundImageState
                )
            }
        }
        LoadingWheel()
    }
//    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Section(
    section: Section,
    sectionIndex: Int,
    backgroundImageState: MutableState<String>,
) {
    val listState = rememberTvLazyListState()
//    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(section.movieList) {
        listState.scrollToItem(0)
//        focusRequester.requestFocus()
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
//                modifier = if (cardIndex==0 && sectionIndex==PythonInitializer.sectionList.value.size - 1) {Modifier.focusRequester(focusRequester)} else {Modifier},
                card = card,
                isSelected = PythonInitializer.manager.getSelectedIndexForSection(sectionIndex)==cardIndex,
                onClick = {
                    if(!PythonInitializer.isLoading.value) {
                        PythonInitializer.SetSection(card.id, sectionIndex, cardIndex)
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


@Composable
fun LoadingWheel() {
    if (PythonInitializer.isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}