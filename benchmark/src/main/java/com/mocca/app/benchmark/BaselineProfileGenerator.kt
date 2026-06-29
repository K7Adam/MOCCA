package com.mocca.app.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Generates a Baseline Profile for MOCCA's critical user journeys.
 *
 * Run on a rooted emulator or physical device with:
 *   ./gradlew :benchmark:generateReleaseBaselineProfile
 *
 * The generated baseline-prof.txt is automatically placed in
 * :androidApp/src/release/generated/baselineProfiles/.
 */
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.mocca.app",
        profileBlock = {
            // Critical journey 1: App startup → onboarding/main screen
            startActivityAndWait()

            // Critical journey 2: Chat interaction (wait for main content)
            // The app may land on onboarding if no server is configured;
            // either way, the Compose UI inflation is profiled.
            device.waitForIdle()
        }
    )
}
