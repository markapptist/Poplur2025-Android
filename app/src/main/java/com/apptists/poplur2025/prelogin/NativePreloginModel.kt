package com.apptists.poplur2025.prelogin

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apptists.poplur2025.analytics.AnalyticsRelay
import com.apptists.poplur2025.data.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import org.json.JSONObject
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal suspend fun <T> Task<T>.nativeAwait(): T = suspendCoroutine { c -> addOnSuccessListener { c.resume(it) }.addOnFailureListener { c.resumeWithException(it) } }

enum class PreloginStep { Welcome, SignIn, Account, Intro, Credentials, Business, Verification, Review, Photo, Gender }
data class BusinessPlace(val name: String, val address: String, val latitude: Double, val longitude: Double)

object PreloginValidation {
    fun credentials(email: String, password: String, confirmation: String, birthday: LocalDate?, agreed: Boolean, today: LocalDate = LocalDate.now()): String? = when {
        !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) -> "Enter a valid email address."
        password.length < 6 -> "Use a password with at least 6 characters."
        password != confirmation -> "Your passwords do not match."
        birthday == null -> "Choose your date of birth."
        birthday > today || Period.between(birthday, today).years < 13 -> "You must be at least 13 years old to use Poplur."
        !agreed -> "Please agree to the Terms of Service to continue."
        else -> null
    }
    fun previous(step: PreloginStep): PreloginStep = when (step) {
        PreloginStep.Welcome, PreloginStep.SignIn, PreloginStep.Account -> PreloginStep.Welcome
        PreloginStep.Intro -> PreloginStep.Account
        PreloginStep.Credentials -> PreloginStep.Intro
        PreloginStep.Business, PreloginStep.Photo -> PreloginStep.Credentials
        PreloginStep.Verification -> PreloginStep.Business
        PreloginStep.Review -> PreloginStep.Verification
        PreloginStep.Gender -> PreloginStep.Photo
    }
}

object NativePreloginDraft {
    fun preferences(context: android.content.Context) = context.getSharedPreferences("poplur_pending_onboarding", 0)
    fun saved(context: android.content.Context): JSONObject? = runCatching { JSONObject(preferences(context).getString("draft", null) ?: return null) }.getOrNull()
    fun isPending(context: android.content.Context): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return saved(context)?.optString("email")?.equals(user.email, ignoreCase = true) == true
    }
    fun photoFile(context: android.content.Context) = File(context.filesDir, "native-onboarding-photo.jpg")
    fun clear(context: android.content.Context) { preferences(context).edit().remove("draft").apply(); photoFile(context).delete() }
}

