// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.camera

import android.util.Size
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [CameraConfig].
 */
class CameraConfigTest {
    
    @Test
    fun `default configuration has correct values`() {
        val config = CameraConfig.DEFAULT
        
        assertEquals(Size(1280, 720), config.targetResolution)
        assertEquals(3, config.frameThrottle)
        assertTrue(config.tapToFocusEnabled)
    }
    
    @Test
    fun `high quality configuration has correct values`() {
        val config = CameraConfig.HIGH_QUALITY
        
        assertEquals(Size(1920, 1080), config.targetResolution)
        assertEquals(2, config.frameThrottle)
        assertTrue(config.tapToFocusEnabled)
    }
    
    @Test
    fun `performance configuration has correct values`() {
        val config = CameraConfig.PERFORMANCE
        
        assertEquals(Size(960, 540), config.targetResolution)
        assertEquals(5, config.frameThrottle)
        assertTrue(config.tapToFocusEnabled)
    }
    
    @Test
    fun `custom configuration can be created`() {
        val customResolution = Size(1600, 900)
        val config = CameraConfig(
            targetResolution = customResolution,
            frameThrottle = 4,
            tapToFocusEnabled = false
        )
        
        assertEquals(customResolution, config.targetResolution)
        assertEquals(4, config.frameThrottle)
        assertFalse(config.tapToFocusEnabled)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `zero frame throttle throws exception`() {
        CameraConfig(frameThrottle = 0)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `negative frame throttle throws exception`() {
        CameraConfig(frameThrottle = -1)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `zero width resolution throws exception`() {
        CameraConfig(targetResolution = Size(0, 720))
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `zero height resolution throws exception`() {
        CameraConfig(targetResolution = Size(1280, 0))
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `negative width resolution throws exception`() {
        CameraConfig(targetResolution = Size(-100, 720))
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `negative height resolution throws exception`() {
        CameraConfig(targetResolution = Size(1280, -100))
    }
    
    @Test
    fun `valid frame throttle values are accepted`() {
        // Test various valid throttle values
        for (throttle in 1..10) {
            val config = CameraConfig(frameThrottle = throttle)
            assertEquals(throttle, config.frameThrottle)
        }
    }
    
    @Test
    fun `valid resolutions are accepted`() {
        val testResolutions = listOf(
            Size(640, 480),
            Size(800, 600),
            Size(1280, 720),
            Size(1920, 1080),
            Size(3840, 2160)
        )
        
        testResolutions.forEach { resolution ->
            val config = CameraConfig(targetResolution = resolution)
            assertEquals(resolution, config.targetResolution)
        }
    }
    
    @Test
    fun `tap to focus can be disabled`() {
        val config = CameraConfig(tapToFocusEnabled = false)
        assertFalse(config.tapToFocusEnabled)
    }
    
    @Test
    fun `configuration is immutable via data class copy`() {
        val config1 = CameraConfig.DEFAULT
        val config2 = config1.copy(frameThrottle = 5)
        
        // Original unchanged
        assertEquals(3, config1.frameThrottle)
        // Copy has new value
        assertEquals(5, config2.frameThrottle)
        // Other properties copied
        assertEquals(config1.targetResolution, config2.targetResolution)
        assertEquals(config1.tapToFocusEnabled, config2.tapToFocusEnabled)
    }
}
