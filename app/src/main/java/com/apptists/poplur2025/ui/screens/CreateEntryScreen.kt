package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.PoplurAIContentType
import com.apptists.poplur2025.data.PoplurAIStackConfig
import com.apptists.poplur2025.data.PoplurAIGenerationStatus
import com.apptists.poplur2025.data.PoplurPlan
import com.apptists.poplur2025.data.PremiumAIAction
import com.apptists.poplur2025.data.PremiumAIEntitlement
import com.apptists.poplur2025.data.PremiumAIService
import kotlinx.coroutines.launch

@Composable
fun CreateEntryScreen(
    onOriginalContent: () -> Unit,
    onPoplurAI: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(PoplurAIContentType.Image) }
    var prompt by remember { mutableStateOf("") }
    var credits by remember { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf(PoplurPlan.Free) }
    var status by remember { mutableStateOf(PoplurAIGenerationStatus.Queued) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var locationName by remember { mutableStateOf("") }
    var radius by remember { mutableFloatStateOf(1_000f) }
    var wantsBoost by remember { mutableStateOf(false) }
    var showGameMaker by remember { mutableStateOf(false) }
    val config = remember { PoplurAIStackConfig() }
    val cost = config.estimatedCost(selectedType)
    val canGenerate = plan.canGenerateAI(credits) && credits >= cost && prompt.isNotBlank()
    val creationTypes = remember {
        listOf(
            PoplurAIContentType.Image,
            PoplurAIContentType.Video,
            PoplurAIContentType.ThreeDModel,
            PoplurAIContentType.Voice,
            PoplurAIContentType.Music,
            PoplurAIContentType.Game,
            PoplurAIContentType.Vfx,
            PoplurAIContentType.Coupon,
            PoplurAIContentType.Campaign
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Create", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Generate premium AI content with Creator Credits, then publish it to the map.",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { credits += 25 },
                    label = { Text("$credits Creator Credits", fontWeight = FontWeight.Black) },
                    leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null) }
                )
                PlanButton("Free", plan == PoplurPlan.Free) { plan = PoplurPlan.Free }
                PlanButton("Creator", plan == PoplurPlan.Creator) { plan = PoplurPlan.Creator }
                PlanButton("Business", plan == PoplurPlan.Business) { plan = PoplurPlan.Business }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 390.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(creationTypes) { type ->
                    CreationTypeCard(
                        type = type,
                        selected = type == selectedType,
                        cost = config.estimatedCost(type),
                        onClick = {
                            selectedType = type
                            status = PoplurAIGenerationStatus.Queued
                            resultText = null
                            // The Games tool opens the dedicated Game Maker.
                            if (type == PoplurAIContentType.Game) showGameMaker = true
                        }
                    )
                }
            }

            StackPanel {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedType.label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(selectedType.providerLabel, color = Color(0xFF4DFF58), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$cost", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("credits", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it.take(config.maxPromptLength) },
                    placeholder = {
                        Text("Describe what Poplur AI should create.", color = Color.White.copy(alpha = 0.36f))
                    },
                    minLines = 4,
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4DFF58),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!canGenerate) {
                    Text(
                        "Use Creator Credits to generate AI content.",
                        color = Color(0xFFFFB547),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = {
                        credits -= cost
                        status = PoplurAIGenerationStatus.Processing
                        resultText = "Queued through Poplur AI backend. Provider keys stay server-side."
                        onPoplurAI()
                    },
                    enabled = canGenerate,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate", fontWeight = FontWeight.Black)
                }
            }

            PremiumAIAssistPanel(
                mediaType = selectedType,
                locationName = locationName,
                basePrompt = prompt,
                onUseGeneratedText = {
                    prompt = it
                    resultText = it
                    status = PoplurAIGenerationStatus.Completed
                },
                onUpgrade = {
                    resultText = "Poplur AI is included with Premium. Open Premium from settings or the store to upgrade."
                }
            )

            StackPanel {
                Text("Publish to Map", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    placeholder = { Text("Location name", color = Color.White.copy(alpha = 0.36f)) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${radius.toInt()} m radius", color = Color.White.copy(alpha = 0.74f), fontSize = 13.sp, fontWeight = FontWeight.Black)
                Slider(value = radius, onValueChange = { radius = it }, valueRange = 250f..10_000f, steps = 38)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = wantsBoost, onCheckedChange = { wantsBoost = it })
                    Text("Boost radius with credits", color = Color.White, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = { resultText = "Ready to publish ${selectedType.wireValue} to ${locationName.ifBlank { "selected map location" }}." },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publish to Map", fontWeight = FontWeight.Black)
                }
            }

            StackPanel {
                Text("Job Status", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(status.wireValue.replaceFirstChar { it.uppercase() }, color = Color(0xFF4DFF58), fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        resultText ?: "Generated content preview will appear here.",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOriginalContent, modifier = Modifier.weight(1f)) {
                    Text("Camera First", fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onPoplurAI, modifier = Modifier.weight(1f)) {
                    Text("AI Studio", fontWeight = FontWeight.Black)
                }
            }
        }

        // Game Maker overlay (create → save → place on map).
        if (showGameMaker) {
            com.apptists.poplur2025.ui.games.GameMakerScreen(onDismiss = { showGameMaker = false })
        }
    }
}

