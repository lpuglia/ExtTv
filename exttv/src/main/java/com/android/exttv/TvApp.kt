import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

data class Movie(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val backgroundImageUrl: String,
    val description: String,
)

data class Section(
    val title: String,
    val movieList: List<Movie>,
)

object PythonInitializer {
    private var kodi: PyObject? = null

    fun init(context: Activity, sectionList: MutableState<List<Section>>): PyObject? {
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

        kodi?.let {
            SetSection(it, "", sectionList)
//            SetSection(it, "?ewogICAgImFjdGlvbiI6ICJsaXZlIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAidGh1bWJuYWlsIjogImh0dHBzOi8vcmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS9rb2Rpb25kZW1hbmQvbWVkaWEvbWFzdGVyL3RoZW1lcy9kZWZhdWx0L3RodW1iX29uX3RoZV9haXIucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1EaXJldHRlWy9CXSIsCiAgICAidXJsIjogImh0dHBzOi8vd3d3LmxhNy5pdCIKfQ%3D%3D", sectionList)
//            SetSection(it, "?ewogICAgImFjdGlvbiI6ICJmaW5kdmlkZW9zIiwKICAgICJhcmdzIjogIiIsCiAgICAiY2hhbm5lbCI6ICJsYTciLAogICAgImV4dHJhIjogIm1vdmllIiwKICAgICJmb2xkZXIiOiB0cnVlLAogICAgImZvcmNldGh1bWIiOiB0cnVlLAogICAgImZ1bGx0aXRsZSI6ICJMYTciLAogICAgImdsb2JhbHNlYXJjaCI6IGZhbHNlLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAibm9fcmV0dXJuIjogdHJ1ZSwKICAgICJ0aHVtYm5haWwiOiAiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2tvZGlvbmRlbWFuZC9tZWRpYS9tYXN0ZXIvbGl2ZS9sYTcucG5nIiwKICAgICJ0aXRsZSI6ICJbQl1MYTdbL0JdIiwKICAgICJ1cmwiOiAiaHR0cHM6Ly93d3cubGE3Lml0L2RpcmV0dGUtdHYiCn0%3D", sectionList)
//            SetSection(it, "?ewogICAgImFjdGlvbiI6ICJub3ZlZGFkZXMiLAogICAgImNhdGVnb3J5IjogIk5vdml0XHUwMGUwIGluIEZpbG0iLAogICAgImNoYW5uZWwiOiAibmV3cyIsCiAgICAiY29udGV4dCI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJhY3Rpb24iOiAic2V0dGluZ19jaGFubmVsIiwKICAgICAgICAgICAgImNoYW5uZWwiOiAibmV3cyIsCiAgICAgICAgICAgICJleHRyYSI6ICJwZWxpY3VsYXMiLAogICAgICAgICAgICAidGl0bGUiOiAiQ2FuYWxpIGluY2x1c2kgaW46IEZpbG0iCiAgICAgICAgfQogICAgXSwKICAgICJleHRyYSI6ICJwZWxpY3VsYXMiLAogICAgImluZm9MYWJlbHMiOiB7CiAgICAgICAgIm1lZGlhdHlwZSI6ICJtb3ZpZSIKICAgIH0sCiAgICAiaXRlbWxpc3RQb3NpdGlvbiI6IDAsCiAgICAidGh1bWJuYWlsIjogImh0dHBzOi8vcmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbS9rb2Rpb25kZW1hbmQvbWVkaWEvbWFzdGVyL3RoZW1lcy9kZWZhdWx0L3RodW1iX21vdmllLnBuZyIsCiAgICAidGl0bGUiOiAiRmlsbSIKfQ%3D%3D", sectionList)
        }

        return kodi
    }
}

fun SetSection(kodi: PyObject, argv2: String, sectionList: MutableState<List<Section>>){
    val runnable = Runnable {
        Log.d("Python", argv2)
        var newItems : List<Movie> = kodi.callAttr("run", argv2).toJava(List::class.java) as List<Movie>
        Log.d("Python", newItems.toString())

        try {
            Handler(Looper.getMainLooper()).post {
                val currentList = sectionList.value.toMutableList()
                currentList[0] = Section("asd", newItems)
                sectionList.value = currentList
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    Thread(runnable).start()
}

@Composable
fun CatalogBrowser(
    context: Activity,
    onItemSelected: (Movie) -> Unit = {},
) {

    var sectionList = remember {mutableStateOf(listOf(Section(title = "", movieList = listOf<Movie>())))}
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isLoading = remember{mutableStateOf(false)}
    val items = listOf(
        "Home" to Icons.Default.Home,
        "Favourites" to Icons.Default.Favorite,
        "Settings" to Icons.Default.Settings,
    )
    val kodi = PythonInitializer.init(context, sectionList)
    val closeDrawerWidth = 80.dp
    val backgroundContentPadding = 10.dp
    ModalNavigationDrawer(
        drawerContent = {
            Column(
                Modifier
                    .background(Color.Gray)
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
                        onClick = { selectedIndex = index },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    ) {
                        Text(text)
                    }
                }
            }
        }
    ) {
        TvLazyColumn(
            modifier = Modifier
                .padding(start = closeDrawerWidth + backgroundContentPadding)
                .fillMaxWidth(),
        ) {

            items(sectionList.value) { section ->
                if (kodi != null) {
                    Section(
                        section = section,
                        isLoading = isLoading,
                        kodi = kodi,
                        sectionList = sectionList,
                        onItemSelected = onItemSelected
                    )
                }
            }
        }
        LoadingWheel(context, isLoading)
    }
}


@Composable
fun Section(
    section: Section,
    isLoading: MutableState<Boolean>,
    kodi: PyObject,
    sectionList: MutableState<List<Section>>,
    onItemSelected: (Movie) -> Unit = {},
) {
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp) // Add padding below the title
        )
        TvLazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Add start padding
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(section.movieList) { movie ->
                val isSelected = selectedMovie == movie
                MovieCard(
                    movie = movie,
                    isSelected = isSelected,
                    onClick = {
                        if(!isLoading.value) {
                            isLoading.value = true // Start loading indicator

                            // Define your Runnable to execute on a background thread
                            val runnable = Runnable {
                                try {
                                    // Update UI on the main thread
                                    Handler(Looper.getMainLooper()).post {
                                        selectedMovie = if (isSelected) null else movie
                                        Log.d("Python", movie.id)
                                        SetSection(kodi, movie.id.split("?")[1], sectionList)

                                        onItemSelected(movie)
                                        isLoading.value = false // Stop loading indicator
                                    }
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                            Thread(runnable).start()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val expandedModifier = if (isSelected) {
        Modifier
            .width(210.dp)
            .height(210.dp)
    } else {
        Modifier
            .width(200.dp)
            .height(210.dp)
    }
    val bgModifier = if (isSelected) {
        Modifier.background(Color.Red)
    } else {
        Modifier.background(Color.Blue)
    }

    Card(
        modifier = modifier
            .then(expandedModifier)
            .padding(start = 10.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .then(bgModifier)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp) // Add some padding for better presentation
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = movie.thumbnailUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text(
                text = movie.description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp) // Add some padding for better presentation
            )
        }
    }
}


@Composable
fun LoadingWheel(context: Activity, visible: MutableState<Boolean>) {
    if (visible.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}