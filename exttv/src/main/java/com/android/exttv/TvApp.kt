import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.exttv.MainActivity
import com.android.exttv.R
import com.android.exttv.manager.AddonManager
import com.android.exttv.manager.LoadingStatus
import com.android.exttv.manager.Section
import com.android.exttv.manager.SectionManager.CardView
import com.android.exttv.util.GithubDialog
import com.android.exttv.util.RepositoryDialog
import com.android.exttv.util.UninstallDialog
import com.android.exttv.util.UninstallSettingButtons
import com.android.exttv.util.addonKE
import com.android.exttv.util.cleanText
import com.android.exttv.util.nonAddonKE
import com.android.exttv.util.parseText
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.SectionManager as Sections
import com.android.exttv.manager.StatusManager as Status
import com.android.exttv.manager.PythonManager as Python

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogBrowser(
    context: MainActivity,
) {
    Addons.init(context)
    Python.init(context)

    val myIcon: ImageVector = ImageVector.vectorResource(id = R.drawable.icon_drawer)
    val addons = Addons.getAllAddons().map { it to myIcon }
    val extras = listOf(
        "Add from Repository" to Icons.Default.Add,
        "Add from GitHub" to Icons.Default.Add,
        "Settings" to Icons.Default.Settings,
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val drawerWidth by animateDpAsState(
        targetValue = if (drawerState.currentValue == DrawerValue.Open) 410.dp else 80.dp,
        label = ""
    )
    val focusRequesters by rememberUpdatedState(
        newValue = List(addons.size) { FocusRequester() }
    )
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LazyColumn(
                Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0F2B31), Color(0x000F2B31)),
                            start = Offset(0f, 0f),
                            end = Offset(700f, 0f)
                        )
                    )
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(addons+extras){ addonIndex, item ->
                    Row {
                        var modifier = Modifier.padding(0.dp)
                        var isSelected = false
                        if(addonIndex<addons.size){
                            UninstallSettingButtons(addonIndex, item)
                            modifier = modifier.focusRequester(focusRequesters[addonIndex])
                            modifier = modifier.onKeyEvent { event -> addonKE(event, addonIndex)}
                            isSelected = Addons.isSelected(addonIndex)
                        }else{
                            modifier = modifier.onKeyEvent { event -> nonAddonKE(event) }
                        }
                        val (text, icon) = item
                        NavigationDrawerItem(
                            selected = isSelected,
                            modifier = modifier,
                            onClick = {
                                if(text=="Add from Repository") Status.showRepositoryDialog = true
                                else if(text=="Add from GitHub") Status.showGithubDialog = true
                                else if (text!="Settings") Python.selectAddon(text)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                containerColor = Color(0xFF1D2E31),
                                focusedContainerColor = Color(0xFF2B474D),
                                pressedContentColor = Color(0xFF426C75),
                                selectedContainerColor = Color(0xFF426C75)
                            ),
                            leadingContent = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        ) {
                            Text(
                                text,
                                modifier = Modifier.basicMarquee(iterations = if (isSelected) 100 else 0),
                                color = if (isSelected) Color.White else Color.LightGray
                                )
                        }
                    }
                    if(addonIndex==addons.size-1){
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .width(250.dp)
                        )
                    }
                }
            }
        }
    ) {
        Content(context)
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
    if (Status.showGithubDialog) GithubDialog();
    if (Status.showRepositoryDialog) RepositoryDialog(context);
    if (Status.showContextMenu) UninstallDialog(Addons.focusedIndex);

    LaunchedEffect(drawerState.currentValue, AddonManager.selectedIndex) {
        if (drawerState.currentValue == DrawerValue.Open)
            if (AddonManager.selectedIndex>=0)
                focusRequesters[AddonManager.selectedIndex].requestFocus()
        if (Sections.isEmpty()) { // keep the drawer open when sections is empty
            drawerState.setValue(DrawerValue.Open)
            focusRequesters[0].requestFocus()
        }
    }

}