@Composable
private fun PremiumAIAssistPanel(
    mediaType: PoplurAIContentType,
    locationName: String,
    basePrompt: String,
    onUseGeneratedText: (String) -> Unit,
    onUpgrade: () -> Unit
) {
    val service = remember { PremiumAIService() }
    val scope = rememberCoroutineScope()
    var entitlement by remember { mutableStateOf<PremiumAIEntitlement?>(null) }
    var selectedAction by remember { mutableStateOf(PremiumAIAction.GenerateCaption) }
    var captionDirection by remember { mutableStateOf("") }
    var ideaCategory by remember { mutableStateOf("") }
    var ideaVibe by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var offerDetails by remember { mutableStateOf("") }
    var translateText by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("") }
    var generatedText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entitlement = runCatching { service.fetchEntitlement() }
            .onFailure { errorText = it.localizedMessage }
            .getOrElse {
                PremiumAIEntitlement(
                    isPremium = false,
                    premiumTier = null,
                    monthlyLimit = 0,
                    usedThisMonth = 0,
                    month = ""
                )
            }
    }

    val canRun = when (selectedAction) {
        PremiumAIAction.GenerateCaption -> captionDirection.ifBlank { basePrompt }.isNotBlank()
        PremiumAIAction.SuggestPostIdea -> ideaCategory.isNotBlank() && ideaVibe.isNotBlank()
        PremiumAIAction.BusinessPromo -> businessName.isNotBlank() && businessType.isNotBlank() && offerDetails.isNotBlank()
        PremiumAIAction.TranslateCaption -> translateText.ifBlank { basePrompt }.isNotBlank() && targetLanguage.isNotBlank()
    }

    StackPanel {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Premium AI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                val usageText = when {
                    entitlement == null -> "Checking Premium access..."
                    entitlement?.isPremium != true -> "Included with Premium."
                    else -> "${entitlement?.remaining ?: 0} AI generations left this month"
                }
                Text(usageText, color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (loading || entitlement == null) {
                CircularProgressIndicator(color = Color(0xFF4DFF58), modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            }
        }

        if (entitlement?.isPremium != true) {
            Text(
                "Poplur AI is included with Premium.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Upgrade to Premium", fontWeight = FontWeight.Black)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumAIAction.entries.forEach { action ->
                    val selected = action == selectedAction
                    Button(
                        onClick = {
                            selectedAction = action
                            errorText = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color.White else Color.White.copy(alpha = 0.12f),
                            contentColor = if (selected) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(action.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(action.label, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            when (selectedAction) {
                PremiumAIAction.GenerateCaption -> PremiumAITextField(
                    label = "Caption direction",
                    value = captionDirection,
                    onValueChange = { captionDirection = it },
                    placeholder = basePrompt.ifBlank { "Tell AI the style or angle you want." }
                )
                PremiumAIAction.SuggestPostIdea -> {
                    Text("Location: ${locationName.ifBlank { "Selected location" }}", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                    PremiumAITextField("Category", ideaCategory, { ideaCategory = it }, "Food, news, nightlife, shopping...")
                    PremiumAITextField("Vibe", ideaVibe, { ideaVibe = it }, "Funny, luxury, urgent, cozy...")
                }
                PremiumAIAction.BusinessPromo -> {
                    PremiumAITextField("Business name", businessName, { businessName = it }, "Business name")
                    PremiumAITextField("Business type", businessType, { businessType = it }, "Cafe, barber, boutique...")
                    PremiumAITextField("Offer details", offerDetails, { offerDetails = it }, "What should the promo include?")
                    Text("Location: ${locationName.ifBlank { "Selected location" }}", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                }
                PremiumAIAction.TranslateCaption -> {
                    PremiumAITextField("Caption", translateText, { translateText = it }, basePrompt.ifBlank { "Paste caption to translate." })
                    PremiumAITextField("Target language", targetLanguage, { targetLanguage = it }, "Spanish, French, Arabic...")
                }
            }

            errorText?.let {
                Text(it, color = Color(0xFFFF5B5B), fontSize = 13.sp, fontWeight = FontWeight.Black)
            }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        errorText = null
                        runCatching {
                            service.run(
                                selectedAction,
                                premiumAIInput(
                                    selectedAction = selectedAction,
                                    mediaType = mediaType,
                                    locationName = locationName,
                                    basePrompt = basePrompt,
                                    captionDirection = captionDirection,
                                    ideaCategory = ideaCategory,
                                    ideaVibe = ideaVibe,
                                    businessName = businessName,
                                    businessType = businessType,
                                    offerDetails = offerDetails,
                                    translateText = translateText,
                                    targetLanguage = targetLanguage
                                )
                            )
                        }.onSuccess { result ->
                            entitlement = result.usage
                            generatedText = result.editableText
                        }.onFailure {
                            errorText = it.localizedMessage ?: "AI generation failed. Please try again."
                        }
                        loading = false
                    }
                },
                enabled = canRun && !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Generating..." else "Run AI Assist", fontWeight = FontWeight.Black)
            }

            if (generatedText.isNotBlank()) {
                Text("Editable Result", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = generatedText,
                    onValueChange = { generatedText = it },
                    minLines = 5,
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onUseGeneratedText(generatedText) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Use In Create Prompt", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremiumAITextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.36f)) },
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4DFF58),
                unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun PremiumAIAction.icon(): ImageVector = when (this) {
    PremiumAIAction.GenerateCaption -> Icons.Filled.Textsms
    PremiumAIAction.SuggestPostIdea -> Icons.Filled.Lightbulb
    PremiumAIAction.BusinessPromo -> Icons.Filled.Campaign
    PremiumAIAction.TranslateCaption -> Icons.Filled.Language
}

private fun premiumAIInput(
    selectedAction: PremiumAIAction,
    mediaType: PoplurAIContentType,
    locationName: String,
    basePrompt: String,
    captionDirection: String,
    ideaCategory: String,
    ideaVibe: String,
    businessName: String,
    businessType: String,
    offerDetails: String,
    translateText: String,
    targetLanguage: String
): Map<String, Any> {
    val mediaWireValue = if (mediaType == PoplurAIContentType.Video || mediaType == PoplurAIContentType.VfxVideo) "video" else "image"
    return when (selectedAction) {
        PremiumAIAction.GenerateCaption -> mapOf(
            "mediaType" to mediaWireValue,
            "locationName" to locationName,
            "userPrompt" to captionDirection.ifBlank { basePrompt }
        )
        PremiumAIAction.SuggestPostIdea -> mapOf(
            "locationName" to locationName.ifBlank { "Selected location" },
            "category" to ideaCategory,
            "vibe" to ideaVibe
        )
        PremiumAIAction.BusinessPromo -> mapOf(
            "businessName" to businessName,
            "businessType" to businessType,
            "offerDetails" to offerDetails,
            "locationName" to locationName.ifBlank { "Selected location" }
        )
        PremiumAIAction.TranslateCaption -> mapOf(
            "text" to translateText.ifBlank { basePrompt },
            "targetLanguage" to targetLanguage
        )
    }.filterValues { value -> value.isNotBlank() }
}

@Composable
private fun PlanButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.Black else Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

@Composable
private fun StackPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun CreationTypeCard(
    type: PoplurAIContentType,
    selected: Boolean,
    cost: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(122.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (selected) Color(0xFF4DFF58) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(type.icon(), contentDescription = null, tint = Color.White)
        Column {
            Text(type.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(type.providerLabel, color = Color.White.copy(alpha = 0.54f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$cost credits", color = Color(0xFF4DFF58), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun PoplurAIContentType.icon(): ImageVector = when (this) {
    PoplurAIContentType.Image -> Icons.Filled.Image
    PoplurAIContentType.Video -> Icons.Filled.VideoCameraBack
    PoplurAIContentType.Edit -> Icons.Filled.AutoAwesome
    PoplurAIContentType.Vfx, PoplurAIContentType.VfxVideo -> Icons.Filled.Movie
    PoplurAIContentType.ThreeDModel -> Icons.Filled.ViewInAr
    PoplurAIContentType.Voice -> Icons.Filled.GraphicEq
    PoplurAIContentType.Music -> Icons.Filled.MusicNote
    PoplurAIContentType.Game -> Icons.Filled.Gamepad
    PoplurAIContentType.Coupon -> Icons.Filled.ConfirmationNumber
    PoplurAIContentType.Campaign -> Icons.Filled.Campaign
}
