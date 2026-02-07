#!/usr/bin/env python3
"""
Convert SVG images to PNG using Python (cairosvg or alternative methods)
Fallback if ImageMagick is not available
"""

import os
import sys

try:
    import cairosvg
    HAS_CAIROSVG = True
except ImportError:
    HAS_CAIROSVG = False

def convert_with_cairosvg(svg_path, png_path, scale=2):
    """Convert using cairosvg (requires: pip install cairosvg)"""
    cairosvg.svg2png(
        url=svg_path,
        write_to=png_path,
        scale=scale  # 2x for better quality
    )

def main():
    images_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(images_dir)
    
    svg_files = [f for f in os.listdir('.') if f.endswith('.svg')]
    
    if not svg_files:
        print("No SVG files found in current directory")
        return 1
    
    if not HAS_CAIROSVG:
        print("❌ cairosvg not installed")
        print("")
        print("Please install it:")
        print("  pip3 install cairosvg")
        print("")
        print("Or use the shell script with ImageMagick instead:")
        print("  brew install imagemagick")
        print("  ./convert-svg-to-png.sh")
        return 1
    
    print(f"Converting {len(svg_files)} SVG files to PNG...")
    print(f"Directory: {images_dir}")
    print("")
    
    for svg_file in svg_files:
        png_file = svg_file.replace('.svg', '.png')
        print(f"Converting: {svg_file} → {png_file}")
        
        try:
            convert_with_cairosvg(svg_file, png_file)
            size = os.path.getsize(png_file) / 1024
            print(f"  ✓ Created: {png_file} ({size:.1f} KB)")
        except Exception as e:
            print(f"  ✗ Failed: {str(e)}")
    
    print("")
    print("✅ Conversion complete!")
    print("")
    print("Next step: Update markdown files to use PNG:")
    print("  cd ../")
    print("  sed -i '' 's/\\.svg)$/.png)/g' *.md")
    
    return 0

if __name__ == '__main__':
    sys.exit(main())
