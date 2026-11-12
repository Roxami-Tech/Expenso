package com.example.myempty.expenso

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ExpensoDao_Impl(
  __db: RoomDatabase,
) : ExpensoDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMoneyTransaction: EntityInsertAdapter<MoneyTransaction>

  private val __insertAdapterOfCategoryBudget: EntityInsertAdapter<CategoryBudget>
  init {
    this.__db = __db
    this.__insertAdapterOfMoneyTransaction = object : EntityInsertAdapter<MoneyTransaction>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `transactions` (`id`,`title`,`amount`,`category`,`type`,`note`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MoneyTransaction) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.category)
        statement.bindText(5, entity.type)
        statement.bindText(6, entity.note)
        statement.bindLong(7, entity.createdAt)
      }
    }
    this.__insertAdapterOfCategoryBudget = object : EntityInsertAdapter<CategoryBudget>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `budgets` (`category`,`limitAmount`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryBudget) {
        statement.bindText(1, entity.category)
        statement.bindDouble(2, entity.limitAmount)
      }
    }
  }

  public override suspend fun insertTransaction(transaction: MoneyTransaction): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMoneyTransaction.insert(_connection, transaction)
  }

  public override suspend fun saveBudget(budget: CategoryBudget): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfCategoryBudget.insert(_connection, budget)
  }

  public override fun observeTransactions(): Flow<List<MoneyTransaction>> {
    val _sql: String = "SELECT * FROM transactions ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<MoneyTransaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: MoneyTransaction
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              MoneyTransaction(_tmpId,_tmpTitle,_tmpAmount,_tmpCategory,_tmpType,_tmpNote,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeBudgets(): Flow<List<CategoryBudget>> {
    val _sql: String = "SELECT * FROM budgets ORDER BY category ASC"
    return createFlow(__db, false, arrayOf("budgets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _result: MutableList<CategoryBudget> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryBudget
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_columnIndexOfLimitAmount)
          _item = CategoryBudget(_tmpCategory,_tmpLimitAmount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTransaction(transactionId: Long) {
    val _sql: String = "DELETE FROM transactions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, transactionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBudget(category: String) {
    val _sql: String = "DELETE FROM budgets WHERE category = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
