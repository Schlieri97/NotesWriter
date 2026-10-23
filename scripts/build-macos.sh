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
case "$arch" in arm64|x86_64) ;; *) echo "Nicht unterstützte Architektur: $arch" >&2; exit 1 ;; esac

version="$(sed -nE 's/.*String VERSION = "([0-9]+\.[0-9]+\.[0-9]+)";.*/\1/p' src/main/java/com/code/noteswriter/AppInfo.java)"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'AppInfo.VERSION muss 1.0.0 entsprechen.' >&2; exit 1; }
if [[ "${GITHUB_REF_TYPE:-}" == tag && "${GITHUB_REF_NAME:-}" != "v$version" ]]; then
  echo "Release-Tag muss zur App-Version passen: v$version" >&2
  exit 1
fi

name='SZ-Notenschreiber'
output="dist/macos/$name-$version-$arch.dmg"
[[ ! -e "$output" ]] || { echo "Ausgabedatei existiert bereits: $output" >&2; exit 1; }

bash mvnw --batch-mode --no-transfer-progress clean verify javafx:jlink

jpackage --type app-image --name "$name" --app-version "$version" \
  --vendor Schlieri97 --runtime-image target/app \
  --module com.example.noteswriter/com.code.noteswriter.Main \
  --mac-package-identifier de.schlieri97.noteswriter --mac-package-name NotesWriter \
  --dest target/macos-image

app="target/macos-image/$name.app"
python3 - "$app/Contents/Info.plist" <<'PY'
import pathlib, plistlib, sys
path = pathlib.Path(sys.argv[1])
info = plistlib.loads(path.read_bytes())
info["NSMicrophoneUsageDescription"] = "Das Mikrofon erkennt gespielte Floetentoene und fuegt passende Noten ein. Audiodaten werden nicht gespeichert oder hochgeladen."
path.write_bytes(plistlib.dumps(info, sort_keys=False))
PY
plutil -lint "$app/Contents/Info.plist"
codesign --force --sign - "$app"
codesign --verify --deep --strict "$app"
"$app/Contents/runtime/Contents/Home/bin/java" --list-modules | grep -E '^com.example.noteswriter(@|$)'

jpackage --type dmg --name "$name" --app-version "$version" \
  --app-image "$app" --dest target/macos-installer
mkdir -p dist/macos
mv "target/macos-installer/$name-$version.dmg" "$output"
(cd dist/macos && shasum -a 256 "$(basename "$output")" > "$(basename "$output").sha256")
echo "Mac-Paket erstellt: $output"
