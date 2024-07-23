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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

fun parseText(input: String): AnnotatedString {
    val annotatedString = AnnotatedString.Builder()
    val regex = "\\[(B|I|LIGHT|UPPERCASE|LOWERCASE|CAPITALIZE)](.*?)\\[/\\1]".toRegex()

    var currentIndex = 0
    for (match in regex.findAll(input)) {
        val (tag, content) = match.destructured
        val startIndex = match.range.first

        if (currentIndex < startIndex) {
            annotatedString.append(AnnotatedString(input.substring(currentIndex, startIndex)))
        }

        when (tag) {
            "B" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    annotatedString.append(parseText(content))
                }
            }
            "I" -> {
                annotatedString.withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    annotatedString.append(parseText(content))
                }
            }
            "LIGHT" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                    annotatedString.append(parseText(content))
                }
            }
            "UPPERCASE" -> {
                annotatedString.append(AnnotatedString(content.uppercase()))
            }
            "LOWERCASE" -> {
                annotatedString.append(AnnotatedString(content.lowercase()))
            }
            "CAPITALIZE" -> {
                annotatedString.append(AnnotatedString(content.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }))
            }
        }

        currentIndex = match.range.last + 1
    }

    if (currentIndex < input.length) {
        annotatedString.append(AnnotatedString(input.substring(currentIndex)))
    }

    return annotatedString.toAnnotatedString()
}

fun cleanText(input: String): String {
    // Remove color tags

    // Remove carriage returns
    var cleanedText = input.replace(Regex("\\[CR\\]"), "")

    // Remove tabulators
    cleanedText = cleanedText.replace(Regex("\\[TABS](\\d+)\\[/TABS]")) { match ->
        val tabs = match.groupValues[1].toIntOrNull() ?: 0
        "\t".repeat(tabs)
    }
    cleanedText = cleanedText.replace(Regex("\\[COLOR\\s+[^\\]]*](.*?)\\[/COLOR]"), "$1")

    return cleanedText
}

data class CardView(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val backgroundImageUrl: String,
    val description: String,
)

data class Section(
    val title: String,
    val movieList: List<CardView>,
)

class SectionManager() {
    private val sections = LinkedHashMap<String, Section>()
    private val selectedIndices: MutableList<Int?> = mutableListOf()

    fun removeAndAdd(index: Int, key: String, newSection: Section) {
        // Convert the map keys to a list to easily access by index
        val keys = sections.keys.toList()

        // Compare newSection with the section at index-1 if index is greater than 0
        if (index > 0 && sections[keys[index - 1]]?.movieList == newSection.movieList) {
            return // Ignore the addition if the newSection is equal to the last added section
        }

        // Ensure the index is within the bounds
        if (index in keys.indices) {

            // Remove all entries after the given index
            for (i in keys.size - 1 downTo index + 1) {
                sections.remove(keys[i])
                selectedIndices.removeAt(i)
            }

            // Replace the entry at the given index or add new if index is out of current bounds
            if (index < keys.size) {
                val keyAtIndex = keys[index]
                sections[keyAtIndex] = newSection
                selectedIndices[index] = null // Reset selected index for the new section
            } else {
                // If the index is out of bounds (greater than current size), add the new section
                sections[key] = newSection
                selectedIndices.add(null)
            }
        } else {
            // If index is out of bounds, just add the new section
            sections[key] = newSection
            selectedIndices.add(null)
        }
    }

    fun getSectionsInOrder(): List<Section> {
        return sections.values.toList()
    }

    fun getLastSectionKey(): String? {
        return sections.keys.lastOrNull()
    }

    fun updateSelectedIndex(sectionIndex: Int, selectedIndex: Int?) {
        if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex] = selectedIndex
        }
    }

    fun getSelectedIndexForSection(sectionIndex: Int): Int? {
        return if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex]
        } else {
            null
        }
    }
}

object PythonInitializer {
    private var kodi: PyObject? = null
    lateinit var sectionList: MutableState<List<Section>>
    lateinit var isLoading: MutableState<Boolean>
    var manager : SectionManager = SectionManager()
    private val titleMap: MutableMap<String, String> = mutableMapOf(Pair("plugin://plugin.video.kod/","Menu"))

