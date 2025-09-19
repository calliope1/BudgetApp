package com.example.budgetapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val client = OkHttpClient()

    private val prefs = app.getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
    private val serverUrl: String
        get() {
            val url = prefs.getString("server_url", "http://127.0.0.1:5000")!!.trim()
            return if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }
        }
    private val sharedSecret : String
        get() = prefs.getString("shared_secret", "")!!
    private val weekCache = mutableMapOf<Int, MutableLiveData<WeekUiState>>()
    private val cacheRadius = 2
    private var currentCenterOffset: Int = 0
    private val loadingOffsets: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

    private var weeklyBudgetCached: Double? = null
    private var weeklyBudget: Double = 110.0
    private val _uiState = MutableLiveData<UiState>()

    private val _toastMessage = MutableLiveData<String?>()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        fetchExpenses()
    }

    // UI state for a week
    sealed class WeekUiState {
        object Loading : WeekUiState()
        data class Success(
            val weekStart: LocalDate,
            val expenses: List<Expense>,
            val weeklyTotal: Double,
            val remainingBudget: Double
        ) : WeekUiState()
        data class Error(val weekStart: LocalDate, val message: String) : WeekUiState()
    }

    fun setCenterOffset(offset: Int) {
        currentCenterOffset = offset
        for (o in (offset - cacheRadius)..(offset + cacheRadius)) {
            preload(o)
        }
    }

    private fun weekStartForOffset(offset: Int): LocalDate {
        val today = LocalDate.now()
        val dow = today.dayOfWeek.value // 1..7, Monday=1
        val monday = today.minusDays((dow - 1).toLong())
        return monday.plusWeeks(offset.toLong())
    }

    fun getWeekLiveData(offset: Int): LiveData<WeekUiState> {
        return weekCache.getOrPut(offset) {
            val live = MutableLiveData<WeekUiState>()
            loadWeek(offset, live)
            live
        }
    }

    private fun preload(offset: Int) {
        if (kotlin.math.abs(offset - currentCenterOffset) > cacheRadius) return
        if (loadingOffsets.contains(offset)) return
        val live = weekCache[offset]
        if (live != null && live.value !is WeekUiState.Error) return
        loadingOffsets.add(offset)
        val weekLiveData = live ?: MutableLiveData<WeekUiState>().also { weekCache[offset] = it }
        loadWeek(offset, weekLiveData)
    }

    private fun loadWeek(offset: Int, live: MutableLiveData<WeekUiState>, manual: Boolean = false) {
        if (!manual) {
            live.postValue(WeekUiState.Loading)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (weeklyBudgetCached == null) {
                    weeklyBudgetCached = fetchBudget()
                }
                val weekStart = weekStartForOffset(offset)
                val expenses = fetchWeekFromServer(weekStart)
                val weeklyTotal = expenses.sumOf { it.amount }
                val remaining = (weeklyBudgetCached ?: 0.0) - weeklyTotal
                live.postValue(WeekUiState.Success(weekStart, expenses, weeklyTotal, remaining))

                // preload neighbors for smooth UX
                preload(offset - 1)
                preload(offset + 1)
            } catch (e: Exception) {
                live.postValue(WeekUiState.Error(weekStartForOffset(offset), e.message ?: "Error"))
            } finally {
                loadingOffsets.remove(offset)
            }

            preload(offset - 1)
            preload(offset + 1)
        }
    }

    fun refreshWeek(offset: Int, manual: Boolean = false) {
        val live = getWeekLiveData(offset) as MutableLiveData<WeekUiState>
        loadWeek(offset, live, manual)
    }
    fun refreshCurrentWeek(manual: Boolean = false) {
        refreshWeek(currentCenterOffset, manual)
    }
    private suspend fun fetchBudget(): Double = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$serverUrl/budget").get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to load budget: ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty budget response")
            val obj = JSONObject(body)
            obj.getDouble("weekly_budget")
        }
    }
    private suspend fun fetchWeekFromServer(weekStart: LocalDate): List<Expense> = withContext(Dispatchers.IO) {
        val weekStr = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val url = "$serverUrl/expenses?week_commencing=$weekStr"
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to load week: ${resp.code}")
            val body = resp.body?.string() ?: "[]"
            val arr = org.json.JSONArray(body)
            val out = mutableListOf<Expense>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = if (o.has("id")) o.optString("id") else null
                out.add(Expense(id, o.getDouble("amount"), o.getString("description"), o.getString("date")))
            }
            out
        }
    }



    private suspend fun fetchWeeklyBudgetFromServer(): Double = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$serverUrl/budget")
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
            .url("$serverUrl/expenses")
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

    fun addExpense(amount: Double, description: String, date: LocalDate, offset: Int) {
        viewModelScope.launch {
            try {
                val newExpense = Expense(amount = amount, description = description, date = date.format(dateFormatter))
                newExpense.id = postExpenseToServer(newExpense)
                _toastMessage.postValue("Expense added successfully")
            } catch (e: Exception) {
                _toastMessage.postValue("Error adding expense: ${e.message}")
            }
            refreshWeek(offset)
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
        val signature = hmacHex(sharedSecret, jsonString)

        val req = Request.Builder()
            .url("$serverUrl/expenses")
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

    fun updateExpense(expenseId: String, amount: Double, description: String, date: String, offset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("amount", amount)
                    put("description", description)
                    put("date", date)
                }
                val jsonString = jsonObject.toString()
                val signature = hmacHex(sharedSecret, jsonString) // keep existing HMAC helper
                val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val url = "$serverUrl/expenses/id/${java.net.URLEncoder.encode(expenseId, "UTF-8")}"
                val req = Request.Builder()
                    .url(url)
                    .method("PATCH", body)
                    .addHeader("X-Signature", signature)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("PATCH failed: ${resp.code} - ${resp.body?.string()}")
                }
                Log.d("UpdateExpense", "Tried to update expense $expenseId with json $jsonString")

                // reload same LiveData instance
                val live = getWeekLiveData(offset) as MutableLiveData<WeekUiState>
                loadWeek(offset, live)
            } catch (e: Exception) {
                val live = weekCache[offset]
                live?.postValue(WeekUiState.Error(weekStartForOffset(offset), e.message ?: "Update failed"))
            }
            Log.d("UpdateExpense", "Updated expense. TODO: Better debug.")
        }
    }

    fun deleteExpense(expenseId: String?, offset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("id", expenseId)
                }
                val jsonString = jsonObject.toString()
                val signature = hmacHex(sharedSecret, jsonString) // keep existing HMAC helper
                val body =
                    jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val url = "$serverUrl/expenses/id/$expenseId"
                val req = Request.Builder()
                    .url(url)
                    .method("DELETE", body)
                    .addHeader("X-Signature", signature)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("DELETE failed: ${resp.code} - ${resp.body?.string()}")
                }
                // After successful delete, reload this week's LiveData (same instance)
                val live = getWeekLiveData(offset) as MutableLiveData<WeekUiState>
                loadWeek(offset, live)
            } catch (e: Exception) {
                // post error into the same LiveData so UI can show feedback
                val live = weekCache[offset]
                live?.postValue(
                    WeekUiState.Error(
                        weekStartForOffset(offset),
                        e.message ?: "Delete failed"
                    )
                )
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
                println("Warning: Could not parse date for expense: ${e.date} (${ex.message})")
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
}
