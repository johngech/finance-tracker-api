#!/bin/sh
#
# Git hooks setup script
#
# Configures git to use .githooks/ as the hooks directory.
# Run once after cloning the repository.
#
# Usage: bash .githooks/setup.sh
#

set -e

# Navigate to project root (parent of .githooks/)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# Configure git to use .githooks/
git config core.hooksPath .githooks

echo ""
echo "✅ Git hooks configured successfully!"
echo "   Hooks directory: $(pwd)/.githooks/"
echo ""
echo "   Hooks installed:"
echo "     pre-commit  → Secret detection (Gitleaks)"
echo "     pre-push    → Compile + Test gate"
echo ""
echo "   Prerequisites:"
echo "     • gitleaks (for pre-commit): https://github.com/gitleaks/gitleaks#installation"
echo "     • maven + java 17 (for pre-push): sdk env"
echo ""
