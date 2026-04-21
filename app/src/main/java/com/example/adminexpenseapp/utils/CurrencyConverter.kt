package com.example.adminexpenseapp.utils

/**
 * A simple currency converter utility.
 * In a real-world app, these rates would be fetched from an API.
 */
object CurrencyConverter {

    // Relative to 1 USD
    private val rates = mapOf(
        "USD" to 1.0,
        "GBP" to 0.79,
        "EUR" to 0.95,
        "VND" to 25400.0,
        "JPY" to 150.0,
        "AUD" to 1.54,
        "CAD" to 1.41,
        "SGD" to 1.34
    )

    /**
     * Converts an amount from one currency to another.
     */
    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        
        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0
        
        // Convert to USD first, then to target currency
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }
}
