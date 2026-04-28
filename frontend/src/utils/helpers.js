export function escapeHtml(text) {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = String(text)
  return div.innerHTML
}

export function formatTime(timeStr) {
  if (!timeStr) return ''
  
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else {
    const year = date.getFullYear()
    const month = date.getMonth() + 1
    const day = date.getDate()
    return `${year}年${month}月${day}日`
  }
}

export function formatDateTime(dateTimeString) {
  if (!dateTimeString) return ''
  
  const date = new Date(dateTimeString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

export function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  return num.toString()
}

export function getPlainTextPreview(html) {
  if (!html) return '无内容'
  
  const temp = document.createElement('div')
  temp.innerHTML = html
  const text = temp.textContent || temp.innerText || ''
  
  return text.substring(0, 100) + (text.length > 100 ? '...' : '')
}

export function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}
