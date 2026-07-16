package com.apptists.poplur2025.ui.components

/** Copy pool from the approved pre-login prototype. No generated launch copy is loaded. */
object SplashLoadingLines {
    private val pool = listOf(
        "Warming up the pinetrees",
        "Rolling out the clay roads",
        "Inflating the palm trees",
        "Teaching the pin to drop",
        "Buffing the city lights",
        "Sprinkling pink stars",
        "Kneading the little mountains",
        "Waking up the tugboat",
        "Charging the flip phone",
        "Tuning the clay TV",
        "Squishing fresh hearts",
        "Painting the dotted lines",
        "Polishing the big P",
        "Fluffing the clouds",
        "Measuring your radius",
        "Dusting off the globe",
        "Pinning the hotspots",
        "Baking a fresh batch of pins",
    )

    fun sessionLines(): List<String> = pool.shuffled()
}
