# Motibilis | Checkmate

A fully featured, highly polished Android Chess game built with Jetpack Compose.
This project showcases a premium UI design featuring custom 3D-styled pieces, dynamic lighting effects, an advanced chess engine, and beautiful animations.

## Features

- **Zero-Config, 100% Offline-First**: No external API keys, database credentials, or server setups are required. Clone, compile, and play instantly.
- **Modern Jetpack Compose UI**: Entirely built with declarative UI patterns and gorgeous premium styling.
- **Premium Themes**: Multiple stunning board themes (Obsidian Gold, Walnut Ivory, Midnight Emerald, Crimson Royale).
- **Custom Piece Styles**: Ranging from ultra-realistic 3D metallic medallions to frosted glass silhouettes and neon holograms.
- **Advanced Chess Engine**: Includes a custom-built, optimized chess engine capable of evaluating moves and playing at various difficulty levels.
- **Real-Time Analysis**: Visual indicators for last moves, valid moves, and checks.
- **Multiplayer / Solo Play**: Play against a friend locally or challenge the AI engine.
- **Move History & Replays**: Keep track of the game with full move logging.

## Screenshots

*(Add your screenshots here)*

## Architecture

- **MVI Architecture**: State flows cleanly from the ViewModel to the Compose UI.
- **Jetpack Navigation**: Seamless screen transitions.
- **Kotlin Coroutines**: For background engine calculations and animations.
- **Custom Compose Graphics**: Heavy use of `Canvas` and `Modifier.drawBehind` for custom shadows, gradients, and 3D effects.

## Setup Instructions

1. Clone this repository.
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Run the app on an emulator or physical device (API level 24+).

## License

This project is licensed under the MIT License.
