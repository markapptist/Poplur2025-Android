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
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/** Google Play product catalog shared by native store UI and the web checkout bridge. */
object PoplurProducts {
    const val DAILY_UPGRADE = "daily_upgrade"

    data class RadiusPackage(
        val productId: String,
        val title: String,
        val radiusMeters: Int,
        val fallbackPrice: String
    )

    data class CreditPackage(
        val productId: String,
        val title: String,
        val credits: Int,
        val fallbackPrice: String
    )

    data class CardCreditPackage(
        val productId: String,
        val cardId: String,
        val title: String,
        val credits: Int,
        val fallbackPrice: String,
        val description: String,
        val modelAssetPath: String,
        /** Meshy exported this model on XZ instead of XY. */
        val presentationRotationX: Float = 0f
    )

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

    val cardPackages = listOf(
        CardCreditPackage(
            productId = "com.apptists.poplur2025.card.inverted",
            cardId = "inverted",
            title = "Inverted Card",
            credits = 300,
            fallbackPrice = "$2.99",
            description = "A sharp inside-out Poplur finish. Adds 300 Poplur Credits after a verified purchase.",
            modelAssetPath = "store_cards/poplur_card_inverted.glb"
        ),
        CardCreditPackage(
            productId = "com.apptists.poplur2025.card.lens",
            cardId = "lens",
            title = "Lens Card",
            credits = 550,
            fallbackPrice = "$4.99",
            description = "A glossy optical card with a camera-lens core. Adds 550 Poplur Credits after a verified purchase.",
            modelAssetPath = "store_cards/poplur_card_lens.glb"
        ),
        CardCreditPackage(
            productId = "com.apptists.poplur2025.card.splitdiamond",
            cardId = "splitDiamond",
            title = "Split Diamond Card",
            credits = 800,
            fallbackPrice = "$6.99",
            description = "A faceted split-diamond card built for standout profiles. Adds 800 Poplur Credits after a verified purchase.",
            modelAssetPath = "store_cards/poplur_card_split_diamond.glb"
        ),
        CardCreditPackage(
            productId = "com.apptists.poplur2025.card.swoosh",
            cardId = "swoosh",
            title = "Swoosh Card",
            credits = 1_200,
            fallbackPrice = "$9.99",
            description = "A flowing sculpted card with Poplur motion. Adds 1,200 Poplur Credits after a verified purchase.",
            modelAssetPath = "store_cards/poplur_card_swoosh.glb",
            presentationRotationX = 90f
        )
    )

    val cardProductIds: Set<String> = cardPackages.mapTo(linkedSetOf()) { it.productId }
    val consumableIds: Set<String> = (
        radiusPackages.map { it.productId } +
            creditPackages.map { it.productId } +
            cardPackages.map { it.productId }
        ).toSet()

    val allIds: List<String> = consumableIds.toList() + DAILY_UPGRADE

    fun cardPackage(productId: String): CardCreditPackage? =
        cardPackages.firstOrNull { it.productId == productId }

    /** Accepts Android SKU, stable card ID, and the older iOS-style SKU from web drafts. */
    fun resolveCardProductId(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        cardPackage(raw)?.let { return it.productId }
        val stableId = raw.substringAfterLast('.').replace("-", "").lowercase()
        return cardPackages.firstOrNull {
            it.cardId.replace("-", "").lowercase() == stableId
        }?.productId
    }
}

enum class PurchaseEventStatus {
    Success,
    Pending,
    Cancelled,
    Failed
}

data class PurchaseEvent(
    val productId: String,
    val status: PurchaseEventStatus,
    val message: String,
    val credits: Int = 0,
    val transactionId: String? = null
)

