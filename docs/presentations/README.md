# Contract-First Integration Presentation

Professional presentation demonstrating contract-first development patterns using REST APIs, Kafka events, and database migrations.

## Contents

- **slides-content.md** - Source content in markdown format (40 slides)
- **contract-first-integration.pptx** - Generated PowerPoint presentation
- **generate_pptx.py** - Python script to regenerate PowerPoint
- **google-slides-script.gs** - Google Apps Script for Google Slides version

## Presentation Overview

**Title**: Contract-First Integration: Enabling Parallel Systems Development

**Author**: Wallace Espindola
**Topics Covered**:
- What is Contract-First development
- Problems with traditional integration
- Three contract types: REST (OpenAPI), Kafka (Avro), Database (Flyway)
- Parallel development patterns
- Real-world implementation examples
- CI/CD integration and enforcement
- Best practices and common pitfalls
- Success metrics and case studies

**Slides**: 40 total
- Title slide with author information
- Content slides with modern formatting
- Code examples with syntax highlighting
- Section dividers for major topics
- Resources and call-to-action

## Design Features

**Color Scheme**:
- Primary: Blue (#0066CC)
- Accent: Orange (#FF5722)
- Dark: Almost Black (#212121)
- Light: Off-White (#FAFAFA)
- Code Background: Dark (#282C34)

**Layout**:
- Professional title bars with gradient backgrounds
- Consistent footers with page numbers
- Code blocks with dark backgrounds and monospace fonts
- Bullet points with proper hierarchy
- Section dividers for topic transitions

## Using the PowerPoint Version

The PowerPoint file is ready to use:

```bash
# Open the presentation
open contract-first-integration.pptx
```

### Regenerating PowerPoint

If you need to regenerate from the markdown source:

```bash
# Install dependencies
pip install python-pptx

# Run the generator
python3 generate_pptx.py
```

This will create a new `contract-first-integration.pptx` file.

### Editing the Content

To modify the presentation content:

1. Edit `slides-content.md` with your changes
2. Run `python3 generate_pptx.py` to regenerate
3. Open the new .pptx file

**Markdown Format**:
- `---` separates slides
- `# Title` creates the main title slide
- `## Slide Title` creates content slides
- Bullet points with `-` or `•`
- Code blocks with ` ```language `
- Bold with `**text**`

## Using the Google Slides Version

To create a Google Slides version:

1. **Open Google Slides**: https://slides.google.com
2. **Create blank presentation**
3. **Open Script Editor**: Extensions → Apps Script
4. **Copy script**: Paste contents of `google-slides-script.gs`
5. **Save project**: Name it "Contract-First Integration"
6. **Run function**: Select `createPresentation` and click Run
7. **Authorize**: Grant permissions when prompted
8. **Wait**: Script will create all 40 slides
9. **Find presentation**: Check your Google Drive

The script will:
- Create a new presentation in your Google Drive
- Generate all 40 slides with proper formatting
- Apply the same color scheme as PowerPoint
- Format code blocks, bullets, and sections
- Add page numbers and branding

## Customization

### Changing Colors

**PowerPoint** - Edit `generate_pptx.py`:
```python
COLORS = {
    'primary': RGBColor(0, 102, 204),      # Change these RGB values
    'secondary': RGBColor(255, 87, 34),
    # ...
}
```

**Google Slides** - Edit `google-slides-script.gs`:
```javascript
const COLORS = {
  PRIMARY: '#0066CC',        // Change these hex codes
  SECONDARY: '#FF5722',
  // ...
};
```

### Adding Slides

Add content to `slides-content.md`:

```markdown
---

## Your New Slide Title

- Bullet point 1
- Bullet point 2

**Bold emphasis**

```java
// Code example
public class Example {
    // Your code here
}
```
```

Then regenerate using either method.

### Removing Slides

Delete the slide content between `---` separators in `slides-content.md` and regenerate.

## Presentation Tips

**Before Presenting**:
- Review all slides for accuracy
- Add speaker notes if needed
- Test animations and transitions
- Verify all code examples are readable
- Check timing (expect 45-60 minutes for full deck)

**Suggested Sections for Shorter Presentations**:
- **Quick Overview (15 min)**: Slides 1-10, 38-40 (problem, solution, key takeaways)
- **Technical Deep Dive (30 min)**: Slides 1-18, 27, 38-40 (contracts + implementation)
- **Business Value (20 min)**: Slides 1-6, 22-26, 36, 38-40 (benefits + metrics)

**Interactive Elements**:
- Live demo using the Spring Boot application in this repository
- Show actual contract files in `/contracts` directory
- Demonstrate Swagger UI at http://localhost:8080/swagger-ui.html
- Show Schema Registry with Avro schemas

## Source Material

Based on `/docs/context.md` which contains comprehensive documentation about:
- Contract-first development philosophy
- OpenAPI specifications for REST APIs
- Apache Avro schemas for Kafka events
- Flyway migrations for database contracts
- Java/Spring Boot implementation patterns
- Idempotency and schema evolution strategies

## Files Reference

| File | Purpose | Format |
|------|---------|--------|
| `slides-content.md` | Source content | Markdown |
| `contract-first-integration.pptx` | Final presentation | PowerPoint |
| `generate_pptx.py` | PowerPoint generator | Python |
| `google-slides-script.gs` | Google Slides creator | JavaScript |
| `README.md` | This file | Markdown |

## Author

**Wallace Espindola**
Senior Software Engineer

- Email: wallace.espindola@gmail.com
- LinkedIn: https://www.linkedin.com/in/wallaceespindola/
- GitHub: https://github.com/wallaceespindola

## License

Apache License 2.0 - See repository root LICENSE file

## Related Resources

**Repository**: https://github.com/wallaceespindola/contract-first-integrations

**Documentation**:
- `/docs/context.md` - Complete contract-first guide
- `/contracts/openapi/` - REST API contracts
- `/contracts/events/avro/` - Kafka event schemas
- `/contracts/db/flyway/` - Database migrations

**Implementation**:
- `/src/main/java/` - Spring Boot application
- `API_ENDPOINTS.md` - REST API reference
- `VALIDATION_RESULTS.md` - Test results and validation

---

**Last Updated**: 2026-02-07
**Presentation Version**: 1.0
**Total Slides**: 40
