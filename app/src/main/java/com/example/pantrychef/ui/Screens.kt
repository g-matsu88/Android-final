package com.example.pantrychef.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pantrychef.data.MealSummary

// ---------------------------------------------------------------- shared bits
@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content
)

@Composable
private fun <T> StateView(state: UiState<T>, onRetry: (() -> Unit)?, idleText: String, content: @Composable (T) -> Unit) {
    when (state) {
        UiState.Idle -> Centered { Text(idleText) }
        UiState.Loading -> Centered { CircularProgressIndicator() }
        is UiState.Error -> Centered {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        is UiState.Success -> content(state.data)
    }
}

// ---------------------------------------------------------------- Pantry
@Composable
fun PantryScreen(vm: MainViewModel, onFind: () -> Unit) {
    val items by vm.pantry.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("My Pantry", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onFind, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Find recipes with these ingredients")
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Centered { Text("Your pantry is empty.\nTap + to add your first ingredient.") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(items, key = { it.id }) { item ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    if (item.quantity.isNotBlank()) Text(item.quantity, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { vm.deleteItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add ingredient") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name (e.g. chicken)") }, singleLine = true)
                    OutlinedTextField(qty, { qty = it }, label = { Text("Quantity (e.g. 500 g)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.addItem(name, qty); showDialog = false }, enabled = name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

// ---------------------------------------------------------------- Results
@Composable
fun ResultsScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val state by vm.results.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recipes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        StateView(state, onRetry = { vm.searchRecipes() }, idleText = "Go to the Pantry tab and tap \"Find recipes\".") { meals ->
            if (meals.isEmpty()) {
                Centered { Text("No recipes found for your ingredients.\nTry simpler names like \"chicken\" or \"rice\".") }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meals, key = { it.id }) { MealCard(it, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealSummary, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(meal.id) }) {
        Column {
            AsyncImage(
                model = meal.thumb, contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Column(Modifier.padding(8.dp)) {
                Text(meal.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text("Uses ${meal.matches} of your items", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------------------------------------------------------- Detail
@Composable
fun DetailScreen(vm: MainViewModel, id: String, onBack: () -> Unit) {
    LaunchedEffect(id) { vm.loadDetail(id) }
    val state by vm.detail.collectAsState()
    val favorites by vm.favorites.collectAsState()

    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        StateView(state, onRetry = { vm.loadDetail(id) }, idleText = "") { d ->
            val isFav = favorites.any { it.id == d.id }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = d.thumb, contentDescription = d.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium)
                )
                Text(d.name, style = MaterialTheme.typography.headlineSmall)
                Text("${d.category} • ${d.area}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { vm.toggleFavorite(d) }) {
                    Icon(Icons.Default.Favorite, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFav) "Remove from favorites" else "Save to favorites")
                }
                Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                d.ingredients.forEach { Text("• $it") }
                Text("Instructions", style = MaterialTheme.typography.titleMedium)
                Text(d.instructions)
            }
        }
    }
}

// ---------------------------------------------------------------- Favorites
@Composable
fun FavoritesScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val favs by vm.favorites.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (favs.isEmpty()) {
            Centered { Text("No saved recipes yet.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favs, key = { it.id }) { f ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(f.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = f.thumb, contentDescription = f.name, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp)
                            )
                            Text(f.name, Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- AI Chef
@Composable
fun AiChefScreen(vm: MainViewModel) {
    val chat by vm.chat.collectAsState()
    val loading by vm.aiLoading.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chat.size, loading) {
        if (chat.isNotEmpty()) listState.animateScrollToItem(chat.size)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Chef", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.askAi("Create a simple recipe using my pantry ingredients.") },
            enabled = !loading, modifier = Modifier.fillMaxWidth()
        ) { Text("Create a recipe from my pantry") }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (chat.isEmpty()) item {
                Text("Ask me anything, e.g. \"What can I use instead of butter?\"", style = MaterialTheme.typography.bodyMedium)
            }
            items(chat) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (msg.fromUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) { Text(msg.text, Modifier.padding(12.dp)) }
                }
            }
            if (loading) item { CircularProgressIndicator(Modifier.size(24.dp)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                input, { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Ask the chef...") }, maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { vm.askAi(input); input = "" },
                enabled = input.isNotBlank() && !loading
            ) { Text("Send") }
        }
    }
}
