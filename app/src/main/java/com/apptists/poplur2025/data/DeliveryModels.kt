package com.apptists.poplur2025.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

enum class DeliveryProviderKind(val rawValue: String, val displayName: String) {
    UberDirect("uber_direct", "Uber Direct"),
    DoorDashDrive("doordash_drive", "DoorDash Drive"),
    Mock("mock", "Mock Courier");

    companion object {
        fun from(rawValue: String?): DeliveryProviderKind =
            entries.firstOrNull { it.rawValue == rawValue } ?: Mock
    }
}

enum class DeliveryOrderStatus(val rawValue: String, val label: String, val sortIndex: Int) {
    Requested("requested", "Requested", 0),
    CourierAssigned("courier_assigned", "Courier assigned", 1),
    PickedUp("picked_up", "Picked up", 2),
    InTransit("in_transit", "In transit", 3),
    Delivered("delivered", "Delivered", 4),
    FailedCancelled("failed_cancelled", "Failed/cancelled", 5);

    companion object {
        fun from(rawValue: String?): DeliveryOrderStatus =
            entries.firstOrNull { it.rawValue == rawValue } ?: Requested
    }
}

data class DeliveryLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val label: String
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "latitude" to latitude,
        "longitude" to longitude,
        "address" to address,
        "label" to label
    )
}

data class DeliveryCheckoutDraft(
    val deliveryAddress: String,
    val contactName: String,
    val phoneNumber: String,
    val itemNotes: String,
    val dropoffLocation: DeliveryLocation
)

data class DeliveryQuote(
    val provider: DeliveryProviderKind,
    val feeEstimate: Double,
    val currency: String,
    val etaMinutes: Int
) {
    val displayFee: String
        get() = "$" + String.format(Locale.US, "%.2f", feeEstimate)

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "amount" to feeEstimate,
        "currency" to currency,
        "display" to displayFee,
        "etaMinutes" to etaMinutes
    )
}

data class ProviderDelivery(
    val id: String,
    val trackingUrl: String?
)

data class DeliveryOrder(
    val id: String,
    val postId: String,
    val buyerUserId: String,
    val merchantUserId: String,
    val pickupLocation: DeliveryLocation,
    val dropoffLocation: DeliveryLocation,
    val provider: DeliveryProviderKind,
    val providerDeliveryId: String,
    val status: DeliveryOrderStatus,
    val trackingUrl: String?,
    val feeEstimate: DeliveryQuote,
    val contactName: String,
    val phoneNumber: String,
    val itemNotes: String
)

interface DeliveryProvider {
    val kind: DeliveryProviderKind
    suspend fun createDelivery(orderId: String, request: DeliveryCheckoutDraft, quote: DeliveryQuote): ProviderDelivery
    suspend fun getQuote(request: DeliveryCheckoutDraft): DeliveryQuote
    suspend fun getDeliveryStatus(providerDeliveryId: String): DeliveryOrderStatus
    suspend fun cancelDelivery(providerDeliveryId: String)
}

class MockDeliveryProvider : DeliveryProvider {
    override val kind: DeliveryProviderKind = DeliveryProviderKind.Mock

    override suspend fun createDelivery(
        orderId: String,
        request: DeliveryCheckoutDraft,
        quote: DeliveryQuote
    ): ProviderDelivery = ProviderDelivery(
        id = "mock_$orderId",
        trackingUrl = "https://poplur.local/delivery/$orderId"
    )

    override suspend fun getQuote(request: DeliveryCheckoutDraft): DeliveryQuote =
        DeliveryQuote(provider = kind, feeEstimate = 6.99, currency = "USD", etaMinutes = 28)

    override suspend fun getDeliveryStatus(providerDeliveryId: String): DeliveryOrderStatus =
        DeliveryOrderStatus.InTransit

    override suspend fun cancelDelivery(providerDeliveryId: String) = Unit
}

