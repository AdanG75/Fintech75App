package com.example.fintech75.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.fintech75.application.ItemClickListener
import com.example.fintech75.data.model.MarketFull
import com.example.fintech75.databinding.ItemMarketBinding

class MarketAdapter(
    private val markets: List<MarketFull>,
    private val itemClickListener: ItemClickListener
): RecyclerView.Adapter<MarketAdapter.MarketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketViewHolder {
        val binding = ItemMarketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = MarketViewHolder(binding)

        binding.root.setOnClickListener {
            val position: Int = holder
                .bindingAdapterPosition
                .takeIf { it != DiffUtil.DiffResult.NO_POSITION } ?: return@setOnClickListener

            itemClickListener.onMarketClick(market = markets[position])
        }

        return holder
    }

    override fun onBindViewHolder(holder: MarketViewHolder, position: Int) {
        holder.bind(markets[position])
    }

    override fun getItemCount(): Int = markets.size

    inner class MarketViewHolder(private val binding: ItemMarketBinding): ViewHolder(binding.root) {
        fun bind(market: MarketFull) {
            binding.tvItemMarketName.text = market.user.name
            binding.tvItemMarketId.text = market.market.idMarket
        }
    }
}