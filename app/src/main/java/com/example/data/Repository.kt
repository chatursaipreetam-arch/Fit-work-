package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FitnessRepository(private val dao: FitnessDao) {

    // --- Goals ---
    val allGoals: Flow<List<FitnessGoal>> = dao.getAllGoals()

    suspend fun insertGoal(goal: FitnessGoal) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: FitnessGoal) = dao.updateGoal(goal)
    suspend fun deleteGoalById(id: Int) = dao.deleteGoalById(id)


    // --- Daily Logs ---
    fun getLogFlowByDate(date: String): Flow<DailyFitnessLog?> = dao.getLogFlowByDate(date)
    val recentLogs: Flow<List<DailyFitnessLog>> = dao.getRecentLogs()

    suspend fun getOrCreateLog(date: String): DailyFitnessLog {
        val existing = dao.getLogByDate(date)
        if (existing != null) {
            return existing
        }
        val defaultLog = DailyFitnessLog(
            date = date,
            caloriesConsumed = 0,
            waterIntakeMl = 0,
            activeTimeMinutes = 0
        )
        dao.insertDailyLog(defaultLog)
        return defaultLog
    }

    suspend fun addCalories(date: String, calories: Int) {
        getOrCreateLog(date) // ensure exists
        dao.addCalories(date, calories)
    }

    suspend fun addWater(date: String, ml: Int) {
        getOrCreateLog(date) // ensure exists
        dao.addWater(date, ml)
    }

    suspend fun addActiveMinutes(date: String, minutes: Int) {
        getOrCreateLog(date) // ensure exists
        dao.addActiveMinutes(date, minutes)
    }

    suspend fun updateDailyLog(log: DailyFitnessLog) {
        dao.insertDailyLog(log)
    }


    // --- Body Products ---
    val allProducts: Flow<List<BodyProduct>> = dao.getAllProducts()

    suspend fun insertProduct(product: BodyProduct) = dao.insertProduct(product)
    suspend fun deleteProductById(id: Int) = dao.deleteProductById(id)

    suspend fun analyzeBodyProduct(name: String, category: String, ingredients: String): String {
        val prompt = """
            You are an expert fitness advisor, dermatologist, and sports science toxicologist.
            Analyze the following body product and its ingredients to give detailed feedback for bodybuilders and fitness enthusiasts:
            
            Product Name: $name
            Category: $category
            Ingredients: $ingredients
            
            Please provide a structured analysis containing:
            1. Ingredient Safety & Toxicity (flag any hormonal disruptors, endocrine inhibitors, or allergens).
            2. Fitness & Bodybuilding Impact (how this category e.g. protein, supplement, creatine, skincare, lotion, body wash impacts muscle recovery, hormone health like testosterone/estrogen, or performance).
            3. Final Recommendation (suitability, instructions of use, and alternative tips).
            
            Keep your language extremely engaging, professional, and clear.
        """.trimIndent()
        
        val result = GeminiClient.generate(prompt)
        val productWithAnalysis = BodyProduct(
            name = name,
            category = category,
            ingredients = ingredients,
            analysisResult = result
        )
        insertProduct(productWithAnalysis)
        return result
    }


    // --- Workout Exercises ---
    val allExercises: Flow<List<WorkoutExercise>> = dao.getAllExercises()

    suspend fun insertExercise(exercise: WorkoutExercise) = dao.insertExercise(exercise)
    suspend fun deleteExerciseById(id: Int) = dao.deleteExerciseById(id)

    /**
     * Seeds default bodybuilding exercises if the list is empty.
     */
    suspend fun seedDefaultExercisesIfEmpty() {
        val exercises = dao.getAllExercises().first()
        if (exercises.isEmpty()) {
            val defaults = listOf(
                WorkoutExercise(
                    name = "Barbell Bench Press",
                    targetMuscle = "Chest",
                    instructions = "Lie on a flat bench, grip the barbell slightly wider than shoulder-width. Lower the bar to chest level and press it back up explosive.",
                    difficulty = "Intermediate",
                    setsReps = "4 sets of 8-12 reps",
                    caloriesBurntPerSet = 18,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Incline Dumbbell Fly",
                    targetMuscle = "Chest",
                    instructions = "Lie on an incline bench with dumbbells. With a slight bend in the elbows, lower arm outward in an arc until a stretch is felt, then squeeze back up.",
                    difficulty = "Intermediate",
                    setsReps = "3 sets of 12-15 reps",
                    caloriesBurntPerSet = 14,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Barbell Squats",
                    targetMuscle = "Legs",
                    instructions = "Rest barbell on upper shoulders. Keep chest up, core tight, and squat down until thighs are parallel to the floor, then push through heels to stand.",
                    difficulty = "Advanced",
                    setsReps = "4 sets of 8-10 reps",
                    caloriesBurntPerSet = 25,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Deadlifts",
                    targetMuscle = "Back",
                    instructions = "Stand with feet hip-width apart. Bend at the hips and knees, grab the barbell, keep spine straight, and lift with core and leg drive.",
                    difficulty = "Advanced",
                    setsReps = "3 sets of 5-8 reps",
                    caloriesBurntPerSet = 28,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Pull-Ups",
                    targetMuscle = "Back",
                    instructions = "Grasp bar with palms facing away, wider than shoulder-width. Pull body up until chin clears the bar, keeping core engaged.",
                    difficulty = "Intermediate",
                    setsReps = "4 sets of Max reps",
                    caloriesBurntPerSet = 15,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Military Press",
                    targetMuscle = "Shoulders",
                    instructions = "Hold barbell at shoulder level, feet shoulder-width. Press the bar straight overhead, locking out elbows and bracing core.",
                    difficulty = "Intermediate",
                    setsReps = "4 sets of 8-10 reps",
                    caloriesBurntPerSet = 18,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Dumbbell Lateral Raise",
                    targetMuscle = "Shoulders",
                    instructions = "Hold dumbbells in hands at sides. Raise arms out horizontally to the sides with a slight bend in elbows, then slowly lower.",
                    difficulty = "Beginner",
                    setsReps = "3 sets of 15 reps",
                    caloriesBurntPerSet = 10,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Barbell Bicep Curl",
                    targetMuscle = "Arms",
                    instructions = "Hold barbell with underhand grip. Keep elbows tucked in close to torso, and curl bar up towards chest using biceps.",
                    difficulty = "Beginner",
                    setsReps = "3 sets of 12 reps",
                    caloriesBurntPerSet = 12,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Triceps Cable Pushdowns",
                    targetMuscle = "Arms",
                    instructions = "Hold cable attachment at upper chest. Keeping elbows stationary, push bar downward until arms are fully extended.",
                    difficulty = "Beginner",
                    setsReps = "3 sets of 12-15 reps",
                    caloriesBurntPerSet = 11,
                    isCustom = false
                ),
                WorkoutExercise(
                    name = "Hanging Knee Raises",
                    targetMuscle = "Core",
                    instructions = "Hang from pull-up bar. Keeping legs straight or bent, lift knees towards chest, highlighting contraction of the abdominal wall.",
                    difficulty = "Beginner",
                    setsReps = "3 sets of 15-20 reps",
                    caloriesBurntPerSet = 12,
                    isCustom = false
                )
            )
            for (item in defaults) {
                dao.insertExercise(item)
            }
        }
    }

    // --- Daily Logs Extended & Seeding ---
    val last30Logs: Flow<List<DailyFitnessLog>> = dao.getLast30Logs()

    suspend fun seedMockLogsIfEmpty() {
        val calendar = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        
        // Let's check if the database only has today's log (i.e., less than 2 logs total)
        val logs = dao.getRecentLogs().first()
        if (logs.size <= 1) {
            // Generate last 30 days of metrics
            for (i in 30 downTo 1) {
                calendar.time = java.util.Date()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(calendar.time)
                
                // Let's create realistic variation with some days on target, some off target
                val baseCal = 1400 + (Math.random() * 900).toInt()
                val baseWater = 1500 + (Math.random() * 2000).toInt()
                val baseActive = 20 + (Math.random() * 65).toInt()
                
                dao.insertDailyLog(
                    DailyFitnessLog(
                        date = dateStr,
                        caloriesConsumed = baseCal,
                        caloriesGoal = 2200,
                        waterIntakeMl = baseWater,
                        waterGoalMl = 3000,
                        activeTimeMinutes = baseActive,
                        activeTimeGoalMinutes = 60
                    )
                )

                // Seed some daily meals for progress consistency
                val mealNames = listOf("Oatmeal & Protein Shake", "Chicken Rice & Broccoli", "Almond Butter Banana Snack", "Lean Steak with Sweet Potato")
                val baseCals = listOf(450, 650, 300, 700)
                val proteins = listOf(35, 52, 12, 48)
                val carbs = listOf(55, 60, 25, 45)
                val fats = listOf(8, 6, 15, 18)
                
                for (m in 0 until 3) {
                    dao.insertMeal(
                        LoggedMeal(
                            date = dateStr,
                            name = mealNames[m],
                            calories = baseCals[m],
                            protein = proteins[m],
                            carbs = carbs[m],
                            fat = fats[m]
                        )
                    )
                }
            }
        }
    }

    // --- Meals API ---
    fun getMealsByDate(date: String): Flow<List<LoggedMeal>> = dao.getMealsByDate(date)

    suspend fun insertMeal(meal: LoggedMeal) {
        dao.insertMeal(meal)
        // Also automatically add calories to the DailyFitnessLog!
        addCalories(meal.date, meal.calories)
    }

    suspend fun deleteMeal(meal: LoggedMeal) {
        dao.deleteMealById(meal.id)
        // Deduct calories from DailyFitnessLog
        addCalories(meal.date, -meal.calories)
    }
}
