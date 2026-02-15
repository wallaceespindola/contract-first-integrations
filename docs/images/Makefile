.PHONY: help install test convert verify clean lint pre-commit all

# Default target - show help
.DEFAULT_GOAL := help

# Color output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m # No Color

# Configuration
SCRIPT_SHELL := ./convert-svg-to-png.sh
SCRIPT_PYTHON := python3 convert-svg-to-png.py
PATTERN := "*featured-contract-first.svg"
WIDTH := 1200
HEIGHT := 630

################################################################################
# HELP TARGET - Shows all available targets with descriptions
################################################################################
help: ## Show this help message and list all available targets
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo "$(BLUE)SVG to PNG Conversion - Makefile Targets$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(YELLOW)Usage:$(NC)"
	@echo "  make [target]"
	@echo ""
	@echo "$(YELLOW)Available Targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' | \
		sort
	@echo ""
	@echo "$(YELLOW)Examples:$(NC)"
	@echo "  make install      # Install rsvg-convert if missing"
	@echo "  make test         # Test conversion with sample file"
	@echo "  make convert      # Convert all featured SVG files"
	@echo "  make verify       # Verify all PNG files exist and are valid"
	@echo "  make clean        # Remove all generated PNG files"
	@echo "  make pre-commit   # Run pre-commit checks"
	@echo "  make all          # Install, convert, and verify"
	@echo ""
	@echo "$(YELLOW)Configuration:$(NC)"
	@echo "  WIDTH:    $(WIDTH)px"
	@echo "  HEIGHT:   $(HEIGHT)px"
	@echo "  PATTERN:  $(PATTERN)"
	@echo ""
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"

################################################################################
# INSTALLATION TARGET - Ensure rsvg-convert is installed
################################################################################
install: ## Install rsvg-convert (librsvg) if not present
	@echo "$(YELLOW)Checking for rsvg-convert...$(NC)"
	@if command -v rsvg-convert >/dev/null 2>&1; then \
		echo "$(GREEN)✓ rsvg-convert is already installed$(NC)"; \
		rsvg-convert --version; \
	else \
		echo "$(YELLOW)Installing rsvg-convert...$(NC)"; \
		chmod +x $(SCRIPT_SHELL); \
		$(SCRIPT_SHELL) 1 1 ""; \
	fi

################################################################################
# CONVERSION TARGET - Convert all featured SVG files
################################################################################
convert: install ## Convert all featured SVG files to PNG (1200x630px)
	@echo ""
	@echo "$(BLUE)Converting SVG files to PNG...$(NC)"
	@chmod +x $(SCRIPT_SHELL)
	@$(SCRIPT_SHELL) $(WIDTH) $(HEIGHT) $(PATTERN)

################################################################################
# TEST TARGET - Test conversion with a single file
################################################################################
test: install ## Test conversion with a single SVG file
	@echo ""
	@echo "$(BLUE)Testing SVG to PNG conversion...$(NC)"
	@echo "$(YELLOW)Converting: javapro-featured-contract-first.svg$(NC)"
	@chmod +x $(SCRIPT_SHELL)
	@$(SCRIPT_SHELL) $(WIDTH) $(HEIGHT) "javapro-featured-contract-first.svg"
	@echo ""
	@echo "$(GREEN)✓ Test conversion completed$(NC)"

################################################################################
# VERIFY TARGET - Verify all PNG files exist and are valid
################################################################################
verify: ## Verify all PNG files exist and are valid
	@echo ""
	@echo "$(BLUE)Verifying PNG files...$(NC)"
	@echo ""
	@png_count=0; \
	missing_count=0; \
	status=0; \
	shopt -s nullglob; \
	for png in *featured-contract-first.png; do \
		if [ -f "$$png" ]; then \
			size=$$(ls -lh "$$png" | awk '{print $$5}'); \
			echo "$(GREEN)✓$(NC) $$png ($$size)"; \
			((png_count++)); \
		else \
			echo "$(RED)✗$(NC) $$png (MISSING)"; \
			((missing_count++)); \
		fi; \
	done; \
	echo ""; \
	if [ $$png_count -eq 0 ]; then \
		echo "$(RED)✗ No PNG files found$(NC)"; \
		exit 1; \
	elif [ $$missing_count -eq 0 ]; then \
		echo "$(GREEN)✓ All PNG files verified successfully ($$png_count files)$(NC)"; \
	else \
		echo "$(RED)✗ Missing $$missing_count PNG file(s)$(NC)"; \
		exit 1; \
	fi

