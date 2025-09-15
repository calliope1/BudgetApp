package com.example.budgetapp

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
import java.time.format.DateTimeFormatter
import java.util.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeekFragment : Fragment() {
    private val vm: ExpenseViewModel by activityViewModels()
    private var offset: Int = 0
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private lateinit var adapter: ArrayAdapter<Expense>

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
//                            lifecycleScope.launch {
//                                try {
//                                    vm.deleteExpense(id, offset)
//                                } catch (e: Exception) {
//                                    Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
//                                }
//                            }
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

        // Observe the LiveData for this offset
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
                    tvWeekLabel.text = "${weekStart}—${weekStart.plusDays(6)}"
                    tvBudget.text = String.format(Locale.US, "Remaining: £%.2f", state.remainingBudget)
//                    val sorted = state.expenses.sortedWith(compareByDescending<Expense> {
//                        try { LocalDate.parse(it.date, dateFormatter) } catch (e: Exception) { LocalDate.MIN }
//                    })
                    val sorted = state.expenses
                        .sortedBy { LocalDate.parse(it.date) }
                        .asReversed()
                    adapter.clear()
                    adapter.addAll(sorted)
                    adapter.notifyDataSetChanged()
                }
                is ExpenseViewModel.WeekUiState.Error -> {
                    progress.visibility = View.GONE
                    tvWeekLabel.text = "Error"
                    tvBudget.text = ""
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

        // prefill
        etAmount.setText(expense.amount.toString())
        etDesc.setText(expense.description)
        btnDate.text = expense.date
        var chosenDate = java.time.LocalDate.parse(expense.date)

        btnDate.setOnClickListener { /* TODO: date picker that updates chosenDate and btnDate.text */ }

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
                            chosenDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
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
