package com.example.fintech75.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fintech75.application.ItemClickListener
import com.example.fintech75.data.model.CreditBase
import com.example.fintech75.databinding.ItemCreditBinding

class CreditAdapter(
    private var credits: List<CreditBase>,
    private val itemClickListener: ItemClickListener
): RecyclerView.Adapter<CreditAdapter.CreditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val binding = ItemCreditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CreditViewHolder(binding)

        binding.root.setOnClickListener{
            val position: Int = holder
                .bindingAdapterPosition
                .takeIf { it != DiffUtil.DiffResult.NO_POSITION } ?: return@setOnClickListener

            itemClickListener.onCreditClick(credits[position])
        }

        return holder
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        holder.binding(credit = credits[position])
    }

    override fun getItemCount(): Int = credits.size

    fun updateCredits(newCredits: List<CreditBase>){
        credits = newCredits
        notifyDataSetChanged()
    }

    inner class CreditViewHolder(private val binding: ItemCreditBinding): RecyclerView.ViewHolder(binding.root) {
        fun binding(credit: CreditBase) {
            binding.tvItemCreditAlias.text = credit.aliasCredit
            "${credit.idCredit} (${credit.typeCredit})".also {
                binding.tvItemCreditId.text = it
            }
            binding.tvItemCreditAmount.text = credit.amount
        }
    }
}