class UberDirectDeliveryProvider : DeliveryProvider {
    override val kind: DeliveryProviderKind = DeliveryProviderKind.UberDirect

    override suspend fun createDelivery(
        orderId: String,
        request: DeliveryCheckoutDraft,
        quote: DeliveryQuote
    ): ProviderDelivery {
        // TODO: Call Uber Direct Create Delivery with Poplur pickup/dropoff payload.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun getQuote(request: DeliveryCheckoutDraft): DeliveryQuote {
        // TODO: Call Uber Direct quote endpoint once credentials are available.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun getDeliveryStatus(providerDeliveryId: String): DeliveryOrderStatus {
        // TODO: Map Uber Direct delivery statuses into DeliveryOrderStatus.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun cancelDelivery(providerDeliveryId: String) {
        // TODO: Call Uber Direct cancel endpoint.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }
}

class DoorDashDriveDeliveryProvider : DeliveryProvider {
    override val kind: DeliveryProviderKind = DeliveryProviderKind.DoorDashDrive

    override suspend fun createDelivery(
        orderId: String,
        request: DeliveryCheckoutDraft,
        quote: DeliveryQuote
    ): ProviderDelivery {
        // TODO: Call DoorDash Drive delivery creation with Poplur pickup/dropoff payload.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun getQuote(request: DeliveryCheckoutDraft): DeliveryQuote {
        // TODO: Call DoorDash Drive quote endpoint once credentials are available.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun getDeliveryStatus(providerDeliveryId: String): DeliveryOrderStatus {
        // TODO: Map DoorDash Drive statuses into DeliveryOrderStatus.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }

    override suspend fun cancelDelivery(providerDeliveryId: String) {
        // TODO: Call DoorDash Drive cancel endpoint.
        error("${kind.displayName} is ready for API key integration, but is not configured yet.")
    }
}

object DeliveryRepository {
    private const val CollectionName = "delivery_orders"
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val provider: DeliveryProvider = MockDeliveryProvider()
    private val mockScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getQuote(request: DeliveryCheckoutDraft): DeliveryQuote =
        provider.getQuote(request)

