# Auto-Installation Setup Guide

Both conversion scripts now include **automatic detection and installation** of `rsvg-convert` if it's not found on your system.

## How Auto-Installation Works

### Shell Script Flow

```
1. Check if rsvg-convert is installed
   ├─ If YES → Continue to conversion
   └─ If NO  → Detect OS and install automatically
       ├─ macOS        → brew install librsvg
       ├─ Ubuntu/Debian → sudo apt-get install librsvg2-bin
       ├─ Fedora/RHEL  → sudo dnf install librsvg2-tools
       ├─ WSL          → sudo apt-get install librsvg2-bin
       └─ Other        → Show manual installation instructions
2. Verify installation success
3. Proceed with conversion
```

### Python Script Flow

Same as shell script, with additional support for:
- Cross-platform OS detection
- Automatic distribution identification
- Better error messages and logging
- Fallback instructions for unsupported systems

## Usage - Zero Setup Required

You can now run the scripts **without pre-installing rsvg-convert**:

### Before (Manual Setup)
```bash
# Had to manually install first
brew install librsvg
./convert-svg-to-png.sh
```

### After (Auto-Installation)
```bash
# Just run it - installs if needed
./convert-svg-to-png.sh
```

## Supported Operating Systems

| OS | Auto-Install | Command | Notes |
|---|---|---|---|
| **macOS** | ✅ Yes | `brew install librsvg` | Requires Homebrew |
| **Ubuntu/Debian** | ✅ Yes | `sudo apt-get install librsvg2-bin` | Requires sudo |
| **Fedora/RHEL/CentOS** | ✅ Yes | `sudo dnf install librsvg2-tools` | Requires sudo |
| **WSL (Windows)** | ✅ Yes | `sudo apt-get install librsvg2-bin` | Uses Ubuntu/Debian backend |
| **Other Linux** | ⚠️ Manual | See error message | Instructions provided |

## Examples

### Scenario 1: macOS User Without Homebrew

```bash
./convert-svg-to-png.sh
```

**Output:**
```
⚠ rsvg-convert not found. Installing...

Detected: macOS
✗ Homebrew not found

Please install Homebrew first:
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

Then run this script again.
```

### Scenario 2: Ubuntu User

```bash
./convert-svg-to-png.sh
```

**Output:**
```
⚠ rsvg-convert not found. Installing...

Detected: Ubuntu/Debian
Installing librsvg2-bin via apt...
[sudo prompts for password]
✓ librsvg2-bin installed successfully

Configuration:
  Output dimensions: 1200x630px
  ...

Found 7 SVG file(s)
Converting: devto-featured-contract-first.svg         ✓ (140K)
...
```

### Scenario 3: Already Installed

```bash
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

Tool: rsvg-convert version 2.61.3
════════════════════════════════════════════════════════

Found 7 SVG file(s)
Converting: devto-featured-contract-first.svg         ✓ (140K)
...
```

## Requirements for Auto-Installation

### macOS
- Homebrew must be installed (or script will provide installation link)
- No `sudo` required (Homebrew handles permissions)

### Linux (Ubuntu/Debian/WSL)
- `sudo` privilege required
- APT package manager must be available
- Internet access for package download

### Linux (Fedora/RHEL/CentOS)
- `sudo` privilege required
- DNF package manager must be available
- Internet access for package download

## Troubleshooting Auto-Installation

### Issue: "Permission Denied" Error

**Cause**: Script is not executable
**Solution**:
```bash
chmod +x convert-svg-to-png.sh
./convert-svg-to-png.sh
```

### Issue: "Homebrew Not Found" (macOS)

**Cause**: Homebrew is not installed
**Solution**: Install Homebrew first, then run script again
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
./convert-svg-to-png.sh
```

### Issue: Installation Hangs or Times Out

**Cause**: Network issues or large downloads
**Solution**: Install manually in a separate terminal
```bash
# macOS
brew install librsvg

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install librsvg2-bin

