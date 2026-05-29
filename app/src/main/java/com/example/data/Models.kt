package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fitness_goals")
data class FitnessGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val category: String // "Weight", "Workout", "Calories", "Water", "Steps"
)

@Entity(tableName = "daily_fitness_logs")
data class DailyFitnessLog(
    @PrimaryKey val date: String, // format "YYYY-MM-DD"
    val caloriesConsumed: Int,
    val caloriesGoal: Int = 2000,
    val waterIntakeMl: Int,
    val waterGoalMl: Int = 3000,
    val activeTimeMinutes: Int = 0,
    val activeTimeGoalMinutes: Int = 60
)

@Entity(tableName = "body_products")
data class BodyProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Supplement", "Skincare", "Shampoo", "Bodywash", "Other"
    val ingredients: String,
    val analysisResult: String, // AI analysis
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_exercises")
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetMuscle: String, // "Chest", "Back", "Legs", "Shoulders", "Arms", "Core"
    val instructions: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val setsReps: String, // e.g. "4 sets of 10 reps"
    val caloriesBurntPerSet: Int = 15,
    val isCustom: Boolean = false
)

@Entity(tableName = "logged_meals")
data class LoggedMeal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "YYYY-MM-DD"
    val name: String,
    val calories: Int,
    val protein: Int, // in grams
    val carbs: Int,   // in grams
    val fat: Int      // in grams
)

