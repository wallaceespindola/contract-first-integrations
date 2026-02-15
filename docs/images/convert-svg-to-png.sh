#!/bin/bash

################################################################################
# SVG to PNG Converter Script with Auto-Installation
#
# Purpose: Convert SVG files to PNG using rsvg-convert (librsvg)
#
# This is the RECOMMENDED method for consistent, high-quality SVG to PNG conversion
#
# Features:
#   - Automatic detection and installation of rsvg-convert if missing
#   - Works on macOS, Linux (Ubuntu/Debian, Fedora), and WSL
#   - Configurable output dimensions
#   - Batch conversion with pattern matching
#
# Usage:
#   ./convert-svg-to-png.sh [width] [height] [pattern]
#
# Examples:
#   # Convert all featured SVG files to 1200x630px (default)
#   ./convert-svg-to-png.sh
#
#   # Convert to custom dimensions
#   ./convert-svg-to-png.sh 1920 1080
#
#   # Convert specific pattern
#   ./convert-svg-to-png.sh 1200 630 "*featured*.svg"
#
################################################################################

set -e

# Default parameters
WIDTH=${1:-1200}
HEIGHT=${2:-630}
PATTERN=${3:-"*featured-contract-first.svg"}

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}SVG to PNG Converter (librsvg)${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""
echo "Configuration:"
echo "  Output dimensions: ${WIDTH}x${HEIGHT}px"
echo "  File pattern: $PATTERN"
echo "  Directory: $SCRIPT_DIR"
echo ""

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif grep -qi microsoft /proc/version 2>/dev/null; then
        echo "wsl"
    elif [ -f /etc/os-release ]; then
        . /etc/os-release
        echo "$ID"
    else
        echo "unknown"
    fi
}

# Check if rsvg-convert is installed
if ! command -v rsvg-convert &> /dev/null; then
    echo -e "${YELLOW}⚠ rsvg-convert not found. Installing...${NC}"
    echo ""

    OS=$(detect_os)

    case $OS in
        macos)
            echo "Detected: macOS"
            echo "Installing librsvg via Homebrew..."

            # Check if Homebrew is installed
            if ! command -v brew &> /dev/null; then
                echo -e "${RED}✗ Homebrew not found${NC}"
                echo ""
                echo "Please install Homebrew first:"
                echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                echo ""
                echo "Then run this script again."
                exit 1
            fi

            if brew install librsvg; then
                echo -e "${GREEN}✓ librsvg installed successfully${NC}"
            else
                echo -e "${RED}✗ Failed to install librsvg${NC}"
                exit 1
            fi
            ;;

        ubuntu|debian)
            echo "Detected: Ubuntu/Debian"
            echo "Installing librsvg2-bin via apt..."

            if sudo apt-get update && sudo apt-get install -y librsvg2-bin; then
                echo -e "${GREEN}✓ librsvg2-bin installed successfully${NC}"
            else
                echo -e "${RED}✗ Failed to install librsvg2-bin${NC}"
                exit 1
            fi
            ;;

        fedora|rhel|centos)
            echo "Detected: Fedora/RHEL/CentOS"
            echo "Installing librsvg2-tools via dnf..."

            if sudo dnf install -y librsvg2-tools; then
                echo -e "${GREEN}✓ librsvg2-tools installed successfully${NC}"
            else
                echo -e "${RED}✗ Failed to install librsvg2-tools${NC}"
                exit 1
            fi
            ;;

        wsl)
            echo "Detected: Windows Subsystem for Linux (WSL)"
            echo "Installing librsvg2-bin..."

            if sudo apt-get update && sudo apt-get install -y librsvg2-bin; then
                echo -e "${GREEN}✓ librsvg2-bin installed successfully${NC}"
            else
                echo -e "${RED}✗ Failed to install librsvg2-bin${NC}"
                exit 1
            fi
            ;;

        *)
            echo -e "${RED}✗ Unknown OS: $OS${NC}"
            echo ""
            echo "Please install librsvg manually:"
            echo "  macOS:  brew install librsvg"
            echo "  Ubuntu/Debian:  sudo apt-get install librsvg2-bin"
            echo "  Fedora:  sudo dnf install librsvg2-tools"
            echo "  Other:  Install librsvg from your package manager"
            echo ""
            exit 1
            ;;
    esac

    echo ""
fi

# Verify rsvg-convert version
RSVG_VERSION=$(rsvg-convert --version 2>/dev/null | head -1)
echo "Tool: $RSVG_VERSION"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""

# Count matching files
SVG_FILES=$(ls -1 $PATTERN 2>/dev/null || echo "")
FILE_COUNT=$(echo "$SVG_FILES" | wc -l)

if [ -z "$SVG_FILES" ] || [ "$FILE_COUNT" -eq 0 ]; then
    echo -e "${RED}✗ No SVG files matching pattern: $PATTERN${NC}"
    exit 1
fi

echo "Found $FILE_COUNT SVG file(s)"
echo ""

# Convert files
SUCCESS=0
FAILED=0
FAILED_FILES=""

for svg_file in $PATTERN; do
    if [ -f "$svg_file" ]; then
        # Generate output filename
        png_file="${svg_file%.svg}.png"

        # Show progress
        printf "Converting: %-45s " "$svg_file"

        # Convert using rsvg-convert with explicit dimensions
        # This is the CORRECT method for consistent rendering
        if rsvg-convert -w "$WIDTH" -h "$HEIGHT" "$svg_file" -o "$png_file" 2>/dev/null; then
            FILE_SIZE=$(ls -lh "$png_file" | awk '{print $5}')
            echo -e "${GREEN}✓${NC} ($FILE_SIZE)"
            ((SUCCESS++))
        else
            echo -e "${RED}✗ Failed${NC}"
            ((FAILED++))
            FAILED_FILES="$FAILED_FILES\n  - $svg_file"
        fi
    fi
done

# Print summary
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Conversion Summary${NC}"
echo "  Successful: $SUCCESS"
if [ $FAILED -gt 0 ]; then
    echo -e "  ${RED}Failed: $FAILED${NC}"
    echo -e "${RED}Failed files:${NC}$FAILED_FILES"
else
    echo -e "  ${GREEN}All files converted successfully!${NC}"
fi
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"

exit $FAILED
