package com.apptists.poplur2025.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Google Play Billing for Poplur, mirroring the iOS StoreKit flow
 * (PurchaseViewModel.swift): consumable radius boosts and creator-credit
 * packs, plus the permanent daily-quota upgrade entitlement. Delivery writes
 * the same Firestore documents as iOS with platform: "android".
 */
object PoplurProducts {
    const val DAILY_UPGRADE = "daily_upgrade"

    data class RadiusPackage(val productId: String, val title: String, val radiusMeters: Int, val fallbackPrice: String)
    data class CreditPackage(val productId: String, val title: String, val credits: Int, val fallbackPrice: String)

    val radiusPackages = listOf(
        RadiusPackage("radius_starter", "Starter Radius", 5_000, "$1.99"),
        RadiusPackage("radius_city", "City Boost", 25_000, "$4.99"),
        RadiusPackage("radius_world", "World Push", 100_000, "$9.99")
    )

    val creditPackages = listOf(
        CreditPackage("credits_100", "Starter Pack", 100, "$1.99"),
        CreditPackage("credits_500", "Creator Pack", 500, "$7.99"),
        CreditPackage("credits_1500", "Pro Pack", 1500, "$19.99")
    )

    val consumableIds: Set<String> =
        (radiusPackages.map { it.productId } + creditPackages.map { it.productId }).toSet()

    val allIds: List<String> = consumableIds.toList() + DAILY_UPGRADE
}

class PoplurBillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PoplurBilling"

        @Volatile private var instance: PoplurBillingManager? = null

        fun getInstance(context: Context): PoplurBillingManager =
            instance ?: synchronized(this) {
                instance ?: PoplurBillingManager(context.applicationContext).also { instance = it }
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = FirebaseFirestore.getInstance()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _purchasedRadiusDelta = MutableStateFlow(0)
    /** Cumulative meters delivered this session — UI can observe to update the radius display. */
    val purchasedRadiusDelta: StateFlow<Int> = _purchasedRadiusDelta

    private val _creditsDelta = MutableStateFlow(0)
    val creditsDelta: StateFlow<Int> = _creditsDelta

    private val _hasDailyUpgrade = MutableStateFlow(false)
    val hasDailyUpgrade: StateFlow<Boolean> = _hasDailyUpgrade

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var isConnecting = false

    fun connect() {
        if (billingClient.isReady || isConnecting) return
        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
            }
        })
    }

    private fun queryProducts() {
        val products = PoplurProducts.allIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = detailsList.associateBy { it.productId }
                if (detailsList.isEmpty()) {
                    _message.value = "In-app products are not configured in Google Play yet."
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    fun displayPrice(productId: String, fallback: String): String =
        _productDetails.value[productId]?.oneTimePurchaseOfferDetails?.formattedPrice ?: fallback

    fun launchPurchase(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            _message.value = "This product is not available yet."
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _message.value = "Purchase failed: ${result.debugMessage}"
        }
    }

    /** Re-checks owned purchases; delivers anything bought but not yet consumed/acknowledged. */
    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            _message.value = "Sign in before purchasing."
            return
        }

        scope.launch {
            purchase.products.forEach { productId ->
                try {
                    when {
                        productId in PoplurProducts.consumableIds -> {
                            deliverConsumable(productId, purchase, userId)
                            consume(purchase)
                        }
                        productId == PoplurProducts.DAILY_UPGRADE -> {
                            _hasDailyUpgrade.value = true
                            if (!purchase.isAcknowledged) acknowledge(purchase)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Delivery failed for $productId", e)
                    _message.value = "Could not deliver purchase: ${e.message}"
                }
            }
        }
    }

    /**
     * Writes the same Firestore shape as iOS (PurchaseViewModel.deliverRadius /
     * deliverCredits), keyed by orderId so redelivery is idempotent.
     */
    private suspend fun deliverConsumable(productId: String, purchase: Purchase, userId: String) {
        val orderId = purchase.orderId ?: purchase.purchaseToken
        val userRef = db.collection("users").document(userId)

        PoplurProducts.radiusPackages.firstOrNull { it.productId == productId }?.let { pkg ->
            val purchaseRef = userRef.collection("radiusPurchases").document(orderId)
            val existing = purchaseRef.get().await()
            if (existing.exists()) return

            db.runBatch { batch ->
                batch.set(
                    purchaseRef,
                    mapOf(
                        "productID" to pkg.productId,
                        "title" to pkg.title,
                        "radiusMeters" to pkg.radiusMeters,
                        "storeTransactionID" to orderId,
                        "purchasedAt" to FieldValue.serverTimestamp(),
                        "platform" to "android"
                    )
                )
                batch.set(
                    userRef,
                    mapOf(
                        "purchasedRadiusMeters" to FieldValue.increment(pkg.radiusMeters.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }.await()

            _purchasedRadiusDelta.value += pkg.radiusMeters
            _message.value = "Added ${formatRadius(pkg.radiusMeters)} to your radius."
            return
        }

        PoplurProducts.creditPackages.firstOrNull { it.productId == productId }?.let { pkg ->
            val purchaseRef = userRef.collection("creditPurchases").document(orderId)
            val balanceRef = db.collection("creatorCreditBalances").document(userId)
            val existing = purchaseRef.get().await()
            if (existing.exists()) return

            db.runBatch { batch ->
                batch.set(
                    purchaseRef,
                    mapOf(
                        "productID" to pkg.productId,
                        "title" to pkg.title,
                        "credits" to pkg.credits,
                        "storeTransactionID" to orderId,
                        "purchasedAt" to FieldValue.serverTimestamp(),
                        "platform" to "android"
                    )
                )
                batch.set(
                    balanceRef,
                    mapOf(
                        "availableCredits" to FieldValue.increment(pkg.credits.toLong()),
                        "lifetimeEarned" to FieldValue.increment(pkg.credits.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }.await()

            _creditsDelta.value += pkg.credits
            _message.value = "Added ${pkg.credits} Creator Credits!"
        }
    }

    private fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "consume failed: ${result.debugMessage}")
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "acknowledge failed: ${result.debugMessage}")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun formatRadius(meters: Int): String =
        if (meters >= 1000) "${meters / 1000} km" else "$meters m"
}
