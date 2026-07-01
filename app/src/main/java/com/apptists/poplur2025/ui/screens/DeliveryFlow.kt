package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apptists.poplur2025.data.DeliveryCheckoutDraft
import com.apptists.poplur2025.data.DeliveryLocation
import com.apptists.poplur2025.data.DeliveryOrder
import com.apptists.poplur2025.data.DeliveryOrderStatus
import com.apptists.poplur2025.data.DeliveryRepository
import com.apptists.poplur2025.data.Post
import kotlinx.coroutines.launch

private val DeliveryGreen = Color(0xFF45FF61)
private val DeliveryPanel = Color.White.copy(alpha = 0.08f)
private val DeliveryStroke = Color.White.copy(alpha = 0.13f)

@Composable
fun DeliveryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = DeliveryGreen,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(999.dp),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Deliver", fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun DeliveryFlowDialog(
    post: Post,
    onDismiss: () -> Unit
) {
    var orderId by remember(post.id) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF163219),
                                Color.Black,
                                Color(0xFF0B0B13)
                            )
                        )
                    )
            ) {
                if (orderId == null) {
                    DeliveryCheckoutContent(
                        post = post,
                        onDismiss = onDismiss,
                        onOrderCreated = { orderId = it }
                    )
                } else {
                    DeliveryStatusContent(
                        orderId = orderId.orEmpty(),
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryCheckoutContent(
    post: Post,
    onDismiss: () -> Unit,
    onOrderCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var deliveryAddress by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var itemNotes by remember { mutableStateOf("") }
    var estimateText by remember { mutableStateOf("$6.99 estimate") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val draft = remember(deliveryAddress, contactName, phoneNumber, itemNotes, post.id) {
        DeliveryCheckoutDraft(
            deliveryAddress = deliveryAddress,
            contactName = contactName,
            phoneNumber = phoneNumber,
            itemNotes = itemNotes,
            dropoffLocation = DeliveryLocation(
                latitude = post.latitude ?: 0.0,
                longitude = post.longitude ?: 0.0,
                address = deliveryAddress,
                label = "Dropoff"
            )
        )
    }

    LaunchedEffect(draft) {
        runCatching { DeliveryRepository.getQuote(draft) }
            .onSuccess { estimateText = "${it.displayFee} estimate" }
            .onFailure { estimateText = "$6.99 estimate" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DeliveryHeader(
            title = "Deliver to Me",
            subtitle = post.displayName.ifBlank { post.username.ifBlank { "Poplur post" } },
            onDismiss = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DeliveryPickupCard(post)
            DeliveryTextField(
                title = "Delivery address",
                value = deliveryAddress,
                placeholder = "Street, unit, city",
                minLines = 2,
                onValueChange = { deliveryAddress = it }
            )
            DeliveryTextField(
                title = "Contact name",
                value = contactName,
                placeholder = "Name for courier",
                onValueChange = { contactName = it }
            )
            DeliveryTextField(
                title = "Phone number",
                value = phoneNumber,
                placeholder = "Courier contact number",
                onValueChange = { phoneNumber = it }
            )
            DeliveryTextField(
                title = "Item/package notes",
                value = itemNotes,
                placeholder = "Door code, handoff notes, item details",
                minLines = 3,
                onValueChange = { itemNotes = it }
            )
            DeliveryEstimateCard(estimateText)

            errorMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFFFF6565),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val address = deliveryAddress.trim()
                    val name = contactName.trim()
                    val phone = phoneNumber.trim()
                    if (address.isBlank() || name.isBlank() || phone.isBlank()) {
                        errorMessage = "Add a delivery address, contact name, and phone number."
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        runCatching { DeliveryRepository.createOrder(post, draft) }
                            .onSuccess { order ->
                                isSubmitting = false
                                onOrderCreated(order.id)
                            }
                            .onFailure { throwable ->
                                isSubmitting = false
                                errorMessage = throwable.localizedMessage ?: "Delivery request failed."
                            }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeliveryGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = DeliveryGreen.copy(alpha = 0.55f),
                    disabledContentColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSubmitting) "Requesting courier" else "Request Delivery",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DeliveryStatusContent(
    orderId: String,
    onDismiss: () -> Unit
) {
    var order by remember(orderId) { mutableStateOf<DeliveryOrder?>(null) }

    DisposableEffect(orderId) {
        val listener = DeliveryRepository.listenToOrder(orderId) { latest ->
            order = latest
        }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DeliveryHeader(
            title = "Delivery Status",
            subtitle = order?.provider?.displayName ?: "Mock Courier",
            onDismiss = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DeliveryOrderSummary(order)
            DeliveryStatusTimeline(order?.status ?: DeliveryOrderStatus.Requested)
            order?.trackingUrl?.takeIf { it.isNotBlank() }?.let { trackingUrl ->
                DeliveryTrackingCard(trackingUrl)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DeliveryHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeliveryPickupCard(post: Post) {
    DeliveryPanelBox {
        Text(
            text = "PICKUP",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = post.locationName.ifBlank { "Poplur post location" },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${"%.4f".format(post.latitude ?: 0.0)}, ${"%.4f".format(post.longitude ?: 0.0)}",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DeliveryTextField(
    title: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = Color.White.copy(alpha = 0.38f), fontWeight = FontWeight.Bold)
            },
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = DeliveryPanel,
                unfocusedContainerColor = DeliveryPanel,
                cursorColor = DeliveryGreen,
                focusedBorderColor = DeliveryGreen,
                unfocusedBorderColor = DeliveryStroke
            )
        )
    }
}

@Composable
private fun DeliveryEstimateCard(estimateText: String) {
    DeliveryPanelBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ESTIMATED DELIVERY FEE",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = estimateText,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Icon(Icons.Default.Bolt, contentDescription = null, tint = DeliveryGreen)
        }
    }
}

@Composable
private fun DeliveryOrderSummary(order: DeliveryOrder?) {
    DeliveryPanelBox {
        Text(
            text = order?.status?.label ?: DeliveryOrderStatus.Requested.label,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = order?.feeEstimate?.displayFee ?: "$6.99",
            color = DeliveryGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = order?.dropoffLocation?.address?.ifBlank { "Delivery address pending" } ?: "Delivery address pending",
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DeliveryStatusTimeline(currentStatus: DeliveryOrderStatus) {
    val timeline = listOf(
        DeliveryOrderStatus.Requested,
        DeliveryOrderStatus.CourierAssigned,
        DeliveryOrderStatus.PickedUp,
        DeliveryOrderStatus.InTransit,
        DeliveryOrderStatus.Delivered
    )
    DeliveryPanelBox {
        timeline.forEach { status ->
            DeliveryStatusRow(
                status = status,
                isComplete = currentStatus != DeliveryOrderStatus.FailedCancelled &&
                    status.sortIndex <= currentStatus.sortIndex,
                isFailure = false
            )
        }
        if (currentStatus == DeliveryOrderStatus.FailedCancelled) {
            DeliveryStatusRow(
                status = DeliveryOrderStatus.FailedCancelled,
                isComplete = false,
                isFailure = true
            )
        }
    }
}

@Composable
private fun DeliveryStatusRow(
    status: DeliveryOrderStatus,
    isComplete: Boolean,
    isFailure: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                isFailure -> Icons.Default.Close
                isComplete -> Icons.Default.CheckCircle
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
                isFailure -> Color(0xFFFF5252)
                isComplete -> DeliveryGreen
                else -> Color.White.copy(alpha = 0.32f)
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = status.label,
            color = if (isComplete || isFailure) Color.White else Color.White.copy(alpha = 0.42f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun DeliveryTrackingCard(trackingUrl: String) {
    DeliveryPanelBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = DeliveryGreen)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TRACKING",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = trackingUrl,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeliveryPanelBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DeliveryPanel)
            .padding(16.dp),
        content = content
    )
}
