#!/bin/zsh
# Re-captures the six README screenshots:
#   macos_01.png  setup with source, destination and recent folders
#   macos_02.png  ignored directories
#   macos_03.png  finding files to copy
#   macos_04.png  copying (once at least 20 % are done)
#   macos_05.png  deleting orphaned files (once at least 30 % are done)
#   macos_06.png  finished
#
# Source and destination live in /tmp/monicopy-screenshots and are created by the
# prepareScreenshots Gradle task, which also writes matching MoniCopy preferences.
# Before that, the preference domains Java uses (com.thomaskuenneth.monicopy and
# com.apple.java.util.prefs) are exported. When the script ends and MoniCopy has quit,
# every domain whose contents changed is deleted and re-imported from its backup, and
# /tmp/monicopy-screenshots is deleted. A copy is only started when the saved source and
# destination are the /tmp folders and the window shows the /tmp recent folders.
#
# Usage: scripts/capture-screenshots.sh [app-path] [output-dir]
# Without app-path, the release app of this project is built and used.
# Needs screen recording and accessibility permission for the terminal running it.
set -euo pipefail

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
APP="${1:-}"
OUT="${2:-$PROJECT/screenshots}"
ROOT="/tmp/monicopy-screenshots"
DOMAINS=(com.thomaskuenneth.monicopy com.apple.java.util.prefs)
BACKUPS="${TMPDIR:-/tmp}/monicopy-screenshots-preferences"
BUNDLE_ID="com.thomaskuenneth.monicopy"
WORK="$(mktemp -d)"
APP_PID=""
BACKED_UP=""

log() { echo "$(date +%T) $*"; }

app_pid() {
    pgrep -f "$APP/Contents/MacOS/MoniCopy" | head -1 || true
}

quit_app() {
    [[ -z "$(app_pid)" ]] && return 0
    osascript -e "tell application id \"$BUNDLE_ID\" to quit" >/dev/null 2>&1 || true
    local deadline=$((SECONDS + 20))
    while [[ -n "$(app_pid)" && $SECONDS -lt $deadline ]]; do
        sleep 0.5
    done
    [[ -z "$(app_pid)" ]]
}

same_plist() {
    python3 -c 'import plistlib, sys; a, b = (plistlib.load(open(p, "rb")) for p in sys.argv[1:]); sys.exit(a != b)' "$1" "$2"
}

backup_preferences() {
    rm -rf "$BACKUPS"
    mkdir -p "$BACKUPS"
    local domain
    for domain in $DOMAINS; do
        defaults export "$domain" "$BACKUPS/$domain.plist"
        plutil -lint -s "$BACKUPS/$domain.plist" || { echo "backup of $domain is not a valid plist" >&2; exit 1; }
    done
    BACKED_UP=1
    log "backed up ${DOMAINS[*]} to $BACKUPS"
}

restore_preferences() {
    local domain
    for domain in $DOMAINS; do
        defaults export "$domain" "$WORK/$domain.now.plist"
        if same_plist "$BACKUPS/$domain.plist" "$WORK/$domain.now.plist"; then
            log "unchanged: $domain"
        else
            defaults delete "$domain" 2>/dev/null || true
            defaults import "$domain" "$BACKUPS/$domain.plist"
            defaults export "$domain" "$WORK/$domain.now.plist"
            same_plist "$BACKUPS/$domain.plist" "$WORK/$domain.now.plist" || { echo "restoring $domain failed; backup kept in $BACKUPS" >&2; return 1; }
            log "restored: $domain"
        fi
    done
    rm -rf "$BACKUPS"
}

finish() {
    local result=$?
    if [[ -n "$BACKED_UP" ]]; then
        if quit_app; then
            restore_preferences || result=1
        else
            echo "MoniCopy did not quit. Quit it, then restore each domain in $BACKUPS with: defaults delete <domain>; defaults import <domain> $BACKUPS/<domain>.plist" >&2
            result=1
        fi
    fi
    rm -rf "$ROOT" "$WORK"
    exit $result
}

if [[ -n "$(pgrep -x MoniCopy || true)" ]]; then
    echo "MoniCopy is running; quit it first" >&2
    exit 1
fi
if [[ -z "$APP" ]]; then
    log "building the release app"
    (cd "$PROJECT" && ./gradlew -q createReleaseDistributable) > "$WORK/build.log" 2>&1 || { cat "$WORK/build.log" >&2; exit 1; }
    APP="$PROJECT/build/compose/binaries/main-release/app/MoniCopy.app"
fi
[[ -d "$APP" ]] || { echo "app not found: $APP" >&2; exit 1; }
mkdir -p "$OUT"

cat > "$WORK/window_id.swift" << 'SWIFT'
import CoreGraphics
let pid = Int(CommandLine.arguments[1])!
let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as? [[String: Any]] ?? []
for window in list where (window[kCGWindowOwnerPID as String] as? Int) == pid {
    if let title = window[kCGWindowName as String] as? String, title.hasPrefix("MoniCopy"),
       let id = window[kCGWindowNumber as String] as? Int {
        print(id)
        break
    }
}
SWIFT
swiftc -O "$WORK/window_id.swift" -o "$WORK/window_id"

cat > "$WORK/ax.swift" << 'SWIFT'
import ApplicationServices
let arguments = CommandLine.arguments
let application = AXUIElementCreateApplication(pid_t(arguments[2])!)

func attribute(_ element: AXUIElement, _ name: String) -> CFTypeRef? {
    var value: CFTypeRef?
    return AXUIElementCopyAttributeValue(element, name as CFString, &value) == .success ? value : nil
}

