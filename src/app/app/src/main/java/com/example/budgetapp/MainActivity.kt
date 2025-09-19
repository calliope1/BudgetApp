package com.example.budgetapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.viewpager2.widget.ViewPager2
import com.example.budgetapp.settings.SettingsActivity

class MainActivity : AppCompatActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        result -> if (result.resultCode == Activity.RESULT_OK) {
            expenseViewModel.refreshCurrentWeek()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                val pager = findViewById<ViewPager2>(R.id.weekPager)
                val position = pager.currentItem
                val offset = position - WeekPagerAdapter.START_POS
                expenseViewModel.refreshWeek(offset)
                true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAdd: Button = findViewById(R.id.btnAdd)

        val pager = findViewById<ViewPager2>(R.id.weekPager)
        pager.adapter = WeekPagerAdapter(this)
        pager.setCurrentItem(WeekPagerAdapter.START_POS, false)

        // Tweak animations
        /* TODO tweak animations */
        // In fact, I'm not sure that this is doing anything
        pager.reduceDragSensitivity(4)
        pager.setScrollDuration(500)

        expenseViewModel.setCenterOffset(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val offset = position - WeekPagerAdapter.START_POS
                expenseViewModel.setCenterOffset(offset)
            }
        })

        btnAdd.setOnClickListener { showAddDialog() }
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
                val pager = findViewById<ViewPager2>(R.id.weekPager)
                val position = pager.currentItem
                val offset = position - WeekPagerAdapter.START_POS
                expenseViewModel.addExpense(amount, desc, chosenDate, offset)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
