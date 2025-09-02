package com.example.budgetapp

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels // Import for by viewModels()
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvBudget: TextView
    private lateinit var tvWeeklySpent: TextView // Added for clarity
    private lateinit var listExpenses: ListView
    private lateinit var adapter: ExpenseAdapter
    private lateinit var progressBar: ProgressBar // For loading state

    // Use the by viewModels() Kotlin property delegate from activity-ktx
    private val expenseViewModel: ExpenseViewModel by viewModels()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBudget = findViewById(R.id.tvBudget)
        tvWeeklySpent = findViewById(R.id.tvWeeklySpent)
        listExpenses = findViewById(R.id.listExpenses)
        progressBar = findViewById(R.id.progressBar)
        val btnAdd: Button = findViewById(R.id.btnAdd)

        adapter = ExpenseAdapter(
            this,
            mutableListOf(),
            onEdit = { expense -> showEditDialog(expense) },
            onDelete = { expense -> confirmAndDelete(expense) }
        )
        listExpenses.adapter = adapter

        btnAdd.setOnClickListener { showAddDialog() }

        observeViewModel()
    }

    private fun observeViewModel() {
        expenseViewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    listExpenses.visibility = View.GONE
                    tvBudget.text = getString(R.string.loading)
                    tvWeeklySpent.text = ""
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    listExpenses.visibility = View.VISIBLE
                    tvBudget.text = String.format(Locale.US, "Remaining: £%.2f", state.remainingBudget)
                    tvWeeklySpent.text = String.format(Locale.US, "Spent This Week: £%.2f", state.weeklyTotal)

                    val sorted = state.expenses.sortedByDescending { LocalDate.parse(it.date) }
                    adapter.updateExpenses(sorted)
                    adapter.notifyDataSetChanged()
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    listExpenses.visibility = View.GONE // Or show an error message in the list
                    tvBudget.text = getString(R.string.error)
                    tvWeeklySpent.text = ""
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        expenseViewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                expenseViewModel.consumeToastMessage() // Reset after showing
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null) // Create a layout file
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnDate = dialogView.findViewById<Button>(R.id.btnChooseDate)

        var chosenDate = LocalDate.now()
        btnDate.text = chosenDate.format(dateFormatter)

        btnDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                chosenDate = LocalDate.of(year, month + 1, dayOfMonth)
                btnDate.text = chosenDate.format(dateFormatter)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amountStr = etAmount.text.toString()
                val desc = etDesc.text.toString()

                val amount = amountStr.toDoubleOrNull()
                if (amount == null) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (desc.isBlank()) {
                    Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                expenseViewModel.addExpense(amount, desc, chosenDate)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(expense: Expense) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnDate = dialogView.findViewById<Button>(R.id.btnChooseDate)

        etAmount.setText(expense.amount.toString())
        etDesc.setText(expense.description)
        btnDate.text = expense.date
        var chosenDate = LocalDate.parse(expense.date, dateFormatter)

        btnDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                chosenDate = LocalDate.of(year, month + 1, dayOfMonth)
                btnDate.text = chosenDate.format(dateFormatter)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amountStr = etAmount.text.toString()
                val desc = etDesc.text.toString()
                val amt = amountStr.toDouble()
                if (desc.isBlank()) {
                    Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (expense.id == null) {
                    throw Exception("Expense \"${expense.description}\" has null id")
                }
                expenseViewModel.updateExpense(expense.id ?: "", amt, desc, chosenDate.format(dateFormatter))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmAndDelete(expense: Expense) {

        AlertDialog.Builder(this)
            .setTitle("Delete expense")
            .setMessage("Are you sure you want to delete \"${expense.description}\" for £${String.format(Locale.US, "%.2f", expense.amount)}?")
            .setPositiveButton("Delete") { _, _ ->
                expenseViewModel.deleteExpense(expense.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
