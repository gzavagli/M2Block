# Merge Blocks (M2Block)

A highly polished, modern Android drop-and-merge puzzle game built with **Jetpack Compose**. Drop blocks from the top of the grid to match and merge equal numbers, trigger satisfying chain-reaction combos, and unlock awesome power-ups using your in-game coins!

---

## 🎮 How to Play

1. **Drop Blocks**: Tap any column on the grid to drop the active block from the top.
2. **Merge & Match**: Matching adjacent numbers will merge when they touch (e.g., $2 + 2 \rightarrow 4$, $4 + 4 \rightarrow 8$).
3. **Chain Combos**: Merges that trigger additional merges create combos, giving you massive bonus points.
4. **Earn Coins**: Every successful merge rewards you with gold coins based on the resulting block value.
5. **Clean the Board**: As you reach higher numbers, under-valued blocks are automatically purged from the board to keep the game engaging.
6. **Game Over**: If any column fills up to the top row and no more merges are possible, the game is over.

---

## ✨ Features

- ⚡ **Dynamic Gameplay & Animations**: Smooth slide-down dropping, gravity cascades, and merge pop animations.
- 🎨 **4 Immersive Themes**:
  - **Cyberpunk**: Vibrant neon pinks and blues on a dark tech grid.
  - **Midnight**: A sleek, minimal deep space design.
  - **Candy**: Colorful pastel themes with a playful look.
  - **Classic**: Retro-inspired colors for a familiar arcade experience.
- 🚀 **Strategic Power-ups**:
  - 🔨 **Hammer (100 coins)**: Select any block to destroy it.
  - 🌀 **Vortex (150 coins)**: Select any column to clear it entirely.
  - ✖️ **Double (120 coins)**: Double the value of any block on the board.
  - 🔄 **Swap (50 coins)**: Swap the active block with the previewed next block.
- 💾 **Auto-Save & Resume**: Never lose your progress. The game automatically saves your active state, score, coins, and settings so you can resume playing anytime.
- 🔊 **Synthesized Audio Engine**: Real-time synthesized audio effects (`AudioSynthManager`) that dynamically shift pitch with combo streaks.
- 📳 **Haptic Feedback**: Satisfying tactile feel for merges, drops, and power-up usage.
- ⚙️ **Interactive Tutorial & Settings**: Learn the ropes with the built-in guide or customize sound/haptic toggles from the settings menu.

---

## 🛠️ Project Structure

The codebase is organized as a modern Android/Kotlin project using Jetpack Compose:

- [MainActivity.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/MainActivity.kt): Entry point setting up edge-to-edge layouts and binding the Game ViewModel.
- [GameViewModel.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/GameViewModel.kt): Orchestrates the core game loop, gravity cascade, power-up logic, and preference persistence.
- [GameState.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/GameState.kt): Defines the immutable `GameState`, block structures, theme modes, and status states.
- [GameScreens.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/ui/components/GameScreens.kt): Composable layouts for the splash screen, game board, power-up bar, settings, and dialog overlays.
- [Theme.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/ui/theme/Theme.kt): Custom color palettes, typography, and theme styling mapping to game modes.
- [AudioSynthManager.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/utils/AudioSynthManager.kt): Procedural synthesizer generating sounds without local audio assets.
- [HapticManager.kt](file:///Users/gzavagli/Projects/M2Block/app/src/main/java/com/gzavagli/m2block/utils/HapticManager.kt): Interface for Android system haptic triggers.

---

## 🚀 Running the App

### Requirements
- **Android Studio** (Koala or newer recommended)
- **Android SDK** API 26 or higher
- **JDK 17**

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/gzavagli/M2Block.git
   ```
2. Open the project folder in **Android Studio**.
3. Allow Gradle sync to complete.
4. Run the project on an **Android Virtual Device (AVD)** or a physical phone via USB.
