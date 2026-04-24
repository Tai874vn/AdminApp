package com.example.adminexpenseapp.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object CurrencyConverter {

    private const val TAG = "CurrencyConverter"
    private const val COLLECTION_RATES = "currency_rates"
    private const val DOCUMENT_RATES = "exchange_rates"

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

    private var currentRates: Map<String, Double> = fallbackRates

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
                    createDefaultRatesDocument()
                    onComplete?.invoke(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch exchange rates: ${e.message}")
                onComplete?.invoke(false)
            }
    }

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

    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount

        val fromRate = currentRates[from] ?: 1.0
        val toRate = currentRates[to] ?: 1.0

        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    fun getRate(currency: String): Double {
        return currentRates[currency] ?: 1.0
    }

    fun getAvailableCurrencies(): Set<String> {
        return currentRates.keys
    }

    fun isUsingCloudRates(): Boolean {
        return currentRates != fallbackRates
    }
}
