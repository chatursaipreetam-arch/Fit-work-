package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FitnessRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FitnessRepository(database.fitnessDao())
        
        // Seed initial exercises, mock stats for progress, and load today's log
        viewModelScope.launch {
            repository.seedDefaultExercisesIfEmpty()
            repository.seedMockLogsIfEmpty()
            updateSelectedDate(getTodayDateString())
            
            // Seed 2 default goals if goals database is empty, to give users a good starting experience
            val currentGoals = repository.allGoals.first()
            if (currentGoals.isEmpty()) {
                repository.insertGoal(FitnessGoal(title = "Drink water", targetValue = 3000.0, currentValue = 0.0, unit = "ml", category = "Water"))
                repository.insertGoal(FitnessGoal(title = "Burn Calories", targetValue = 2000.0, currentValue = 0.0, unit = "kcal", category = "Calories"))
                repository.insertGoal(FitnessGoal(title = "Active Workouts", targetValue = 4.0, currentValue = 0.0, unit = "times", category = "Workout"))
            }
        }
    }

    // --- State Observables ---

    val goals: StateFlow<List<FitnessGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<BodyProduct>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<WorkoutExercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val currentLog: StateFlow<DailyFitnessLog?> = _selectedDate
        .flatMapLatest { date -> repository.getLogFlowByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentLogs: StateFlow<List<DailyFitnessLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val last30Logs: StateFlow<List<DailyFitnessLog>> = repository.last30Logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealsForSelectedDate: StateFlow<List<LoggedMeal>> = _selectedDate
        .flatMapLatest { date -> repository.getMealsByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeMuscleFilter = MutableStateFlow("All")
    val activeMuscleFilter: StateFlow<String> = _activeMuscleFilter.asStateFlow()

    val filteredExercises: StateFlow<List<WorkoutExercise>> = combine(allExercises, _activeMuscleFilter) { exercises, filter ->
        if (filter == "All") exercises else exercises.filter { it.targetMuscle.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Gemini Interactive States ---
    private val _isAnalyzingProduct = MutableStateFlow(false)
    val isAnalyzingProduct: StateFlow<Boolean> = _isAnalyzingProduct.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // --- Actions ---

    fun updateSelectedDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            repository.getOrCreateLog(date)
        }
    }

    fun setMuscleFilter(muscle: String) {
        _activeMuscleFilter.value = muscle
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            repository.addWater(_selectedDate.value, ml)
            // also update water-related goals
            val todayLog = repository.getOrCreateLog(_selectedDate.value)
            updateGoalProgress("Water", todayLog.waterIntakeMl.toDouble() + ml)
        }
    }

    fun addCalories(calories: Int) {
        viewModelScope.launch {
            repository.addCalories(_selectedDate.value, calories)
            // also update calorie-related goals
            val todayLog = repository.getOrCreateLog(_selectedDate.value)
            updateGoalProgress("Calories", todayLog.caloriesConsumed.toDouble() + calories)
        }
    }

    fun logWorkoutMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.addActiveMinutes(_selectedDate.value, minutes)
            
            // Increment workout related goals
            val currentList = goals.value
            currentList.forEach { goal ->
                if (goal.category == "Workout") {
                    repository.updateGoal(goal.copy(currentValue = goal.currentValue + 1))
                }
            }
        }
    }

    // --- Meals Actions ---
    fun logMeal(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            repository.insertMeal(
                LoggedMeal(
                    date = _selectedDate.value,
                    name = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                )
            )
            // Synchronize budget in today's active state
            val todayLog = repository.getOrCreateLog(_selectedDate.value)
            updateGoalProgress("Calories", todayLog.caloriesConsumed.toDouble() + calories)
        }
    }

    fun deleteMeal(meal: LoggedMeal) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
            // Synchronize goals
            val todayLog = repository.getOrCreateLog(_selectedDate.value)
            updateGoalProgress("Calories", todayLog.caloriesConsumed.toDouble() - meal.calories)
        }
    }

    // --- Goals Actions ---

    fun addNewGoal(title: String, target: Double, unit: String, category: String) {
        viewModelScope.launch {
            repository.insertGoal(
                FitnessGoal(
                    title = title,
                    targetValue = target,
                    currentValue = 0.0,
                    unit = unit,
                    category = category
                )
            )
        }
    }

    private suspend fun updateGoalProgress(category: String, newValue: Double) {
        val currentList = goals.value
        currentList.forEach { goal ->
            if (goal.category.equals(category, ignoreCase = true)) {
                repository.updateGoal(goal.copy(currentValue = newValue))
            }
        }
    }

    fun updateGoalDirect(goalId: Int, progressValue: Double) {
        viewModelScope.launch {
            val list = goals.value
            list.find { it.id == goalId }?.let { goal ->
                repository.updateGoal(goal.copy(currentValue = progressValue))
            }
        }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoalById(goalId)
        }
    }

    // --- Custom Exercises ---

    fun addCustomExercise(name: String, muscle: String, instructions: String, difficulty: String, setsReps: String) {
        viewModelScope.launch {
            repository.insertExercise(
                WorkoutExercise(
                    name = name,
                    targetMuscle = muscle,
                    instructions = instructions,
                    difficulty = difficulty,
                    setsReps = setsReps,
                    isCustom = true
                )
            )
        }
    }

    fun deleteExercise(id: Int) {
        viewModelScope.launch {
            repository.deleteExerciseById(id)
        }
    }


    // --- Gemini Analysis Actions ---

    fun analyzeProduct(name: String, category: String, ingredients: String, onFinished: () -> Unit = {}) {
        if (name.isBlank() || ingredients.isBlank()) {
            _analysisError.value = "Please fill out both product name and ingredients."
            return
        }
        _isAnalyzingProduct.value = true
        _analysisError.value = null
        
        viewModelScope.launch {
            try {
                // calls Gemini REST endpoint, inserts product with response to db
                repository.analyzeBodyProduct(name, category, ingredients)
                onFinished()
            } catch (e: Exception) {
                _analysisError.value = e.localizedMessage ?: "Failed to analyze product. Check connection or api key."
            } finally {
                _isAnalyzingProduct.value = false
            }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProductById(id)
        }
    }

    // --- Helper Utilities ---

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
