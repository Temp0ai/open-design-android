# Dashboard Skill

## Skill Metadata
- **Mode**: prototype
- **Platform**: web
- **Scenario**: design
- **Fidelity**: high
- **Preview Type**: iframe

## Description

Creates data-rich dashboard prototypes with charts, metrics, tables, and interactive controls. Designed for analytics, monitoring, and business intelligence use cases.

## Capabilities

- Chart visualizations (bar, line, pie, area)
- KPI cards with sparklines
- Data tables with sorting/filtering
- Date range pickers
- Filter panels
- Responsive grid layouts

## Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard</title>
    <style>
        :root {
            --sidebar-width: 260px;
            --header-height: 64px;
            --card-radius: 8px;
            --card-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        
        .dashboard {
            display: grid;
            grid-template-columns: var(--sidebar-width) 1fr;
            grid-template-rows: var(--header-height) 1fr;
            height: 100vh;
        }
        
        .sidebar {
            grid-row: 1 / -1;
            background: #1a1a1a;
            color: white;
            padding: 24px 16px;
        }
        
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 24px;
            border-bottom: 1px solid #e8e8e8;
        }
        
        .main {
            padding: 24px;
            overflow-y: auto;
        }
        
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }
        
        .kpi-card {
            background: white;
            border-radius: var(--card-radius);
            padding: 20px;
            box-shadow: var(--card-shadow);
        }
        
        .chart-container {
            background: white;
            border-radius: var(--card-radius);
            padding: 24px;
            box-shadow: var(--card-shadow);
        }
    </style>
</head>
<body>
    <div class="dashboard">
        <aside class="sidebar"><!-- Navigation --></aside>
        <header class="header"><!-- Title, search, user --></header>
        <main class="main">
            <div class="kpi-grid"><!-- KPI cards --></div>
            <div class="chart-container"><!-- Charts --></div>
        </main>
    </div>
</body>
</html>
```

## Prompt Examples

- "An analytics dashboard with user metrics and revenue charts"
- "A project management dashboard with task boards and timelines"
- "A sales dashboard with pipeline and conversion metrics"
- "A system monitoring dashboard with server health and alerts"

## Export Formats

- **HTML**: Interactive self-contained dashboard
- **PDF**: Print-optimized report
- **PNG**: Dashboard screenshot
