package com.example.adminexpenseapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.models.Expense
import com.google.android.material.chip.Chip

class ExpenseAdapter(
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    private var expenses: List<Expense> = emptyList()

    fun setExpenses(list: List<Expense>) {
        expenses = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]

        holder.tvExpenseId.text = expense.expenseId
        holder.tvExpenseDate.text = expense.dateOfExpense
        holder.tvAmount.text = "${expense.currency} ${"%.2f".format(expense.amount)}"
        holder.tvClaimant.text = "Claimant: ${expense.claimant}"
        holder.chipExpenseType.text = expense.expenseType
        holder.chipPaymentStatus.text = expense.paymentStatus
        holder.tvPaymentMethod.text = expense.paymentMethod

        if (expense.description.isNotBlank()) {
            holder.tvDescription.text = expense.description
            holder.tvDescription.visibility = View.VISIBLE
        } else {
            holder.tvDescription.visibility = View.GONE
        }

        // Sync Status for Expense
        if (expense.lastSyncAt == 0L) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_off)
            holder.ivSyncStatus.setColorFilter(Color.GRAY)
        } else if (expense.updatedAt > expense.lastSyncAt) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_on)
            holder.ivSyncStatus.setColorFilter(Color.parseColor("#FF8C00")) // Orange
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_on)
            holder.ivSyncStatus.setColorFilter(Color.parseColor("#4CAF50")) // Green
        }

        holder.btnEdit.setOnClickListener { onEditClick(expense) }
        holder.btnDelete.setOnClickListener { onDeleteClick(expense) }
    }

    override fun getItemCount(): Int = expenses.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvExpenseId: TextView = itemView.findViewById(R.id.tvExpenseId)
        val tvExpenseDate: TextView = itemView.findViewById(R.id.tvExpenseDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvClaimant: TextView = itemView.findViewById(R.id.tvClaimant)
        val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val chipExpenseType: Chip = itemView.findViewById(R.id.chipExpenseType)
        val chipPaymentStatus: Chip = itemView.findViewById(R.id.chipPaymentStatus)
        val ivSyncStatus: ImageView = itemView.findViewById(R.id.ivExpenseSyncStatus)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditExpense)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteExpense)
    }
}
