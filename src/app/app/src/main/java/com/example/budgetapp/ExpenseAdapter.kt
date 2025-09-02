package com.example.budgetapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ExpenseAdapter(
    ctx: Context,
    private var items: MutableList<Expense> = mutableListOf(),
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : ArrayAdapter<Expense>(ctx, 0, items) {

    private val inflater = LayoutInflater.from(ctx)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun updateExpenses(newList: List<Expense>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Expense? = items[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_expense, parent, false)
        val expense = items[position]

        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        // populate fields
        tvDescription.text = expense.description
        tvDate.text = expense.date
        tvAmount.text = String.format(Locale.US, "£%.2f", expense.amount)

        // click handlers pass the Expense object to your callbacks
        btnEdit.setOnClickListener { onEdit(expense) }
        btnDelete.setOnClickListener { onDelete(expense) }

        return view
    }

}
