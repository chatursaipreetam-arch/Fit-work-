package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val filteredExercises by viewModel.filteredExercises.collectAsStateWithLifecycle()
    val currentLog by viewModel.currentLog.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeMuscleFilter.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingProduct.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    
    val tabs = listOf(
        TabItem("Metrics", Icons.Default.Home, Icons.Outlined.Home),
        TabItem("Gym Guide", Icons.Default.FitnessCenter, Icons.Outlined.FitnessCenter),
        TabItem("AI Analyzer", Icons.Default.Science, Icons.Outlined.Science),
        TabItem("Goals Room", Icons.Default.TrackChanges, Icons.Outlined.TrackChanges)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DeepSlateSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonSportGreen,
                            selectedTextColor = NeonSportGreen,
                            unselectedIconColor = SoftTextMuted,
                            unselectedTextColor = SoftTextMuted,
                            indicatorColor = SoftGrayBorder
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = CarbonDarkBg
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        val loggedMeals by viewModel.mealsForSelectedDate.collectAsStateWithLifecycle()
                        val last30Logs by viewModel.last30Logs.collectAsStateWithLifecycle()
                        MetricsDashboardTab(
                            currentLog = currentLog,
                            recentLogs = recentLogs,
                            last30Logs = last30Logs,
                            loggedMeals = loggedMeals,
                            goals = goals,
                            onAddWater = { viewModel.addWater(it) },
                            onAddCalories = { viewModel.addCalories(it) },
                            onAddActiveMinutes = { viewModel.logWorkoutMinutes(it) },
                            onAddMeal = { name, cals, protein, carbs, fat ->
                                viewModel.logMeal(name, cals, protein, carbs, fat)
                            },
                            onDeleteMeal = { viewModel.deleteMeal(it) }
                        )
                    }
                    1 -> GymGuideTab(
                        exercises = filteredExercises,
                        activeMuscleFilter = activeFilter,
                        onSelectMuscleFilter = { viewModel.setMuscleFilter(it) },
                        onCompleteSet = { caloriesMultiplier, repsSets ->
                            viewModel.logWorkoutMinutes(15) // Adds 15 minutes of activity!
                            viewModel.addCalories(-75) // Burns 75 calories!
                        },
                        onAddCustomExercise = { name, muscle, instructions, diff, setsReps ->
                            viewModel.addCustomExercise(name, muscle, instructions, diff, setsReps)
                        },
                        onDeleteExercise = { viewModel.deleteExercise(it) },
                        onCompleteTemplate = { templateName, durationMin, caloriesBurnt ->
                            viewModel.logWorkoutMinutes(durationMin)
                            viewModel.addCalories(-caloriesBurnt)
                        }
                    )
                    2 -> AiAnalyzerTab(
                        analyzedProducts = products,
                        isAnalyzing = isAnalyzing,
                        analysisError = analysisError,
                        onAnalyze = { name, category, ingredients ->
                            viewModel.analyzeProduct(name, category, ingredients)
                        },
                        onDeleteProduct = { viewModel.deleteProduct(it) }
                    )
                    3 -> GoalsRoomTab(
                        goals = goals,
                        onAddGoal = { title, target, unit, category ->
                            viewModel.addNewGoal(title, target, unit, category)
                        },
                        onUpdateProgress = { id, value ->
                            viewModel.updateGoalDirect(id, value)
                        },
                        onDeleteGoal = { viewModel.deleteGoal(it) }
                    )
                }
            }
        }
    }
}

data class TabItem(val title: String, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector, val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector)


data class FoodPreset(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val icon: String = "🥑"
)

val foodPresets = listOf(
    FoodPreset("Grilled Chicken Breast (200g)", 330, 62, 0, 5, "🍗"),
    FoodPreset("Boiled Eggs (2 large)", 140, 12, 1, 10, "🥚"),
    FoodPreset("Whey Protein Shake (1 scoop)", 120, 24, 3, 2, "🥤"),
    FoodPreset("Cooked Brown Rice (150g)", 175, 4, 38, 2, "🍚"),
    FoodPreset("Cooked White Rice (150g)", 195, 4, 42, 0, "🍚"),
    FoodPreset("Seared Salmon Fillet (150g)", 310, 34, 0, 18, "🐟"),
    FoodPreset("Oatmeal with Milk (1 bowl)", 250, 10, 42, 6, "🥣"),
    FoodPreset("Almonds (1 handful / 30g)", 170, 6, 6, 15, "🥜"),
    FoodPreset("Peanut Butter (2 tbsp)", 190, 8, 7, 16, "🥜"),
    FoodPreset("Greek Yogurt (200g)", 150, 17, 8, 4, "🥛"),
    FoodPreset("Sweet Potato (1 medium)", 115, 2, 27, 0, "🍠"),
    FoodPreset("Whole Banana", 105, 1, 27, 0, "🍌"),
    FoodPreset("Avocado (1 medium)", 240, 3, 12, 22, "🥑"),
    FoodPreset("Whole Wheat Bread (2 slices)", 160, 7, 30, 2, "🍞")
)


// ==========================================
// TAB 1: METRICS & DAILY BUDGET TRACKER
// ==========================================

