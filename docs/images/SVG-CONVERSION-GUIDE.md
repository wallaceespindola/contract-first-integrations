# SVG to PNG Conversion Guide

This directory contains two scripts for converting SVG files to PNG format using **rsvg-convert** (librsvg), which provides consistent, high-quality rendering.

## Quick Start

### Shell Script (Recommended for simplicity)

```bash
# Convert all featured SVG files to 1200x630px (default)
./convert-svg-to-png.sh

# Convert with custom dimensions
./convert-svg-to-png.sh 1920 1080

# Convert specific pattern
./convert-svg-to-png.sh 1200 630 "*.svg"
```

### Python Script (Recommended for flexibility)

```bash
# Convert all featured SVG files to 1200x630px (default)
python3 convert-svg-to-png.py

# Convert with custom dimensions
python3 convert-svg-to-png.py 1920 1080

# Convert specific pattern
python3 convert-svg-to-png.py 1200 630 "*.svg"
```

## Installation Requirements

### macOS (Recommended)
```bash
brew install librsvg
```

### Linux (Ubuntu/Debian)
```bash
sudo apt-get install librsvg2-bin
```

### Linux (Fedora)
```bash
sudo dnf install librsvg2-tools
```

## What is rsvg-convert?

**rsvg-convert** is the official conversion tool from librsvg, the SVG rendering library used by Firefox and GNOME. It provides:

- ✅ **Consistent rendering** across platforms
- ✅ **High quality** output with proper gradient/filter support
- ✅ **Fast performance** (C-based, not JavaScript)
- ✅ **Reliable text rendering** with font support
- ✅ **Proper color handling** for complex SVG designs

### Why not ImageMagick or Inkscape?

| Tool | Quality | Speed | Installation | Pros | Cons |
|------|---------|-------|--------------|------|------|
| **rsvg-convert** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✓ Easy | Designed for SVG, industry standard | - |
| ImageMagick | ⭐⭐⭐ | ⭐⭐⭐ | ✓ Exists | Installed on many systems | Requires Ghostscript, inconsistent SVG support |
| Inkscape | ⭐⭐⭐⭐ | ⭐⭐ | ✗ Large | GUI available, very capable | Slow, heavy dependency, CLI less documented |

## Conversion Method

Both scripts use the **CORRECT** rsvg-convert command:

```bash
rsvg-convert -w <width> -h <height> <input.svg> -o <output.png>
```

### Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `-w` | Output width in pixels | `1200` |
| `-h` | Output height in pixels | `630` |
| `<input.svg>` | Input SVG file | `javapro-featured-contract-first.svg` |
| `-o` | Output PNG file | `javapro-featured-contract-first.png` |

### Why This Works

1. **Explicit dimensions**: `-w` and `-h` ensure exact pixel output size
2. **Proper scaling**: SVG is rendered to internal resolution then scaled to exact size
3. **Quality preservation**: All SVG features (gradients, filters, text) are preserved
4. **Consistent output**: Same parameters always produce identical results

## Usage Examples

### Convert All Featured Images

```bash
cd docs/images
./convert-svg-to-png.sh
```

**Output:**
```
════════════════════════════════════════════════════════
SVG to PNG Converter (librsvg)
════════════════════════════════════════════════════════

Configuration:
  Output dimensions: 1200x630px
  File pattern: *featured-contract-first.svg
  Directory: /path/to/docs/images

Tool: rsvg (X.Y.Z)
════════════════════════════════════════════════════════

Found 7 SVG file(s)

Converting: devto-featured-contract-first.svg          ✓ (140K)
Converting: dzone-featured-contract-first.svg         ✓ (134K)
Converting: infoq-featured-contract-first.svg         ✓ (122K)
Converting: javapro-featured-contract-first.svg       ✓ (115K)
Converting: linkedin-featured-contract-first.svg      ✓ (147K)
Converting: medium-featured-contract-first.svg        ✓ (162K)
Converting: substack-featured-contract-first.svg      ✓ (180K)

════════════════════════════════════════════════════════
Conversion Summary
  Successful: 7
  All files converted successfully!
════════════════════════════════════════════════════════
```

### Convert to Different Dimensions

For social media (1920x1080):
```bash
./convert-svg-to-png.sh 1920 1080
```

For mobile (600x315):
```bash
./convert-svg-to-png.sh 600 315
```

