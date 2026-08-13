const imageToken = /note-image:([0-9a-f-]{36})/gi
export function imageIds(markdown: string): string[] {
  return [...markdown.matchAll(imageToken)]
    .map((match) => match[1])
    .filter((id): id is string => typeof id === 'string')
}
export function insertAtCursor(input: HTMLTextAreaElement, token: string) {
  const start = input.selectionStart
  const end = input.selectionEnd
  input.setRangeText(token, start, end, 'end')
  input.dispatchEvent(new Event('input', { bubbles: true }))
  input.focus()
}
export function escapeHtml(value: string) {
  return value.replace(
    /[&<>"']/g,
    (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]!,
  )
}

export function renderMathToHtml(math: string): string {
  let expr = escapeHtml(math.trim())

  expr = expr
    .replace(/\\sum/g, '∑')
    .replace(/\\int/g, '∫')
    .replace(/\\prod/g, '∏')
    .replace(/\\infty/g, '∞')
    .replace(/\\pm/g, '±')
    .replace(/\\mp/g, '∓')
    .replace(/\\times/g, '×')
    .replace(/\\div/g, '÷')
    .replace(/\\neq/g, '≠')
    .replace(/\\le/g, '≤')
    .replace(/\\ge/g, '≥')
    .replace(/\\approx/g, '≈')
    .replace(/\\alpha/g, 'α')
    .replace(/\\beta/g, 'β')
    .replace(/\\gamma/g, 'γ')
    .replace(/\\delta/g, 'δ')
    .replace(/\\epsilon/g, 'ε')
    .replace(/\\theta/g, 'θ')
    .replace(/\\lambda/g, 'λ')
    .replace(/\\mu/g, 'μ')
    .replace(/\\pi/g, 'π')
    .replace(/\\sigma/g, 'σ')
    .replace(/\\omega/g, 'ω')
    .replace(/\\Delta/g, 'Δ')
    .replace(/\\Sigma/g, 'Σ')
    .replace(/\\Omega/g, 'Ω')

  expr = expr.replace(/\\frac\{([^}]+)\}\{([^}]+)\}/g, '<span class="math-frac"><span class="math-num">$1</span><span class="math-den">$2</span></span>')
  expr = expr.replace(/\\sqrt\{([^}]+)\}/g, '<span class="math-sqrt-symbol">√</span><span class="math-sqrt-body">$1</span>')
  expr = expr.replace(/\^{([^}]+)\}/g, '<sup>$1</sup>').replace(/\^([0-9a-zA-Z+-=]+)/g, '<sup>$1</sup>')
  expr = expr.replace(/_\{([^}]+)\}/g, '<sub>$1</sub>').replace(/_([0-9a-zA-Z+-=]+)/g, '<sub>$1</sub>')

  return `<span class="math-formula">${expr}</span>`
}

export function renderMarkdown(markdown: string, urls: Map<string, string>) {
  const mathPlaceholders: string[] = []
  let text = markdown.replace(/\$\$([\s\S]+?)\$\$/g, (_, math: string) => {
    const idx = mathPlaceholders.length
    mathPlaceholders.push(`<div class="math-block">${renderMathToHtml(math)}</div>`)
    return `___MATH_PLACEHOLDER_${idx}___`
  }).replace(/\$([^\$\n]+?)\$/g, (_, math: string) => {
    const idx = mathPlaceholders.length
    mathPlaceholders.push(`<span class="math-inline">${renderMathToHtml(math)}</span>`)
    return `___MATH_PLACEHOLDER_${idx}___`
  })

  let safe = escapeHtml(text)
  safe = safe.replace(imageToken, (_, id: string) =>
    urls.has(id)
      ? `<img src="${urls.get(id)!}" alt="Note image" loading="lazy">`
      : `<span class="image-loading">Loading image…</span>`,
  )
  safe = safe
    .replace(/^### (.*)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*)$/gm, '<h1>$1</h1>')
    .replace(/^&gt; (.*)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^\s*-\s+(.*)$/gm, '<li class="math-list-item">$1</li>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+|mailto:[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>',
    )

  mathPlaceholders.forEach((html, idx) => {
    safe = safe.replace(`___MATH_PLACEHOLDER_${idx}___`, html)
  })

  return safe
    .split(/\n{2,}/)
    .map((part) =>
      /^<h[1-3]>|^<blockquote>|^<div class="math-block">/.test(part) ? part : `<p>${part.replace(/\n/g, '<br>')}</p>`,
    )
    .join('')
}
