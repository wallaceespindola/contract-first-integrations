#!/bin/bash

# Convert SVG images to PNG for better IntelliJ markdown preview compatibility
# This script requires one of: ImageMagick, rsvg-convert, or Inkscape

set -e

IMAGES_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$IMAGES_DIR"

echo "Converting SVG images to PNG..."
echo "Directory: $IMAGES_DIR"
echo ""

# Check which tool is available
if command -v convert &> /dev/null; then
    TOOL="imagemagick"
    echo "Using ImageMagick (convert)"
elif command -v magick &> /dev/null; then
    TOOL="magick"
    echo "Using ImageMagick (magick)"
elif command -v rsvg-convert &> /dev/null; then
    TOOL="rsvg"
    echo "Using rsvg-convert"
elif command -v inkscape &> /dev/null; then
    TOOL="inkscape"
    echo "Using Inkscape"
else
    echo "❌ Error: No SVG conversion tool found!"
    echo ""
    echo "Please install one of:"
    echo "  - ImageMagick:  brew install imagemagick"
    echo "  - librsvg:      brew install librsvg"
    echo "  - Inkscape:     brew install --cask inkscape"
    echo ""
    echo "Or use online conversion: https://cloudconvert.com/svg-to-png"
    exit 1
fi

echo ""

# Convert each SVG file
for svg_file in *.svg; do
    if [ "$svg_file" = "*.svg" ]; then
        echo "No SVG files found"
        exit 0
    fi

    png_file="${svg_file%.svg}.png"
    echo "Converting: $svg_file → $png_file"

    case $TOOL in
        imagemagick)
            convert -density 300 -background none "$svg_file" -quality 90 "$png_file"
            ;;
        magick)
            magick -density 300 -background none "$svg_file" -quality 90 "$png_file"
            ;;
        rsvg)
            rsvg-convert -f png -d 300 -p 300 -b white "$svg_file" -o "$png_file"
            ;;
        inkscape)
            inkscape "$svg_file" --export-filename="$png_file" --export-dpi=300
            ;;
    esac

    if [ -f "$png_file" ]; then
        size=$(ls -lh "$png_file" | awk '{print $5}')
        echo "  ✓ Created: $png_file ($size)"
    else
        echo "  ✗ Failed: $png_file"
    fi
done

echo ""
echo "✅ Conversion complete!"
echo ""
echo "Next steps:"
echo "1. The PNG files are now in: $IMAGES_DIR"
echo "2. Markdown files will be updated to reference PNG images"
echo "3. SVG files will remain for web/print use"