For print (2400x1260 @ 300 DPI equivalent):
```bash
./convert-svg-to-png.sh 2400 1260
```

### Convert Specific Files

```bash
# Only convert LinkedIn and Medium
./convert-svg-to-png.sh 1200 630 "*linkedin*featured*.svg"

# Only convert files with "dzone" in name
python3 convert-svg-to-png.py 1200 630 "*dzone*.svg"
```

## Troubleshooting

### "rsvg-convert: command not found"

Install librsvg:
```bash
# macOS
brew install librsvg

# Ubuntu/Debian
sudo apt-get install librsvg2-bin

# Fedora
sudo dnf install librsvg2-tools
```

### PNG file sizes are too small (< 50K)

This usually means the conversion failed silently. Check:
1. SVG file is valid XML
2. SVG has actual content (not empty)
3. All referenced fonts/images exist in SVG

### Text appears wrong or is missing

1. Ensure SVG has embedded fonts or uses system fonts
2. Check SVG doesn't rely on external stylesheets
3. Verify SVG displays correctly in browser first

### Colors look different in PNG vs SVG

This is usually due to:
1. Browser color profiles affecting SVG display
2. PNG using different color space
3. Opacity/filter rendering differences

Solution: Always check actual SVG file in consistent environment (Firefox/Chrome, same monitor)

## Script Features

### Shell Script (`convert-svg-to-png.sh`)

✅ **Pros:**
- Lightweight (4KB)
- No dependencies (pure bash)
- Very fast execution
- Works on any Unix-like system

❌ **Cons:**
- Less flexible for complex scenarios
- Limited error handling
- No cross-platform (macOS/Windows) support

### Python Script (`convert-svg-to-png.py`)

✅ **Pros:**
- More robust error handling
- Better logging and diagnostics
- Cross-platform (works on Windows with Python installed)
- Easier to extend/modify
- Detailed failure messages

❌ **Cons:**
- Requires Python 3.6+
- Slightly slower startup (Python interpreter)
- Larger file (5.5KB)

### Recommendation

- **Quick conversions**: Use shell script
- **Batch processing**: Use Python script
- **CI/CD pipelines**: Use Python script (more reliable)
- **One-time conversion**: Either script

## Integration with CI/CD

### GitHub Actions Example

```yaml
- name: Convert SVG to PNG
  run: |
    cd docs/images
    ./convert-svg-to-png.sh 1200 630
```

### GitLab CI Example

```yaml
convert_images:
  script:
    - cd docs/images
    - python3 convert-svg-to-png.py 1200 630
```

## Common Tasks

### Update all images after editing SVGs

```bash
cd docs/images
./convert-svg-to-png.sh
```

### Generate multiple resolutions for responsive images

```bash
# Generate 1200x630 (default social media)
./convert-svg-to-png.sh 1200 630

# Generate 1920x1080 (larger screens)
./convert-svg-to-png.sh 1920 1080 "*featured*.svg"

# Generate 600x315 (mobile)
./convert-svg-to-png.sh 600 315 "*featured*.svg"
```

### Add to git commit before publishing

```bash
git add docs/images/*.png
git commit -m "Regenerate PNG images from updated SVGs"
```

## Technical Details

### Rendering Process

1. **Parse**: rsvg-convert reads SVG XML structure
2. **Render**: Converts vector shapes to pixel buffer at high internal resolution
3. **Scale**: Downsamples to exact requested dimensions (1200x630)
4. **Encode**: Compresses and saves as PNG format
5. **Output**: File size depends on image complexity (typically 115K-180K for featured images)

### File Size Expectations

| Image Type | Typical Size |
|-----------|--------------|
| Simple gradient + text | 50-100K |
| Complex gradients + filters | 100-150K |
| Animated SVG (static export) | 120-180K |

## References

- [librsvg Documentation](https://wiki.gnome.org/Projects/librsvg)
- [rsvg-convert Manual](https://wiki.gnome.org/Projects/librsvg#)
- [SVG Specification](https://www.w3.org/TR/SVG2/)

## Support

For issues with:
- **Script execution**: Check file permissions (`chmod +x convert-svg-to-png.sh`)
- **rsvg-convert**: Install librsvg for your OS
- **SVG content**: Validate SVG with [W3C Validator](https://www.w3.org/svg/validator/)
- **Image quality**: Ensure SVG displays correctly in browser first
