# The Beginning of the End — interactive reader

A dependency-free static site that presents the manga pages in full quality with
scroll-driven animation.

## Run it

Any static server works — there is no build step:

```bash
cd manga
python3 -m http.server 8000
# open http://localhost:8000
```

It can be published as-is to GitHub Pages, Netlify, Vercel or any static host.

## Structure

```
manga/
├── index.html              page shell, hero, drawer, lightbox markup
├── assets/
│   ├── css/style.css       theme tokens, layout, scroll-reveal primitives
│   ├── js/pages.js         page manifest (order, titles, quotes, dimensions)
│   ├── js/app.js           reader engine — no dependencies
│   ├── pages/*.webp        full-resolution pages (WebP q94, native size)
│   └── thumbs/*.webp       index thumbnails + blur-up placeholders
```

## Interactions

| Action | Where |
| --- | --- |
| Open a page full-screen | click/tap any page, or `#<id>!` in the URL |
| Zoom | scroll wheel, pinch, `+` / `-`, or the toolbar |
| Pan | drag while zoomed |
| Fit to screen | `0` or the **Fit** button |
| Change page | `←` / `→`, or the side arrows |
| Fullscreen | `F` or the ⤢ button |
| Close | `Esc` |
| Chapter index | `I` or the **Index** button |
| Jump between episodes | `Shift` + `↑` / `↓`, or the progress dots |

## Animation

Reveals are driven by `IntersectionObserver` (one class toggle per section, then
unobserved) and CSS transitions — each page enters with a combined
`clip-path` / scale / blur / rotateX move while its heading lines stagger in.
A single `requestAnimationFrame`-throttled scroll handler drives the progress bar,
hero parallax, per-page image parallax and active-chapter tracking. Odd and even
sections mirror their layout and entry direction so the scroll has a rhythm.

Everything collapses to static content under `prefers-reduced-motion: reduce`.

## Adding or reordering pages

Drop the image into `assets/pages/`, add a matching thumbnail in `assets/thumbs/`,
then add an entry to `window.MANGA_PAGES` in `assets/js/pages.js`. Array order is
reading order; `w`/`h` must be the real pixel dimensions (they reserve layout space
and seed the lightbox fit calculation).
