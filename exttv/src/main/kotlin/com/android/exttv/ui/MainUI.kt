import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.exttv.R
import com.android.exttv.model.manager.LoadingStatus
import com.android.exttv.ui.GithubDialog
import com.android.exttv.ui.RepositoryDialog
import com.android.exttv.ui.UninstallDialog
import com.android.exttv.ui.ContextButtons
import com.android.exttv.ui.FavouriteButtons
import com.android.exttv.ui.FavouriteMenu
import com.android.exttv.ui.NewPlaylistMenu
import com.android.exttv.ui.RemoveDialog
import com.android.exttv.ui.SectionView
import com.android.exttv.ui.SettingsWindow
import com.android.exttv.ui.UpdateDialog
import com.android.exttv.ui.addonKE
import com.android.exttv.ui.nonAddonKE
import com.android.exttv.util.cleanText
import com.android.exttv.util.parseText
import com.android.exttv.util.updateSection
import com.android.exttv.model.manager.AddonManager as Addons
import com.android.exttv.model.manager.FavouriteManager as Favourites
import com.android.exttv.model.manager.SectionManager as Sections
import com.android.exttv.model.manager.StatusManager as Status
import com.android.exttv.model.manager.PythonManager as Python

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogBrowser() {
    updateSection()
    val drawerState = rememberDrawerState(initialValue = if(Sections.isEmpty) DrawerValue.Open else DrawerValue.Closed)
    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        modifier = Modifier.onKeyEvent { event ->
            if(event.key == Key.Back) {
                if(drawerState.currentValue == DrawerValue.Open){
                    false
                }else {
                    drawerState.setValue(DrawerValue.Open)
                    true
                }
            }else
               false
        },
        drawerState = drawerState,
        drawerContent = {
            LazyColumn(
                Modifier
                    .background(Color(0xFF222222))
                    .width(if (drawerState.currentValue == DrawerValue.Open) 280.dp else 0.dp)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                state = listState,
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(Status.drawerItems) { drawerIndex, drawerItem ->
                    val icon =
                    if (drawerIndex < Addons.size) ImageVector.vectorResource(id = R.drawable.icon_drawer)
                    else if (drawerIndex < Addons.size + Favourites.size) Icons.Default.Star
                    else Icons.Default.Add

                    if (drawerIndex == 0 || drawerIndex == Addons.size || drawerIndex == Addons.size + Favourites.size) {
                        Row(modifier = Modifier.width(255.dp)) {
                            @Composable
                            fun HeaderText(textProvider: () -> String) {
                                Text(
                                    text = textProvider(),
                                    maxLines = 1,
                                    color = Color.White,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 5.dp)
                                )
                            }
                            if (drawerIndex == 0 && Addons.size > 0)  HeaderText { "Addons" }
                            if (drawerIndex == Addons.size && Favourites.size > 0) HeaderText { "Favourites" }
                            if (drawerIndex == Addons.size + Favourites.size) HeaderText { "Menu" }
                            Divider(
                                color = Color.Gray,
                                thickness = 1.dp,
                                modifier = Modifier
                                    .padding(top = 15.dp, bottom = 20.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                    val focusRequester = FocusRequester()
                    Box {
                        var modifier = Modifier.padding(0.dp)
                        var isSelected = false
                        modifier = modifier.focusRequester(focusRequester)
                        if (drawerIndex < Addons.size) {
                            ContextButtons(drawerIndex)
                            modifier = modifier.onKeyEvent { event -> addonKE(event, drawerIndex) }
                            isSelected = Status.selectedAddonIndex == drawerIndex
                        } else if (drawerIndex < Addons.size + Favourites.size) {
                            FavouriteButtons(drawerIndex - Addons.size)
                            modifier = modifier.onKeyEvent { event -> addonKE(event, drawerIndex) }
                            isSelected = Status.selectedAddonIndex == drawerIndex
                        } else {
                            modifier = modifier.onKeyEvent { event -> nonAddonKE(event) }
                        }
                        NavigationDrawerItem(
                            selected = isSelected,
                            modifier = modifier.onFocusChanged {
                                if(it.hasFocus) Status.focusedAddonIndex = drawerIndex
                            },
                            onClick = {
                                if (drawerItem == "Add from Repository") Status.showRepositoryDialog =
                                    true
                                else if (drawerItem == "Add from GitHub") Status.showGithubDialog =
                                    true
                                else if (drawerIndex < Addons.size) Python.selectAddon(drawerItem)
                                else Python.selectFavourite(drawerItem)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF111111)
                            ),
                            leadingContent = {
                                if (drawerIndex < Addons.size) {
                                    Image(
                                        painter = rememberAsyncImagePainter(Status.appContext.filesDir.path + "/exttv_home/addons/${Addons.getIdByName(drawerItem)}/${Addons.getIconByName(drawerItem)}"),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop // Adjust content scale as needed
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color.LightGray
                                    )
                                }
                            }
                        ) {
                            Text(
                                drawerItem,
                                modifier = Modifier.basicMarquee(iterations = if (isSelected) 100 else 0),
                                color = if (isSelected) Color.White else Color.LightGray
                            )
                        }
                    }

                    LaunchedEffect(Status.refocus) {
                        if(Status.focusedAddonIndex==drawerIndex){
                            focusRequester.requestFocus()
                            Status.refocus = false
                        }
                    }

                    LaunchedEffect(drawerState.currentValue, Status.selectedAddonIndex) {
                        if (Status.selectedAddonIndex == drawerIndex) {
                            if (drawerState.currentValue == DrawerValue.Open){
                                listState.scrollToItem(Status.selectedAddonIndex)
                                focusRequester.requestFocus()
                            }
                            if (Sections.isEmpty && Status.loadingState == LoadingStatus.DONE && drawerIndex == 0) { // keep the drawer open when sections is empty and Done
                                drawerState.setValue(DrawerValue.Open)
                                focusRequester.requestFocus()
                            }
                        }
                    }
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxHeight()){
//            Box(
//                modifier = Modifier
//                    .width(if (drawerState.currentValue == DrawerValue.Open) 280.dp else 0.dp)
//                    .fillMaxHeight()
//                    .background(Color.Black)
//            ){
//                Text(text = "Drawer Content", color = Color.White)
//            }
            Content()
        }
    }
    if (Status.loadingState != LoadingStatus.DONE) {
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
    GithubDialog(); RepositoryDialog(); UninstallDialog();
    RemoveDialog(); UpdateDialog(); FavouriteMenu();
    NewPlaylistMenu(); SettingsWindow();

}

@Composable
fun Content() {
    val placeholderDrawable = ResourcesCompat.getDrawable(
        Status.appContext.resources,
        R.drawable.placeholder,
        Status.appContext.theme
    )

    val listState = rememberLazyListState()

    fun Modifier.fadingEdge(brush: Brush) = this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }
    Box{
        Box(
            modifier = Modifier
                .background(Color.Black)
                .zIndex(0f)
        ) {
            val topBottomFade =
                Brush.verticalGradient(0.0f to Color.Red, 0.9f to Color.Transparent)
            val leftRightFade =
                Brush.horizontalGradient(0f to Color.Transparent, 0.1f to Color.Red)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Status.bgImage)
//                .placeholder(placeholderDrawable) // Set the placeholder here
                    .error(placeholderDrawable)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fadingEdge(topBottomFade)
//                            .fadingEdge(leftRightFade)
                    .fillMaxHeight()
                    .fillMaxWidth()
//                            .height(100.dp)
                    .graphicsLayer(alpha = 0.3f)
                    .align(Alignment.TopEnd),
                contentScale = ContentScale.Crop,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().zIndex(1f),
            verticalArrangement = Arrangement.Bottom,
//            horizontalAlignment = Alignment.End,
//            contentPadding = PaddingValues(start = 40.dp, end = 40.dp),
        )
        {
            itemsIndexed(Sections.getSectionsInOrder()) { index, section ->
                Text(
                    text = parseText(cleanText(section.title)),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        start = 40.dp,
                        top = 10.dp,
                        bottom = 10.dp
                    ) // Add padding below the title
                )
                SectionView(
                    section.cardList,
                    sectionIndex = index,
                    sectionsListState = listState,
                )
            }
        }
    }
}