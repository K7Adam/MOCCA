---
name: reverse-api-engineer
description: Step-by-step workflow for reverse engineering web APIs. Captures browser traffic (HAR files) and generates production-ready API clients in Python, JavaScript, or TypeScript. Use when mentioning reverse engineer API, capture network traffic, HAR file analysis, or create API client from website.
---

# Reverse API Engineering Skill

This skill provides a complete workflow for reverse engineering web APIs by capturing browser traffic and generating production-ready API clients.

## Prerequisites

Before starting, ensure these tools are available:

```bash
# Check Python version (3.11+ required)
python --version

# Install reverse-api-engineer if not present
uv tool install reverse-api-engineer

# Install browser (one-time)
playwright install chromium
```

## Workflow Phases

### Phase 1: Preparation

**Determine the capture strategy:**

| Scenario | Recommended Mode | Command |
|----------|------------------|---------|
| Need manual login/CAPTCHA | Manual | `reverse-api-engineer` |
| Simple predictable navigation | Agent | `reverse-api-engineer agent "<task>"` |
| Already have HAR file | Engineer | `reverse-api-engineer engineer <path>` |
| Re-generate from previous run | Engineer | `reverse-api-engineer engineer <run_id>` |

**Ask clarifying questions:**
1. What data do you want to extract?
2. Does the site require authentication?
3. Are there CAPTCHAs or anti-bot measures?
4. What output language do you prefer? (Python/JavaScript/TypeScript)

### Phase 2: Browser Capture

**Option A: Manual Mode (Recommended for first capture)**

```bash
# Launch interactive CLI
reverse-api-engineer

# When prompted:
# 1. Enter task description (e.g., "fetch all job listings from Apple")
# 2. Enter starting URL (e.g., https://jobs.apple.com)
# 3. Browser opens - navigate and interact manually
# 4. Close browser when done capturing
# 5. AI automatically generates API client
```

**Option B: Agent Mode (Autonomous)**

```bash
# Fully automated capture
reverse-api-engineer agent "navigate to example.com and extract product data" --url https://example.com
```

### Phase 3: HAR Analysis

After capture, the HAR file is saved to:
```
~/.reverse-api/runs/har/{run_id}/recording.har
```

**Key analysis steps:**

1. **Filter noise** - Remove static assets, analytics, ads
2. **Identify endpoints** - Find `/api/`, `/v1/`, `/graphql` patterns
3. **Extract authentication** - Cookies, Bearer tokens, API keys
4. **Map request/response** - Parameters, body schemas, status codes

### Phase 4: Code Generation

The AI automatically generates:

**For Python:**
```
./scripts/{task_name}/
├── api_client.py    # requests-based client with type hints
└── README.md        # Usage documentation
```

**For JavaScript:**
```
./scripts/{task_name}/
├── api_client.js    # ESM module with fetch/axios
├── package.json     # Dependencies
└── README.md        # Usage documentation
```

**For TypeScript:**
```
./scripts/{task_name}/
├── api_client.ts    # Typed client with interfaces
├── package.json     # Dependencies + tsx
└── README.md        # Usage documentation
```

### Phase 5: Testing & Validation

**Test the generated client:**

```bash
# Python
cd ./scripts/{task_name}
python api_client.py

# JavaScript
cd ./scripts/{task_name}
npm install && node api_client.js

# TypeScript
cd ./scripts/{task_name}
npm install && npx tsx api_client.ts
```

**Validation checklist:**
- [ ] All discovered endpoints are implemented
- [ ] Authentication is properly handled
- [ ] Error handling is comprehensive
- [ ] Type hints/interfaces are accurate
- [ ] README has clear usage examples

## Authentication Patterns Reference

### Cookies / Session Tokens
```python
# Generated code handles this automatically
session = requests.Session()
# Cookies are persisted across requests
```

### Bearer Token / JWT
```python
headers = {
    "Authorization": f"Bearer {token}"
}
```

### API Key (Header)
```python
headers = {
    "X-API-Key": os.environ["API_KEY"]
}
```

### API Key (Query Parameter)
```python
params = {
    "api_key": os.environ["API_KEY"]
}
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Bot detection / 403 | Use manual mode with real browser, or add stealth headers |
| Rate limiting | Add delays between requests, check Retry-After header |
| Session expires | Implement token refresh in generated client |
| CORS errors | N/A for Python (server-side), check browser console for JS |
| Missing endpoints | Re-capture with more thorough navigation |
| Wrong base URL | Edit generated client to fix base URL |
