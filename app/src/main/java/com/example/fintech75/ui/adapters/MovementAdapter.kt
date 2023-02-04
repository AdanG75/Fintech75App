package com.example.fintech75.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fintech75.data.model.MovementComplete
import com.example.fintech75.databinding.ItemMovementBinding

class MovementAdapter(
    private val movements: List<MovementComplete>
): RecyclerView.Adapter<MovementAdapter.MovementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementViewHolder {
        val binding = ItemMovementBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return MovementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovementViewHolder, position: Int) {
        holder.binding(movements[position])
    }

    override fun getItemCount(): Int = movements.size

    inner class MovementViewHolder(private val binding: ItemMovementBinding): RecyclerView.ViewHolder(binding.root) {

        fun binding(movement: MovementComplete) {
            binding.tvItemMovementType.text = getTypeMovement(movement.typeMovement)
            binding.tvItemMovementDate.text = movement.createdTime.substring(0, 10)
            binding.tvItemMovementAmount.text = movement.amount

            val movNature = if (movement.extra.movementNature.lowercase() == "income") {
                "Ingreso"
            } else {
                "Egreso"
            }

            val subType = if (movement.extra.typeSubMovement.lowercase() == "paypal") {
                "PayPal"
            } else {
                "Crédito"
            }

            "$movNature ($subType)".also {
                binding.tvItemMovementExtra.text = it
            }
        }

        private fun getTypeMovement(typeMovement: String): String {
            return when(typeMovement.lowercase()) {
                "payment" -> "Pago"
                "deposit" -> "Depósito"
                "transfer" -> "Transferencia"
                "withdraw" -> "Retiro"
                else -> "Transacción"
            }
        }
    }
}