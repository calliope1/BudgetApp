package com.example.budgetapp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class Expense(var id: String? = null, val amount: Double, val description: String, val date: String)

sealed class UiState {
    object Loading : UiState()
    data class Success(val expenses: List<Expense>, val remainingBudget: Double, val weeklyTotal: Double) : UiState()
    data class Error(val message: String) : UiState()
}

class ExpenseViewModel : ViewModel() {

    private val client = OkHttpClient()

    private val SERVER_URL = BuildConfig.SERVER_URL
    private val SHARED_SECRET = BuildConfig.SHARED_SECRET
    private var weeklyBudget: Double = 110.0
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        fetchExpenses()
    }

    private suspend fun fetchWeeklyBudgetFromServer(): Double = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$SERVER_URL/budget")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to load budget: ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty budget response")
            val json = JSONObject(body)
            return@withContext json.getDouble("weekly_budget")
        }
    }


    fun fetchExpenses() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                weeklyBudget = fetchWeeklyBudgetFromServer()
                val expenses = fetchExpensesFromServer()
                val weeklyTotal = calculateWeeklyTotal(expenses)
                val remaining = weeklyBudget - weeklyTotal
                _uiState.postValue(UiState.Success(expenses, remaining, weeklyTotal))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error("Error fetching expenses: ${e.message}"))
            }
        }
    }

    private suspend fun fetchExpensesFromServer(): List<Expense> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$SERVER_URL/expenses")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to load: ${resp.code}")
            val body = resp.body?.string() ?: "[]"
            parseExpenses(body)
        }
    }

    private fun parseExpenses(jsonString: String): List<Expense> {
        val expenses = mutableListOf<Expense>()
        val arr = JSONArray(jsonString)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // Assuming server might also return an ID
            val id = o.getString("id")
            val amount = o.getDouble("amount")
            val desc = o.getString("description")
            val date = o.getString("date")
            expenses.add(Expense(id, amount, desc, date))
        }
        return expenses
    }

    fun addExpense(amount: Double, description: String, date: LocalDate) {
        viewModelScope.launch {
            try {
                val newExpense = Expense(amount = amount, description = description, date = date.format(dateFormatter))
                newExpense.id = postExpenseToServer(newExpense)
                _toastMessage.postValue("Expense added successfully")
                fetchExpenses() // Refresh the list
            } catch (e: Exception) {
                _toastMessage.postValue("Error adding expense: ${e.message}")
            }
        }
    }

    private suspend fun postExpenseToServer(expense: Expense) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject().apply {
            put("amount", expense.amount)
            put("description", expense.description)
            put("date", expense.date)
        }
        val jsonString = jsonObject.toString()
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val signature = hmacHex(SHARED_SECRET, jsonString)

        val req = Request.Builder()
            .url("$SERVER_URL/expenses")
            .post(body)
            .addHeader("X-Signature", signature)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string()
                throw IOException("Failed to add expense: ${resp.code} - $errorBody")
            }

            val bodyString = resp.body?.string()
            if (bodyString == null) {
                throw IOException("Empty body message")
            }
            val responseJson = JSONObject(bodyString)

            responseJson.getJSONObject("expense").getString("id")

        }
    }

    fun updateExpense(expenseId: String, amount: Double, description: String, date: String) {
        viewModelScope.launch {
            try {
                patchExpenseToServer(expenseId, amount, description, date)
                _toastMessage.postValue("Expense updated successfully")
                fetchExpenses()
            } catch (e: Exception) {
                Log.e("PATCH_ERROR","Exception: ${e.message}")
                _toastMessage.postValue("Error updating expense: ${e.message}")
            }
        }
    }

    private suspend fun patchExpenseToServer(expenseId: String, amount: Double, description: String, date: String) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject().apply {
            put("amount", amount)
            put("description", description)
            put("date", date)
        }
        val jsonString = jsonObject.toString()
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val signature = hmacHex(SHARED_SECRET, jsonString)
        Log.d("PATCH_DEBUG","SIG: $signature")

        Log.d("PATCH_DEBUG", "URL: $SERVER_URL/expenses/${expenseId}")

        val req = Request.Builder()
            .url("$SERVER_URL/expenses/${expenseId}")
            .method("PATCH",body)
            .addHeader("X-Signature", signature)
            .build()

        Log.d("PATCH_DEBUG","REQ: ${req.toString()}")

        client.newCall(req).execute().use { resp ->
            Log.d("PATCH_DEBUG","RESP: ${resp.code}")
            Log.d("PATCH_DEBUG","RESP: ${resp.body?.string()}")
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string()
                throw IOException("Failed to update expense: ${resp.code} - $errorBody")
            }
        }
    }

    fun deleteExpense(expenseId: String?) {
        if (expenseId == null) {
            throw Exception("Expense has no id")
        }
        viewModelScope.launch {
            try {
                deleteExpenseToServer(expenseId)
                _toastMessage.postValue("Expense deleted")
                fetchExpenses()
            } catch (e: Exception) {
                _toastMessage.postValue("Error deleting expense: ${e.message}")
            }
        }
    }

    private suspend fun deleteExpenseToServer(expenseId: String) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject().apply {
            put("id", expenseId)
        }
        val jsonString = jsonObject.toString()
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val signature = hmacHex(SHARED_SECRET, jsonString)

        val req = Request.Builder()
            .url("$SERVER_URL/expenses/$expenseId")
            .delete(body)
            .addHeader("X-Signature",signature)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string()
                throw IOException("Failed to delete expense: ${resp.code} - $errorBody")
            }
        }
    }


    private fun calculateWeeklyTotal(list: List<Expense>): Double {
        val today = LocalDate.now()
        // Find the most recent Monday
        val lastMonday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong()) // Monday = 1, Sunday = 7
        var sum = 0.0
        for (e in list) {
            try {
                val d = LocalDate.parse(e.date, dateFormatter)
                if (!d.isBefore(lastMonday)) {
                    sum += e.amount
                }
            } catch (ex: Exception) {
                println("Warning: Could not parse date for expense: ${e.date}")
            }
        }
        return sum
    }


    private fun hmacHex(secret: String, data: String): String {
        try {
            val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            return bytes.joinToString("") { String.format(Locale.US, "%02x", it) }
        } catch (e: Exception) {
            // It's better to throw or handle this more gracefully
            throw RuntimeException("Failed to generate HMAC signature", e)
        }
    }

    fun consumeToastMessage() {
        _toastMessage.value = null
    }
}
