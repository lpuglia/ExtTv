package com.android.exttv.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.android.exttv.manager.PyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RepositoryDialog(
    showDialog: MutableState<Boolean>,
    drawerState: DrawerState
) {
    val items = listOf("Item 1", "Item 2", "Item 3")

    var selectedItem by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { showDialog.value = false }) {
        Surface(
            modifier = Modifier.padding(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select an Item:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

                items.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .focusable()
                            .clickable {
                                selectedItem = item
                            }
                    )
                }
            }
        }
    }
}



@Composable
fun GithubDialog(showDialog: MutableState<Boolean>, drawerState: DrawerState) {
    var username by remember { mutableStateOf("") }
    var repoName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }

    val textFieldValues = listOf(
        Pair("Username", username) to { newValue: String -> username = newValue },
        Pair("Repository", repoName) to { newValue: String -> repoName = newValue },
        Pair("Branch", branchName) to { newValue: String -> branchName = newValue }
    )

    val focusRequesters = List(textFieldValues.size) { FocusRequester() }
    val saveButtonRequester = FocusRequester()
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { showDialog.value = false }) {
        Surface(
            modifier = Modifier.background(Color.White),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Enter GitHub Details")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textFieldValues.forEachIndexed { index, (labelValue, onValueChange) ->
                        val (label, value) = labelValue
                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            label = { Text(label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = if (index < textFieldValues.size - 1) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (index < focusRequesters.size - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                },
                                onDone = {
                                    saveButtonRequester.requestFocus()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[index])
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.Tab -> {
                                                if (index < focusRequesters.size - 1) {
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { showDialog.value = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showDialog.value = false
                            drawerState.setValue(DrawerValue.Closed)
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    PyManager.isLoadingSection = true
                                    PyManager.addPlugin() // This might be a long operation
                                }
                            }
                        },
                        modifier = Modifier.focusRequester(saveButtonRequester)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}