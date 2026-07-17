package com.pausenow.app.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageExclusionPolicyTest {
    private val policy = PackageExclusionPolicy("com.pausenow.app")

    @Test
    fun excludesOwnSystemLauncherAndKeyboardPackages() {
        assertTrue(policy.shouldExclude("com.pausenow.app"))
        assertTrue(policy.shouldExclude("com.android.systemui"))
        assertTrue(policy.shouldExclude("com.android.launcher3"))
        assertTrue(policy.shouldExclude("com.google.android.inputmethod.latin"))
    }

    @Test
    fun acceptsTargetThirdPartyPackage() {
        assertFalse(policy.shouldExclude("com.ss.android.ugc.aweme"))
    }
}