@Composable
fun MetricsDashboardTab(
    currentLog: DailyFitnessLog?,
    recentLogs: List<DailyFitnessLog>,
    last30Logs: List<DailyFitnessLog>,
    loggedMeals: List<LoggedMeal>,
    goals: List<FitnessGoal>,
    onAddWater: (Int) -> Unit,
    onAddCalories: (Int) -> Unit,
    onAddActiveMinutes: (Int) -> Unit,
    onAddMeal: (String, Int, Int, Int, Int) -> Unit,
    onDeleteMeal: (LoggedMeal) -> Unit
) {
    var activeMetricsSection by remember { mutableStateOf(0) } // 0 = Daily Logs, 1 = Meal Diary, 2 = Trend Reports

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Professional Polish" Elegant Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THURSDAY, MAY 28",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoftTextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Hello, Marcus",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MB",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Sub-switcher Segmented Navigation Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSlateSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, SoftGrayBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Daily Logs", "Meal Diary", "Trend Reports").forEachIndexed { index, title ->
                    val isSelected = activeMetricsSection == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonSportGreen else Color.Transparent)
                            .clickable { activeMetricsSection = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CarbonDarkBg else InkColor
                        )
                    }
                }
            }
        }

        if (activeMetricsSection == 0) {
            // TODAY'S ORIGINAL SCOREBOARDS & METRICS
            item {
                val log = currentLog ?: DailyFitnessLog("", 0, 2000, 0, 3000, 0, 60)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Scoreboards",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = InkColor
                            )
                            Box(
                                modifier = Modifier
                                    .background(NeonSportGreen.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active Today",
                                    color = NeonSportGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Calories Consumed Ring
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val pct = if (log.caloriesGoal > 0) (log.caloriesConsumed.toFloat() / log.caloriesGoal.toFloat()).coerceIn(0f, 1f) else 0f
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = SoftGrayBorder,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = BrightTeal,
                                            startAngle = -90f,
                                            sweepAngle = pct * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${log.caloriesConsumed}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = InkColor
                                        )
                                        Text(
                                            text = "/${log.caloriesGoal} kcal",
                                            fontSize = 10.sp,
                                            color = SoftTextMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Calories", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrightTeal)
                            }

                            // Water Circle Ring
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val pct = if (log.waterGoalMl > 0) (log.waterIntakeMl.toFloat() / log.waterGoalMl.toFloat()).coerceIn(0f, 1f) else 0f
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = SoftGrayBorder,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = NeonSportGreen,
                                            startAngle = -90f,
                                            sweepAngle = pct * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${log.waterIntakeMl}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = InkColor
                                        )
                                        Text(
                                            text = "/${log.waterGoalMl} ml",
                                            fontSize = 10.sp,
                                            color = SoftTextMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Hydration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeonSportGreen)
                            }

                            // Active Minutes Ring
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val pct = if (log.activeTimeGoalMinutes > 0) (log.activeTimeMinutes.toFloat() / log.activeTimeGoalMinutes.toFloat()).coerceIn(0f, 1f) else 0f
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = SoftGrayBorder,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = ActiveOrange,
                                            startAngle = -90f,
                                            sweepAngle = pct * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${log.activeTimeMinutes}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = InkColor
                                        )
                                        Text(
                                            text = "/${log.activeTimeGoalMinutes} min",
                                            fontSize = 10.sp,
                                            color = SoftTextMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Workout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ActiveOrange)
                            }
                        }
                    }
                }
            }

            // Quick Input Actions
            item {
                Column {
                    Text(
                        text = "Quick Logging Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddWater(250) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_water_250_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = NeonSportGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+250ml Water", fontSize = 11.sp, maxLines = 1)
                            }
                        }

                        Button(
                            onClick = { onAddCalories(350) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_calories_350_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = ActiveOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+350kcal Food", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddWater(500) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_water_500_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = NeonSportGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+500ml Shaker", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { onAddActiveMinutes(15) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_workout_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = BrightTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+15m Calisthenics", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Daily calorie/water history chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last 7 Days Balance (Cal / Water)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = InkColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (recentLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recent log data. Start logging!", color = SoftTextMuted, fontSize = 12.sp)
                            }
                        } else {
                            val maxCal = recentLogs.maxByOrNull { it.caloriesConsumed }?.caloriesConsumed?.toFloat() ?: 2500f
                            val maxWater = recentLogs.maxByOrNull { it.waterIntakeMl }?.waterIntakeMl?.toFloat() ?: 3000f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                recentLogs.reversed().forEach { prevLog ->
                                    val dateParts = prevLog.date.split("-")
                                    val label = if (dateParts.size == 3) "${dateParts[1]}/${dateParts[2]}" else prevLog.date
                                    
                                    val pctCal = if (maxCal > 0) prevLog.caloriesConsumed.toFloat() / maxCal else 0.1f
                                    val pctWat = if (maxWater > 0) prevLog.waterIntakeMl.toFloat() / maxWater else 0.1f

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.height(100.dp)
                                        ) {
                                            // Calorie Bar (Teal)
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .fillMaxHeight(pctCal.coerceIn(0.1f, 1.0f))
                                                    .background(BrightTeal, RoundedCornerShape(4.dp))
                                            )
                                            // Water Bar (Lime)
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .fillMaxHeight(pctWat.coerceIn(0.1f, 1.0f))
                                                    .background(NeonSportGreen, RoundedCornerShape(4.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(BrightTeal, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calories", fontSize = 11.sp, color = SoftTextMuted)
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(modifier = Modifier.size(8.dp).background(NeonSportGreen, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Water intake", fontSize = 11.sp, color = SoftTextMuted)
                            }
                        }
                    }
                }
            }

            // Goals room checklists
            item {
                Column {
                    Text(
                        text = "Goal Room Milestones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (goals.isEmpty()) {
                        Text(
                            text = "You don't have any fitness goals. Swipe to the Goals Room to pin some targets!",
                            fontSize = 12.sp,
                            color = SoftTextMuted
                        )
                    } else {
                        goals.forEach { goal ->
                            GoalMiniRow(goal = goal)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        } else if (activeMetricsSection == 1) {
            // MEAL LOGGING & FUEL DIARY
            val totalCals = loggedMeals.sumOf { it.calories }
            val totalProtein = loggedMeals.sumOf { it.protein }
            val totalCarbs = loggedMeals.sumOf { it.carbs }
            val totalFat = loggedMeals.sumOf { it.fat }

            val calGoal = currentLog?.caloriesGoal ?: 2000
            val pGoal = 135
            val cGoal = 220
            val fGoal = 70

            // Macronutrient Overview Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Today's Macronutrients Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Calories bar
                        Text(
                            text = "Energy: $totalCals / $calGoal kcal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrightTeal
                        )
                        LinearProgressIndicator(
                            progress = { if (calGoal > 0) (totalCals.toFloat() / calGoal).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BrightTeal,
                            trackColor = SoftGrayBorder
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Protein: ${totalProtein}g / ${pGoal}g", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ActiveOrange)
                                LinearProgressIndicator(
                                    progress = { (totalProtein.toFloat() / pGoal).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ActiveOrange,
                                    trackColor = SoftGrayBorder
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Carbs: ${totalCarbs}g / ${cGoal}g", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeonSportGreen)
                                LinearProgressIndicator(
                                    progress = { (totalCarbs.toFloat() / cGoal).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeonSportGreen,
                                    trackColor = SoftGrayBorder
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fats: ${totalFat}g / ${fGoal}g", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                LinearProgressIndicator(
                                    progress = { (totalFat.toFloat() / fGoal).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = SoftGrayBorder
                                )
                            }
                        }
                    }
                }
            }

            // Search Food preset DB
            item {
                var searchQuery by remember { mutableStateOf("") }
                var showCustomForm by remember { mutableStateOf(false) }

                var customName by remember { mutableStateOf("") }
                var customCals by remember { mutableStateOf("") }
                var customProtein by remember { mutableStateOf("") }
                var customCarbs by remember { mutableStateOf("") }
                var customFat by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Search & Log Food Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = InkColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search ingredients (e.g. Chicken, Rice)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("food_search_bar"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrightTeal,
                                focusedLabelColor = BrightTeal
                            )
                        )

                        val filteredPresets = if (searchQuery.isBlank()) {
                            foodPresets.take(3) // show some smart suggestions when empty
                        } else {
                            foodPresets.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }

                        if (filteredPresets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "Popular Items" else "Matching Search Results",
                                fontSize = 11.sp,
                                color = SoftTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredPresets.forEach { preset ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CarbonDarkBg, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(preset.icon, modifier = Modifier.padding(end = 6.dp))
                                            Column {
                                                Text(preset.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkColor)
                                                Text(
                                                    "${preset.calories} kcal | P: ${preset.protein}g | C: ${preset.carbs}g | F: ${preset.fat}g",
                                                    fontSize = 10.sp,
                                                    color = SoftTextMuted
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                onAddMeal(preset.name, preset.calories, preset.protein, preset.carbs, preset.fat)
                                                searchQuery = "" // Reset search bar
                                            },
                                            modifier = Modifier.size(36.dp).testTag("quick_add_food_btn")
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = "Add Food", tint = NeonSportGreen)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SoftGrayBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showCustomForm = !showCustomForm },
                            modifier = Modifier.fillMaxWidth().testTag("toggle_custom_meal_form_btn")
                        ) {
                            Icon(if (showCustomForm) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showCustomForm) "Close Custom Creator" else "Log Freeform Custom Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showCustomForm) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Meal description / Custom Food", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("custom_meal_name_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrightTeal,
                                    focusedLabelColor = BrightTeal
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = customCals,
                                    onValueChange = { customCals = it },
                                    label = { Text("Calories", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f).testTag("custom_meal_cals_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrightTeal,
                                        focusedLabelColor = BrightTeal
                                    )
                                )
                                OutlinedTextField(
                                    value = customProtein,
                                    onValueChange = { customProtein = it },
                                    label = { Text("Protein (g)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrightTeal,
                                        focusedLabelColor = BrightTeal
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = customCarbs,
                                    onValueChange = { customCarbs = it },
                                    label = { Text("Carbs (g)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrightTeal,
                                        focusedLabelColor = BrightTeal
                                    )
                                )
                                OutlinedTextField(
                                    value = customFat,
                                    onValueChange = { customFat = it },
                                    label = { Text("Fat (g)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrightTeal,
                                        focusedLabelColor = BrightTeal
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val cals = customCals.toIntOrNull() ?: 0
                                    val prot = customProtein.toIntOrNull() ?: 0
                                    val carb = customCarbs.toIntOrNull() ?: 0
                                    val fat = customFat.toIntOrNull() ?: 0
                                    if (customName.isNotBlank()) {
                                        onAddMeal(customName, cals, prot, carb, fat)
                                        customName = ""
                                        customCals = ""
                                        customProtein = ""
                                        customCarbs = ""
                                        customFat = ""
                                        showCustomForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("add_custom_meal_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightTeal, contentColor = CarbonDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Log Custom Plate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List of today's logged meals
            item {
                Text(
                    text = "Today's Logged Plates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = InkColor
                )
            }

            if (loggedMeals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlateSurface, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No food logged for today yet.", color = SoftTextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                loggedMeals.forEach { meal ->
                    item(key = meal.id) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepSlateSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, SoftGrayBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BrightTeal.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = BrightTeal)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(meal.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkColor)
                                    Text(
                                        "${meal.calories} kcal | Protein: ${meal.protein}g / Carbs: ${meal.carbs}g / Fat: ${meal.fat}g",
                                        fontSize = 11.sp,
                                        color = SoftTextMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteMeal(meal) },
                                modifier = Modifier.testTag("delete_meal_btn_${meal.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Meal", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        } else if (activeMetricsSection == 2) {
            // PROGRESS TRENDS & COMPREHENSIVE PERIODIC REPORTS
            item {
                var isMonthlyReport by remember { mutableStateOf(false) }
                val periodLogs = if (isMonthlyReport) last30Logs else last30Logs.take(7)

                // aggregates
                val avgCal = if (periodLogs.isNotEmpty()) periodLogs.map { it.caloriesConsumed }.average().roundToInt() else 0
                val avgWat = if (periodLogs.isNotEmpty()) periodLogs.map { it.waterIntakeMl }.average().roundToInt() else 0
                val totalMin = if (periodLogs.isNotEmpty()) periodLogs.sumOf { it.activeTimeMinutes } else 0
                val workoutDaysCount = if (periodLogs.isNotEmpty()) periodLogs.count { it.activeTimeMinutes > 0 } else 0

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Interval switch segment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlateSurface, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isMonthlyReport) BrightTeal else Color.Transparent)
                                .clickable { isMonthlyReport = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Weekly Breakdown", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (!isMonthlyReport) CarbonDarkBg else InkColor)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMonthlyReport) BrightTeal else Color.Transparent)
                                .clickable { isMonthlyReport = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Monthly Breakdown", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isMonthlyReport) CarbonDarkBg else InkColor)
                        }
                    }

                    // Key KPI Cards Grid
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isMonthlyReport) "Last 30 Days Ledger Overview" else "Last 7 Days Ledger Overview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = InkColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$avgCal kcal", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrightTeal)
                                    Text("Avg Daily Cal", fontSize = 10.sp, color = SoftTextMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${(avgWat / 1000.0).toString().take(3)} L", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NeonSportGreen)
                                    Text("Avg Water", fontSize = 10.sp, color = SoftTextMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$workoutDaysCount days", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ActiveOrange)
                                    Text("Workouts Done", fontSize = 10.sp, color = SoftTextMuted)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${totalMin}m", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text("Total Minutes", fontSize = 10.sp, color = SoftTextMuted)
                                }
                            }
                        }
                    }

                    // Gorgeous Progression Trend Graph (Canvas)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Progression Velocity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = InkColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (periodLogs.size < 2) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Generating telemetry curves...", color = SoftTextMuted, fontSize = 12.sp)
                                }
                            } else {
                                val logsSorted = periodLogs.reversed()
                                val maxVal = logsSorted.maxOf { it.caloriesConsumed }.toFloat().coerceAtLeast(2000f)
                                
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    val widthStep = size.width / (logsSorted.size - 1)
                                    val points = logsSorted.mapIndexed { idx, log ->
                                        val x = idx * widthStep
                                        val pct = log.caloriesConsumed.toFloat() / maxVal
                                        val y = size.height - (size.height * pct).coerceIn(10f, size.height - 10f)
                                        Offset(x, y)
                                    }

                                    // Draw line path
                                    val linePath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }

                                    // Fill gradient underneath
                                    val gradPath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(points[0].x, size.height)
                                        for (point in points) {
                                            lineTo(point.x, point.y)
                                        }
                                        lineTo(points.last().x, size.height)
                                        close()
                                    }

                                    drawPath(
                                        path = gradPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(BrightTeal.copy(alpha = 0.35f), Color.Transparent),
                                            startY = 0f,
                                            endY = size.height
                                        )
                                    )

                                    drawPath(
                                        path = linePath,
                                        color = BrightTeal,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Draw node dots
                                    for (point in points) {
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4.dp.toPx(),
                                            center = point
                                        )
                                        drawCircle(
                                            color = BrightTeal,
                                            radius = 2.dp.toPx(),
                                            center = point
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(logsSorted.first().date.drop(5), fontSize = 8.sp, color = SoftTextMuted)
                                    Text("Daily Calorie Trajectory", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SoftTextMuted)
                                    Text(logsSorted.last().date.drop(5), fontSize = 8.sp, color = SoftTextMuted)
                                }
                            }
                        }
                    }

                    // Improvements & Target Areas for Focus Reports Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = NeonSportGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Improvements Highlighted", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkColor)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // High Fidelity Dynamic logic analysis
                            val metWaterTargetDays = periodLogs.count { it.waterIntakeMl >= it.waterGoalMl }
                            val metCalTargetDays = periodLogs.count { it.caloriesConsumed <= it.caloriesGoal && it.caloriesConsumed > 0 }
                            
                            val listImprovements = mutableListOf<String>()
                            if (workoutDaysCount > 2) {
                                listImprovements.add("Strong exercise discipline: Logged workouts on $workoutDaysCount different days!")
                            } else {
                                listImprovements.add("Initial foundation set: Completed $totalMin total active minutes. Consistent loading of base moves triggers thermogenesis.")
                            }
                            if (metWaterTargetDays >= (periodLogs.size / 2)) {
                                listImprovements.add("Outstanding hydration habits: Cleared the 3.0-liter threshold on $metWaterTargetDays distinct calendar days.")
                            } else {
                                listImprovements.add("Your highest calorie burning day registered a massive active expenditure of 75+ kcal.")
                            }
                            if (avgCal in 1600..2300) {
                                listImprovements.add("Clean calorie balancing: Your daily intake averaged $avgCal kcal, displaying excellent alignment with muscular growth coefficients.")
                            }

                            listImprovements.take(2).forEach { BulletPointItem(text = it, typeIcon = Icons.Default.Check, tint = NeonSportGreen) }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = ActiveOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Precise Areas for Focus", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkColor)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            val listFocus = mutableListOf<String>()
                            if (avgWat < 2200) {
                                listFocus.add("Slightly low cell loading. Increase average daily fluid volume by 250ml to support high-performance electrolyte transport.")
                            }
                            if (workoutDaysCount < (periodLogs.size / 2)) {
                                listFocus.add("Muscular building frequency: Introduce short 15-minute conditioning modules on non-training weekdays to boost motor stamina.")
                            }
                            if (avgCal > 2400) {
                                listFocus.add("Thermogenic ceiling alert: Calorie ceiling exceeded on average. Focus on selecting high-protein presets like Grilled Chicken.")
                            }
                            if (listFocus.isEmpty()) {
                                listFocus.add("Maintain current optimal load indices. Increase barbell weight slowly by 2.5% on chest work to trigger progressive tension.")
                            }

                            listFocus.take(2).forEach { BulletPointItem(text = it, typeIcon = Icons.Default.Info, tint = ActiveOrange) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPointItem(text: String, typeIcon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = typeIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = SoftTextMuted,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun GoalMiniRow(goal: FitnessGoal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftGrayBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val progress = if (goal.targetValue > 0) (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat() else 0f
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonSportGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (goal.category) {
                        "Water" -> Icons.Default.WaterDrop
                        "Calories" -> Icons.Filled.LocalFireDepartment
                        else -> Icons.Default.TrackChanges
                    },
                    contentDescription = null,
                    tint = NeonSportGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = InkColor
                    )
                    Text(
                        text = "${goal.currentValue.roundToInt()}/${goal.targetValue.roundToInt()} ${goal.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonSportGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeonSportGreen,
                    trackColor = SoftGrayBorder,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}


// ==========================================
// TAB 2: GYM GUIDE & BODYBUILDING CATALOGUE
// ==========================================

data class WorkoutTemplate(
    val name: String,
    val level: String, // "Beginner", "Intermediate", "Advanced"
    val goal: String,  // "Weight Loss", "Muscle Gain", "Endurance"
    val durationMin: Int,
    val caloriesBurnt: Int,
    val description: String,
    val exercises: String,
    val icon: String = "⚡"
)

val workoutTemplates = listOf(
    WorkoutTemplate(
        name = "Express Slim Starter",
        level = "Beginner",
        goal = "Weight/Fat Loss",
        durationMin = 25,
        caloriesBurnt = 180,
        description = "Elevates heart rate with minimal joint load.",
        exercises = "Jumping Jacks (3x30s), Air Squats (3x12), Knee Push-ups (3x10)",
        icon = "🏃"
    ),
    WorkoutTemplate(
        name = "Alpha strength Basics",
        level = "Beginner",
        goal = "Muscle Building",
        durationMin = 30,
        caloriesBurnt = 200,
        description = "Classic compound lifting base for raw linear progress.",
        exercises = "Goblet Squats (3x10), Dumbbell Press (3x10), Kettlebell Rows (3x12)",
        icon = "🏋️"
    ),
    WorkoutTemplate(
        name = "Stamina Base Builder",
        level = "Beginner",
        goal = "Endurance",
        durationMin = 28,
        caloriesBurnt = 150,
        description = "Maintains optimal aerobic zone parameters.",
        exercises = "Steady Treadmill (15m), Air Bike (10m), Plank Holds (3x45s)",
        icon = "🚴"
    ),
    WorkoutTemplate(
        name = "HIIT Torch Hyper-Pulse",
        level = "Intermediate",
        goal = "Weight/Fat Loss",
        durationMin = 35,
        caloriesBurnt = 300,
        description = "EPOC thermogenesis. Rapid calorie burn cycles.",
        exercises = "Burpees (4x12), Kettlebell Swings (4x20), Mountain Climbers (4x30s)",
        icon = "🔥"
    ),
    WorkoutTemplate(
        name = "Hypertrophy Push-Pull Split",
        level = "Intermediate",
        goal = "Muscle Building",
        durationMin = 40,
        caloriesBurnt = 320,
        description = "High density layout with focused volume splits.",
        exercises = "Incline Bench (4x10), Lat Pulldowns (4x12), Barbell curls (3x12)",
        icon = "💪"
    ),
    WorkoutTemplate(
        name = "Dynamic Capacity Engine",
        level = "Intermediate",
        goal = "Endurance",
        durationMin = 45,
        caloriesBurnt = 250,
        description = "Improves glycogen conservation dynamic capacity.",
        exercises = "Rowing Intervals (15m), Jump Rope Loops (5x1m), Leg Raises (3x20)",
        icon = "📈"
    ),
    WorkoutTemplate(
        name = "Elite Thermogenic Carnage",
        level = "Advanced",
        goal = "Weight/Fat Loss",
        durationMin = 45,
        caloriesBurnt = 420,
        description = "Extreme metabolic complexes with complex barbells.",
        exercises = "Thrusters (5x12), Pull-ups (5xMax), Double Unders (5x100)",
        icon = "🦾"
    ),
    WorkoutTemplate(
        name = "Absolute Power Heavy Load",
        level = "Advanced",
        goal = "Muscle Building",
        durationMin = 50,
        caloriesBurnt = 450,
        description = "Heavy load compound structures for extreme muscle density.",
        exercises = "Heavy Squats (5x5), Bench Press (5x5), Heavy Deadlifts (5x3)",
        icon = "👑"
    ),
    WorkoutTemplate(
        name = "Iron-Lung Stamina Matrix",
        level = "Advanced",
        goal = "Endurance",
        durationMin = 60,
        caloriesBurnt = 500,
        description = "Elite athletic capacity parameters and VO2Max scaling.",
        exercises = "Assault Bike (20m), Row Machine (20m), Burpee Box Jumps (5x15)",
        icon = "🌪️"
    )
)

@Composable
fun GymGuideTab(
    exercises: List<WorkoutExercise>,
    activeMuscleFilter: String,
    onSelectMuscleFilter: (String) -> Unit,
    onCompleteSet: (Int, String) -> Unit,
    onAddCustomExercise: (String, String, String, String, String) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onCompleteTemplate: (String, Int, Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val muscles = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

    // Custom dialog states
    var nameState by remember { mutableStateOf("") }
    var muscleState by remember { mutableStateOf("Chest") }
    var instructionsState by remember { mutableStateOf("") }
    var difficultyState by remember { mutableStateOf("Beginner") }
    var setsRepsState by remember { mutableStateOf("4 sets of 10-12 reps") }

    var selectedLevelFilter by remember { mutableStateOf("All") }
    var selectedGoalFilter by remember { mutableStateOf("All") }
    var activeAppliedTemplateName by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bodybuilders Gym",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrightTeal
                    )
                    Text(
                        text = "Seed compound routines & track active minutes",
                        fontSize = 12.sp,
                        color = SoftTextMuted
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_custom_exercise_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Exercise", tint = CarbonDarkBg, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom", color = CarbonDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Muscle filter chips row
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                            .background(DeepSlateSurface)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏆", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Routine Templates Catalog",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = InkColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(BrightTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("SMART ROUTINES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = BrightTeal)
                            }
                        }
                        
                        Text(
                            "Select and complete certified workout blueprints matching your specific skill tier and training objectives.",
                            fontSize = 11.sp,
                            color = SoftTextMuted,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Level filter
                        Text("Skill Level Tier:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("All", "Beginner", "Intermediate", "Advanced").forEach { level ->
                                val isSel = selectedLevelFilter == level
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) BrightTeal else CarbonDarkBg, RoundedCornerShape(6.dp))
                                        .clickable { selectedLevelFilter = level }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        level,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) CarbonDarkBg else InkColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Goal filter
                        Text("Training Target:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("All", "Weight/Fat Loss", "Muscle Building", "Endurance").forEach { goal ->
                                val isSel = selectedGoalFilter == goal
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) BrightTeal else CarbonDarkBg, RoundedCornerShape(6.dp))
                                        .clickable { selectedGoalFilter = goal }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        goal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) CarbonDarkBg else InkColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        val filteredTemplates = workoutTemplates.filter { t ->
                            (selectedLevelFilter == "All" || t.level == selectedLevelFilter) &&
                            (selectedGoalFilter == "All" || t.goal == selectedGoalFilter)
                        }
                        
                        if (filteredTemplates.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CarbonDarkBg, RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No templates match the active filters.", color = SoftTextMuted, fontSize = 11.sp)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                filteredTemplates.forEach { template ->
                                    Card(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .border(1.dp, SoftGrayBorder, RoundedCornerShape(12.dp))
                                            .testTag("template_card_${template.name.replace(" ", "_").lowercase()}"),
                                        colors = CardDefaults.cardColors(containerColor = CarbonDarkBg)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(template.icon, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(template.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkColor)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Difficulty Badge
                                                val lvlColor = when (template.level) {
                                                    "Beginner" -> NeonSportGreen
                                                    "Intermediate" -> ActiveOrange
                                                    else -> Color.Red
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(lvlColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, lvlColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(template.level, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = lvlColor)
                                                }
                                                
                                                // Goal Badge
                                                Box(
                                                    modifier = Modifier
                                                        .background(BrightTeal.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(template.goal, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrightTeal)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(template.description, fontSize = 10.sp, color = SoftTextMuted, minLines = 2)
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = SoftGrayBorder.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text("Routine Elements:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = InkColor)
                                            Text(template.exercises, fontSize = 9.sp, color = SoftTextMuted, lineHeight = 11.sp, minLines = 2)
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Time: ${template.durationMin} min", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkColor)
                                                    Text("Burn: ${template.caloriesBurnt} kcal", fontSize = 9.sp, color = NeonSportGreen, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        onCompleteTemplate(template.name, template.durationMin, template.caloriesBurnt)
                                                        activeAppliedTemplateName = template.name
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonSportGreen),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .testTag("apply_template_btn_${template.name.replace(" ", "_").lowercase()}")
                                                ) {
                                                    Text("LOG ROUTINE", color = CarbonDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        activeAppliedTemplateName?.let { templateName ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonSportGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, NeonSportGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonSportGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Logged: $templateName (+Minutes & -Calories)!",
                                        color = InkColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { activeAppliedTemplateName = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SoftTextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Column {
                        Text("Target Muscle Group", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            muscles.take(4).forEach { muscle ->
                                MuscleChip(
                                    muscle = muscle,
                                    isSelected = activeMuscleFilter == muscle,
                                    onClick = { onSelectMuscleFilter(muscle) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            muscles.drop(4).forEach { muscle ->
                                MuscleChip(
                                    muscle = muscle,
                                    isSelected = activeMuscleFilter == muscle,
                                    onClick = { onSelectMuscleFilter(muscle) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Muscle Builders Routine Database",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )
                }

                if (exercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No exercises in this category.", color = SoftTextMuted)
                        }
                    }
                } else {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onCompleteSet = { onCompleteSet(exercise.caloriesBurntPerSet, exercise.setsReps) },
                            onDelete = if (exercise.isCustom) { { onDeleteExercise(exercise.id) } } else null
                        )
                    }
                }
            }
        }

        // Add Custom Exercise Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add Custom Lift",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = BrightTeal
                        )

                        OutlinedTextField(
                            value = nameState,
                            onValueChange = { nameState = it },
                            label = { Text("Exercise Name (e.g. Incline Bench)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrightTeal,
                                focusedLabelColor = BrightTeal
                            )
                        )

                        Text("Select Target Muscle Target", fontSize = 12.sp, color = InkColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Chest", "Back", "Legs", "Shoulders").forEach { muscle ->
                                val isSelected = muscleState == muscle
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) BrightTeal else SoftGrayBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { muscleState = muscle }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = muscle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) CarbonDarkBg else InkColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Arms", "Core").forEach { muscle ->
                                val isSelected = muscleState == muscle
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) BrightTeal else SoftGrayBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { muscleState = muscle }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = muscle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) CarbonDarkBg else InkColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = setsRepsState,
                            onValueChange = { setsRepsState = it },
                            label = { Text("Suggested Sets & Reps") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrightTeal,
                                focusedLabelColor = BrightTeal
                            )
                        )

                        OutlinedTextField(
                            value = instructionsState,
                            onValueChange = { instructionsState = it },
                            label = { Text("Lift execution guide") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrightTeal,
                                focusedLabelColor = BrightTeal
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Difficulty Level:", fontSize = 12.sp, color = InkColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Beginner", "Intermediate", "Advanced").forEach { diff ->
                                    val isSelected = difficultyState == diff
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (isSelected) ActiveOrange else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(SoftGrayBorder, RoundedCornerShape(8.dp))
                                            .clickable { difficultyState = diff }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = diff,
                                            fontSize = 9.sp,
                                            color = if (isSelected) ActiveOrange else InkColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = SoftTextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (nameState.isNotBlank()) {
                                        onAddCustomExercise(
                                            nameState,
                                            muscleState,
                                            instructionsState,
                                            difficultyState,
                                            setsRepsState
                                        )
                                        // Reset fields
                                        nameState = ""
                                        instructionsState = ""
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightTeal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_exercise_btn")
                            ) {
                                Text("Save Exercise", color = CarbonDarkBg)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleChip(
    muscle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) BrightTeal else SoftGrayBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = muscle,
            fontSize = 11.sp,
            color = if (isSelected) CarbonDarkBg else InkColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    onCompleteSet: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftGrayBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(BrightTeal.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = exercise.targetMuscle,
                                fontSize = 9.sp,
                                color = BrightTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(ActiveOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = exercise.difficulty,
                                fontSize = 9.sp,
                                color = ActiveOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCompleteSet,
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonSportGreen.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Log Set Completed",
                            tint = NeonSportGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Custom Exercise",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(color = SoftGrayBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suggested Workout: ${exercise.setsReps}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonSportGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.instructions,
                        fontSize = 11.sp,
                        color = SoftTextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔬 Completing this routine spends approx ${exercise.caloriesBurntPerSet * 4} active calories.",
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = BrightTeal
                    )
                }
            }
        }
    }
}


// ==========================================
// TAB 3: AI BEAUTY & SUPPLEMENT ANALYZER
// ==========================================

@Composable
fun AiAnalyzerTab(
    analyzedProducts: List<BodyProduct>,
    isAnalyzing: Boolean,
    analysisError: String?,
    onAnalyze: (String, String, String) -> Unit,
    onDeleteProduct: (Int) -> Unit
) {
    var pName by remember { mutableStateOf("") }
    var pCategory by remember { mutableStateOf("Supplement") }
    var pIngredients by remember { mutableStateOf("") }
    
    // For collapsing/expanding historical analyses
    var expandedAnalysisId by remember { mutableStateOf<Int?>(null) }
    
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI Body Suitability Matcher",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonSportGreen
                )
                Text(
                    text = "Analyze ingredients of skin, shower products, supplements, or proteins with Gemini AI to safeguard health & muscle synthesis",
                    fontSize = 11.sp,
                    color = SoftTextMuted
                )
            }
        }

        // Search Input Terminal Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "New Product Analysis Terminal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )

                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Product name (e.g. Hydrolyzed Whey, Clear Soap)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonSportGreen,
                            focusedLabelColor = NeonSportGreen
                        )
                    )

                    Text("Product Formulation Category", fontSize = 12.sp, color = InkColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf("Supplement", "Skincare", "Bodywash", "Other").forEach { cat ->
                            val isSelected = pCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NeonSportGreen else SoftGrayBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { pCategory = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    color = if (isSelected) CarbonDarkBg else InkColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pIngredients,
                        onValueChange = { pIngredients = it },
                        label = { Text("Ingredients List (Separate with commas, copy from label)") },
                        placeholder = { Text("e.g. Creatine monohydrate; or Triclosan, Parabens, Sodium Laureth Sulfate (SLS)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("ingredients_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonSportGreen,
                            focusedLabelColor = NeonSportGreen
                        )
                    )

                    // Error display
                    if (analysisError != null) {
                        Text(
                            text = "⚠️ $analysisError",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onAnalyze(pName, pCategory, pIngredients)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonSportGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_btn"),
                        enabled = !isAnalyzing && pName.isNotBlank() && pIngredients.isNotBlank()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CarbonDarkBg, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini is dissecting formulas...", color = CarbonDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = CarbonDarkBg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Gemini AI Suitability", color = CarbonDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Simple Preset Clicker helper for testing empty-state flows
                    PresetHelpers(
                        onSelectPreset = { name, cat, ingredients ->
                            pName = name
                            pCategory = cat
                            pIngredients = ingredients
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "History of Analyzed Formulations",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = InkColor
            )
        }

        if (analyzedProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No product logs yet. Submit a formulation above to dissect ingredients!", color = SoftTextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(analyzedProducts, key = { it.id }) { product ->
                val isExpanded = expandedAnalysisId == product.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            expandedAnalysisId = if (isExpanded) null else product.id
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = InkColor
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .background(NeonSportGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = product.category,
                                        fontSize = 9.sp,
                                        color = NeonSportGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = SoftTextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteProduct(product.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete analyzed product",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = SoftGrayBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Ingredients:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrightTeal
                            )
                            Text(
                                text = product.ingredients,
                                fontSize = 11.sp,
                                color = SoftTextMuted,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🤖 Gemini AI Evaluation:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeonSportGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonDarkBg),
                                modifier = Modifier.fillMaxWidth().border(1.dp, SoftGrayBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = product.analysisResult,
                                    fontSize = 11.sp,
                                    color = InkColor,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetHelpers(onSelectPreset: (String, String, String) -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Try sample presets formulation:", fontSize = 10.sp, color = SoftTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, SoftGrayBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        onSelectPreset(
                            "Mass Supplement Blend",
                            "Supplement",
                            "Creatine monohydrate, Beta-alanine, Soy Lecithin, Maltodextrin, Sucralose"
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Supplement Blend", fontSize = 9.sp, color = BrightTeal, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .border(1.dp, SoftGrayBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        onSelectPreset(
                            "Standard Gym Bodywash",
                            "Bodywash",
                            "Water, Triclosan, Methylparaben, Sodium Laureth Sulfate (SLS), Synthetic Fragrance"
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Hormonal Bodywash", fontSize = 9.sp, color = ActiveOrange, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// TAB 4: GOAL BOARD & STRATEGY MILIEU
// ==========================================

@Composable
fun GoalsRoomTab(
    goals: List<FitnessGoal>,
    onAddGoal: (String, Double, String, String) -> Unit,
    onUpdateProgress: (Int, Double) -> Unit,
    onDeleteGoal: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var titleState by remember { mutableStateOf("") }
    var targetState by remember { mutableStateOf("") }
    var unitState by remember { mutableStateOf("kg") }
    var catState by remember { mutableStateOf("Weight") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "The Goal Board",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonSportGreen
                    )
                    Text(
                        text = "Formulate targets, drag performance levers",
                        fontSize = 12.sp,
                        color = SoftTextMuted
                    )
                }

                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonSportGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pin_goal_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Goal", tint = CarbonDarkBg, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pin New Target", color = CarbonDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(48.dp), tint = SoftTextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No goals pinned. Add some bodybuilding benchmarks!", color = SoftTextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(goals, key = { it.id }) { goal ->
                        InteractiveGoalCard(
                            goal = goal,
                            onUpdate = { onUpdateProgress(goal.id, it) },
                            onDelete = { onDeleteGoal(goal.id) }
                        )
                    }
                }
            }
        }

        // Add Target Dialog
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Pin New Benchmarks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NeonSportGreen
                        )

                        OutlinedTextField(
                            value = titleState,
                            onValueChange = { titleState = it },
                            label = { Text("Goal Title (e.g. Bench press weight)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonSportGreen,
                                focusedLabelColor = NeonSportGreen
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetState,
                                onValueChange = { targetState = it },
                                label = { Text("Target Value") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_target_value_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonSportGreen,
                                    focusedLabelColor = NeonSportGreen
                                )
                            )

                            OutlinedTextField(
                                value = unitState,
                                onValueChange = { unitState = it },
                                label = { Text("Unit (e.g., kg, ml, rep)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_unit_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonSportGreen,
                                    focusedLabelColor = NeonSportGreen
                                )
                            )
                        }

                        Text("Performance Metric Category", fontSize = 12.sp, color = InkColor)
                        val categories = listOf("Weight", "Workout", "Calories", "Water")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.take(2).forEach { category ->
                                    val isSelected = catState == category
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isSelected) NeonSportGreen else SoftGrayBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) NeonSportGreen.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { catState = category }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeonSportGreen else InkColor)
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.drop(2).forEach { category ->
                                    val isSelected = catState == category
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isSelected) NeonSportGreen else SoftGrayBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) NeonSportGreen.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { catState = category }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeonSportGreen else InkColor)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel", color = SoftTextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val dTarget = targetState.toDoubleOrNull() ?: 0.0
                                    if (titleState.isNotBlank() && dTarget > 0.0) {
                                        onAddGoal(titleState, dTarget, unitState, catState)
                                        // Reset fields
                                        titleState = ""
                                        targetState = ""
                                        showDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonSportGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_goal_btn")
                            ) {
                                Text("Pin Target", color = CarbonDarkBg)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveGoalCard(
    goal: FitnessGoal,
    onUpdate: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var sliderVal by remember(goal.currentValue) { mutableStateOf(goal.currentValue.toFloat()) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftGrayBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonSportGreen.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (goal.category) {
                                "Water" -> Icons.Default.WaterDrop
                                "Calories" -> Icons.Filled.LocalFireDepartment
                                else -> Icons.Default.TrackChanges
                            },
                            contentDescription = null,
                            tint = NeonSportGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = goal.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkColor
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Status",
                    fontSize = 11.sp,
                    color = SoftTextMuted
                )
                Text(
                    text = "${sliderVal.roundToInt()} / ${goal.targetValue.roundToInt()} ${goal.unit}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonSportGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = sliderVal,
                onValueChange = { newVal ->
                    sliderVal = newVal
                },
                onValueChangeFinished = {
                    onUpdate(sliderVal.toDouble())
                },
                valueRange = 0f..goal.targetValue.toFloat().coerceAtLeast(10f),
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonSportGreen,
                    inactiveTrackColor = SoftGrayBorder,
                    thumbColor = NeonSportGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
