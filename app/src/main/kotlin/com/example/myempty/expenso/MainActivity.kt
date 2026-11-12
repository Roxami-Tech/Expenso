package com.example.myempty.expenso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -----------------------------------------------------------------------------
// EXPENSO — by Roxami Tech
// One-file starter implementation. Room entities, DAO, database, ViewModel,
// navigation and Compose screens are all kept in this file for convenience.
// -----------------------------------------------------------------------------

private val Purple = Color(0xFF8B5CF6)
private val Lavender = Color(0xFFC4B5FD)
private val Dark = Color(0xFF171329)
private val AppBackground = Color(0xFFFAF9FC)
private val IncomeGreen = Color(0xFF22A06B)
private val ExpenseCoral = Color(0xFFF87171)
private val Muted = Color(0xFF888398)
private val CardWhite = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = ExpensoDatabase.getInstance(applicationContext)
        val factory = ExpensoViewModelFactory(database)
        setContent {
            ExpensoTheme {
                val vm: ExpensoViewModel = viewModel(factory = factory)
                ExpensoApp(viewModel = vm)
            }
        }
    }
}

// Room data model --------------------------------------------------------------

@Entity(tableName = "transactions")
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String, // "INCOME" or "EXPENSE"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class CategoryBudget(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Dao
interface ExpensoDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<MoneyTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: MoneyTransaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun observeBudgets(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudget(budget: CategoryBudget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)
}

@Database(
    entities = [MoneyTransaction::class, CategoryBudget::class],
    version = 1,
    exportSchema = false
)
abstract class ExpensoDatabase : RoomDatabase() {
    abstract fun expensoDao(): ExpensoDao

    companion object {
        @Volatile private var INSTANCE: ExpensoDatabase? = null

        fun getInstance(context: android.content.Context): ExpensoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpensoDatabase::class.java,
                    "expenso_database"
                ).build().also { INSTANCE = it }
            }
    }
}

// ViewModel --------------------------------------------------------------------

data class ExpensoUiState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList()
) {
    val income: Double get() = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenses: Double get() = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance: Double get() = income - expenses
}

class ExpensoViewModel(private val dao: ExpensoDao) : ViewModel() {
    val uiState: Flow<ExpensoUiState> =
        combine(dao.observeTransactions(), dao.observeBudgets()) { transactions, budgets ->
            ExpensoUiState(transactions, budgets)
        }

    fun addTransaction(
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String
    ) {
        viewModelScope.launch {
            dao.insertTransaction(
                MoneyTransaction(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    type = type,
                    note = note.trim()
                )
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { dao.deleteTransaction(id) }
    }

    fun saveBudget(category: String, amount: Double) {
        viewModelScope.launch { dao.saveBudget(CategoryBudget(category, amount)) }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch { dao.deleteBudget(category) }
    }
}

class ExpensoViewModelFactory(private val database: ExpensoDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensoViewModel::class.java)) {
            return ExpensoViewModel(database.expensoDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// App theme --------------------------------------------------------------------

@Composable
fun ExpensoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            secondary = Lavender,
            background = AppBackground,
            surface = CardWhite,
            onPrimary = Color.White,
            onBackground = Dark,
            onSurface = Dark
        ),
        typography = Typography(),
        content = content
    )
}

// Navigation -------------------------------------------------------------------

private enum class AppTab(val label: String) {
    HOME("Home"),
    TRANSACTIONS("Activity"),
    BUDGETS("Budgets"),
    INSIGHTS("Insights")
}

