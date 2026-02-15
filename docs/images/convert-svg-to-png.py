#!/usr/bin/env python3

"""
SVG to PNG Converter Script with Auto-Installation

Purpose: Convert SVG files to PNG using rsvg-convert (librsvg)

This is the RECOMMENDED method for consistent, high-quality SVG to PNG conversion.

Features:
    - Automatic detection and installation of rsvg-convert if missing
    - Works on macOS, Linux (Ubuntu/Debian, Fedora), and WSL
    - Configurable output dimensions
    - Batch conversion with pattern matching
    - Cross-platform compatibility

Usage:
    python3 convert-svg-to-png.py [width] [height] [pattern]

Examples:
    # Convert all featured SVG files to 1200x630px (default)
    python3 convert-svg-to-png.py

    # Convert to custom dimensions
    python3 convert-svg-to-png.py 1920 1080

    # Convert specific pattern
    python3 convert-svg-to-png.py 1200 630 "*featured*.svg"
"""

import os
import sys
import subprocess
import glob
import platform
from pathlib import Path


class Colors:
    """ANSI color codes for terminal output"""
    GREEN = '\033[0;32m'
    BLUE = '\033[0;34m'
    RED = '\033[0;31m'
    YELLOW = '\033[1;33m'
    RESET = '\033[0m'


def print_header():
    """Print script header"""
    print(f"{Colors.BLUE}{'='*56}{Colors.RESET}")
    print(f"{Colors.BLUE}SVG to PNG Converter (librsvg){Colors.RESET}")
    print(f"{Colors.BLUE}{'='*56}{Colors.RESET}")
    print()


def print_config(width, height, pattern, directory):
    """Print configuration"""
    print("Configuration:")
    print(f"  Output dimensions: {width}x{height}px")
    print(f"  File pattern: {pattern}")
    print(f"  Directory: {directory}")
    print()


def detect_os():
    """Detect operating system and return identifier"""
    system = platform.system()

    if system == 'Darwin':
        return 'macos'
    elif system == 'Linux':
        # Check if running in WSL
        try:
            with open('/proc/version', 'r') as f:
                if 'microsoft' in f.read().lower():
                    return 'wsl'
        except:
            pass

        # Try to detect Linux distribution
        try:
            with open('/etc/os-release', 'r') as f:
                for line in f:
                    if line.startswith('ID='):
                        distro = line.split('=')[1].strip().strip('"')
                        if distro in ['ubuntu', 'debian']:
                            return distro
                        elif distro in ['fedora', 'rhel', 'centos']:
                            return 'fedora'
        except:
            pass

        return 'linux'
    elif system == 'Windows':
        return 'windows'
    else:
        return 'unknown'


