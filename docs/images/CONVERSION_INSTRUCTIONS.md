# SVG to PNG Conversion Instructions

## Problem

IntelliJ IDEA's markdown preview has limited SVG support. To make images visible in IntelliJ, we need PNG versions.

## Quick Fix

### Option 1: Install ImageMagick (Recommended)

```bash
# Install ImageMagick via Homebrew
brew install imagemagick

# Navigate to images directory
cd /Users/wallaceespindola/git/contract-first-integrations/docs/images

# Run conversion script
./convert-svg-to-png.sh
```

### Option 2: Install librsvg

```bash
# Install librsvg via Homebrew
brew install librsvg

# Run conversion script
cd /Users/wallaceespindola/git/contract-first-integrations/docs/images
./convert-svg-to-png.sh
```

### Option 3: Online Conversion

If you don't want to install anything:

1. Go to https://cloudconvert.com/svg-to-png
2. Upload each SVG file:
   - `dzone-featured-contract-first.svg`
   - `medium-featured-contract-first.svg`
   - `linkedin-featured-contract-first.svg`
   - `devto-featured-contract-first.svg`
   - `javapro-featured-contract-first.svg`
   - `substack-featured-contract-first.svg`
   - `infoq-featured-contract-first.svg`
3. Download PNG versions to `/docs/images/` directory
4. PNG files should have same name but `.png` extension

### Option 4: Manual Conversion (macOS)

```bash
cd /Users/wallaceespindola/git/contract-first-integrations/docs/images

# For each SVG file, open in Safari and export as PNG
# Or use Preview.app: Open SVG → File → Export → PNG
```

## After Conversion

Once PNG files are created, the markdown articles will automatically display them in IntelliJ.

Expected PNG files:
- `dzone-featured-contract-first.png` (1200x628px)
- `medium-featured-contract-first.png` (1200x627px)
- `linkedin-featured-contract-first.png` (1200x627px)
- `devto-featured-contract-first.png` (1000x420px)
- `javapro-featured-contract-first.png` (1200x800px)
- `substack-featured-contract-first.png` (1200x675px)
- `infoq-featured-contract-first.png` (1200x630px)

## GitHub Badges Issue

The CI and CodeQL badges in README.md won't work until the GitHub Actions workflows are pushed to the repository. The workflows need to be in:
- `.github/workflows/ci.yml`
- `.github/workflows/codeql.yml`

These already exist in the repository, so once you push to GitHub, the badges will start working.

## Verification

After conversion, check that images are visible:

```bash
# Open an article in IntelliJ
open -a "IntelliJ IDEA" docs/dzone-contract-first-integration-patterns.md

# Images should now be visible in the markdown preview
```

## Troubleshooting

### Images still not visible in IntelliJ

1. **Enable markdown preview**: View → Tool Windows → Markdown Preview
2. **Refresh preview**: Close and reopen the markdown file
3. **Check paths**: Ensure `./images/filename.png` exists
4. **IntelliJ settings**: Preferences → Languages & Frameworks → Markdown → Preview → Enable images

### Conversion script fails

```bash
# Check if script is executable
ls -l convert-svg-to-png.sh

# Make executable if needed
chmod +x convert-svg-to-png.sh

# Run with verbose output
bash -x convert-svg-to-png.sh
```

## Keep Both Formats

**Important**: Keep both SVG and PNG files:
- **PNG**: For IntelliJ preview and broad compatibility
- **SVG**: For web publishing (smaller file size, scalable, better quality)

When publishing to platforms:
- Use SVG for web articles (most platforms support it)
- Use PNG as fallback for email newsletters or older browsers
