# 🔧 Quick Fix for IntelliJ Image Visibility

## The Problem

You're seeing this because:
1. ❌ SVG images don't display in IntelliJ's markdown preview
2. ❌ GitHub badges don't work until workflows are pushed to GitHub

## Quick Solution (2 minutes)

### Step 1: Install ImageMagick

```bash
brew install imagemagick
```

### Step 2: Convert Images

```bash
cd /Users/wallaceespindola/git/contract-first-integrations/docs/images
./convert-svg-to-png.sh
```

### Step 3: Update Article References

```bash
cd /Users/wallaceespindola/git/contract-first-integrations/docs

# Update all articles to use PNG instead of SVG
sed -i '' 's/\.svg)$/.png)/g' *.md
```

That's it! Images will now be visible in IntelliJ.

---

## Alternative: Manual Conversion (if you don't want to install ImageMagick)

### Option A: Use Online Converter

1. Go to: https://cloudconvert.com/svg-to-png
2. Upload all 7 SVG files from `/docs/images/`
3. Download PNG files to same directory
4. Run the sed command from Step 3 above

### Option B: Use macOS Preview

```bash
cd /Users/wallaceespindola/git/contract-first-integrations/docs/images

# For each SVG file:
# 1. Double-click to open in Preview
# 2. File → Export → Format: PNG
# 3. Save with same name but .png extension
```

---

## About GitHub Badges

The badges in README.md won't work until you:

1. **Push code to GitHub** (if not already done)
2. **Workflows run at least once**

The workflow files already exist at:
- `.github/workflows/ci.yml` ✅
- `.github/workflows/codeql.yml` ✅

Once you push to GitHub, the badges will automatically update.

---

## Verify It Worked

```bash
# Open an article in IntelliJ
open -a "IntelliJ IDEA" docs/dzone-contract-first-integration-patterns.md

# Images should now be visible in the markdown preview pane
```

---

## Need Help?

If conversion fails:
```bash
# Check what's available
which convert
which magick
which rsvg-convert

# If none found, install ImageMagick:
brew install imagemagick

# Then try again
./convert-svg-to-png.sh
```

**Still having issues?** Check `CONVERSION_INSTRUCTIONS.md` for detailed troubleshooting.
