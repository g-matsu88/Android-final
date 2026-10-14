package com.example.pantrychef.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.pantrychef.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChatMsg(val fromUser: Boolean, val text: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(
        Room.databaseBuilder(app, AppDatabase::class.java, "pantrychef.db").build()
    )

    val pantry: StateFlow<List<PantryItem>> = repo.pantryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteMeal>> = repo.favoriteDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _results = MutableStateFlow<UiState<List<MealSummary>>>(UiState.Idle)
    val results = _results.asStateFlow()

    private val _detail = MutableStateFlow<UiState<MealDetail>>(UiState.Idle)
    val detail = _detail.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatMsg>>(emptyList())
    val chat = _chat.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    // ----- Pantry (Room) -----
    fun addItem(name: String, quantity: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.pantryDao.insert(PantryItem(name = name.trim(), quantity = quantity.trim()))
    }

    fun deleteItem(item: PantryItem) = viewModelScope.launch { repo.pantryDao.delete(item) }

    // ----- Recipes (TheMealDB via Retrofit + coroutines) -----
    fun searchRecipes() {
        val names = pantry.value.map { it.name }
        if (names.isEmpty()) {
            _results.value = UiState.Error("Add some ingredients to your pantry first.")
            return
        }
        viewModelScope.launch {
            _results.value = UiState.Loading
            _results.value = try {
                UiState.Success(repo.findRecipes(names))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun loadDetail(id: String) = viewModelScope.launch {
        _detail.value = UiState.Loading
        _detail.value = try {
            repo.mealDetail(id)?.let { UiState.Success(it) } ?: UiState.Error("Recipe not found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Network error")
        }
    }

    fun toggleFavorite(d: MealDetail) = viewModelScope.launch {
        if (favorites.value.any { it.id == d.id }) repo.favoriteDao.deleteById(d.id)
        else repo.favoriteDao.insert(FavoriteMeal(d.id, d.name, d.thumb))
    }

    // ----- AI Chef (Gemini) -----
    fun askAi(request: String) {
        if (request.isBlank() || _aiLoading.value) return
        val pantryText = pantry.value.joinToString { "${it.name} (${it.quantity})" }
            .ifBlank { "nothing listed" }
        val prompt = "You are a friendly home-cooking assistant. " +
            "The user's pantry contains: $pantryText. " +
            "User request: $request. " +
            "Answer concisely with an ingredient list and numbered steps where relevant. " +
            "Suggest substitutions for anything missing."
        viewModelScope.launch {
            _chat.value += ChatMsg(true, request)
            _aiLoading.value = true
            val reply = try {
                repo.askGemini(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Sorry, the AI request failed: ${e.message}"
            }
            _chat.value += ChatMsg(false, reply)
            _aiLoading.value = false
        }
    }
}