@Composable
fun ExpensoApp(viewModel: ExpensoViewModel) {
    val state by viewModel.uiState.collectAsState(initial = ExpensoUiState())
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Purple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.HOME,
                    onClick = { selectedTab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TRANSACTIONS,
                    onClick = { selectedTab = AppTab.TRANSACTIONS },
                    icon = { Icon(Icons.Default.ReceiptLong, null) },
                    label = { Text("Activity") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.BUDGETS,
                    onClick = { selectedTab = AppTab.BUDGETS },
                    icon = { Icon(Icons.Default.Savings, null) },
                    label = { Text("Budgets") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.INSIGHTS,
                    onClick = { selectedTab = AppTab.INSIGHTS },
                    icon = { Icon(Icons.Default.Analytics, null) },
                    label = { Text("Insights") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    state = state,
                    onAddClick = { showAddDialog = true },
                    onSeeAll = { selectedTab = AppTab.TRANSACTIONS }
                )
                AppTab.TRANSACTIONS -> TransactionsScreen(
                    transactions = state.transactions,
                    onDelete = viewModel::deleteTransaction
                )
                AppTab.BUDGETS -> BudgetsScreen(
                    state = state,
                    onAddBudget = { showBudgetDialog = true },
                    onDeleteBudget = viewModel::deleteBudget
                )
                AppTab.INSIGHTS -> InsightsScreen(state)
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, type, note ->
                viewModel.addTransaction(title, amount, category, type, note)
                showAddDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        AddBudgetDialog(
            existingCategories = state.budgets.map { it.category },
            onDismiss = { showBudgetDialog = false },
            onSave = { category, amount ->
                viewModel.saveBudget(category, amount)
                showBudgetDialog = false
            }
        )
    }
}

// Home -------------------------------------------------------------------------

@Composable
private fun HomeScreen(
    state: ExpensoUiState,
    onAddClick: () -> Unit,
    onSeeAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("EXPENSO", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Good day 👋", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Dark)
                    Text("Every Expense, Under Control.", color = Muted, fontSize = 13.sp)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Dark)
                }
            }
        }
        item {
            BalanceCard(state.balance)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Income",
                    amount = state.income,
                    color = IncomeGreen,
                    icon = Icons.Default.ArrowDownward
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    amount = state.expenses,
                    color = ExpenseCoral,
                    icon = Icons.Default.ArrowUpward
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("See all", color = Purple) }
            }
        }
        if (state.transactions.isEmpty()) {
            item { EmptyState("No transactions yet", "Tap + to add your first income or expense.") }
        } else {
            items(state.transactions.take(5), key = { it.id }) { transaction ->
                TransactionRow(transaction)
            }
        }
        item {
            Text(
                "A ROXAMI TECH PRODUCT",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Dark)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("TOTAL BALANCE", color = Lavender, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))
            Text(money(balance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Lavender))
                Spacer(Modifier.width(7.dp))
                Text("Your finances at a glance", color = Color(0xFFD7D0E8), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(money(amount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Dark)
        }
    }
}

// Transactions -----------------------------------------------------------------

