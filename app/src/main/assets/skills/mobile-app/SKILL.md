# Mobile App Skill

## Skill Metadata
- **Mode**: prototype
- **Platform**: mobile
- **Scenario**: design
- **Fidelity**: high
- **Preview Type**: device-frame

## Description

Creates mobile app prototypes with realistic device frames and native-feeling interactions. Supports iOS and Android design patterns.

## Capabilities

- Device-specific chrome (iPhone, Android)
- Native navigation patterns
- Touch-optimized interactions
- Status bar simulation
- Safe area handling
- Platform-specific typography

## Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mobile Prototype</title>
    <style>
        :root {
            --device-width: 390px;
            --device-height: 844px;
            --safe-top: 59px;
            --safe-bottom: 34px;
            --nav-height: 44px;
        }
        
        .device-frame {
            width: var(--device-width);
            height: var(--device-height);
            border-radius: 44px;
            border: 12px solid #1a1a1a;
            overflow: hidden;
            background: white;
            position: relative;
        }
        
        .status-bar {
            height: var(--safe-top);
            display: flex;
            justify-content: space-between;
            padding: 16px 24px 0;
            font-size: 14px;
            font-weight: 600;
        }
        
        .content {
            height: calc(100% - var(--safe-top) - var(--safe-bottom));
            overflow-y: auto;
        }
        
        .tab-bar {
            height: var(--safe-bottom);
            display: flex;
            justify-content: space-around;
            align-items: center;
            border-top: 1px solid #e8e8e8;
        }
    </style>
</head>
<body>
    <div class="device-frame">
        <div class="status-bar">
            <span>9:41</span>
            <span>●●● WiFi 🔋</span>
        </div>
        <div class="content">
            <!-- App content -->
        </div>
        <div class="tab-bar">
            <!-- Tab items -->
        </div>
    </div>
</body>
</html>
```

## Prompt Examples

- "A fitness tracking app with daily stats and workout history"
- "A food delivery app with restaurant listing and cart"
- "A social media app with feed, stories, and messaging"
- "A finance app with account overview and transactions"

## Export Formats

- **HTML**: Self-contained with device frame
- **PNG**: High-resolution screenshot
- **GIF**: Animated prototype walkthrough
