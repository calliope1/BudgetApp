package com.example.budgetapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.util.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.time.format.DateTimeFormatter

class WeekFragment : Fragment() {
    private val vm: ExpenseViewModel by activityViewModels()
    private var offset: Int = 0
    private lateinit var adapter: ArrayAdapter<Expense>
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offset = arguments?.getInt(ARG_OFFSET) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_week, container, false)
        val tvWeekLabel = v.findViewById<TextView>(R.id.tvWeekLabel)
        val tvBudget = v.findViewById<TextView>(R.id.tvBudget)
        val progress = v.findViewById<ProgressBar>(R.id.progressBar)
        val list = v.findViewById<ListView>(R.id.listExpenses)
        val swipe = v.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        adapter = object : ArrayAdapter<Expense>(requireContext(), R.layout.list_item_expense, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.list_item_expense, parent, false)
                val expense = getItem(position)!!

                val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
                val tvDate = view.findViewById<TextView>(R.id.tvDate)
                val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
                val btnEdit = view.findViewById<Button>(R.id.btnEdit)
                val btnDelete = view.findViewById<Button>(R.id.btnDelete)

                tvDescription.text = expense.description
                tvDate.text = expense.date
                tvAmount.text = String.format(Locale.US, "£%.2f", expense.amount)

                btnEdit.setOnClickListener {
                    showEditExpenseDialog(expense)
                }
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete")
                        .setMessage("Delete this expense?")
                        .setPositiveButton("Delete") { _, _ ->
                            val id = expense.id
                            if (id == null) {
                                Toast.makeText(requireContext(), "Cannot delete: missing id", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            vm.deleteExpense(id, offset)
                            Toast.makeText(requireContext(), "Deleting...", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                return view
            }
        }
        list.adapter = adapter

        vm.getWeekLiveData(offset).observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExpenseViewModel.WeekUiState.Loading -> {
                    progress.visibility = View.VISIBLE
                    list.visibility = View.GONE
                }
                is ExpenseViewModel.WeekUiState.Success -> {
                    progress.visibility = View.GONE
                    list.visibility = View.VISIBLE
                    val weekStart = state.weekStart
                    tvWeekLabel.text =
                        getString(R.string.week_range, weekStart, weekStart.plusDays(6))
                    tvBudget.text = String.format(Locale.US, "Remaining: £%.2f", state.remainingBudget)
                    // Sorted by day descending (most recent day on the top), but each individual day is reversed.
                    // Different to .sortedByDescending, which flips the individual days.
                    val sorted = state.expenses
                        .sortedBy { LocalDate.parse(it.date) }
                        .asReversed()
                    adapter.clear()
                    adapter.addAll(sorted)
                    adapter.notifyDataSetChanged()
                }
                is ExpenseViewModel.WeekUiState.Error -> {
                    progress.visibility = View.GONE
                    tvWeekLabel.text = getString(R.string.error)
                    tvBudget.text = ""
                }
            }
        }

        swipe.setOnRefreshListener {
            vm.refreshWeek(offset, manual = true)
        }

        vm.getWeekLiveData(offset).observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExpenseViewModel.WeekUiState.Loading -> {
                    progress.visibility = View.VISIBLE
                    list.visibility = View.GONE
                }
                is ExpenseViewModel.WeekUiState.Success -> {
                    progress.visibility = View.GONE
                    list.visibility = View.VISIBLE
                    swipe.isRefreshing = false
                }
                is ExpenseViewModel.WeekUiState.Error -> {
                    progress.visibility = View.GONE
                    swipe.isRefreshing = false
                }
            }
        }



        return v
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnDate = dialogView.findViewById<Button>(R.id.btnChooseDate)

        etAmount.setText(expense.amount.toString())
        etDesc.setText(expense.description)
        btnDate.text = expense.date
        var chosenDate = LocalDate.parse(expense.date)

        btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                chosenDate = LocalDate.of(year, month + 1, dayOfMonth)
                btnDate.text = chosenDate.format(dateFormatter)
            }, chosenDate.year, chosenDate.monthValue - 1, chosenDate.dayOfMonth)
                .show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newAmount = etAmount.text.toString().toDoubleOrNull()
                val newDesc = etDesc.text.toString()
                if (newAmount == null) {
                    Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val id = expense.id ?: run {
                            Toast.makeText(requireContext(), "Missing id", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        vm.updateExpense(
                            id,
                            newAmount,
                            newDesc,
                            chosenDate.format(dateFormatter),
                            offset
                        )
                        Toast.makeText(requireContext(), "Updating...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_OFFSET = "arg_offset"
        fun newInstance(offset: Int): WeekFragment {
            val f = WeekFragment()
            f.arguments = Bundle().apply { putInt(ARG_OFFSET, offset) }
            return f
        }
    }
}
