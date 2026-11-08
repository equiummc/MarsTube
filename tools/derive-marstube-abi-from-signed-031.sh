#!/usr/bin/env bash
# Derive ABI-specific APKs from tested signed 0.0.31 universal APK.
# No Gradle, compilation or source checkout mutation.
set -euo pipefail
src="$1"
out="$2"
: "$MARS_SIGNING_KEYSTORE_B64"
: "$MARS_SIGNING_STORE_PASS"
: "$MARS_SIGNING_KEY_PASS"
: "$MARS_SIGNING_KEY_ALIAS"
test -f "$src"
src="$(realpath "$src")"
mkdir -p "$out"
out="$(realpath "$out")"
bt="$ANDROID_HOME/build-tools/37.0.0"
for exe in "$bt/apksigner" "$bt/zipalign" "$bt/aapt"; do test -x "$exe"; done
for exe in zip unzip sha256sum python3 base64; do command -v "$exe" >/dev/null; done
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

"$bt/apksigner" verify --print-certs "$src" > "$tmp/source-cert.txt"
"$bt/aapt" dump badging "$src" > "$tmp/source-badging.txt"
grep -Fq "name='de.equiummc.marstube2'" "$tmp/source-badging.txt"
grep -Fq "versionCode='12031'" "$tmp/source-badging.txt"
grep -Fq "versionName='0.0.31'" "$tmp/source-badging.txt"
unzip -Z1 "$src" > "$tmp/source-entries.txt"
grep -q '^lib/armeabi-v7a/.*\.so$' "$tmp/source-entries.txt"
grep -q '^lib/arm64-v8a/.*\.so$' "$tmp/source-entries.txt"
sha="$(sha256sum "$src" | cut -d' ' -f1)"
expected="$(awk '$2=="MarsTube-0.0.31.apk"{print $1}' "$(dirname "$src")/SHA256SUMS")"
test -n "$expected" && test "$sha" = "$expected"

printf '%s' "$MARS_SIGNING_KEYSTORE_B64" | base64 -d > "$tmp/release.jks"
chmod 600 "$tmp/release.jks"
keytool -list -keystore "$tmp/release.jks" -storepass:env MARS_SIGNING_STORE_PASS \
    -alias "$MARS_SIGNING_KEY_ALIAS" > /dev/null

for entry in 'armeabi-v7a:arm64-v8a:ARM32' 'arm64-v8a:armeabi-v7a:ARM64'; do
  IFS=: read -r wanted discard label <<< "$entry"
  raw="$tmp/unsealed-$label.apk"
  aligned="$tmp/aligned-$label.apk"
  target="$out/MarsTube-0.0.31-$label.apk"
  test ! -e "$target" || { echo "Refusing to overwrite $target" >&2; exit 1; }
  cp "$src" "$raw"
  zip -dq "$raw" "lib/$discard/*"
  python3 - "$raw" "$wanted" "$discard" <<'PY'
import sys, zipfile
p, wanted, discard = sys.argv[1:]
with zipfile.ZipFile(p) as z:
    libs=[e for e in z.infolist() if e.filename.startswith('lib/') and e.filename.endswith('.so')]
    assert libs, 'Missing native libraries'
    assert {e.filename.split('/')[1] for e in libs}=={wanted}, 'Unexpected native ABI'
    assert all(e.compress_type==zipfile.ZIP_STORED for e in libs), 'Native libs not STORED'
    assert not any(e.filename.startswith('lib/'+discard+'/') for e in z.infolist()), 'ABI not removed'
    assert z.getinfo('resources.arsc').compress_type==zipfile.ZIP_STORED, 'resources compressed'
PY
  "$bt/zipalign" -P 16 -f 4 "$raw" "$aligned"
  "$bt/zipalign" -c -P 16 4 "$aligned" > /dev/null
  "$bt/apksigner" sign --ks "$tmp/release.jks" \
     --ks-key-alias "$MARS_SIGNING_KEY_ALIAS" \
     --ks-pass env:MARS_SIGNING_STORE_PASS \
     --key-pass env:MARS_SIGNING_KEY_PASS \
     --out "$target" "$aligned"
  "$bt/apksigner" verify --verbose --print-certs "$target" > "$tmp/$label-cert.txt"
  "$bt/zipalign" -c -P 16 4 "$target" > /dev/null
  "$bt/aapt" dump badging "$target" > "$tmp/$label-badging.txt"
  cmp -s <(grep "^package:" "$tmp/source-badging.txt") <(grep "^package:" "$tmp/$label-badging.txt")
  src_cert="$(sed -n 's/.*certificate SHA-256 digest: //p' "$tmp/source-cert.txt" | head -n1)"
  dst_cert="$(sed -n 's/.*certificate SHA-256 digest: //p' "$tmp/$label-cert.txt" | head -n1)"
  test -n "$src_cert" && test "$src_cert" = "$dst_cert"
  echo "Verified signed $label: $(stat -c%s "$target") bytes"
done
(cd "$out" && sha256sum MarsTube-0.0.31-ARM32.apk MarsTube-0.0.31-ARM64.apk > SHA256SUMS)
echo 'PASS: same package, versionCode, signing certificate, 16KiB native alignment and one ABI per APK.'
