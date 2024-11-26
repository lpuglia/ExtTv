package com.android.exttv.ui

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.exttv.R
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.manager.LoadingStatus
import com.android.exttv.model.manager.PlayerManager
import com.android.exttv.model.manager.PythonManager
import com.android.exttv.model.manager.SectionManager as Sections
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.util.cleanText
import com.android.exttv.util.parseText


@Composable
fun SectionView(
    cardList: List<CardItem>,
    sectionIndex: Int,
    sectionsListState: LazyListState,
    isNotPlayer: Boolean = true
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 40.dp, end = 40.dp),
    ) {
        itemsIndexed(cardList) { cardIndex, card ->
            CardView(
                card = card,
                sectionIndex,
                cardIndex,
                sectionsListState,
                listState,
                isNotPlayer
            )
        }
    }

    if(isNotPlayer) {
        // this scroll to the next new line which may be lay below the current view
        // it also rescroll to 0 a section that is selected after the previous section was not scrolled back
        LaunchedEffect(Sections.focusedIndex, Sections.focusedCardIndex, Sections.refocus) {
            sectionsListState.scrollToItem(Sections.focusedIndex)
            if(Sections.focusedCardIndex == 0 && sectionIndex == Sections.focusedIndex) {
                listState.scrollToItem(0)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardView(
    card: CardItem,
    sectionIndex: Int,
    cardIndex: Int,
    sectionsListState: LazyListState,
    cardListState: LazyListState,
    isNotPlayer: Boolean = true,
) {
    val focusRequester by remember { mutableStateOf(FocusRequester()) }
    // modify the background color based on the selected card
    val bgModifier = if (Sections.getSelectedSection(sectionIndex) == cardIndex && isNotPlayer) {
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
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.height(200.dp)) {
        Card(
            modifier = Modifier
                .padding(start = 10.dp)
                .width(200.dp)
                .onKeyEvent { event ->
                    cardIndex == 0 && event.key == Key.DirectionLeft
                }
                .onFocusChanged {
                    if (it.isFocused) {
                        if (isNotPlayer) {
                            Sections.focusedIndex = sectionIndex
                            Sections.focusedCardIndex = cardIndex
                            StatusManager.bgImage = card.secondaryArt
                        } else {
                            Sections.focusedCardPlayerIndex = cardIndex
                        }
                    }
                    isFocused = it.isFocused
                }
                .focusRequester(focusRequester),
            onClick = {
                if (isNotPlayer) {
                    if (StatusManager.loadingState == LoadingStatus.DONE) {
                        PythonManager.selectSection(card, sectionIndex, cardIndex)
                    }
                } else {
                    if (!PlayerManager.isLoading) {
                        PlayerManager.isLoading = true
                        PythonManager.selectSection(card, sectionIndex, cardIndex)
                    }
                }
            },
            onLongClick = {
                StatusManager.showFavouriteMenu = true
                StatusManager.reboundEnter = true
            },
            colors = CardDefaults.colors(
                containerColor = Color(0xFF222222)
            ),
        ) {
            Column(modifier = Modifier.height(170.dp)) {
                val img = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (isNotPlayer) {
                            card.primaryArt
//                                if (!card.isFolder && Sections.focusedIndex == sectionIndex && Sections.focusedCardIndex == cardIndex)
//                                    card.secondaryArt else card.primaryArt
                        } else
                            if (!card.isFolder && Sections.focusedCardPlayerIndex == cardIndex)
                                card.secondaryArt else card.primaryArt
                    ).error(placeholderDrawable) // Optional: set an error placeholder
                    .build()

//                AsyncImage(
//                    model = img,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(120.dp)
//                        .background(Color(0x88000000)),
//                    contentScale = ContentScale.Crop
//                )
                Box( modifier = Modifier
                    .height(120.dp)
                    .clip(RectangleShape)
                ){
                    // Blurred cropped background
                    AsyncImage(
                        model = img,
                        contentDescription = null,
                        modifier = Modifier.zIndex(0f)
                            .fillMaxWidth()
                            .height(120.dp)
                            .scale(4f)
                            .alpha(0.3f), // Apply transparency
                        contentScale = ContentScale.Crop // Crop to fill the box
                    )

                    // Foreground image with proper aspect ratio
                    AsyncImage(
                        model = img,
                        contentDescription = null,
                        modifier = Modifier.zIndex(1f)
                            .fillMaxWidth().height(120.dp),
                        contentScale = ContentScale.Fit // Maintain aspect ratio
                    )

                    Box(modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .alpha(if (Sections.getSelectedSection(sectionIndex) == cardIndex && isNotPlayer) 1f else 0f)
                        .background(Color(0xAAAA0000)
                        )
                    )
                }
                Box (
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                        if(isFocused)
                            Color(0xCCFFFFFF)
                        else
                            Color(0xFF222222)
                    )
                ){
                    Text(
                        // favouriteLabel is only set for favourite cards
                        text = parseText(cleanText(if (card.favouriteLabel != "") card.favouriteLabel else card.label,)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if(isFocused) Color.Black else Color.White,
                        modifier = Modifier
                            .padding(start = 10.dp, top = 5.dp, end = 10.dp)
                            .width(200.dp)
                            .basicMarquee(iterations = if (isFocused) 100 else 0),
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (card.plot.isNotEmpty()) {
                        Text(
                            text = parseText(cleanText(card.plot)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if(isFocused) Color.Black else Color.White,
                            modifier = Modifier
                                //                    .alpha(if (isFocused) 1f else 0f)
                                .padding(start = 10.dp, top = 25.dp, end = 10.dp)
                                .width(200.dp)
                                .basicMarquee(iterations = if (isFocused) 100 else 0),
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    if(isNotPlayer) {
        LaunchedEffect(Sections.focusedCardIndex, Sections.focusedIndex, Sections.refocus) {
            if (Sections.focusedIndex == sectionIndex && Sections.focusedCardIndex == cardIndex) {
                sectionsListState.scrollToItem(sectionIndex)
                cardListState.scrollToItem(maxOf(0, cardIndex-2))
                focusRequester.requestFocus()
                Sections.refocus = false
            }
        }
    }else{
        LaunchedEffect(Sections.focusedCardPlayerIndex, Sections.refocus) {
            if (PlayerManager.isVisibleCardList && Sections.focusedCardPlayerIndex == cardIndex) {
                focusRequester.requestFocus()
                cardListState.scrollToItem(cardIndex)
                Sections.refocus = false
            }else{
                focusRequester.freeFocus()
            }
        }

    }
}
