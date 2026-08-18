package com.apptists.poplur2025.data

// Mobile builds must not contain AI provider API keys.
// Provider credentials belong on Poplur's backend:
// ANTHROPIC_API_KEY, GOOGLE_AI_API_KEY, MESHY_API_KEY, ELEVENLABS_API_KEY,
// SUNO_API_KEY, GAME_PROVIDER_API_KEY, and VFX_PROVIDER_API_KEY.
// Every Claude call goes through PoplurAIBackendClient.
object Secrets
