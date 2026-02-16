# UI/UX Improvement Plan for MOCCA

## 1. Interaction Design (Haptics & Feedback)
- **Current State**: Uses `LongPress` for send, `TextHandleMove` for streaming.
- **Problem**: `LongPress` feels heavy/slow. Streaming feedback can be overwhelming if not throttled.
- **Proposal**:
    - Switch "Send" button to `VirtualKey` or `ContextClick` for a crisp, mechanical feel.
    - Implement a "Typing Indicator" animation that pulses slightly.
    - Add subtle success/error vibrations for commands.

## 2. Visual Hierarchy & Typography
- **Current State**: Monospace "Terminal" theme. High contrast.
- **Problem**: Can feel a bit stark. Dense text blocks.
- **Proposal**:
    - **Code Blocks**: Add syntax highlighting for more languages (currently basic).
    - **Headers**: Use a slightly different font weight or color for headers to distinguish from body text.
    - **Spacing**: Increase line height in chat bubbles for readability.

## 3. Motion & Transitions
- **Current State**: Standard Compose transitions.
- **Problem**: "Terminal" feels static.
- **Proposal**:
    - **Message Entry**: Animate new messages with a "slide up + fade in" effect.
    - **Panel Swiping**: Add a parallax effect to the background when swiping between panels.
    - **Loading States**: Replace spinners with a "scanning" or "compiling" ASCII animation.

## 4. Input Experience
- **Current State**: Fixed bottom bar.
- **Problem**: Can feel disconnected from the chat.
- **Proposal**:
    - **Floating Input**: When scrolling up, shrink the input bar to a pill shape.
    - **Command Autocomplete**: Show a popup list of available slash commands as you type `/`.

## 5. The "Pitch Black" Theme
- **Current State**: Pure black background.
- **Proposal**:
    - **OLED Saving**: Keep pure black.
    - **Depth**: Add very subtle dark grey borders (1px) to distinguish panels without raising the black level.
    - **Glow**: Add a faint Mint Green glow behind the active panel or focused input field.

## Implementation Roadmap
1.  **Phase 1 (Done)**: Fix critical bugs (Keyboard, Update).
2.  **Phase 2**: Refine Haptics & Input Animation.
3.  **Phase 3**: Implement "Command Autocomplete" and Syntax Highlighting.
4.  **Phase 4**: Add "Scanning" loading animations.
