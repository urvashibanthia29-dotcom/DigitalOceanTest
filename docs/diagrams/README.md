# Architecture diagram assets

Static exports of the diagrams in [README.md](../README.md).

## Design document

- **[LLM-Proxy-Design-Document.docx](LLM-Proxy-Design-Document.docx)** — Word development design document (problem statement, goals, architecture, trade-offs, etc.)
- Regenerate: `python3 generate_design_doc.py`

| Diagram | PNG | SVG | Source |
|---------|-----|-----|--------|
| Layer map (API · sync path · shadow pool) | [architecture-layer-map.png](architecture-layer-map.png) | [architecture-layer-map.svg](architecture-layer-map.svg) | [architecture-layer-map.mmd](architecture-layer-map.mmd) |
| Request sequence | [architecture-sequence.png](architecture-sequence.png) | [architecture-sequence.svg](architecture-sequence.svg) | [architecture-sequence.mmd](architecture-sequence.mmd) |

## Regenerate

Edit the `.mmd` files, then render with [Kroki](https://kroki.io):

```bash
cd docs/diagrams
curl -X POST https://kroki.io/mermaid/svg -H "Content-Type: text/plain" \
  --data-binary @architecture-layer-map.mmd -o architecture-layer-map.svg
curl -X POST https://kroki.io/mermaid/png -H "Content-Type: text/plain" \
  --data-binary @architecture-layer-map.mmd -o architecture-layer-map.png
curl -X POST https://kroki.io/mermaid/svg -H "Content-Type: text/plain" \
  --data-binary @architecture-sequence.mmd -o architecture-sequence.svg
curl -X POST https://kroki.io/mermaid/png -H "Content-Type: text/plain" \
  --data-binary @architecture-sequence.mmd -o architecture-sequence.png
```

Or with Mermaid CLI (requires Chromium):

```bash
npx @mermaid-js/mermaid-cli -i architecture-layer-map.mmd -o architecture-layer-map.svg
```
