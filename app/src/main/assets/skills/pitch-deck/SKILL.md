# Pitch Deck Skill

## Skill Metadata
- **Mode**: deck
- **Platform**: web
- **Scenario**: marketing
- **Fidelity**: high
- **Preview Type**: slides

## Description

Creates presentation-style pitch decks with slide navigation, animations, and export to PDF/PPTX. Designed for startup pitches, product launches, and business presentations.

## Capabilities

- Multi-slide layouts
- Keyboard navigation
- Slide transitions
- Data visualization
- Speaker notes
- Export to PDF/PPTX

## Slide Types

1. **Title Slide** — Logo, tagline, date
2. **Problem** — Pain point visualization
3. **Solution** — Value proposition
4. **Market** — TAM/SAM/SOM
5. **Product** — Features and screenshots
6. **Traction** — Metrics and growth
7. **Business Model** — Revenue streams
8. **Competition** — Positioning matrix
9. **Team** — Founders and advisors
10. **Ask** — Funding request
11. **Contact** — Email, website, social

## Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pitch Deck</title>
    <style>
        :root {
            --slide-width: 960px;
            --slide-height: 540px;
            --primary: #6C5CE7;
            --text: #1a1a1a;
            --background: #ffffff;
        }
        
        .deck {
            width: var(--slide-width);
            height: var(--slide-height);
            margin: 0 auto;
            position: relative;
            overflow: hidden;
        }
        
        .slide {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0;
            left: 0;
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 60px 80px;
            opacity: 0;
            transform: translateX(100px);
            transition: all 0.5s ease;
        }
        
        .slide.active {
            opacity: 1;
            transform: translateX(0);
        }
        
        .slide h1 {
            font-size: 48px;
            font-weight: 800;
            margin-bottom: 16px;
        }
        
        .slide h2 {
            font-size: 32px;
            font-weight: 600;
            margin-bottom: 24px;
        }
        
        .slide p {
            font-size: 20px;
            line-height: 1.6;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="deck">
        <div class="slide active"><!-- Title --></div>
        <div class="slide"><!-- Problem --></div>
        <div class="slide"><!-- Solution --></div>
        <!-- More slides -->
    </div>
    
    <script>
        // Keyboard navigation
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowRight') nextSlide();
            if (e.key === 'ArrowLeft') prevSlide();
        });
    </script>
</body>
</html>
```

## Prompt Examples

- "A startup pitch deck for an AI design tool"
- "A product launch deck for a new mobile app"
- "A quarterly business review deck"
- "A fundraising deck for Series A"

## Export Formats

- **HTML**: Interactive presentation
- **PDF**: Print-optimized
- **PPTX**: Microsoft PowerPoint
