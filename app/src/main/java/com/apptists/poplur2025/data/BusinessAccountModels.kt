package com.apptists.poplur2025.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions

enum class AccountType(val wireValue: String, val label: String) {
    User("user", "User"),
    Business("business", "Business");

    companion object {
        fun fromWire(value: String?): AccountType =
            entries.firstOrNull { it.wireValue == value } ?: User
    }
}

enum class BusinessVerificationStatus(val wireValue: String, val label: String) {
    NotVerified("not_verified", "Not Verified"),
    PendingReview("pending_review", "Pending Review"),
    Verified("verified", "Verified"),
    Rejected("rejected", "Rejected"),
    NeedsMoreInfo("needs_more_info", "Needs More Info");

    companion object {
        fun fromWire(value: String?): BusinessVerificationStatus =
            entries.firstOrNull { it.wireValue == value } ?: NotVerified
    }
}

enum class BusinessVerificationMethod(val wireValue: String, val label: String) {
    OfficialEmailDomain("official_email_domain", "Official business email domain"),
    BusinessPhone("business_phone", "Phone linked to the business"),
    BusinessLicense("business_license", "Business license or registration"),
    GovernmentId("government_id", "Government ID of owner/admin"),
    UtilityOrBankStatement("utility_or_bank_statement", "Utility bill or bank statement"),
    VideoOrInPerson("video_or_in_person", "Video or in-person verification"),
    ThirdPartyPartner("third_party_partner", "Third-party partner verification");

    companion object {
        fun fromWire(value: String): BusinessVerificationMethod? =
            entries.firstOrNull { it.wireValue == value }
    }
}

data class BusinessProfile(
    val businessName: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 43.6532,
    val longitude: Double = -79.3832,
    val contactEmail: String = "",
    val contactPhone: String = "",
    val website: String = "",
    val logoPhotoNotes: String = "",
    val verificationStatus: BusinessVerificationStatus = BusinessVerificationStatus.NotVerified,
    val verificationMethods: List<String> = emptyList(),
    val proofSummary: String = "",
    val rejectionReason: String? = null,
    val requiresReverification: Boolean = false
) {
    val isVerified: Boolean get() = verificationStatus == BusinessVerificationStatus.Verified
}

object BusinessAccountService {
    fun saveAccount(
        uid: String,
        email: String,
        accountType: AccountType,
        businessProfile: BusinessProfile?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val username = email.trim().substringBefore("@").ifBlank { "poplur_user" }
        val userDocument = mutableMapOf<String, Any>(
            "accountType" to accountType.wireValue,
            "email" to email.trim(),
            "username" to username,
            "displayName" to if (accountType == AccountType.Business) {
                businessProfile?.businessName?.trim().takeUnless { it.isNullOrEmpty() } ?: username
            } else {
                username
            },
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        if (businessProfile != null) {
            val businessMap = businessProfile.toFirestoreMap()
            userDocument["businessProfile"] = businessMap
            userDocument["businessName"] = businessProfile.businessName.trim()
            userDocument["businessCategory"] = businessProfile.category.trim()
            userDocument["businessAddress"] = businessProfile.address.trim()
            userDocument["businessLocation"] = GeoPoint(businessProfile.latitude, businessProfile.longitude)
            userDocument["isBusinessAccount"] = true
            userDocument["isVerifiedBusiness"] = businessProfile.isVerified
        }

        db.collection("users")
            .document(uid)
            .set(userDocument, SetOptions.merge())
            .addOnSuccessListener {
                if (businessProfile == null) {
                    onResult(true, null)
                    return@addOnSuccessListener
                }

                val businessDocument = businessProfile.toFirestoreMap().toMutableMap().apply {
                    put("ownerUserID", uid)
                    put("email", email.trim())
                    put("accountType", AccountType.Business.wireValue)
                    put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                    put("submittedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                }
                db.collection("businessProfiles")
                    .document(uid)
                    .set(businessDocument, SetOptions.merge())
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { onResult(false, it.localizedMessage) }
            }
            .addOnFailureListener { onResult(false, it.localizedMessage) }
    }
}

fun BusinessProfile.toFirestoreMap(): Map<String, Any> =
    buildMap {
        put("businessName", businessName.trim())
        put("businessCategory", category.trim())
        put("businessDescription", description.trim())
        put("businessAddress", address.trim())
        put("latitude", latitude)
        put("longitude", longitude)
        put("location", GeoPoint(latitude, longitude))
        put("contactEmail", contactEmail.trim())
        put("contactPhone", contactPhone.trim())
        put("website", website.trim())
        put("logoPhotoNotes", logoPhotoNotes.trim())
        put("verificationStatus", verificationStatus.wireValue)
        put("verificationStatusLabel", verificationStatus.label)
        put("verificationMethods", verificationMethods)
        put(
            "verificationMethodLabels",
            verificationMethods.mapNotNull { BusinessVerificationMethod.fromWire(it)?.label }
        )
        put("proofSummary", proofSummary.trim())
        put("requiresReverification", requiresReverification)
        put("mapVisibility", true)
        put("markerType", "verified_business_profile")
        rejectionReason?.trim()?.takeIf { it.isNotEmpty() }?.let {
            put("rejectionReason", it)
        }
    }

fun Map<String, Any?>.poplurAccountType(): AccountType =
    AccountType.fromWire(this["accountType"] as? String)

fun Map<String, Any?>.businessProfileOrNull(): BusinessProfile? {
    val nested = (this["businessProfile"] as? Map<*, *>)
        ?.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }
        ?.toMap()
    val data = nested ?: this
    val name = data.stringValue("businessName", "name")
    val category = data.stringValue("businessCategory", "category")
    val address = data.stringValue("businessAddress", "address")
    if (name.isBlank() && category.isBlank() && address.isBlank()) return null

    val point = data["location"] as? GeoPoint
    val methods = (data["verificationMethods"] as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()

    return BusinessProfile(
        businessName = name,
        category = category,
        description = data.stringValue("businessDescription", "description"),
        address = address,
        latitude = data.doubleValue("latitude") ?: point?.latitude ?: 43.6532,
        longitude = data.doubleValue("longitude") ?: point?.longitude ?: -79.3832,
        contactEmail = data.stringValue("contactEmail"),
        contactPhone = data.stringValue("contactPhone"),
        website = data.stringValue("website"),
        logoPhotoNotes = data.stringValue("logoPhotoNotes"),
        verificationStatus = BusinessVerificationStatus.fromWire(data["verificationStatus"] as? String),
        verificationMethods = methods,
        proofSummary = data.stringValue("proofSummary"),
        rejectionReason = data["rejectionReason"] as? String,
        requiresReverification = data["requiresReverification"] as? Boolean ?: false
    )
}

private fun Map<String, Any?>.stringValue(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> this[key] as? String }.orEmpty().trim()

private fun Map<String, Any?>.doubleValue(key: String): Double? =
    when (val value = this[key]) {
        is Double -> value
        is Float -> value.toDouble()
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
