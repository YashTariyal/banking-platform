## Exporting the LinkedIn images (SVG → PNG)

LinkedIn uploads **PNG/JPG** most reliably (SVG is often not accepted).

### Option A (fastest): Export via browser

1. Open one of these files in your browser:
   - `assets/linkedin-banking-platform-architecture.svg`
   - `assets/linkedin-banking-platform-tech-stack.svg`
2. Use **File → Print** (or **Save as PDF**) and then export to PNG, *or* take a high-res screenshot.

### Option B: Export via command line (if you have a converter installed)

If you have one of these tools installed locally, you can export clean PNGs:

- **librsvg** (`rsvg-convert`):

```bash
rsvg-convert -w 1200 -h 627 assets/linkedin-banking-platform-architecture.svg -o banking-architecture.png
rsvg-convert -w 1200 -h 627 assets/linkedin-banking-platform-tech-stack.svg -o banking-tech-stack.png
```

- **ImageMagick** (`magick`):

```bash
magick -density 200 assets/linkedin-banking-platform-architecture.svg -resize 1200x627 banking-architecture.png
magick -density 200 assets/linkedin-banking-platform-tech-stack.svg -resize 1200x627 banking-tech-stack.png
```

### Suggested LinkedIn format

- Size: **1200×627**
- Filetype: **PNG**
- Post: add the image as the first attachment, then paste your text.


