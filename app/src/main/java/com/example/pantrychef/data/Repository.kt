package com.example.pantrychef.data

import com.example.pantrychef.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class MealSummary(val id: String, val name: String, val thumb: String, val matches: Int)

data class MealDetail(
    val id: String,
    val name: String,
    val thumb: String,
    val category: String,
    val area: String,
    val instructions: String,
    val ingredients: List<String>
)

class Repository(db: AppDatabase) {
    val pantryDao = db.pantryDao()
    val favoriteDao = db.favoriteDao()

    /** Searches TheMealDB once per pantry ingredient IN PARALLEL (coroutines),
     *  then ranks recipes by how many of your ingredients they use. */
    suspend fun findRecipes(ingredients: List<String>): List<MealSummary> = coroutineScope {
        val perIngredient = ingredients.map { ing ->
            async {
                Network.meals
                    .byIngredient(ing.trim().lowercase().replace(" ", "_"))
                    .meals.orEmpty()
            }
        }.awaitAll()

        perIngredient.flatten()
            .groupBy { it["idMeal"].orEmpty() }
            .map { (id, list) ->
                val m = list.first()
                MealSummary(id, m["strMeal"].orEmpty(), m["strMealThumb"].orEmpty(), list.size)
            }
            .sortedByDescending { it.matches }
    }

    suspend fun mealDetail(id: String): MealDetail? {
        val m = Network.meals.details(id).meals?.firstOrNull() ?: return null
        val ingredients = (1..20).mapNotNull { i ->
            val name = m["strIngredient$i"]?.trim().orEmpty()
            val measure = m["strMeasure$i"]?.trim().orEmpty()
            if (name.isBlank()) null else "$measure $name".trim()
        }
        return MealDetail(
            id = m["idMeal"].orEmpty(),
            name = m["strMeal"].orEmpty(),
            thumb = m["strMealThumb"].orEmpty(),
            category = m["strCategory"].orEmpty(),
            area = m["strArea"].orEmpty(),
            instructions = m["strInstructions"].orEmpty(),
            ingredients = ingredients
        )
    }

    suspend fun askGemini(prompt: String): String {
        if (BuildConfig.GEMINI_KEY.isBlank()) {
            return "No Gemini API key found. Add GEMINI_KEY=your_key to local.properties and rebuild."
        }
        val response = Network.gemini.generate(
            BuildConfig.GEMINI_KEY,
            GeminiRequest(listOf(GContent(listOf(GPart(prompt)))))
        )
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "The AI returned no answer."
    }
}
