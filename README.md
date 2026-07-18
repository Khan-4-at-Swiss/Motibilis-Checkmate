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

## Build APK Locally

From the project root you can build a debug APK with Gradle:

```bash
./gradlew :app:assembleDebug
```

The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

To produce a release build sign it with your keystore and run:

```bash
./gradlew :app:assembleRelease
```

## Pushing to GitHub

If you want to push local changes to a GitHub repository:

```powershell
cd 'C:\path\to\motibilis-_-checkmate (1)'
git init            # only if not already a repo
git add .
git commit -m "Your message"
git remote add origin https://github.com/USERNAME/REPO.git
git branch -M main
git push -u origin main
```

If the remote has existing history and you want to overwrite it (destructive):

```powershell
git push -u origin main --force
```


## License

This project is licensed under the MIT License.
