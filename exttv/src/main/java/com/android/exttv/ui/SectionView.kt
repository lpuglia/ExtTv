package com.android.exttv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = Modifier
            .width(200.dp)
    ){
        var isFocused by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(120.dp)
                .onKeyEvent { event ->
                    cardIndex==0 && event.key == Key.DirectionLeft
                }
                .onFocusChanged {
                    if (it.isFocused) {
                        if (isNotPlayer) {
                            Sections.focusedIndex = sectionIndex
                            Sections.focusedCardIndex = cardIndex
                            StatusManager.bgImage = card.secondaryArt
                        }else{
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
                }else{
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
            colors = CardDefaults.colors(containerColor = Color(0x00000000)),
        ) {
            Box() {
                AsyncImage(
                    model =  ImageRequest.Builder(LocalContext.current)
                        .data(card.primaryArt)
//                        .placeholder(placeholderDrawable) // Set the placeholder here
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
            // favouriteLabel is only set for favourite cards
            text = parseText(cleanText(if(card.favouriteLabel!="") card.favouriteLabel else card.label,)),
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
//                    .alpha(if (isFocused) 1f else 0f)
                    .padding(start = 10.dp, top = 5.dp, end = 10.dp)
                    .width(200.dp)
                    .basicMarquee(iterations = if (isFocused) 100 else 0),
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    if(isNotPlayer) {
        LaunchedEffect(Sections.focusedCardIndex, Sections.focusedIndex, Sections.refocus) {
            if (Sections.focusedIndex == sectionIndex && Sections.focusedCardIndex == cardIndex) {
                sectionsListState.scrollToItem(sectionIndex)
                cardListState.scrollToItem(cardIndex)
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