/**
 * Google Play Billing manager for consumable radius, creator-credit, and card-credit packages.
 * Card packages are never fulfilled by a trusted-by-default client write: the purchase token is
 * verified by the `verifyCardPurchase` callable before Play consumption and success delivery.
 */
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
    private val functions = FirebaseFunctions.getInstance()

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

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 16)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        // Billing 8 dropped the no-argument form; one-time products are the
        // only kind Poplur sells.
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    @Volatile private var isConnecting = false
    @Volatile private var pendingProductId: String? = null

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
                    val message = "Billing setup failed: ${result.debugMessage}"
                    Log.w(TAG, message)
                    _message.value = message
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
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            // Billing 8 hands back a result object; unfetched ids are listed
            // separately and are simply absent from the map, as before.
            val detailsList = queryResult.productDetailsList
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

    /** Returns true only when Google Play accepted presentation of the billing sheet. */
    fun launchPurchase(activity: Activity, productId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            failLaunch(productId, "Sign in before purchasing.")
            return false
        }
        val details = _productDetails.value[productId]
        if (details == null) {
            failLaunch(productId, "This product is not available in Google Play yet.")
            return false
        }
        if (pendingProductId != null) {
            failLaunch(productId, "Another purchase is already in progress.")
            return false
        }

        pendingProductId = productId
        val params = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(sha256Hex(userId))
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            // A prior verified grant may still be waiting for Play consumption, or
            // payment may still be pending. Reconcile that token instead of opening
            // a second charge path for the same consumable.
            pendingProductId = null
            _message.value = "Finishing your existing Google Play purchase…"
            restorePurchases()
            return true
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingProductId = null
            failLaunch(productId, result.debugMessage.ifBlank { "Google Play could not start checkout." })
            return false
        }
        return true
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    val productId = pendingProductId ?: return
                    pendingProductId = null
                    emitEvent(productId, PurchaseEventStatus.Failed, "Google Play returned no purchase.")
                } else {
                    purchases.forEach(::handlePurchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                val productId = pendingProductId ?: "unknown"
                pendingProductId = null
                emitEvent(productId, PurchaseEventStatus.Cancelled, "Purchase cancelled.")
            }
            else -> {
                val productId = pendingProductId ?: "unknown"
                pendingProductId = null
                emitEvent(
                    productId,
                    PurchaseEventStatus.Failed,
                    "Purchase failed: ${result.debugMessage.ifBlank { "Google Play error ${result.responseCode}" }}"
                )
            }
        }
    }

    /** Re-checks owned purchases; delivers anything bought but not yet consumed/acknowledged. */
    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            val message = "Purchase pending in Google Play. Credits will arrive after payment completes."
            purchase.products.forEach { productId ->
                if (pendingProductId == productId) pendingProductId = null
                emitEvent(productId, PurchaseEventStatus.Pending, message)
            }
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            purchase.products.forEach { productId ->
                if (pendingProductId == productId) pendingProductId = null
                emitEvent(productId, PurchaseEventStatus.Failed, "Sign in before purchasing.")
            }
            return
        }

        scope.launch {
            purchase.products.forEach { productId ->
                try {
                    when {
                        PoplurProducts.cardPackage(productId) != null -> {
                            val pkg = requireNotNull(PoplurProducts.cardPackage(productId))
                            val verification = verifyCardPurchase(productId, purchase)
                            consume(purchase)
                            if (!verification.alreadyGranted && verification.creditsAdded > 0) {
                                _creditsDelta.value += verification.creditsAdded
                                _message.value = "Added ${verification.creditsAdded} Poplur Credits from ${pkg.title}."
                            } else {
                                _message.value = "${pkg.title} was already delivered to this account."
                            }
                            if (pendingProductId == productId) pendingProductId = null
                            emitEvent(
                                productId = productId,
                                status = PurchaseEventStatus.Success,
                                message = _message.value ?: "${pkg.title} verified.",
                                credits = verification.creditsAdded,
                                transactionId = purchase.orderId ?: purchase.purchaseToken
                            )
                        }
                        productId in PoplurProducts.consumableIds -> {
                            deliverLegacyConsumable(productId, purchase, userId)
                            consume(purchase)
                            if (pendingProductId == productId) pendingProductId = null
                            emitEvent(
                                productId = productId,
                                status = PurchaseEventStatus.Success,
                                message = _message.value ?: "Purchase delivered.",
                                transactionId = purchase.orderId ?: purchase.purchaseToken
                            )
                        }
                        productId == PoplurProducts.DAILY_UPGRADE -> {
                            _hasDailyUpgrade.value = true
                            if (!purchase.isAcknowledged) acknowledge(purchase)
                        }
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Delivery failed for $productId", error)
                    emitEvent(
                        productId,
                        PurchaseEventStatus.Failed,
                        "Could not verify or deliver purchase: ${error.message ?: "unknown error"}"
                    )
                } finally {
                    if (pendingProductId == productId) pendingProductId = null
                }
            }
        }
    }

    /**
     * The callable verifies the token with Android Publisher v3, checks SKU and the SHA-256
     * obfuscated Firebase account ID, then grants the card credits transactionally/idempotently.
     */
    private data class CardVerification(
        val creditsAdded: Int,
        val alreadyGranted: Boolean
    )

    private suspend fun verifyCardPurchase(
        productId: String,
        purchase: Purchase
    ): CardVerification {
        val result = functions.getHttpsCallable("verifyCardPurchase").call(
            mapOf(
                "platform" to "android",
                "productId" to productId,
                "purchaseToken" to purchase.purchaseToken
            )
        ).await()
        val response = result.getData() as? Map<*, *>
            ?: throw IllegalStateException("The purchase server returned an invalid response.")
        if (response["verified"] != true || response["productId"]?.toString() != productId) {
            throw IllegalStateException("The purchase server did not verify this product.")
        }
        return CardVerification(
            creditsAdded = (response["creditsAdded"] as? Number)?.toInt() ?: 0,
            alreadyGranted = response["alreadyGranted"] == true
        )
    }

    /** Existing radius and general credit products retain their current Android delivery path. */
    private suspend fun deliverLegacyConsumable(
        productId: String,
        purchase: Purchase,
        userId: String
    ) {
        val orderId = purchase.orderId ?: purchase.purchaseToken
        val userRef = db.collection("users").document(userId)

        PoplurProducts.radiusPackages.firstOrNull { it.productId == productId }?.let { pkg ->
            val purchaseRef = userRef.collection("radiusPurchases").document(orderId)
            val delivered = db.runTransaction { transaction ->
                if (transaction.get(purchaseRef).exists()) return@runTransaction false
                transaction.set(
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
                transaction.set(
                    userRef,
                    mapOf(
                        "purchasedRadiusMeters" to FieldValue.increment(pkg.radiusMeters.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                true
            }.await()
            if (delivered) _purchasedRadiusDelta.value += pkg.radiusMeters
            _message.value = "Added ${formatRadius(pkg.radiusMeters)} to your radius."
            return
        }

        PoplurProducts.creditPackages.firstOrNull { it.productId == productId }?.let { pkg ->
            val purchaseRef = userRef.collection("creditPurchases").document(orderId)
            val balanceRef = db.collection("creatorCreditBalances").document(userId)
            val delivered = db.runTransaction { transaction ->
                if (transaction.get(purchaseRef).exists()) return@runTransaction false
                transaction.set(
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
                transaction.set(
                    balanceRef,
                    mapOf(
                        "availableCredits" to FieldValue.increment(pkg.credits.toLong()),
                        "lifetimeEarned" to FieldValue.increment(pkg.credits.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                true
            }.await()
            if (delivered) _creditsDelta.value += pkg.credits
            _message.value = "Added ${pkg.credits} Creator Credits!"
        }
    }

    private suspend fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = suspendCancellableCoroutine<BillingResult> { continuation ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                if (continuation.isActive) continuation.resume(billingResult)
            }
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw IllegalStateException(
                result.debugMessage.ifBlank { "Google Play could not finish this consumable purchase." }
            )
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

    private fun failLaunch(productId: String, message: String) {
        _message.value = message
        emitEvent(productId, PurchaseEventStatus.Failed, message)
    }

    private fun emitEvent(
        productId: String,
        status: PurchaseEventStatus,
        message: String,
        credits: Int = 0,
        transactionId: String? = null
    ) {
        _message.value = message
        _purchaseEvents.tryEmit(
            PurchaseEvent(productId, status, message, credits, transactionId)
        )
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun formatRadius(meters: Int): String =
        if (meters >= 1000) "${meters / 1000} km" else "$meters m"
}
