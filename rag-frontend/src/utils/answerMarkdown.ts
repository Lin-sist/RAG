import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ html: false, linkify: true, typographer: true, breaks: true })
const defaultValidateLink = md.validateLink.bind(md)
md.validateLink = (url: string) => defaultValidateLink(url)
  && (!/^[a-z][a-z\d+.-]*:/i.test(url) || /^(https?:|mailto:)/i.test(url))

md.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index]!
  const language = md.utils.escapeHtml(token.info.trim().split(/\s+/)[0] || '代码')
  const code = md.utils.escapeHtml(token.content)
  return `<section class="answer-code"><div class="code-toolbar"><span>${language}</span><span class="code-copy-result" role="status" aria-live="polite"></span><button type="button" class="code-copy" aria-label="复制代码">复制代码</button></div><pre tabindex="0" aria-label="代码内容"><code>${code}</code></pre></section>\n`
}
md.renderer.rules.table_open = () => '<div class="answer-table" tabindex="0" role="region" aria-label="回答表格"><table>\n'
md.renderer.rules.table_close = () => '</table></div>\n'
const renderLink = md.renderer.rules.link_open
md.renderer.rules.link_open = (tokens, index, options, env, renderer) => {
  tokens[index]!.attrSet('target', '_blank')
  tokens[index]!.attrSet('rel', 'noopener noreferrer')
  return renderLink ? renderLink(tokens, index, options, env, renderer) : renderer.renderToken(tokens, index, options)
}

export function renderAnswerMarkdown(content: string): string {
  return content ? md.render(content) : ''
}
