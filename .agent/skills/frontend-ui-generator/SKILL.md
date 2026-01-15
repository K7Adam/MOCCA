---
name: frontend-ui-generator
description: AI-powered React/Tailwind component generation for pages, dashboards, cards, and styling. Use for creating new UI components, redesigning existing pages, or generating landing pages.
---

# Frontend UI Generator Skill

**Gemini is your frontend developer.** For all UI/design work, use the Gemini Design MCP tools.

## Available Tools

The `gemini-design-mcp` MCP server provides these tools:

| Tool | Purpose |
|------|---------|
| `snippet_frontend` | Create a NEW visual component (popup, card, section, etc.) |
| `create_frontend` | Create a NEW complete page or feature |
| `modify_frontend` | REDESIGN or RESTYLE an existing element |

## Decision Flow

Before writing any UI code, ask yourself:

1. **Is it a NEW visual component?** (popup, card, section, etc.)
   - Use `snippet_frontend` or `create_frontend`

2. **Is it a REDESIGN of an existing element?**
   - Use `modify_frontend`, NOT snippet_frontend

3. **Is it just text/logic, or a trivial change?**
   - Do it yourself without Gemini

## Critical Rules

1. **If UI already exists and you need to redesign/restyle it** -> use `modify_frontend`, NOT snippet_frontend
2. **Tasks can be mixed** (logic + UI). Mentally separate them:
   - Do the logic yourself
   - Delegate the UI to Gemini

## Best Practices

### Be Specific About What You Want

**Avoid**: "Make a card"
**Better**: "Make a pricing card with 3 tiers, dark theme, highlighted Pro option"

### Mention Existing Styles to Match

**Avoid**: "Add a sidebar"
**Better**: "Add a sidebar that matches the header style"

### Iterate in Small Steps

**Avoid**: "Create a full dashboard with everything"
**Better**: "Create the stats section first, then we'll add charts"

### Reference Real Products for Style

**Avoid**: "Make it look good"
**Better**: "Make it look like Stripe's dashboard"

## Example Prompts

### Creating New Components
- "Create a pricing page with 3 tiers"
- "Build me a dashboard with charts and stats"
- "Make a landing page for my SaaS"
- "Create a settings page with tabs"
- "Build a login page with social auth buttons"

### Redesigning Existing UI
- "Make this header look more modern"
- "Redesign this card to be more premium"
- "This table looks ugly, fix it"
- "Make the sidebar look like Stripe's"
- "This form looks dated, refresh it"

### Adding Components
- "Add a search bar to the header"
- "Add a notification dropdown"
- "Add a dark mode toggle"
- "Add a user avatar menu"
- "Add a stats section above the table"

### Style & Polish
- "Make everything more compact"
- "Use a darker color scheme"
- "Add more spacing between sections"
- "Make buttons more rounded"
- "Add subtle animations"

## Tech Stack Support

Primarily supports:
- React + Tailwind CSS (primary)
- Vue
- Vanilla HTML/CSS
- Other frameworks

## Workflow Integration

1. **Analyze the request** - Determine if it's UI work that needs Gemini
2. **Select the right tool** - Based on new vs existing UI
3. **Provide context** - Existing styles, project patterns, specific requirements
4. **Iterate** - Ask for changes if the result isn't quite right

## Notes

- Gemini analyzes your existing styles and generates matching code
- Generated code is production-ready React/Tailwind
- Results are saved directly to your project files
- Free tier: 10K tokens/month (~6-10 component generations)
