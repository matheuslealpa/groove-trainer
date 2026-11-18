#!/usr/bin/env bash
#
# Installs or updates Groove Trainer on Linux, without sudo.
#
#   curl -fsSL https://raw.githubusercontent.com/matheuslealpa/groove-trainer/main/install.sh | bash
#
# Options (pass them after "bash -s --" when piping from curl):
#   --version X.Y.Z   install a specific version instead of the latest
#   --force           reinstall even if that version is already installed
#   --uninstall       remove Groove Trainer
#
set -euo pipefail

REPO="matheuslealpa/groove-trainer"
ASSET="groove-trainer-linux-x64.tar.gz"
DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
INSTALL_DIR="${GROOVE_TRAINER_DIR:-$DATA_HOME/groove-trainer}"
BIN_DIR="${GROOVE_TRAINER_BIN_DIR:-$HOME/.local/bin}"
DESKTOP_FILE="$DATA_HOME/applications/groove-trainer.desktop"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

refresh_menu() {
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$DATA_HOME/applications" >/dev/null 2>&1 || true
    fi
}

uninstall() {
    rm -rf "$INSTALL_DIR"
    rm -f "$BIN_DIR/groove-trainer" "$DESKTOP_FILE"
    refresh_menu
    info "Groove Trainer removed."
}

version=""
force=false
while [ $# -gt 0 ]; do
    case "$1" in
        --version) version="${2#v}"; shift 2 ;;
        --force) force=true; shift ;;
        --uninstall) uninstall; exit 0 ;;
        *) fail "unknown option: $1" ;;
    esac
done

[ "$(uname -s)" = "Linux" ] || fail "this installer is Linux only. On Windows, use the .msi from the releases page."
[ "$(uname -m)" = "x86_64" ] || fail "architecture $(uname -m) is not supported yet (x86_64 only)."
command -v curl >/dev/null 2>&1 || fail "curl not found."
command -v tar >/dev/null 2>&1 || fail "tar not found."

# Find the latest version from the /releases/latest redirect (no API call, no rate limit).
if [ -z "$version" ]; then
    latest_url=$(curl -fsSLI -o /dev/null -w '%{url_effective}' "https://github.com/$REPO/releases/latest") \
        || fail "could not fetch the latest version from GitHub."
    version="${latest_url##*/v}"
    [ -n "$version" ] && [ "$version" != "$latest_url" ] || fail "no release published yet."
fi

installed=""
[ -f "$INSTALL_DIR/version" ] && installed=$(cat "$INSTALL_DIR/version")
if [ "$installed" = "$version" ] && [ "$force" = false ]; then
    info "Groove Trainer $version is already installed and up to date."
    exit 0
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

if [ -n "${GROOVE_TRAINER_ARCHIVE:-}" ]; then
    # For local testing: use an existing .tar.gz instead of downloading.
    cp "$GROOVE_TRAINER_ARCHIVE" "$tmp/$ASSET"
else
    info "Downloading Groove Trainer $version..."
    curl -fL --progress-bar -o "$tmp/$ASSET" \
        "https://github.com/$REPO/releases/download/v$version/$ASSET" \
        || fail "download failed. Does version $version exist and include $ASSET?"
fi

tar -xzf "$tmp/$ASSET" -C "$tmp"
[ -x "$tmp/GrooveTrainer/bin/GrooveTrainer" ] || fail "downloaded archive does not have the expected layout."

# Replace the previous installation.
mkdir -p "$INSTALL_DIR"
rm -rf "$INSTALL_DIR/app"
mv "$tmp/GrooveTrainer" "$INSTALL_DIR/app"
printf '%s\n' "$version" > "$INSTALL_DIR/version"

mkdir -p "$BIN_DIR"
ln -sf "$INSTALL_DIR/app/bin/GrooveTrainer" "$BIN_DIR/groove-trainer"

mkdir -p "$(dirname "$DESKTOP_FILE")"
cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Name=Groove Trainer
Comment=Metronome and timing trainer for drum practice
Exec=$INSTALL_DIR/app/bin/GrooveTrainer
Icon=$INSTALL_DIR/app/lib/GrooveTrainer.png
Categories=AudioVideo;Audio;Music;
Terminal=false
EOF
refresh_menu

if [ -n "$installed" ]; then
    info "Groove Trainer updated: $installed -> $version"
else
    info "Groove Trainer $version installed."
fi

case ":$PATH:" in
    *":$BIN_DIR:"*) info "Open it from the applications menu or run: groove-trainer" ;;
    *) info "Open it from the applications menu. To run it from a terminal, add $BIN_DIR to your PATH:"
       printf '    echo '\''export PATH="%s:$PATH"'\'' >> ~/.bashrc\n' "$BIN_DIR" ;;
esac
