# Makefile Guide - SVG to PNG Conversion

Quick reference for all available `make` targets for SVG to PNG conversion.

## Quick Start

```bash
# See all available targets
make help

# Run complete workflow (install, convert, verify)
make all

# Convert all SVG files to PNG
make convert

# Test with single file
make test
```

## All Available Targets

### 🎯 Essential Targets

#### `make help`
Shows all available targets with descriptions and usage examples.

```bash
$ make help
# Output: Interactive help menu with all targets listed
```

#### `make install`
Install rsvg-convert (librsvg) if not already installed. Auto-detects OS.

```bash
$ make install
# Output: Checks for rsvg-convert, installs if missing
```

#### `make convert`
Convert all featured SVG files to PNG at 1200x630px.

```bash
$ make convert
# Converts: *featured-contract-first.svg → .png
# All 7 featured images converted
```

#### `make test`
Test conversion with a single file (javapro-featured-contract-first.svg).

```bash
$ make test
# Safe test without affecting other images
```

#### `make verify`
Verify all PNG files exist and are valid.

```bash
$ make verify
# Checks all PNG files and reports status
```

#### `make clean`
Remove all generated PNG files (keeps SVG files).

```bash
$ make clean
# Safely removes PNG files for fresh conversion
```

### 📋 Workflow Targets

#### `make all`
Run complete workflow: clean → install → convert → verify.

```bash
$ make all
# Full workflow with all steps
# Best for: CI/CD, fresh setup
```

#### `make pre-commit`
Run pre-commit checks: lint → verify.

```bash
$ make pre-commit
# Checks before committing
```

### 🔧 Advanced Targets

#### `make status`
Show current status of SVG and PNG files.

```bash
$ make status
# Shows:
#   - rsvg-convert version
#   - SVG file count
#   - PNG file count
#   - Overall conversion status
```

#### `make version`
Show version information for all tools.

```bash
$ make version
# Shows: rsvg-convert, Bash, Python, linting tools
```

#### `make lint`
Lint markdown files and shell scripts.

```bash
$ make lint
# Optional: requires shellcheck and markdownlint
```

#### `make python`
Convert using Python script (instead of shell script).

```bash
$ make python
# Uses: convert-svg-to-png.py instead of .sh
```

#### `make shell`
Convert using shell script (default).

```bash
$ make shell
# Uses: convert-svg-to-png.sh
```

#### `make custom`
Convert with custom dimensions.

```bash
$ make custom WIDTH=1920 HEIGHT=1080
# Converts all SVG files to 1920x1080px
```

#### `make debug`
Show Makefile variables and configuration.

```bash
$ make debug
# Shows: Script paths, dimensions, file list
```

#### `make help-full`
Show extended help with detailed examples.

```bash
$ make help-full
# Shows: All examples and detailed usage
```

## Common Workflows

### Workflow 1: Initial Setup

```bash
# First time setup with full workflow
make all
```

**Steps:**
1. `make clean` - Remove old PNG files
2. `make install` - Ensure rsvg-convert is installed
3. `make convert` - Convert all SVG files
4. `make verify` - Verify all PNG files exist

### Workflow 2: Daily Development

```bash
# Test changes before committing
make test
make pre-commit
```

**Steps:**
1. `make test` - Quick test of conversion
2. `make pre-commit` - Run pre-commit checks
3. If all pass, commit changes

### Workflow 3: Custom Conversion

```bash
# Convert to different sizes for different use cases
make convert              # 1200x630 (default - featured images)
make custom WIDTH=1920 HEIGHT=1080  # 1920x1080 (large screens)
make custom WIDTH=600 HEIGHT=315   # 600x315 (mobile)
```

### Workflow 4: CI/CD Pipeline

```bash
# In your CI/CD configuration
make all
git add *.png
git commit -m "Regenerate PNG images from SVGs"
```

## Configuration

### Change Dimensions

Default is 1200x630px. Override with:

```bash
make convert WIDTH=1920 HEIGHT=1080
make custom WIDTH=600 HEIGHT=315
```

### Change File Pattern

Default pattern: `*featured-contract-first.svg`

```bash
make convert PATTERN="*javapro*.svg"
make convert PATTERN="*linkedin*.svg"
```

### Use Python Instead of Shell

```bash
make python
```

Equivalent to: `python3 convert-svg-to-png.py 1200 630 *featured-contract-first.svg`

## Examples

### Example 1: First Time User

```bash
# User clones repo and wants to generate PNG files
$ make help           # Show available commands
$ make all            # Run everything needed
# Result: All PNG files generated and verified
```

### Example 2: Check Current Status

```bash
$ make status
# Output shows if all conversions are up-to-date
```

### Example 3: Pre-commit Hook

