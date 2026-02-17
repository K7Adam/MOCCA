package com.mocca.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Modern Design System Colors
 * Foundation for the new sleek UI.
 */
object ModernColors {
    // ═══════════════════════════════════════════════════════════════════════════
    // BRAND / ACCENTS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Electric Violet - Primary Brand Color */
    val primary = Color(0xFF8B5CF6) // Violet 500
    
    /** Deep Indigo - Secondary Brand Color */
    val secondary = Color(0xFF6366F1) // Indigo 500
    
    /** Emerald - Success / Status */
    val success = Color(0xFF10B981) // Emerald 500
    
    /** Rose - Error / Destructive */
    val error = Color(0xFFF43F5E) // Rose 500
    
    /** Amber - Warning */
    val warning = Color(0xFFF59E0B) // Amber 500

    // ═══════════════════════════════════════════════════════════════════════════
    // BACKGROUNDS (Deep Slate / Navy Theme)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Deepest Navy - Main Background */
    val background = Color(0xFF0F172A) // Slate 900
    
    /** Surface Level 1 - Cards, Panels */
    val surface = Color(0xFF1E293B) // Slate 800
    
    /** Surface Level 2 - Elevated elements, Modals */
    val surfaceElevated = Color(0xFF334155) // Slate 700
    
    /** Surface Level 3 - Hover states, Input fields */
    val surfaceHighlight = Color(0xFF475569) // Slate 600

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTENT / TEXT
    // ═══════════════════════════════════════════════════════════════════════════

    /** High Emphasis Text */
    val textPrimary = Color(0xFFF8FAFC) // Slate 50
    
    /** Medium Emphasis Text */
    val textSecondary = Color(0xFFCBD5E1) // Slate 300
    
    /** Low Emphasis Text */
    val textTertiary = Color(0xFF94A3B8) // Slate 400
    
    /** Disabled Text */
    val textDisabled = Color(0xFF64748B) // Slate 500

    // ═══════════════════════════════════════════════════════════════════════════
    // BORDERS / DIVIDERS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Subtle Border */
    val border = Color(0xFF334155) // Slate 700
    
    /** Focus Ring */
    val focusRing = Color(0x808B5CF6) // Primary with 50% opacity
}
