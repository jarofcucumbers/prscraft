# PRSCraft development

Use the existing checkout. Java 21, Minecraft 1.21.1, NeoForge 21.1.264; build with `./gradlew build`, integration checks with `./gradlew runGameTestServer`.

Every user-facing feature or behavior change MUST update the in-game handbook: `HandbookContent.java`, the `book.prscraft.*` translations in both `en_us.json` and `uk_ua.json`, and relevant diagrams/formulas in `HandbookScreen.java`. Keep the handbook version equal to the mod version. Add every new block/item to handbook coverage; the integration test checks this. Explain signal units, assumptions and simulation limits honestly. Keep README usage examples current.

Preserve backwards-compatible NBT defaults for existing worlds, templates and recordings. Server-side settings validation and read-only analyzer behavior are required. Keep PSD/ESD normalization consistent with Parseval and distinguish finite-window metrics from whole-signal energy. Q-based AWGN theory is a symbol-space model and must not be advertised as an exact BER prediction for arbitrary nonlinear RF chains.

When delivering a new release, build and validate the JAR, exclude GameTest fixtures, copy it and its SHA-256 into `releases/`. Never claim interactive UI validation unless it was actually run.

Signal computation is authoritative in `CircuitCapture`/`CircuitNetwork`; analyzer topology is deliberately undirected and spans active devices. Keep A=back, B=left, OUT=front consistent in code, models and handbook. Apply cable segment losses/noise before the next DSP stage. Integrator/correlation dimensions must never be silently exported as W/J spectra. Custom pose-scaled screens use `ScaledButton`, not vanilla scrolling buttons (scissors ignore the outer pose in 1.21.1).

Drawing boards use server-authoritative palette bytes (128×128, 0=blank), shared by editor and block renderer. Charge actual tool durability even in creative; the eraser stays at max damage and only a water-cauldron wash restores it. Recoloring chalk must preserve components/wear. Keep world, dropped-item and client-sync canvas persistence tested. Unipolar URZ power/energy normalization includes duty; receiver A1 is a manually configured received amplitude. Handbook formula legends (`.symbols`) and challenge conditions must be updated in both languages.

Gray conversion is MSB-first, capture-aligned and limited to 128 output bits; ADC index encoding uses measured input and calibrated min/max, never source metadata. Keep Gray templates/recordings/NBT defaults tested. Port FACING is cosmetic mounting (six faces), independent of passive connectivity; processor A/B/OUT directions must match models, analyzer and engine in every horizontal orientation.