    suspend fun createOrder(post: Post, draft: DeliveryCheckoutDraft): DeliveryOrder {
        val orderRef = firestore.collection(CollectionName).document()
        val quote = provider.getQuote(draft)
        val providerDelivery = provider.createDelivery(orderRef.id, draft, quote)
        val buyerUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_mock_buyer"
        val pickupAddress = post.locationName.ifBlank { "Poplur post pickup" }
        val pickupLocation = DeliveryLocation(
            latitude = post.latitude ?: 0.0,
            longitude = post.longitude ?: 0.0,
            address = pickupAddress,
            label = "Pickup"
        )

        val data = mutableMapOf<String, Any?>(
            "postId" to post.id,
            "buyerUserId" to buyerUserId,
            "merchantUserId" to post.userId,
            "postOwnerId" to post.userId,
            "pickupLocation" to pickupLocation.toFirestoreMap(),
            "dropoffLocation" to draft.dropoffLocation.toFirestoreMap(),
            "provider" to provider.kind.rawValue,
            "providerDeliveryId" to providerDelivery.id,
            "status" to DeliveryOrderStatus.Requested.rawValue,
            "feeEstimate" to quote.toFirestoreMap(),
            "contactName" to draft.contactName,
            "phoneNumber" to draft.phoneNumber,
            "itemNotes" to draft.itemNotes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        providerDelivery.trackingUrl?.let { data["trackingUrl"] = it }

        orderRef.set(data).await()

        if (provider.kind == DeliveryProviderKind.Mock) {
            startMockStatusSimulation(orderRef.id)
        }

        return DeliveryOrder(
            id = orderRef.id,
            postId = post.id,
            buyerUserId = buyerUserId,
            merchantUserId = post.userId,
            pickupLocation = pickupLocation,
            dropoffLocation = draft.dropoffLocation,
            provider = provider.kind,
            providerDeliveryId = providerDelivery.id,
            status = DeliveryOrderStatus.Requested,
            trackingUrl = providerDelivery.trackingUrl,
            feeEstimate = quote,
            contactName = draft.contactName,
            phoneNumber = draft.phoneNumber,
            itemNotes = draft.itemNotes
        )
    }

    fun listenToOrder(orderId: String, onUpdate: (DeliveryOrder?) -> Unit): ListenerRegistration =
        firestore.collection(CollectionName).document(orderId).addSnapshotListener { snapshot, _ ->
            onUpdate(snapshot?.data?.let { data -> data.toDeliveryOrder(snapshot.id) })
        }

    suspend fun getDeliveryStatus(order: DeliveryOrder): DeliveryOrderStatus =
        provider.getDeliveryStatus(order.providerDeliveryId)

    suspend fun cancelOrder(order: DeliveryOrder) {
        provider.cancelDelivery(order.providerDeliveryId)
        firestore.collection(CollectionName).document(order.id).update(
            mapOf(
                "status" to DeliveryOrderStatus.FailedCancelled.rawValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun startMockStatusSimulation(orderId: String) {
        mockScope.launch {
            listOf(
                DeliveryOrderStatus.CourierAssigned,
                DeliveryOrderStatus.PickedUp,
                DeliveryOrderStatus.InTransit,
                DeliveryOrderStatus.Delivered
            ).forEach { status ->
                delay(3_500L)
                runCatching {
                    firestore.collection(CollectionName).document(orderId).update(
                        mapOf(
                            "status" to status.rawValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }
    }
}

fun Post.isDeliveryEligible(): Boolean {
    val ownerId = userId.trim()
    return ownerId.isNotEmpty() &&
        !ownerId.startsWith("poplur_ad_") &&
        !ownerId.startsWith("sponsored_ad_") &&
        ownerId != "ad_partner" &&
        !isLiveStream &&
        latitude != null &&
        longitude != null
}

private fun Map<String, Any>.toDeliveryOrder(id: String): DeliveryOrder {
    val provider = DeliveryProviderKind.from(this["provider"] as? String)
    return DeliveryOrder(
        id = id,
        postId = this["postId"] as? String ?: "",
        buyerUserId = this["buyerUserId"] as? String ?: "",
        merchantUserId = this["merchantUserId"] as? String ?: this["postOwnerId"] as? String ?: "",
        pickupLocation = (this["pickupLocation"] as? Map<*, *>).toDeliveryLocation("Pickup"),
        dropoffLocation = (this["dropoffLocation"] as? Map<*, *>).toDeliveryLocation("Dropoff"),
        provider = provider,
        providerDeliveryId = this["providerDeliveryId"] as? String ?: "",
        status = DeliveryOrderStatus.from(this["status"] as? String),
        trackingUrl = this["trackingUrl"] as? String,
        feeEstimate = (this["feeEstimate"] as? Map<*, *>).toDeliveryQuote(provider),
        contactName = this["contactName"] as? String ?: "",
        phoneNumber = this["phoneNumber"] as? String ?: "",
        itemNotes = this["itemNotes"] as? String ?: ""
    )
}

private fun Map<*, *>?.toDeliveryLocation(fallbackLabel: String): DeliveryLocation =
    DeliveryLocation(
        latitude = this?.get("latitude") as? Double ?: 0.0,
        longitude = this?.get("longitude") as? Double ?: 0.0,
        address = this?.get("address") as? String ?: "",
        label = this?.get("label") as? String ?: fallbackLabel
    )

private fun Map<*, *>?.toDeliveryQuote(provider: DeliveryProviderKind): DeliveryQuote =
    DeliveryQuote(
        provider = provider,
        feeEstimate = this?.get("amount") as? Double ?: 0.0,
        currency = this?.get("currency") as? String ?: "USD",
        etaMinutes = (this?.get("etaMinutes") as? Number)?.toInt() ?: 0
    )
