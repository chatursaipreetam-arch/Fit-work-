package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    // --- Goals ---
    @Query("SELECT * FROM fitness_goals")
    fun getAllGoals(): Flow<List<FitnessGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FitnessGoal)

    @Update
    suspend fun updateGoal(goal: FitnessGoal)

    @Query("DELETE FROM fitness_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)


    // --- Daily Logs ---
    @Query("SELECT * FROM daily_fitness_logs WHERE date = :date")
    suspend fun getLogByDate(date: String): DailyFitnessLog?

    @Query("SELECT * FROM daily_fitness_logs WHERE date = :date")
    fun getLogFlowByDate(date: String): Flow<DailyFitnessLog?>

    @Query("SELECT * FROM daily_fitness_logs ORDER BY date DESC LIMIT 7")
    fun getRecentLogs(): Flow<List<DailyFitnessLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyFitnessLog)

    @Query("UPDATE daily_fitness_logs SET caloriesConsumed = caloriesConsumed + :calories WHERE date = :date")
    suspend fun addCalories(date: String, calories: Int)

    @Query("UPDATE daily_fitness_logs SET waterIntakeMl = waterIntakeMl + :ml WHERE date = :date")
    suspend fun addWater(date: String, ml: Int)

    @Query("UPDATE daily_fitness_logs SET activeTimeMinutes = activeTimeMinutes + :minutes WHERE date = :date")
    suspend fun addActiveMinutes(date: String, minutes: Int)


    // --- Body Products ---
    @Query("SELECT * FROM body_products ORDER BY timestamp DESC")
    fun getAllProducts(): Flow<List<BodyProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: BodyProduct)

    @Query("DELETE FROM body_products WHERE id = :id")
    suspend fun deleteProductById(id: Int)


    // --- Workout Exercises ---
    @Query("SELECT * FROM workout_exercises")
    fun getAllExercises(): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE targetMuscle = :muscle")
    fun getExercisesByMuscle(muscle: String): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Int)

    // --- Daily Logs Extended ---
    @Query("SELECT * FROM daily_fitness_logs ORDER BY date DESC LIMIT 30")
    fun getLast30Logs(): Flow<List<DailyFitnessLog>>

    // --- Logged Meals ---
    @Query("SELECT * FROM logged_meals WHERE date = :date")
    fun getMealsByDate(date: String): Flow<List<LoggedMeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: LoggedMeal)

    @Query("DELETE FROM logged_meals WHERE id = :id")
    suspend fun deleteMealById(id: Int)
}