@Composable
private fun TransactionsScreen(
    transactions: List<MoneyTransaction>,
    onDelete: (Long) -> Unit
) {
    var filter by remember { mutableStateOf("ALL") }
    val visible = transactions.filter { filter == "ALL" || it.type == filter }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text("Activity", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Your income and spending history", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "EXPENSE", "INCOME").forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(if (item == "ALL") "All" else item.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple.copy(alpha = .14f), selectedLabelColor = Purple)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (visible.isEmpty()) {
            EmptyState("Nothing to show", "Your transactions will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.id }) { transaction ->
                    TransactionRow(transaction, onDelete = { onDelete(transaction.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: MoneyTransaction,
    onDelete: (() -> Unit)? = null
) {
    val isIncome = transaction.type == "INCOME"
    val color = if (isIncome) IncomeGreen else ExpenseCoral
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isIncome) Icons.Default.ArrowDownward else Icons.Default.Category, null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${transaction.category} • ${formatDate(transaction.createdAt)}", color = Muted, fontSize = 11.sp)
                if (transaction.note.isNotBlank()) Text(transaction.note, color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "−") + money(transaction.amount),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(28.dp)) {
                        Text("Delete", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// Budgets ----------------------------------------------------------------------

@Composable
private fun BudgetsScreen(
    state: ExpensoUiState,
    onAddBudget: () -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    val expenseByCategory = state.transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Budgets", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("Set limits and stay on track", color = Muted, fontSize = 13.sp)
                }
                FilledTonalIconButton(onClick = onAddBudget) {
                    Icon(Icons.Default.Add, contentDescription = "Add budget", tint = Purple)
                }
            }
        }
        if (state.budgets.isEmpty()) {
            item { EmptyState("No budgets yet", "Create a category budget to start tracking your limits.") }
        } else {
            items(state.budgets, key = { it.category }) { budget ->
                val spent = expenseByCategory[budget.category] ?: 0.0
                val progress = (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(budget.category, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${money(spent)} / ${money(budget.limitAmount)}", fontSize = 12.sp, color = Muted)
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (spent > budget.limitAmount) ExpenseCoral else Purple,
                            trackColor = Lavender.copy(alpha = .25f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (spent > budget.limitAmount) "Over budget by ${money(spent - budget.limitAmount)}"
                                else "${money((budget.limitAmount - spent).coerceAtLeast(0.0))} remaining",
                                color = if (spent > budget.limitAmount) ExpenseCoral else Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onDeleteBudget(budget.category) }) {
                                Text("Remove", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBudgetDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var category by remember { mutableStateOf("Food & Dining") }
    var amount by remember { mutableStateOf("") }
    val categories = expenseCategories
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category", color = Muted, fontSize = 12.sp)
                CategorySelector(category, categories) { category = it }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Budget limit (AED)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                if (category in existingCategories) {
                    Text("Saving will update this category's budget.", color = Muted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { onSave(category, it) } },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// Insights ---------------------------------------------------------------------

@Composable
private fun InsightsScreen(state: ExpensoUiState) {
    val expenses = state.transactions.filter { it.type == "EXPENSE" }
    val totals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList().sortedByDescending { it.second }
    val total = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Insights", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Understand where your money goes", color = Muted, fontSize = 13.sp)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Dark)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Lavender)
                        Spacer(Modifier.width(8.dp))
                        Text("Your spending overview", color = Lavender, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(money(total), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Total expenses recorded", color = Color(0xFFD7D0E8), fontSize = 12.sp)
                }
            }
        }
        if (totals.isEmpty()) {
            item { EmptyState("Insights will appear here", "Add expenses in a few categories to see your spending breakdown.") }
        } else {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Spending by category", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(16.dp))
                        SpendingDonut(totals = totals, total = total)
                        Spacer(Modifier.height(12.dp))
                        totals.forEachIndexed { index, pair ->
                            val share = if (total > 0) pair.second / total else 0.0
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(chartColors[index % chartColors.size]))
                                Spacer(Modifier.width(9.dp))
                                Text(pair.first, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                Text("${(share * 100).toInt()}%", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(money(pair.second), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item {
                val top = totals.first()
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Purple.copy(alpha = .10f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Purple)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Expenso tip", fontWeight = FontWeight.Bold, color = Dark)
                            Text(
                                "${top.first} is your largest recorded spending category at ${money(top.second)}. Review this category to find opportunities to save.",
                                color = Dark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingDonut(totals: List<Pair<String, Double>>, total: Double) {
    val colors = chartColors
    Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(174.dp)) {
            var startAngle = -90f
            totals.forEachIndexed { index, pair ->
                val sweep = if (total > 0) (pair.second / total * 360.0).toFloat() else 0f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOTAL SPENT", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(money(total), color = Dark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Dialogs and reusable components ---------------------------------------------

private val expenseCategories = listOf(
    "Food & Dining", "Shopping", "Transport", "Bills & Utilities",
    "Entertainment", "Health", "Education", "Travel", "Other"
)
private val incomeCategories = listOf(
    "Salary", "Freelance", "Business", "Investment", "Gift", "Other"
)
private val chartColors = listOf(
    Purple, Color(0xFF67C7C2), ExpenseCoral, Color(0xFF6C8EE3),
    Color(0xFFB77AE8), Color(0xFFF2B84B), Color(0xFF54A88B)
)

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(expenseCategories.first()) }
    var note by remember { mutableStateOf("") }
    val categories = if (type == "EXPENSE") expenseCategories else incomeCategories

    LaunchedEffect(type) { category = categories.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseCoral.copy(alpha = .14f), selectedLabelColor = ExpenseCoral)
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = .14f), selectedLabelColor = IncomeGreen)
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Grocery shopping") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (AED)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Category", color = Muted, fontSize = 12.sp)
                CategorySelector(category, categories) { category = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (title.isNotBlank() && parsed != null && parsed > 0) {
                        onSave(title, parsed, category, type, note)
                    }
                },
                enabled = title.isNotBlank() && (amount.toDoubleOrNull()?.let { it > 0 } == true)
            ) { Text("Save transaction") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategorySelector(
    selected: String,
    categories: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Purple.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Purple, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(message, color = Muted, fontSize = 12.sp)
    }
}

// Formatting -------------------------------------------------------------------

private fun money(value: Double): String =
    "AED " + String.format(Locale.US, "%,.2f", value)

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
