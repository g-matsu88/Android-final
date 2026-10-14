# Pantry Chef (Android, Kotlin, Jetpack Compose)

Track your pantry, find matching recipes online, and ask an AI chef for custom recipes and substitutions.

| Requirement | Where |
|---|---|
| Multiple screens | Pantry, Recipes, Detail, AI Chef, Favorites (`ui/App.kt`, `ui/Screens.kt`) |
| Data persistence | Room: pantry items + favorites (`data/Database.kt`) |
| External API + coroutines | TheMealDB via Retrofit `suspend` calls, parallel `async` per ingredient (`data/Repository.kt`) |
| AI function | Gemini API via Retrofit (`data/Network.kt`, `Repository.askGemini`) |

## Setup
1. Open the `PantryChef` folder in Android Studio (Koala or newer, JDK 17). Let Gradle sync.
2. Get a free key at https://aistudio.google.com/apikey
3. Open `local.properties` (project root) and add: `GEMINI_KEY=your_key_here`
4. Sync again, then Run on an emulator or device (min SDK 24).

If the AI returns a "model not found" error, update the model name in `GeminiApi` (`data/Network.kt`).

## Demo flow
Add 3 ingredients -> Find recipes -> open one and Save -> close and reopen the app (data persists) -> AI Chef tab.