# Fedora
sudo dnf install librsvg2-tools
```

Then re-run the script.

### Issue: "sudo: command not found" (WSL/Linux)

**Cause**: WSL or custom Linux environment without sudo
**Solution**: Install manually as root
```bash
apt-get update
apt-get install librsvg2-bin
```

Then run the script.

## Manual Installation (If Auto-Install Fails)

### macOS
```bash
# Install Homebrew if not present
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install librsvg
brew install librsvg

# Verify installation
rsvg-convert --version
```

### Ubuntu/Debian
```bash
# Update package lists
sudo apt-get update

# Install librsvg2-bin
sudo apt-get install librsvg2-bin

# Verify installation
rsvg-convert --version
```

### Fedora/RHEL/CentOS
```bash
# Install librsvg2-tools
sudo dnf install librsvg2-tools

# Verify installation
rsvg-convert --version
```

### Alpine/Minimal Linux
```bash
# Alpine uses apk
apk add librsvg

# Verify installation
rsvg-convert --version
```

## What Gets Installed

The auto-installer installs **librsvg** (rsvg-convert), which includes:

- **rsvg-convert**: Command-line SVG to PNG converter
- **librsvg library**: SVG rendering engine
- **Dependencies**: Cairo, Pango, Harfbuzz, Fontconfig

**Total size**: ~10-20 MB (very lightweight)

**What it provides**:
- ✅ Professional-grade SVG rendering
- ✅ High-quality PNG conversion
- ✅ Font support
- ✅ Gradient and filter rendering
- ✅ Fast performance

## CI/CD Integration

### GitHub Actions

```yaml
name: Convert SVG Images

on: [push, pull_request]

jobs:
  convert:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Convert SVGs to PNG
        run: |
          cd docs/images
          ./convert-svg-to-png.sh
          # Or: python3 convert-svg-to-png.py

      - name: Commit changes
        run: |
          git config user.name "github-actions"
          git config user.email "actions@github.com"
          git add docs/images/*.png
          git diff --cached --quiet || git commit -m "Auto-regenerate PNG images from SVGs"
          git push
```

### GitLab CI

```yaml
convert_images:
  stage: build
  image: ubuntu:latest
  script:
    - apt-get update && apt-get install -y librsvg2-bin
    - cd docs/images
    - python3 convert-svg-to-png.py
  artifacts:
    paths:
      - docs/images/*.png
    expire_in: 1 day
```

## Testing the Auto-Installation

To test the auto-installation feature (without uninstalling rsvg-convert):

```bash
# Simulate missing rsvg-convert
cd docs/images
mv /usr/local/bin/rsvg-convert /tmp/rsvg-convert.bak

# Run script - should auto-install
./convert-svg-to-png.sh

# Restore if needed
mv /tmp/rsvg-convert.bak /usr/local/bin/rsvg-convert
```

## Advantages of Auto-Installation

1. **Zero Setup**: Users don't need to know about dependencies
2. **Cross-Platform**: Works the same on macOS, Linux, WSL
3. **Self-Documenting**: Clear error messages guide users
4. **Reliable**: Uses official package managers
5. **Minimal**: Only installs what's needed
6. **Non-Intrusive**: Doesn't modify system beyond installation

## Security Considerations

- ✅ Uses official package managers (brew, apt, dnf)
- ✅ Installs only required packages
- ✅ No custom build scripts or downloads from third parties
- ✅ Transparent about what's being installed
- ✅ Requires `sudo` only when necessary (Linux)

## Support

For issues with auto-installation:

1. **Check OS detection**: Run the script and look at "Detected:" message
2. **Manual install**: See sections above for your OS
3. **Check permissions**: Ensure script is executable (`chmod +x`)
4. **Verify network**: Ensure internet connection for package downloads
5. **Report issues**: If auto-installation fails, please report with error output
