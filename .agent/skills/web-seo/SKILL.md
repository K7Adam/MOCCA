---
name: web-seo
description: Meta tags, structured data, Core Web Vitals, Open Graph, accessibility for SEO. Use for SEO audits, improving search rankings, or implementing technical SEO.
---

# Web SEO Optimization Skill

Implement comprehensive SEO optimizations for web pages and applications.

## When to Use

- Auditing website SEO
- Improving search rankings
- Implementing structured data
- Optimizing Core Web Vitals
- Improving accessibility for SEO impact

## Optimization Areas

### 1. Technical SEO

- [ ] **Meta Tags**: Add/optimize titles and descriptions
- [ ] **Structured Data**: Implement JSON-LD schema markup
- [ ] **Robots & Sitemap**: Create/update robots.txt and sitemap.xml
- [ ] **Open Graph**: Add OG and Twitter Card meta tags
- [ ] **Canonical URLs**: Optimize canonical URL structure
- [ ] **Heading Hierarchy**: Proper H1-H6 structure

### 2. Performance SEO (Core Web Vitals)

- [ ] **LCP** (Largest Contentful Paint): < 2.5s
- [ ] **FID** (First Input Delay): < 100ms
- [ ] **CLS** (Cumulative Layout Shift): < 0.1
- [ ] **Image Optimization**: Lazy loading, WebP format
- [ ] **JavaScript/CSS**: Minimize and defer
- [ ] **Compression**: Enable gzip/brotli
- [ ] **Caching**: Proper cache headers

### 3. Content SEO

- [ ] **Keywords**: Optimize content for target keywords
- [ ] **Internal Linking**: Improve link structure
- [ ] **Alt Text**: Add to all images
- [ ] **Mobile Responsive**: Ensure mobile-first design
- [ ] **URL Structure**: Clean, descriptive URLs

### 4. Accessibility (SEO Impact)

- [ ] **WCAG 2.1 AA**: Compliance
- [ ] **Semantic HTML**: Use proper elements
- [ ] **ARIA Labels**: Where needed
- [ ] **Keyboard Navigation**: Full support

## Implementation Examples

### Meta Tags
```html
<head>
  <title>Page Title - Brand Name</title>
  <meta name="description" content="Clear, compelling 150-160 char description">
  <link rel="canonical" href="https://example.com/page">
</head>
```

### Open Graph
```html
<meta property="og:title" content="Page Title">
<meta property="og:description" content="Description for social shares">
<meta property="og:image" content="https://example.com/image.jpg">
<meta property="og:url" content="https://example.com/page">
<meta property="og:type" content="website">
```

### Twitter Cards
```html
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Page Title">
<meta name="twitter:description" content="Description">
<meta name="twitter:image" content="https://example.com/image.jpg">
```

### Structured Data (JSON-LD)
```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "Company Name",
  "url": "https://example.com",
  "logo": "https://example.com/logo.png",
  "contactPoint": {
    "@type": "ContactPoint",
    "telephone": "+1-555-555-5555",
    "contactType": "customer service"
  }
}
</script>
```

### Robots.txt
```
User-agent: *
Allow: /
Disallow: /admin/
Disallow: /api/

Sitemap: https://example.com/sitemap.xml
```

### Sitemap.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://example.com/</loc>
    <lastmod>2024-01-01</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
</urlset>
```

## Audit Checklist

### Technical
- [ ] Each page has unique title (50-60 chars)
- [ ] Each page has unique meta description (150-160 chars)
- [ ] Proper heading hierarchy (one H1 per page)
- [ ] All images have alt text
- [ ] Canonical URLs set correctly
- [ ] Structured data validates in Google Rich Results Test

### Performance
- [ ] Page loads in < 3 seconds
- [ ] Core Web Vitals pass thresholds
- [ ] Images optimized (WebP, lazy loading)
- [ ] CSS/JS minimized

### Mobile
- [ ] Mobile-friendly (Google Mobile Test)
- [ ] Touch targets 44px minimum
- [ ] No horizontal scroll

## Tools for Validation

- [Google Search Console](https://search.google.com/search-console)
- [Google PageSpeed Insights](https://pagespeed.web.dev/)
- [Google Rich Results Test](https://search.google.com/test/rich-results)
- [Schema.org Validator](https://validator.schema.org/)
- [Mobile-Friendly Test](https://search.google.com/test/mobile-friendly)
