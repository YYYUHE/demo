export const MENTION_REGEX = /@\[uid:(\d+)\]/g

export function extractMentionUids(content) {
  if (!content) return []
  
  const uids = new Set()
  let match
  
  MENTION_REGEX.lastIndex = 0
  
  while ((match = MENTION_REGEX.exec(content)) !== null) {
    uids.add(parseInt(match[1], 10))
  }
  
  return Array.from(uids)
}

export function renderMentions(content, uidToNameMap = new Map()) {
  if (!content) return content
  
  if (content.includes('mention-tag')) {
    return content
  }
  
  return content.replace(MENTION_REGEX, (match, uid) => {
    const userId = parseInt(uid, 10)
    const username = uidToNameMap.get(userId) || `用户${uid}`
    
    return `<span class="mention-tag" data-uid="${userId}" title="@${username}">@${username}</span>`
  })
}

export function insertMentionToTextarea(textarea, user) {
  const mentionText = `@[uid:${user.id}]`
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const value = textarea.value
  
  let beforeCursor = value.substring(0, start)
  const lastAtSymbol = beforeCursor.lastIndexOf('@')
  
  if (lastAtSymbol !== -1 && lastAtSymbol === start - 1) {
    beforeCursor = beforeCursor.substring(0, lastAtSymbol)
  }
  
  const newValue = beforeCursor + mentionText + ' ' + value.substring(end)
  
  textarea.value = newValue
  
  const newCursorPos = beforeCursor.length + mentionText.length + 1
  textarea.setSelectionRange(newCursorPos, newCursorPos)
  textarea.focus()
  
  return newValue
}

export function getCaretCoordinates(textarea) {
  const { selectionStart } = textarea
  const div = document.createElement('div')
  
  const style = window.getComputedStyle(textarea)
  div.style.width = style.width
  div.style.font = style.font
  div.style.padding = style.padding
  div.style.border = style.border
  div.style.whiteSpace = 'pre-wrap'
  div.style.wordWrap = 'break-word'
  div.style.position = 'absolute'
  div.style.visibility = 'hidden'
  div.style.top = '0'
  div.style.left = '0'
  
  const text = textarea.value.substring(0, selectionStart)
  div.textContent = text
  
  const span = document.createElement('span')
  span.textContent = textarea.value.substring(selectionStart) || '.'
  div.appendChild(span)
  
  document.body.appendChild(div)
  
  const coordinates = {
    top: span.offsetTop + textarea.offsetTop,
    left: span.offsetLeft + textarea.offsetLeft
  }
  
  document.body.removeChild(div)
  
  return coordinates
}

export function highlightMentionsInHtml(htmlContent) {
  if (!htmlContent) return htmlContent
  
  return htmlContent.replace(
    /@\[uid:(\d+)\]/g,
    '<span class="mention-tag" data-uid="$1">@$1</span>'
  )
}

export function getMentionSearchKeyword(text, cursorPos) {
  if (!text || cursorPos <= 0) return null
  
  const textBeforeCursor = text.substring(0, cursorPos)
  const lastAtIndex = textBeforeCursor.lastIndexOf('@')
  
  if (lastAtIndex === -1) return null
  
  const textAfterAt = textBeforeCursor.substring(lastAtIndex + 1)
  
  if (textAfterAt.includes(' ') || textAfterAt.includes('\n')) return null
  
  const mentionPattern = /@\[uid:\d+\]/
  const textFromAt = text.substring(lastAtIndex)
  if (mentionPattern.test(textFromAt)) return null
  
  return {
    keyword: textAfterAt,
    atIndex: lastAtIndex
  }
}
