package com.mochyy.mochymusic.constants

/**
 * What a player style is, as plain data: the player, the selector, the studio and the tv read
 * these instead of naming styles, so a new style says everything about itself next to its entry
 * in [PlayerStyle] and the drawing goes in ui/player/styles/PlayerStyleRegistry.kt. No compose in
 * here on purpose, so it can move to the shared module with the rest of the player later.
 */
data class PlayerStyleTraits(
    val family: StyleFamily = StyleFamily.Own,
    val editor: StyleEditor = StyleEditor.None,
    // has a landscape layout of its own, so its menu offers the rotate button
    val turnable: Boolean = false,
    // laid out sideways until someone turns it upright, the way a phone stands on a nightstand
    val startsSideways: Boolean = false,
    // reads well across a room and stays smooth on a tv's gpu
    val onTv: Boolean = false,
    // the grab handle over the player; styles with their own chrome at the top go without
    val showsHandle: Boolean = true,
    // its menu is reached from its lyrics, so the menu opens over them
    val menuFromLyrics: Boolean = false,
)

enum class StyleFamily {
    // drawn by the flagship player itself (cover, controls, card), not a design of its own
    Flagship,
    // the nightstand styles share their sheet, their night look and their lock screen rules
    Bedside,
    Own,
}

enum class StyleEditor {
    None,
    // the studio's simple editor (colours, a few toggles)
    Simple,
    // the full editor: cover, middle part, controls and bottom bar
    Full,
}