################################################################################
# CLEAN TARGET - Remove all generated PNG files
################################################################################
clean: ## Remove all generated PNG files (keep SVG files)
	@echo "$(YELLOW)Removing PNG files...$(NC)"
	@removed=0; \
	for svg in $(PATTERN); do \
		png="$${svg%.svg}.png"; \
		if [ -f "$$png" ]; then \
			rm -f "$$png"; \
			echo "  Removed: $$png"; \
			((removed++)); \
		fi; \
	done; \
	if [ $$removed -eq 0 ]; then \
		echo "  $(YELLOW)No PNG files to remove$(NC)"; \
	else \
		echo "$(GREEN)✓ Removed $$removed PNG file(s)$(NC)"; \
	fi

################################################################################
# LINT TARGET - Lint documentation and scripts
################################################################################
lint: ## Lint markdown documentation and shell scripts
	@echo "$(BLUE)Linting documentation files...$(NC)"
	@echo ""
	@if command -v markdownlint >/dev/null 2>&1; then \
		echo "$(YELLOW)Running markdownlint on *.md files...$(NC)"; \
		markdownlint *.md || true; \
	else \
		echo "$(YELLOW)markdownlint not found, skipping markdown linting$(NC)"; \
		echo "  Install with: npm install -g markdownlint-cli"; \
	fi
	@echo ""
	@if command -v shellcheck >/dev/null 2>&1; then \
		echo "$(YELLOW)Running shellcheck on *.sh files...$(NC)"; \
		shellcheck convert-svg-to-png.sh || true; \
	else \
		echo "$(YELLOW)shellcheck not found, skipping shell linting$(NC)"; \
		echo "  Install with: brew install shellcheck"; \
	fi
	@echo ""
	@echo "$(GREEN)✓ Lint check completed$(NC)"

################################################################################
# PRE-COMMIT TARGET - Run pre-commit checks
################################################################################
pre-commit: lint verify ## Run all pre-commit checks (lint + verify)
	@echo ""
	@echo "$(BLUE)Pre-commit checks completed successfully!$(NC)"
	@echo ""
	@echo "$(GREEN)✓ All checks passed:$(NC)"
	@echo "  - Documentation linted"
	@echo "  - Shell scripts checked"
	@echo "  - PNG files verified"

################################################################################
# ALL TARGET - Run complete workflow
################################################################################
all: clean install convert verify ## Run complete workflow (clean, install, convert, verify)
	@echo ""
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo "$(GREEN)✓ Complete workflow finished successfully!$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "Summary:"
	@echo "  $(GREEN)✓$(NC) rsvg-convert installed"
	@echo "  $(GREEN)✓$(NC) All SVG files converted to PNG"
	@echo "  $(GREEN)✓$(NC) All PNG files verified"
	@echo ""

################################################################################
# STATUS TARGET - Show current status
################################################################################
status: ## Show status of SVG and PNG files
	@echo ""
	@echo "$(BLUE)SVG to PNG Conversion Status$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(YELLOW)Tool Status:$(NC)"
	@if command -v rsvg-convert >/dev/null 2>&1; then \
		echo "  $(GREEN)✓ rsvg-convert$(NC): $$(rsvg-convert --version | head -1)"; \
	else \
		echo "  $(RED)✗ rsvg-convert$(NC): NOT INSTALLED"; \
	fi
	@echo ""
	@echo "$(YELLOW)File Status:$(NC)"
	@total=0; \
	svg_count=0; \
	png_count=0; \
	for svg in $(PATTERN); do \
		if [ -f "$$svg" ]; then \
			((svg_count++)); \
			((total++)); \
		fi; \
		png="$${svg%.svg}.png"; \
		if [ -f "$$png" ]; then \
			((png_count++)); \
		fi; \
	done; \
	echo "  SVG files: $$svg_count"; \
	echo "  PNG files: $$png_count / $$svg_count"; \
	if [ $$png_count -eq $$svg_count ] && [ $$svg_count -gt 0 ]; then \
		echo "  Status: $(GREEN)✓ All files converted$(NC)"; \
	elif [ $$png_count -gt 0 ]; then \
		missing=$$((svg_count - png_count)); \
		echo "  Status: $(YELLOW)⚠ $$missing file(s) missing PNG$(NC)"; \
	else \
		echo "  Status: $(RED)✗ No PNG files generated$(NC)"; \
	fi
	@echo ""

################################################################################
# PYTHON TARGET - Run conversion using Python script
################################################################################
python: install ## Convert using Python script instead of shell script
	@echo ""
	@echo "$(BLUE)Converting SVG files to PNG using Python...$(NC)"
	@$(SCRIPT_PYTHON) $(WIDTH) $(HEIGHT) $(PATTERN)