@Composable
fun Content(
    context: MainActivity
) {
    val placeholderDrawable = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.placeholder,
        context.theme
    )

    fun Modifier.fadingEdge(brush: Brush) = this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val topBottomFade = Brush.verticalGradient(0.7f to Color.Red, 1f to Color.Transparent)
        val leftRightFade = Brush.horizontalGradient(0f to Color.Transparent, 0.1f to Color.Red)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Status.bgImage)//.error(placeholderDrawable)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fadingEdge(topBottomFade)
                .fadingEdge(leftRightFade)
                .width(800.dp)
                .height(400.dp)
                .graphicsLayer(alpha = 0.3f)
                .align(Alignment.TopEnd),
            contentScale = ContentScale.Crop,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66167c6c), Color(0x0004282d)),
                    center = Offset.Infinite,
                    radius = 1200f
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66167c6c), Color(0x0004282d)),
                    center = Offset.Zero,
                    radius = 1200f
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x6601080a), Color(0x660b465f), Color(0xFF01080a)),
                    start = Offset.Zero,
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .padding(start = 80.dp)
    ) {
        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        )
        {
            itemsIndexed(Status.sectionList) { index, section ->
                Section(
                    section = section,
                    sectionIndex = index
                )
            }
        }
    }
}

@Composable
fun Section(
    section: Section,
    sectionIndex: Int
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
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp),
    ) {
        itemsIndexed(section.movieList) { cardIndex, card ->
            CardItem(
                card = card,
                isSelected = Sections.getSelectedSection(sectionIndex)==cardIndex,
                onClick = {
                    if(Status.loadingState == LoadingStatus.DONE){
                        Python.selectSection(card.id, sectionIndex, cardIndex)
                    }
                },
                requestFocus = cardIndex==0 && sectionIndex == Status.sectionList.size-1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardItem(
    card: CardView,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    requestFocus: Boolean
) {
    val bgModifier = if (isSelected) {
        Modifier.background(Color(0x44BB0000))
    } else {
        Modifier.background(Color(0x00000000))
    }
    val context = LocalContext.current

    val placeholderDrawable = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.placeholder,
        context.theme
    )
    Column(
        modifier = Modifier
//            .background(Color.Red)
            .width(200.dp)
    ){
        var isFocused by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(120.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
                    Status.bgImage = card.fanartUrl
                }
                .let { if (requestFocus) it.focusRequester(Status.focusRequester) else it },
            onClick = onClick,
            colors = CardDefaults.colors(containerColor = Color(0x00000000)),
        ) {
            Box() {
                AsyncImage(
                    model =  ImageRequest.Builder(LocalContext.current)
                        .data(card.thumbnailUrl)
//                        .placeholder(ColorPainter(Color.Gray)) // Set the placeholder here
                        .error(placeholderDrawable) // Optional: set an error placeholder
                        .build(),
                    contentDescription = card.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x88000000)),
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
            modifier = Modifier
                .padding(start = 10.dp, top = 20.dp, end = 10.dp)
                .width(200.dp)
                .basicMarquee(iterations = if (isFocused) 100 else 0),
            overflow = TextOverflow.Ellipsis,
        )
        if(card.plot.isNotEmpty()){
            Text(
                text = parseText(cleanText(card.plot)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .alpha(if (isFocused) 1f else 0f)
                    .padding(start = 10.dp, top = 5.dp, end = 10.dp)
                    .width(200.dp)
                    .basicMarquee(iterations = if (isFocused) 100 else 0),
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
    LaunchedEffect(Status.loadingState) {
        if (requestFocus && Status.sectionList.isNotEmpty() && Status.loadingState==LoadingStatus.SECTION_LOADED) {
            Status.loadingState = LoadingStatus.DONE
            Status.focusRequester.requestFocus()
        }
    }
}