class NativePreloginModel(application: Application) : AndroidViewModel(application) {
    var step by mutableStateOf(PreloginStep.Welcome)
    var accountType by mutableStateOf(AccountType.User)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmation by mutableStateOf("")
    var birthday by mutableStateOf<LocalDate?>(null)
    var agreed by mutableStateOf(false)
    var photo by mutableStateOf<Uri?>(null)
    var gender by mutableStateOf("")
    var query by mutableStateOf("")
    var places by mutableStateOf<List<BusinessPlace>>(emptyList())
    var place by mutableStateOf<BusinessPlace?>(null)
    var category by mutableStateOf("")
    var phone by mutableStateOf("")
    var proof by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var searching by mutableStateOf(false)
    var completed by mutableStateOf(false)
    var createdUID by mutableStateOf<String?>(null)
        private set
    private var searchJob: Job? = null
    init { resumePendingAccount() }
    private fun resumePendingAccount(): Boolean {
        val application = getApplication<Application>()
        if (!NativePreloginDraft.isPending(application)) return false
        val draft = NativePreloginDraft.saved(application) ?: return false
        run {
            email = draft.optString("email"); accountType = AccountType.fromWire(draft.optString("accountType"))
            birthday = runCatching { LocalDate.parse(draft.optString("birthday")) }.getOrNull()
            agreed = true; gender = draft.optString("gender"); category = draft.optString("category")
            phone = draft.optString("phone"); proof = draft.optString("proof")
            draft.optJSONObject("place")?.let { p -> place = BusinessPlace(p.optString("name"), p.optString("address"), p.getDouble("latitude"), p.getDouble("longitude")) }
            photo = NativePreloginDraft.photoFile(application).takeIf { it.exists() }?.let(Uri::fromFile)
            createdUID = FirebaseAuth.getInstance().currentUser?.uid
            step = if (accountType == AccountType.Business) PreloginStep.Review else PreloginStep.Gender
            error = "Finish saving your profile to continue to Poplur."
        }
        return true
    }
    private suspend fun saveDraft() {
        val context = getApplication<Application>()
        val draft = JSONObject().put("email", email.trim()).put("accountType", accountType.wireValue).put("birthday", birthday.toString()).put("gender", gender).put("category", category).put("phone", phone).put("proof", proof)
        place?.let { p -> draft.put("place", JSONObject().put("name", p.name).put("address", p.address).put("latitude", p.latitude).put("longitude", p.longitude)) }
        if (photo != null && createdUID == null) withContext(Dispatchers.IO) {
            val resolver = context.contentResolver; val uri = photo!!
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply { inSampleSize = Integer.highestOneBit((maxOf(bounds.outWidth, bounds.outHeight) / 1024).coerceAtLeast(1)) }
            val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: kotlin.error("This photo could not be opened.")
            val orientation = resolver.openInputStream(uri).use { stream -> if (stream != null) android.media.ExifInterface(stream).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) else 1 }
            val scale = minOf(1f, 1024f / maxOf(bitmap.width, bitmap.height))
            val matrix = android.graphics.Matrix().apply {
                postScale(scale, scale)
                when (orientation) {
                    2 -> postScale(-1f, 1f)
                    3 -> postRotate(180f)
                    4 -> postScale(1f, -1f)
                    5 -> { postRotate(90f); postScale(-1f, 1f) }
                    6 -> postRotate(90f)
                    7 -> { postRotate(-90f); postScale(-1f, 1f) }
                    8 -> postRotate(-90f)
                }
            }
            val normalized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            try { NativePreloginDraft.photoFile(context).outputStream().use { check(normalized.compress(Bitmap.CompressFormat.JPEG, 82, it)) } } finally { if (normalized !== bitmap) normalized.recycle(); bitmap.recycle() }
        }
        NativePreloginDraft.preferences(context).edit().putString("draft", draft.toString()).commit()
    }
    fun go(next: PreloginStep) { if (!busy && createdUID == null) { step = next; error = null } }
    fun back() = go(PreloginValidation.previous(step))
    fun advanceCredentials() {
        error = PreloginValidation.credentials(email, password, confirmation, birthday, agreed)
        if (error == null) go(if (accountType == AccountType.Business) PreloginStep.Business else PreloginStep.Photo)
    }
    @Suppress("DEPRECATION")
    fun search() {
        searchJob?.cancel()
        if (query.isBlank()) { places = emptyList(); return }
        val text = query.trim()
        searchJob = viewModelScope.launch {
            searching = true; error = null
            try {
                places = withContext(Dispatchers.IO) {
                    Geocoder(getApplication()).getFromLocationName(text, 8).orEmpty().map { address ->
                        BusinessPlace(text, address.getAddressLine(0) ?: text, address.latitude, address.longitude)
                    }
                }
                if (places.isEmpty()) error = "No places found. Try the business name, address and city."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { error = e.localizedMessage ?: "Search is unavailable. Please try again." }
            finally { searching = false }
        }
    }
    fun signIn(onSuccess: (() -> Unit)? = null) {
        if (busy) return
        if (email.isBlank() || password.isEmpty()) { error = "Enter your email and password."; return }
        busy = true; error = null
        viewModelScope.launch {
            try {
                val result = FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password).nativeAwait()
                AnalyticsRelay.logLogin(result.user?.uid)
                if (resumePendingAccount()) { password = ""; confirmation = ""; return@launch }
                if (onSuccess != null) onSuccess() else finish()
            } catch (e: Exception) { error = e.localizedMessage }
            finally { busy = false }
        }
    }
    fun finish() { password = ""; confirmation = ""; completed = true }
    fun resetPassword() {
        if (busy) return
        if (!email.contains("@")) { error = "Enter your email address first."; return }
        busy = true; error = null
        viewModelScope.launch {
            try { FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim()).nativeAwait(); notice = "If this email has an account, a password reset link will arrive shortly." }
            catch (e: Exception) { error = e.localizedMessage }
            finally { busy = false }
        }
    }
    fun submit() {
        if (busy) return
        if (createdUID == null) {
            error = PreloginValidation.credentials(email, password, confirmation, birthday, agreed)
            if (error != null) return
        }
        if (accountType == AccountType.User && (photo == null || gender !in listOf("male", "female"))) { error = "Choose your profile photo and cube head."; return }
        if (accountType == AccountType.Business && (place == null || category.isBlank() || proof.isBlank())) { error = "Choose your business, category, and verification details."; return }
        busy = true; error = null
        viewModelScope.launch {
            try {
                saveDraft()
                val uid = createdUID ?: FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password).nativeAwait().user!!.uid.also { createdUID = it; AnalyticsRelay.logSignUp(it) }
                val business = if (accountType == AccountType.Business) place!!.let { p -> BusinessProfile(
                    businessName = p.name, address = p.address, latitude = p.latitude, longitude = p.longitude,
                    category = category, contactEmail = email.trim(), contactPhone = phone,
                    verificationStatus = BusinessVerificationStatus.PendingReview,
                    verificationMethods = listOf(BusinessVerificationMethod.OfficialEmailDomain.wireValue), proofSummary = proof
                ) } else null
                suspendCoroutine<Unit> { c -> BusinessAccountService.saveAccount(uid, email, accountType, business) { ok, message -> if (ok) c.resume(Unit) else c.resumeWithException(IllegalStateException(message ?: "Profile could not be saved.")) } }
                val extras = mutableMapOf<String, Any>("dateOfBirth" to Timestamp(Date.from(birthday!!.atStartOfDay(ZoneId.systemDefault()).toInstant())))
                if (photo != null) {
                    val photoFile = NativePreloginDraft.photoFile(getApplication())
                    withContext(Dispatchers.IO) { photoFile.copyTo(File(getApplication<Application>().filesDir, "profile.jpg"), overwrite = true) }
                    val reference = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid.jpg")
                    reference.putFile(Uri.fromFile(photoFile), StorageMetadata.Builder().setContentType("image/jpeg").build()).nativeAwait()
                    extras["profilePictureURL"] = reference.downloadUrl.nativeAwait().toString()
                    if (accountType == AccountType.User) {
                        extras["profileGender"] = gender
                        getApplication<Application>().getSharedPreferences("poplur_native_onboarding", 0).edit().putString("uid", uid).putString("gender", gender).apply()
                    }
                }
                FirebaseFirestore.getInstance().collection("users").document(uid).set(extras, SetOptions.merge()).nativeAwait()
                NativePreloginDraft.clear(getApplication())
                finish()
            } catch (e: Exception) {
                if (createdUID == null) NativePreloginDraft.clear(getApplication())
                error = if (createdUID == null) e.localizedMessage else "Your account was created, but your profile could not be saved. Tap Retry to finish. ${e.localizedMessage}"
            } finally { busy = false }
        }
    }
}