```bash
# Before committing changes to SVG files
$ make test
$ make pre-commit
$ git add *.png
$ git commit -m "Update featured images"
```

### Example 4: Regenerate Specific Images

```bash
# Only regenerate LinkedIn images
$ make convert PATTERN="*linkedin*.svg"
```

### Example 5: Convert for Different Resolutions

```bash
# Create multiple resolutions for responsive design
$ make convert                                 # 1200x630
$ make convert WIDTH=1920 HEIGHT=1080         # 1920x1080 (large)
$ make convert WIDTH=600 HEIGHT=315 PATTERN="*featured*.svg"  # 600x315 (mobile)
```

## Target Dependencies

```
help
  ↓
install ← install rsvg-convert
  ↓
convert ← depends on: install
  ├─ Uses: convert-svg-to-png.sh or .py
  ├─ Input: SVG files matching PATTERN
  └─ Output: PNG files
  ↓
test ← Single file test (depends on: install)
  ├─ Input: javapro-featured-contract-first.svg
  └─ Output: javapro-featured-contract-first.png
  ↓
verify ← Check PNG files exist
  ├─ Input: PNG files from conversion
  └─ Status: Pass/Fail
  ↓
clean ← Remove PNG files
  ├─ Input: All *.png files
  └─ Output: Deleted files
  ↓
pre-commit ← Check before git commit
  ├─ Depends on: lint, verify
  └─ Status: Pass/Fail
  ↓
all ← Complete workflow
  ├─ Depends on: clean, install, convert, verify
  └─ Status: Success/Failure
```

## Troubleshooting

### Issue: "make: command not found"

**Solution**: Make sure `make` is installed

```bash
# macOS
brew install make

# Ubuntu/Debian
sudo apt-get install make

# Check installation
make --version
```

### Issue: rsvg-convert not installing

**Solution**: Try manual installation

```bash
# macOS
brew install librsvg

# Ubuntu/Debian
sudo apt-get install librsvg2-bin

# Fedora
sudo dnf install librsvg2-tools
```

### Issue: "Permission denied" on shell script

**Solution**: Make scripts executable

```bash
chmod +x convert-svg-to-png.sh convert-svg-to-png.py
make convert
```

### Issue: PNG files not being created

**Solution**: Check SVG files exist and are valid

```bash
make status        # Check file count
make test          # Test with single file
ls -lh *.svg       # List SVG files
```

### Issue: Custom dimensions not working

**Solution**: Ensure proper syntax

```bash
# CORRECT
make convert WIDTH=1920 HEIGHT=1080

# WRONG
make convert width=1920 height=1080
make convert 1920 1080
```

## Integration with CI/CD

### GitHub Actions

```yaml
name: Convert Images
on: [push, pull_request]
jobs:
  convert:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: |
          cd docs/images
          make all
      - run: |
          git config user.name "github-actions"
          git add *.png
          git commit -m "Auto-regenerate PNG images" || true
          git push
```

### GitLab CI

```yaml
convert_images:
  stage: build
  script:
    - cd docs/images
    - make all
  artifacts:
    paths:
      - docs/images/*.png
```

## Related Documentation

- **SVG-CONVERSION-GUIDE.md**: Complete technical guide
- **AUTO-INSTALLATION-SETUP.md**: Auto-installation details
- **convert-svg-to-png.sh**: Shell script implementation
- **convert-svg-to-png.py**: Python script implementation

## Quick Reference Card

```
┌─────────────────────────────────────────────────────┐
│ MAKEFILE QUICK REFERENCE                            │
├─────────────────────────────────────────────────────┤
│ make help              Show all targets              │
│ make install           Install rsvg-convert         │
│ make convert           Convert SVGs → PNGs           │
│ make test              Test conversion              │
│ make verify            Check PNG files              │
│ make clean             Remove PNG files             │
│ make pre-commit        Pre-commit checks            │
│ make status            Show conversion status       │
│ make version           Show tool versions           │
│ make all               Full workflow                │
├─────────────────────────────────────────────────────┤
│ Custom Options:                                     │
│ make convert WIDTH=1920 HEIGHT=1080                 │
│ make convert PATTERN="*javapro*.svg"                │
│ make python            Use Python script            │
│ make shell             Use Shell script             │
└─────────────────────────────────────────────────────┘
```

## Support

For issues:

1. Check `make status` to see current state
2. Run `make help-full` for detailed examples
3. Run `make debug` to see configuration
4. Check script logs: `make convert 2>&1 | tail`
5. See documentation: SVG-CONVERSION-GUIDE.md

## Notes

- All targets are colored output for easy reading
- Targets are safe to run multiple times
- `make clean` only removes PNG files, not SVGs
- `make test` doesn't affect other images
- `make all` is ideal for CI/CD pipelines