func children(_ element: AXUIElement) -> [AXUIElement] {
    attribute(element, kAXChildrenAttribute) as? [AXUIElement] ?? []
}

func ownTexts(_ element: AXUIElement) -> [String] {
    [kAXTitleAttribute, kAXValueAttribute, kAXDescriptionAttribute]
        .compactMap { attribute(element, $0) as? String }
        .filter { !$0.isEmpty }
}

func allTexts(_ element: AXUIElement) -> [String] {
    ownTexts(element) + children(element).flatMap(allTexts)
}

func isPressable(_ element: AXUIElement) -> Bool {
    var names: CFArray?
    AXUIElementCopyActionNames(element, &names)
    return (names as? [String] ?? []).contains(kAXPressAction)
}

func press(_ element: AXUIElement, _ text: String) -> Bool {
    if isPressable(element), allTexts(element).contains(text) {
        return AXUIElementPerformAction(element, kAXPressAction as CFString) == .success
    }
    return children(element).contains { press($0, text) }
}

switch arguments[1] {
case "texts":
    allTexts(application).forEach { print($0) }
case "press":
    exit(press(application, arguments[3]) ? 0 : 1)
default:
    exit(2)
}
SWIFT
swiftc -O "$WORK/ax.swift" -o "$WORK/ax"

cat > "$WORK/park_pointer.swift" << 'SWIFT'
import CoreGraphics
let pid = Int(CommandLine.arguments[1])!
let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as? [[String: Any]] ?? []
for window in list where (window[kCGWindowOwnerPID as String] as? Int) == pid {
    if let title = window[kCGWindowName as String] as? String, title.hasPrefix("MoniCopy"),
       let bounds = window[kCGWindowBounds as String] as? [String: Double],
       let x = bounds["X"], let y = bounds["Y"], let height = bounds["Height"] {
        let point = CGPoint(x: max(x - 40, 1), y: y + height / 2)
        CGWarpMouseCursorPosition(point)
        CGEvent(mouseEventSource: nil, mouseType: .mouseMoved, mouseCursorPosition: point, mouseButton: .left)?
            .post(tap: .cghidEventTap)
        break
    }
}
SWIFT
swiftc -O "$WORK/park_pointer.swift" -o "$WORK/park_pointer"

texts() { "$WORK/ax" texts "$APP_PID" 2>/dev/null || true; }

press() {
    "$WORK/ax" press "$APP_PID" "$1" || { echo "could not press '$1'" >&2; exit 1; }
    log "pressed $1"
}

wait_until() {
    # $1 = timeout seconds, $2 = description, rest = command that must succeed
    local timeout="$1" what="$2"
    shift 2
    local deadline=$((SECONDS + timeout))
    until "$@"; do
        if (( SECONDS >= deadline )); then
            echo "timed out waiting for: $what" >&2
            exit 1
        fi
        sleep 0.1
    done
    log "ready: $what"
}

shows() { texts | grep -qxF "$1"; }

finding_files_only() {
    local current
    current="$(texts)"
    grep -qxF "Finding files to copy" <<< "$current" && ! grep -qxF "Start" <<< "$current"
}

uses_screenshot_folders() {
    local saved
    saved="$(defaults read com.thomaskuenneth.monicopy)"
    grep -qF "fileFrom = \"$ROOT/source\";" <<< "$saved" &&
        grep -qF "fileTo = \"$ROOT/destination\";" <<< "$saved" &&
        shows "$ROOT/Photos" && shows "$ROOT/Archive"
}

percent_at_least() {
    # $1 = progress label, $2 = minimum percent
    local percent
    percent="$(texts | sed -nE "s/^$1, ([0-9]+) percent\$/\\1/p" | head -1)"
    [[ -n "$percent" ]] && (( percent >= $2 ))
}

park_pointer() {
    "$WORK/park_pointer" "$APP_PID"
}

capture() {
    local window
    window="$("$WORK/window_id" "$APP_PID")"
    [[ -n "$window" ]] || { echo "MoniCopy window not found" >&2; exit 1; }
    screencapture -x -o -l"$window" "$OUT/$1"
    log "wrote $OUT/$1"
}

# 1. Back up the preferences, then create the test data and the screenshot preferences
trap finish EXIT
backup_preferences
(cd "$PROJECT" && ./gradlew -q prepareScreenshots)
log "prepared $ROOT"

# 2. Setup and ignored directories
open -a "$APP"
wait_until 30 "MoniCopy started" test -n "$(app_pid)"
APP_PID="$(app_pid)"
wait_until 30 "setup shown" shows "Start"
park_pointer
sleep 1
capture macos_01.png

press "Ignored directories"
wait_until 10 "ignored directories shown" shows "Add"
park_pointer
sleep 1
capture macos_02.png
press "Back"
wait_until 10 "setup shown again" shows "Ignored directories"

# 3. Copying, only with the /tmp folders
if ! uses_screenshot_folders; then
    echo "MoniCopy is not set to $ROOT/source and $ROOT/destination; not starting a copy. Texts mentioning $ROOT:" >&2
    texts | grep -F "$ROOT" >&2 || true
    exit 1
fi
park_pointer
press "Start"
wait_until 30 "finding files" finding_files_only
capture macos_03.png
wait_until 300 "copying at 20 %" percent_at_least "Copying" 20
capture macos_04.png
wait_until 300 "deleting orphans at 30 %" percent_at_least "Deleting orphaned files and folders" 30
capture macos_05.png
wait_until 300 "finished" shows "Finish"
sleep 1
capture macos_06.png