def install_rsvg_convert():
    """Attempt to automatically install rsvg-convert based on OS"""
    os_type = detect_os()

    print(f"{Colors.YELLOW}⚠ rsvg-convert not found. Installing...{Colors.RESET}")
    print()

    try:
        if os_type == 'macos':
            print("Detected: macOS")
            print("Installing librsvg via Homebrew...")

            # Check if Homebrew is installed
            try:
                subprocess.run(['brew', '--version'], capture_output=True, check=True, timeout=5)
            except:
                print(f"{Colors.RED}✗ Homebrew not found{Colors.RESET}")
                print()
                print("Please install Homebrew first:")
                print("  /bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"")
                print()
                print("Then run this script again.")
                return False

            result = subprocess.run(['brew', 'install', 'librsvg'], timeout=300)
            if result.returncode == 0:
                print(f"{Colors.GREEN}✓ librsvg installed successfully{Colors.RESET}")
                return True
            else:
                print(f"{Colors.RED}✗ Failed to install librsvg{Colors.RESET}")
                return False

        elif os_type in ['ubuntu', 'debian', 'wsl']:
            if os_type == 'wsl':
                print("Detected: Windows Subsystem for Linux (WSL)")
            else:
                print(f"Detected: {os_type.capitalize()}")
            print("Installing librsvg2-bin via apt...")

            # Update package list
            result1 = subprocess.run(['sudo', 'apt-get', 'update'], timeout=60)
            result2 = subprocess.run(['sudo', 'apt-get', 'install', '-y', 'librsvg2-bin'], timeout=300)

            if result1.returncode == 0 and result2.returncode == 0:
                print(f"{Colors.GREEN}✓ librsvg2-bin installed successfully{Colors.RESET}")
                return True
            else:
                print(f"{Colors.RED}✗ Failed to install librsvg2-bin{Colors.RESET}")
                return False

        elif os_type == 'fedora':
            print("Detected: Fedora/RHEL/CentOS")
            print("Installing librsvg2-tools via dnf...")

            result = subprocess.run(['sudo', 'dnf', 'install', '-y', 'librsvg2-tools'], timeout=300)
            if result.returncode == 0:
                print(f"{Colors.GREEN}✓ librsvg2-tools installed successfully{Colors.RESET}")
                return True
            else:
                print(f"{Colors.RED}✗ Failed to install librsvg2-tools{Colors.RESET}")
                return False

        else:
            print(f"{Colors.RED}✗ Unknown OS: {os_type}{Colors.RESET}")
            print()
            print("Please install librsvg manually:")
            print("  macOS:  brew install librsvg")
            print("  Ubuntu/Debian:  sudo apt-get install librsvg2-bin")
            print("  Fedora:  sudo dnf install librsvg2-tools")
            print("  Other:  Install librsvg from your package manager")
            print()
            return False

    except subprocess.TimeoutExpired:
        print(f"{Colors.RED}✗ Installation timeout{Colors.RESET}")
        return False
    except Exception as e:
        print(f"{Colors.RED}✗ Installation error: {e}{Colors.RESET}")
        return False


def check_rsvg_convert():
    """Check if rsvg-convert is installed, attempt to install if not"""
    try:
        result = subprocess.run(
            ['rsvg-convert', '--version'],
            capture_output=True,
            text=True,
            timeout=5
        )
        return True, result.stdout.strip()
    except (FileNotFoundError, subprocess.TimeoutExpired):
        # Try to install
        print()
        if install_rsvg_convert():
            # Verify installation
            try:
                result = subprocess.run(
                    ['rsvg-convert', '--version'],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                return True, result.stdout.strip()
            except:
                return False, "rsvg-convert still not found after installation"
        else:
            return False, None


def convert_svg_to_png(svg_file, png_file, width, height):
    """
    Convert SVG to PNG using rsvg-convert.

    Args:
        svg_file: Path to input SVG file
        png_file: Path to output PNG file
        width: Output width in pixels
        height: Output height in pixels

    Returns:
        Tuple of (success: bool, file_size: str or error_msg: str)
    """
    try:
        # Use rsvg-convert with explicit dimensions
        # This is the CORRECT method for consistent rendering
        subprocess.run(
            ['rsvg-convert', '-w', str(width), '-h', str(height),
             svg_file, '-o', png_file],
            capture_output=True,
            text=True,
            timeout=30,
            check=True
        )

        # Get file size
        size_bytes = os.path.getsize(png_file)
        if size_bytes < 1024:
            size_str = f"{size_bytes}B"
        elif size_bytes < 1024 * 1024:
            size_str = f"{size_bytes // 1024}K"
        else:
            size_str = f"{size_bytes // (1024*1024)}M"

        return True, size_str

    except subprocess.CalledProcessError as e:
        return False, f"Conversion error: {e.stderr}"
    except subprocess.TimeoutExpired:
        return False, "Conversion timeout (>30s)"
    except Exception as e:
        return False, str(e)


def main():
    """Main function"""
    # Parse arguments
    width = int(sys.argv[1]) if len(sys.argv) > 1 else 1200
    height = int(sys.argv[2]) if len(sys.argv) > 2 else 630
    pattern = sys.argv[3] if len(sys.argv) > 3 else "*featured-contract-first.svg"

    # Print header
    print_header()

    # Get current directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    # Print config
    print_config(width, height, pattern, script_dir)

    # Check rsvg-convert
    has_rsvg, version = check_rsvg_convert()
    if not has_rsvg:
        print(f"{Colors.RED}✗ Error: rsvg-convert not found{Colors.RESET}")
        print()
        print("Installation:")
        print("  macOS:  brew install librsvg")
        print("  Linux:  sudo apt-get install librsvg2-bin  # Ubuntu/Debian")
        print("          sudo dnf install librsvg2-tools    # Fedora")
        print()
        sys.exit(1)

    print(f"Tool: {version}")
    print(f"{Colors.BLUE}{'='*56}{Colors.RESET}")
    print()

    # Find SVG files
    svg_files = glob.glob(pattern)
    if not svg_files:
        print(f"{Colors.RED}✗ No SVG files matching pattern: {pattern}{Colors.RESET}")
        sys.exit(1)

    print(f"Found {len(svg_files)} SVG file(s)")
    print()

    # Convert files
    success_count = 0
    failed_count = 0
    failed_files = []

    for svg_file in sorted(svg_files):
        if os.path.isfile(svg_file):
            png_file = svg_file.replace('.svg', '.png')

            # Show progress
            status_prefix = f"Converting: {svg_file:<45}"
            sys.stdout.write(status_prefix)
            sys.stdout.flush()

            # Convert
            success, result = convert_svg_to_png(svg_file, png_file, width, height)

            if success:
                print(f"{Colors.GREEN}✓{Colors.RESET} ({result})")
                success_count += 1
            else:
                print(f"{Colors.RED}✗{Colors.RESET} {result}")
                failed_count += 1
                failed_files.append(svg_file)

    # Print summary
    print()
    print(f"{Colors.BLUE}{'='*56}{Colors.RESET}")
    print(f"{Colors.GREEN}Conversion Summary{Colors.RESET}")
    print(f"  Successful: {success_count}")
    if failed_count > 0:
        print(f"  {Colors.RED}Failed: {failed_count}{Colors.RESET}")
        print(f"{Colors.RED}Failed files:{Colors.RESET}")
        for f in failed_files:
            print(f"    - {f}")
    else:
        print(f"  {Colors.GREEN}All files converted successfully!{Colors.RESET}")
    print(f"{Colors.BLUE}{'='*56}{Colors.RESET}")

    sys.exit(failed_count)


if __name__ == '__main__':
    main()
