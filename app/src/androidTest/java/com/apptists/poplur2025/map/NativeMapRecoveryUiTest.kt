package com.apptists.poplur2025.map

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class NativeMapRecoveryUiTest {
    @Test fun flatMapAndClayRetryRecoverTogether() {
        MapRecoveryInstrumentation.runScenario(InstrumentationRegistry.getInstrumentation())
    }
}