    fun init(context: Activity, sectionList: MutableState<List<Section>>, isLoading: MutableState<Boolean>): PyObject? {
        this.sectionList = sectionList
        this.isLoading = isLoading
        if (kodi != null) {
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
//        SetSection("plugin://plugin.video.kod/?ewogICAgImFjdGlvbiI6ICJsaXZlIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAidGh1bWJuYWlsIjogImh0dHBzOi8vcmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS9rb2Rpb25kZW1hbmQvbWVkaWEvbWFzdGVyL3RoZW1lcy9kZWZhdWx0L3RodW1iX29uX3RoZV9haXIucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1EaXJldHRlWy9CXSIsCiAgICAidXJsIjogImh0dHBzOi8vd3d3LmxhNy5pdCIKfQ%3D%3D")
//        SetSection("?ewogICAgImFjdGlvbiI6ICJsaXZlIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAidGh1bWJuYWlsIjogImh0dHBzOi8vcmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS9rb2Rpb25kZW1hbmQvbWVkaWEvbWFzdGVyL3RoZW1lcy9kZWZhdWx0L3RodW1iX29uX3RoZV9haXIucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1EaXJldHRlWy9CXSIsCiAgICAidXJsIjogImh0dHBzOi8vd3d3LmxhNy5pdCIKfQ%3D%3D", sectionList)
//        SetSection("?ewogICAgImFjdGlvbiI6ICJmaW5kdmlkZW9zIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImZvcmNldGh1bWIiOiB0cnVlLAogICAgImZ1bGx0aXRsZSI6ICJMYTciLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAibm9fcmV0dXJuIjogdHJ1ZSwKICAgICJ0aHVtYm5haWwiOiAiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2tvZGlvbmRlbWFuZC9tZWRpYS9tYXN0ZXIvbGl2ZS9sYTcucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1MYTdbL0JdIiwKICAgICJ1cmwiOiAiaHR0cHM6Ly93d3cubGE3Lml0L2RpcmV0dGUtdHYiCn0%3D", sectionList)
//        SetSection("?ewogICAgImFjdGlvbiI6ICJub3ZlZGFkZXMiLAogICAgImNhdGVnb3J5IjogIk5vdml0XHUwMGUwIGluIEZpbG0iLAogICAgImNoYW5uZWwiOiAibmV3cyIsCiAgICAiY29udGV4dCI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJhY3Rpb24iOiAic2V0dGluZ19jaGFubmVsIiwKICAgICAgICAgICAgImNoYW5uZWwiOiAibmV3cyIsCiAgICAgICAgICAgICJleHRyYSI6ICJwZWxpY3VsYXMiLAogICAgICAgICAgICAidGl0bGUiOiAiQ2FuYWxpIGluY2x1c2kgaW46IEZpbG0iCiAgICAgICAgfQogICAgXSwKICAgICJleHRyYSI6ICJwZWxpY3VsYXMiLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAidGh1bWJuYWlsIjogImh0dHBzOi8vcmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS9rb2Rpb25kZW1hbmQvbWVkaWEvbWFzdGVyL3RoZW1lcy9kZWZhdWx0L3RodW1iX21vdmllLnBuZyIsCiAgICAidGl0bGUiOiAiRmlsbSIKfQ%3D%3D", sectionList)

        return kodi
    }

    fun SetSection(argv2: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        isLoading.value = true // Start loading indicator
        val runnable = Runnable {
            val title : String = titleMap.getOrDefault(argv2, "")
            val newSection = Section(title, kodi?.callAttr("run", argv2)?.toJava(List::class.java) as List<CardView>)
            titleMap.putAll(newSection.movieList.associate { it.id to it.title })

            val lastKey = manager.getLastSectionKey()
            manager.removeAndAdd(sectionIndex+1, argv2, newSection)
            if(sectionIndex>=0) {
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
    TvLazyColumn(
            state= listState,
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
            itemsIndexed(PythonInitializer.sectionList.value) { index, section ->
                Section(
                    section = section,
                    sectionIndex = index
                )
            }
        }
        LoadingWheel()
//    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Section(
    section: Section,
    sectionIndex: Int,
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
        modifier = Modifier.padding(start = 40.dp, top = 10.dp, bottom = 20.dp) // Add padding below the title
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
                movie = card,
                isSelected = PythonInitializer.manager.getSelectedIndexForSection(sectionIndex)==cardIndex,
                onClick = {
                    if(!PythonInitializer.isLoading.value) {
                        PythonInitializer.SetSection(card.id, sectionIndex, cardIndex)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Card(
    movie: CardView,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
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
                .padding(start = 5.dp)
                .height(120.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
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
                    model = movie.thumbnailUrl,
                    contentDescription = movie.title,
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
            text = parseText(cleanText(movie.title)),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(8.dp).width(200.dp).basicMarquee(iterations = if (isFocused) 100 else 0),
            overflow = TextOverflow.Ellipsis
        )
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