# Video Stahovač (Android)

Jednoduchý Android Studio projekt v češtině:
- vložení URL videa,
- výběr kvality,
- požadavek na kompatibilní instanci Cobalt API pro podporované weby,
- stažení výsledného souboru přes Android DownloadManager do složky Stažené.

## Důležité
- Tento projekt je zdrojový kód, ne hotový APK. V aktuálním prostředí není Android SDK/Gradle nainstalované, takže APK zde nebylo možné sestavit.
- Ve výchozím nastavení je nastaven endpoint `https://api.cobalt.tools/`. Veřejné instance a jejich API se mohou měnit nebo být omezené; pokud endpoint nefunguje, změň jej v `MainActivity.java` na aktuální instanci kompatibilní s Cobalt API.
- Podpora YouTube/Facebooku závisí na instanci, jejích pravidlech a aktuální podpoře webu. Některé odkazy mohou být odmítnuty, vyžadovat přihlášení nebo nemusí být podporované.
- Používej jen pro obsah, který smíš stahovat. Aplikace neobchází přihlášení, DRM ani soukromá videa.

## Sestavení APK
1. Nainstaluj Android Studio (aktuální stabilní verzi) a Android SDK Platform 35.
2. Otevři složku `VideoStahovacAndroid` v Android Studiu.
3. Nech Gradle dokončit synchronizaci.
4. Zvol **Build > Build APK(s)**.
5. APK najdeš v `app/build/outputs/apk/debug/app-debug.apk`.

## API
Aplikace posílá URL a zvolenou kvalitu na zadaný Cobalt-compatible API endpoint. Neodesílej odkazy obsahující soukromé tokeny nebo citlivé údaje. Endpoint lze upravit přímo v kódu.

## Cloud build (bez Android Studia na mobilu)
V projektu je připraven GitHub Actions workflow `.github/workflows/build-apk.yml`.
Po nahrání celého projektu do GitHub repozitáře se APK sestaví automaticky při pushi do větve `main`; lze jej spustit i ručně přes Actions → Build Android APK → Run workflow. Výsledek se stáhne v běhu workflow v sekci Artifacts jako `Video-Stahovac-APK`.
