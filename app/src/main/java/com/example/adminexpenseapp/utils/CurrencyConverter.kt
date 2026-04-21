package com.example.adminexpenseapp.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Currency converter utility with cloud-based exchange rates.
 *
 * Features:
 * - Fetches latest rates from Firebase Firestore
 * - Falls back to hardcoded rates if offline or fetch fails
 * - Rates are stored in Firestore collection "currency_rates" with document "exchange_rates"
 * - All rates are relative to 1 USD
 */
object CurrencyConverter {

    private const val TAG = "CurrencyConverter"
    private const val COLLECTION_RATES = "currency_rates"
    private const val DOCUMENT_RATES = "exchange_rates"

    // Fallback rates (relative to 1 USD) - used if Firestore fetch fails
    private val fallbackRates = mapOf(
        "USD" to 1.0,
        "GBP" to 0.79,
        "EUR" to 0.95,
        "VND" to 25400.0,
        "JPY" to 150.0,
        "AUD" to 1.54,
        "CAD" to 1.41,
        "SGD" to 1.34
    )

    // Current rates (starts with fallback, updates from cloud)
    private var currentRates: Map<String, Double> = fallbackRates

    /**
     * Fetches latest exchange rates from Firestore.
     * Call this on app startup or when user refreshes.
     *
     * Firestore document structure:
     * currency_rates/exchange_rates {
     *   USD: 1.0,
     *   GBP: 0.79,
     *   EUR: 0.95,
     *   VND: 25400.0,
     *   ...
     * }
     */
    fun fetchRatesFromCloud(context: Context, onComplete: ((success: Boolean) -> Unit)? = null) {
        if (!FirebaseSync.isNetworkAvailable(context)) {
            Log.w(TAG, "No network - using fallback rates")
            onComplete?.invoke(false)
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection(COLLECTION_RATES)
            .document(DOCUMENT_RATES)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val cloudRates = mutableMapOf<String, Double>()

                    // Extract rates from Firestore document
                    for ((currency, value) in document.data ?: emptyMap()) {
                        val rate = (value as? Number)?.toDouble()
                        if (rate != null) {
                            cloudRates[currency] = rate
                        }
                    }

                    if (cloudRates.isNotEmpty()) {
                        currentRates = cloudRates
                        Log.d(TAG, "Successfully loaded ${cloudRates.size} exchange rates from cloud")
                        onComplete?.invoke(true)
                    } else {
                        Log.w(TAG, "Cloud rates document is empty - using fallback")
                        onComplete?.invoke(false)
                    }
                } else {
                    Log.w(TAG, "Exchange rates document doesn't exist - using fallback")
                    // Optionally: Create default document in Firestore
                    createDefaultRatesDocument()
                    onComplete?.invoke(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch exchange rates: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    /**
     * Creates a default exchange rates document in Firestore.
     * This is a one-time setup that can be updated from Firebase console later.
     */
    private fun createDefaultRatesDocument() {
        val db = FirebaseFirestore.getInstance()
        db.collection(COLLECTION_RATES)
            .document(DOCUMENT_RATES)
            .set(fallbackRates)
            .addOnSuccessListener {
                Log.d(TAG, "Created default exchange rates document in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create default rates: ${e.message}")
            }
    }

    /**
     * Converts an amount from one currency to another.
     * Uses current rates (fetched from cloud or fallback).
     */
    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount

        val fromRate = currentRates[from] ?: 1.0
        val toRate = currentRates[to] ?: 1.0

        // Convert to USD first, then to target currency
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    /**
     * Returns the current exchange rate for a currency relative to USD.
     */
    fun getRate(currency: String): Double {
        return currentRates[currency] ?: 1.0
    }

    /**
     * Returns all available currencies.
     */
    fun getAvailableCurrencies(): Set<String> {
        return currentRates.keys
    }

    /**
     * Returns true if currently using cloud rates, false if using fallback.
     */
    fun isUsingCloudRates(): Boolean {
        return currentRates != fallbackRates
    }
}
