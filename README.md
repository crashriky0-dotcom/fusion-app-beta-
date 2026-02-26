# LevabolliApp

App Android per preventivi rimozione bolli da grandine (Paintless Dent Repair - PDR).

## Caratteristiche

- 🚗 Interfaccia visuale per selezionare pannelli carrozzeria
- 📏 Misurazione diametro bolli da foto usando moneta come riferimento
- 💰 Listino prezzi personalizzabile
- 📄 Esportazione preventivi in PDF
- 💾 Salvataggio e recupero preventivi
- 🌍 Supporto multilingua (IT, EN, DE, FR, ES, PT, NL, PL)

## Requisiti

- Android Studio Arctic Fox o superiore
- JDK 11 o superiore
- Android SDK API 24+ (Android 7.0)
- Target SDK: API 34 (Android 14)

## Installazione

1. Clona o scarica il progetto
2. Apri il progetto in Android Studio
3. Aspetta la sincronizzazione Gradle
4. Avvia su emulatore o dispositivo fisico

## Compilazione da linea di comando

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

L'APK generato si troverà in: `app/build/outputs/apk/`

## Struttura del Progetto

```
LevabolliApp/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/levabolliapp/
│   │       │   ├── MainActivity.kt          # Schermata principale
│   │       │   ├── MeasureActivity.kt       # Misurazione da foto
│   │       │   ├── ListinoActivity.kt       # Gestione listino
│   │       │   ├── PreventiviActivity.kt    # Lista preventivi salvati
│   │       │   └── InfoActivity.kt          # Informazioni app
│   │       ├── res/
│   │       │   ├── layout/                  # Layout XML
│   │       │   ├── values/                  # Stringhe, colori, temi
│   │       │   └── mipmap-*/                # Icone app
│   │       └── AndroidManifest.xml
│   ├── build.gradle                         # Config modulo app
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/                             # Gradle Wrapper
├── build.gradle                             # Config progetto root
├── settings.gradle                          # Impostazioni Gradle
├── gradle.properties                        # Proprietà Gradle
├── gradlew                                  # Script Gradle (Unix)
└── gradlew.bat                              # Script Gradle (Windows)
```

## Funzionalità Principali

### MainActivity
- Selezione interattiva pannelli carrozzeria
- Inserimento numero bolli e diametro per pannello
- Calcolo automatico prezzo in base al listino
- Supporto alluminio (+30%)
- Gestione sportellate
- Calcolo sconto totale

### MeasureActivity
- Caricamento foto dalla galleria
- Selezione moneta riferimento (1€ o 2€)
- Misurazione interattiva con touch
- Calcolo diametro reale in millimetri

### ListinoActivity
- Modifica listino prezzi predefinito
- Organizzazione per misura bollo (<10mm, <25mm, <45mm)
- Salvataggio persistente tramite SharedPreferences

### PreventiviActivity
- Lista preventivi salvati
- Caricamento preventivo nella schermata principale
- Eliminazione preventivi

## Tecnologie Utilizzate

- **Linguaggio**: Kotlin
- **UI**: View Binding
- **Storage**: SharedPreferences (JSON)
- **PDF**: PdfDocument (Android framework)
- **Build System**: Gradle 8.0

## Note di Sviluppo

- ViewBinding abilitato per accesso type-safe alle view
- minSdk 24 (Android 7.0) per compatibilità con ~85% dispositivi
- Nessuna dipendenza esterna oltre AndroidX

## Licenza

Progetto didattico/dimostrativo.

## Autore

Generato da assistente AI su richiesta utente.