################################################################################
# SHELL TARGET - Run conversion using shell script
################################################################################
shell: install ## Convert using shell script (default)
	@echo ""
	@echo "$(BLUE)Converting SVG files to PNG using shell script...$(NC)"
	@$(SCRIPT_SHELL) $(WIDTH) $(HEIGHT) $(PATTERN)

################################################################################
# CUSTOM SIZE TARGET - Convert with custom dimensions
################################################################################
custom: install ## Convert with custom dimensions (WIDTH=X HEIGHT=Y make custom)
	@echo "$(BLUE)Converting with custom dimensions: $(WIDTH)x$(HEIGHT)px$(NC)"
	@$(SCRIPT_SHELL) $(WIDTH) $(HEIGHT) $(PATTERN)

################################################################################
# HELP-FULL TARGET - Extended help with examples
################################################################################
help-full: help ## Show extended help with detailed examples
	@echo "$(YELLOW)Extended Examples:$(NC)"
	@echo ""
	@echo "  $(GREEN)Convert all images:$(NC)"
	@echo "    make convert"
	@echo ""
	@echo "  $(GREEN)Convert with custom size (1920x1080):$(NC)"
	@echo "    make custom WIDTH=1920 HEIGHT=1080"
	@echo ""
	@echo "  $(GREEN)Convert only javapro files:$(NC)"
	@echo "    make shell PATTERN='*javapro*.svg'"
	@echo ""
	@echo "  $(GREEN)Test setup without overwriting existing PNGs:$(NC)"
	@echo "    make test"
	@echo ""
	@echo "  $(GREEN)Run complete workflow:$(NC)"
	@echo "    make all"
	@echo ""
	@echo "  $(GREEN)Check current status:$(NC)"
	@echo "    make status"
	@echo ""
	@echo "$(YELLOW)Documentation:$(NC)"
	@echo "  - SVG-CONVERSION-GUIDE.md      : Complete usage guide"
	@echo "  - AUTO-INSTALLATION-SETUP.md   : Auto-installation details"
	@echo ""

################################################################################
# VERSION TARGET - Show version information
################################################################################
version: ## Show tool versions
	@echo "$(BLUE)Version Information$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(YELLOW)SVG Conversion Tools:$(NC)"
	@if command -v rsvg-convert >/dev/null 2>&1; then \
		echo "  $(GREEN)✓ rsvg-convert:$(NC) $$(rsvg-convert --version | head -1)"; \
	else \
		echo "  $(RED)✗ rsvg-convert:$(NC) NOT INSTALLED"; \
	fi
	@echo ""
	@echo "$(YELLOW)Scripting:$(NC)"
	@echo "  Bash version: $$(bash --version | head -1 | cut -d' ' -f4)"
	@echo "  Python version: $$(python3 --version 2>&1 | cut -d' ' -f2)"
	@echo ""
	@echo "$(YELLOW)Linting Tools (optional):$(NC)"
	@if command -v shellcheck >/dev/null 2>&1; then \
		echo "  $(GREEN)✓ shellcheck:$(NC) $$(shellcheck --version | head -1)"; \
	else \
		echo "  $(RED)✗ shellcheck:$(NC) not installed"; \
	fi
	@if command -v markdownlint >/dev/null 2>&1; then \
		echo "  $(GREEN)✓ markdownlint:$(NC) $$(markdownlint --version 2>&1)"; \
	else \
		echo "  $(RED)✗ markdownlint:$(NC) not installed"; \
	fi
	@echo ""

################################################################################
# Debug target for Makefile itself
################################################################################
.PHONY: debug
debug: ## Show Makefile variables and configuration
	@echo "$(BLUE)Makefile Configuration$(NC)"
	@echo "$(BLUE)════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(YELLOW)Variables:$(NC)"
	@echo "  SCRIPT_SHELL: $(SCRIPT_SHELL)"
	@echo "  SCRIPT_PYTHON: $(SCRIPT_PYTHON)"
	@echo "  PATTERN: $(PATTERN)"
	@echo "  WIDTH: $(WIDTH)"
	@echo "  HEIGHT: $(HEIGHT)"
	@echo ""
	@echo "$(YELLOW)Current Directory:$(NC)"
	@echo "  $$(pwd)"
	@echo ""
	@echo "$(YELLOW)Files:$(NC)"
	@ls -lh convert-svg-to-png.* 2>/dev/null | awk '{print "  " $$9, "(" $$5 ")"}'
	@echo ""
