#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
if [[ "$(uname -s)" != Darwin ]]; then
  echo 'Dieser Build benötigt macOS. Unter Windows den Workflow "Mac-App bauen" auf GitHub starten.' >&2
  exit 1
fi

: "${JAVA_HOME:?JAVA_HOME muss auf ein JDK 22 zeigen.}"
export PATH="$JAVA_HOME/bin:$PATH"
arch="$(uname -m)"
case "$arch" in
  arm64|x86_64) ;;
  *) echo "Nicht unterstützte Architektur: $arch" >&2; exit 1 ;;
esac

# AppInfo ist die gemeinsame Versionsquelle für Oberfläche und Mac-Paket.
version="$(sed -nE 's/.*String VERSION = "([0-9]+\.[0-9]+\.[0-9]+)";.*/\1/p' src/main/java/com/code/noteswriter/AppInfo.java)"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo 'AppInfo.VERSION muss eine Versionsnummer wie 1.0.0 enthalten.' >&2
  exit 1
fi
if [[ "${GITHUB_REF_TYPE:-}" == tag && "${GITHUB_REF_NAME:-}" != "v$version" ]]; then
  echo "Release-Tag muss zur App-Version passen: v$version" >&2
  exit 1
fi

name='SZ-Notenschreiber'
output="dist/macos/$name-$version-$arch.dmg"
if [[ -e "$output" ]]; then
  echo "Ausgabedatei existiert bereits: $output. Bitte vor einem neuen Build verschieben." >&2
  exit 1
fi

# Der Wrapper lädt Maven; Maven wählt die nativen JavaFX-Bibliotheken des Macs.
bash mvnw --batch-mode --no-transfer-progress clean verify javafx:jlink

jpackage --type app-image \
  --name "$name" \
  --app-version "$version" \
  --vendor Schlieri97 \
  --runtime-image target/app \
  --module com.example.noteswriter/com.code.noteswriter.Main \
  --mac-package-identifier de.schlieri97.noteswriter \
  --mac-package-name NotesWriter \
  --dest target/macos-image

app="target/macos-image/$name.app"
# Die Java-Sound-Aufnahme benötigt einen Zwecktext in der äußeren App-Plist.
# jpackage kann den Schlüssel bereits mit einem Standardtext anlegen.
python3 - "$app/Contents/Info.plist" <<'PY'
import pathlib
import plistlib
import sys

path = pathlib.Path(sys.argv[1])
info = plistlib.loads(path.read_bytes())
info["NSMicrophoneUsageDescription"] = (
    "Das Mikrofon erkennt gespielte Floetentoene und fuegt die passenden Noten ein. "
    "Audiodaten werden nicht gespeichert oder hochgeladen."
)
path.write_bytes(plistlib.dumps(info, sort_keys=False))
PY
plutil -lint "$app/Contents/Info.plist"
# Nach der Plist-Änderung die lokale Signatur erneuern. Dies ist keine
# Developer-ID-Signatur und keine Apple-Notarisierung.
codesign --force --sign - "$app"
codesign --verify --deep --strict "$app"

# Nur die gebündelte Laufzeit verwenden, nicht die Java-Installation des Runners.
"$app/Contents/runtime/Contents/Home/bin/java" --list-modules | grep -E '^com.example.noteswriter(@|$)'
python3 - "$app/Contents/MacOS/$name" "$arch" <<'PY'
import os
import pathlib
import subprocess
import sys
import tempfile

launcher = pathlib.Path(sys.argv[1]).resolve()
# -verify_arch liest alle folgenden Argumente als Architekturen.
subprocess.run(["lipo", str(launcher), "-verify_arch", sys.argv[2]], check=True)
env = dict(os.environ, JAVA_HOME="/nonexistent", PATH="/usr/bin:/bin:/usr/sbin:/sbin")
with open("target/macos-startup.log", "w+") as log, tempfile.TemporaryDirectory() as cwd:
    process = subprocess.Popen([str(launcher)], cwd=cwd, env=env, stdout=log, stderr=log)
    try:
        process.wait(timeout=15)
        raise SystemExit(f"App wurde unerwartet beendet (Exit-Code {process.returncode}).")
    except subprocess.TimeoutExpired:
        print("App bleibt mit gebündelter Laufzeit gestartet.")
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        log.seek(0)
        startup_log = log.read()
        print(startup_log)
    if "Exception" in startup_log or "Error:" in startup_log:
        raise SystemExit("Fehler im Startprotokoll; Paket wird nicht weitergegeben.")
PY

jpackage --type dmg --name "$name" --app-version "$version" \
  --app-image "$app" --dest target/macos-installer
mkdir -p dist/macos
mv "target/macos-installer/$name-$version.dmg" "$output"
(cd dist/macos && shasum -a 256 "$name-$version-$arch.dmg" > "$name-$version-$arch.dmg.sha256")
echo "Mac-Paket erstellt: $output"
