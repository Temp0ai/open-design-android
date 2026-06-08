# Web Prototype Skill

## Skill Metadata
- **Mode**: prototype
- **Platform**: web
- **Scenario**: design
- **Fidelity**: high
- **Preview Type**: iframe

## Description

Creates responsive, single-page web prototypes with modern HTML/CSS/JavaScript. These prototypes are complete, self-contained artifacts that render in a sandboxed iframe.

## Capabilities

- Responsive layouts (mobile-first)
- Interactive components
- Modern CSS (Grid, Flexbox, Custom Properties)
- Smooth animations and transitions
- Form validation
- Navigation patterns
- Data visualization

## Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prototype</title>
    <style>
        /* Design system tokens */
        :root {
            --color-primary: #6C5CE7;
            --color-background: #FAFAFA;
            --color-text: #1A1A1A;
            --font-family: -apple-system, sans-serif;
            --spacing-base: 16px;
            --radius-base: 8px;
        }
        
        /* Reset */
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        /* Base styles */
        body {
            font-family: var(--font-family);
            background: var(--color-background);
            color: var(--color-text);
        }
    </style>
</head>
<body>
    <!-- Content -->
</body>
</html>
```

## Prompt Examples

- "A SaaS landing page with hero, features, pricing, testimonials"
- "An admin dashboard with sidebar, charts, and data tables"
- "A mobile onboarding flow with 3 steps"
- "An e-commerce product page with gallery and reviews"

## Export Formats

- **HTML**: Self-contained single file
- **PDF**: Print-optimized
- **ZIP**: Source files with assets
