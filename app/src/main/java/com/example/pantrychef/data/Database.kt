package com.example.pantrychef.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: String
)

@Entity(tableName = "favorites")
data class FavoriteMeal(
    @PrimaryKey val id: String,
    val name: String,
    val thumb: String
)

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY name")
    fun getAll(): Flow<List<PantryItem>>

    @Insert
    suspend fun insert(item: PantryItem)

    @Delete
    suspend fun delete(item: PantryItem)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY name")
    fun getAll(): Flow<List<FavoriteMeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: FavoriteMeal)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [PantryItem::class, FavoriteMeal::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun favoriteDao(): FavoriteDao
}
