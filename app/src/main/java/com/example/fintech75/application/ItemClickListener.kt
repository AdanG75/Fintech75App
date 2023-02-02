package com.example.fintech75.application

import com.example.fintech75.data.model.CreditBase
import com.example.fintech75.data.model.MarketFull

interface ItemClickListener {
    fun onCreditClick(credit: CreditBase)

    fun onMarketClick(market: MarketFull)
}