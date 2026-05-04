/**
 * mention.js — @提及功能共享模块
 * 提供：MentionPopup 弹窗、搜索、插入、渲染高亮等
 */
(function (global) {
  'use strict';

  var MENTION_REGEX = /@\[uid:(\d+)\]/g;

  var _uidNameCache = new Map();
  var _currentUserId = null;

  function setCurrentUserId(id) {
    _currentUserId = id;
  }

  function getCurrentUserId() {
    if (_currentUserId != null) return _currentUserId;
    try {
      var raw = localStorage.getItem('user') || sessionStorage.getItem('user');
      if (raw) _currentUserId = JSON.parse(raw).id;
    } catch (e) { /* ignore */ }
    return _currentUserId;
  }

  function extractMentionUids(content) {
    if (!content) return [];
    var uids = new Set();
    MENTION_REGEX.lastIndex = 0;
    var m;
    while ((m = MENTION_REGEX.exec(content)) !== null) {
      uids.add(parseInt(m[1], 10));
    }
    return Array.from(uids);
  }

  function searchMentionUsers(keyword) {
    var params = keyword ? '?keyword=' + encodeURIComponent(keyword) : '';
    return fetch('/api/mentions/search' + params, { credentials: 'include' })
      .then(function (r) { return r.json(); })
      .then(function (json) {
        if (json.code === 200) return json.data || [];
        return [];
      });
  }

  function loadUserNames(uids) {
    var missing = uids.filter(function (id) { return !_uidNameCache.has(id); });
    if (missing.length === 0) return Promise.resolve(_uidNameCache);
    return fetch('/api/users/basic?ids=' + missing.join(','), { credentials: 'include' })
      .then(function (r) { return r.json(); })
      .then(function (json) {
        if (json.code === 200 && Array.isArray(json.data)) {
          json.data.forEach(function (u) {
            if (u && u.id != null) {
              _uidNameCache.set(u.id, u.username || ('\u7528\u6237' + u.id));
            }
          });
        }
        return _uidNameCache;
      })
      .catch(function () { return _uidNameCache; });
  }

  function escapeHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function renderMentionContent(text) {
    if (!text) return '';
    var escaped = escapeHtml(text);
    MENTION_REGEX.lastIndex = 0;
    return escaped.replace(MENTION_REGEX, function (match, uid) {
      var id = parseInt(uid, 10);
      var name = _uidNameCache.get(id) || ('\u7528\u6237' + id);
      return '<span class="mention-tag" data-uid="' + id + '">@' + escapeHtml(name) + '</span>';
    });
  }

  function renderMentionTagsInElement(container) {
    if (!container) return;
    var walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, null, false);
    var nodes = [];
    var n;
    while ((n = walker.nextNode())) nodes.push(n);

    nodes.forEach(function (textNode) {
      var text = textNode.textContent || '';
      MENTION_REGEX.lastIndex = 0;
      if (!MENTION_REGEX.test(text)) return;
      MENTION_REGEX.lastIndex = 0;

      var frag = document.createDocumentFragment();
      var lastIndex = 0;
      var m;
      while ((m = MENTION_REGEX.exec(text)) !== null) {
        if (m.index > lastIndex) {
          frag.appendChild(document.createTextNode(text.substring(lastIndex, m.index)));
        }
        var uid = parseInt(m[1], 10);
        var name = _uidNameCache.get(uid) || ('\u7528\u6237' + uid);
        var span = document.createElement('span');
        span.className = 'mention-tag';
        span.setAttribute('data-uid', uid);
        span.textContent = '@' + name;
        frag.appendChild(span);
        lastIndex = m.index + m[0].length;
      }
      if (lastIndex < text.length) {
        frag.appendChild(document.createTextNode(text.substring(lastIndex)));
      }
      textNode.parentNode.replaceChild(frag, textNode);
    });
  }

  function getCaretCoordinates(textarea) {
    var selStart = textarea.selectionStart;
    var div = document.createElement('div');
    var style = window.getComputedStyle(textarea);
    div.style.width = style.width;
    div.style.font = style.font;
    div.style.padding = style.padding;
    div.style.border = style.border;
    div.style.whiteSpace = 'pre-wrap';
    div.style.wordWrap = 'break-word';
    div.style.position = 'absolute';
    div.style.visibility = 'hidden';
    div.style.top = '0';
    div.style.left = '0';
    var text = textarea.value.substring(0, selStart);
    div.textContent = text;
    var span = document.createElement('span');
    span.textContent = textarea.value.substring(selStart) || '.';
    div.appendChild(span);
    document.body.appendChild(div);
    var coords = {
      top: span.offsetTop + textarea.offsetTop,
      left: span.offsetLeft + textarea.offsetLeft
    };
    document.body.removeChild(div);
    return coords;
  }

  function insertMentionToTextarea(textarea, user) {
    var mentionText = '@[uid:' + user.id + ']';
    var val = textarea.value;
    var cursorPos = textarea.selectionStart;

    var beforeCursor = val.substring(0, cursorPos);
    var afterCursor = val.substring(cursorPos);

    var lastAtIndex = beforeCursor.lastIndexOf('@');
    if (lastAtIndex !== -1) {
      var textAfterAt = beforeCursor.substring(lastAtIndex + 1);
      var isSearching = textAfterAt.indexOf(' ') === -1 && textAfterAt.indexOf('\n') === -1;
      if (isSearching) {
        beforeCursor = beforeCursor.substring(0, lastAtIndex);
      }
    }

    var newVal = beforeCursor + mentionText + ' ' + afterCursor;
    textarea.value = newVal;
    var newCursorPos = beforeCursor.length + mentionText.length + 1;
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    textarea.focus();

    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    return newVal;
  }

  /* ── MentionPopup ── */
  function MentionPopup(options) {
    this.options = options || {};
    this.onSelect = this.options.onSelect || function () {};
    this.onClose = this.options.onClose || function () {};
    this.el = null;
    this.searchInput = null;
    this.userListEl = null;
    this.users = [];
    this.activeIndex = 0;
    this.loading = false;
    this.searchTimer = null;
    this._build();
  }

  MentionPopup.prototype._build = function () {
    var self = this;
    var popup = document.createElement('div');
    popup.className = 'mention-popup';
    popup.style.display = 'none';

    var header = document.createElement('div');
    header.className = 'mention-popup-header';

    var title = document.createElement('span');
    title.className = 'mention-popup-title';
    title.textContent = '@ \u63d0\u53ca';

    var input = document.createElement('input');
    input.type = 'text';
    input.className = 'mention-popup-search';
    input.placeholder = '\u641c\u7d22\u7528\u6237...';
    this.searchInput = input;

    header.appendChild(title);
    header.appendChild(input);

    var list = document.createElement('div');
    list.className = 'mention-popup-list';
    this.userListEl = list;

    popup.appendChild(header);
    popup.appendChild(list);
    document.body.appendChild(popup);
    this.el = popup;

    input.addEventListener('input', function () {
      if (self.searchTimer) clearTimeout(self.searchTimer);
      self.searchTimer = setTimeout(function () {
        self.activeIndex = 0;
        self._loadUsers();
      }, 300);
    });

    input.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        var prev = self.userListEl.querySelector('.mention-popup-item.active');
        if (prev) prev.classList.remove('active');
        self.activeIndex = (self.activeIndex + 1) % self.users.length;
        var next = self.userListEl.children[self.activeIndex];
        if (next) next.classList.add('active');
        self._scrollToActive();
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        var prev = self.userListEl.querySelector('.mention-popup-item.active');
        if (prev) prev.classList.remove('active');
        self.activeIndex = (self.activeIndex - 1 + self.users.length) % self.users.length;
        var next = self.userListEl.children[self.activeIndex];
        if (next) next.classList.add('active');
        self._scrollToActive();
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (self.users.length > 0 && self.activeIndex >= 0) {
          self._selectUser(self.users[self.activeIndex]);
        }
      } else if (e.key === 'Escape') {
        e.preventDefault();
        self.close();
      }
    });

    this._clickOutsideHandler = function (e) {
      if (self.el.style.display === 'none') return;
      if (!self.el.contains(e.target)) {
        self.close();
      }
    };
    document.addEventListener('mousedown', this._clickOutsideHandler);

    this.userListEl.addEventListener('click', function (e) {
      var item = e.target.closest('.mention-popup-item');
      if (!item) return;
      var idx = parseInt(item.getAttribute('data-index'), 10);
      if (!isNaN(idx) && idx >= 0 && idx < self.users.length) {
        self._selectUser(self.users[idx]);
      }
    });

    this.userListEl.addEventListener('mouseover', function (e) {
      var item = e.target.closest('.mention-popup-item');
      if (!item) return;
      var idx = parseInt(item.getAttribute('data-index'), 10);
      if (idx !== self.activeIndex) {
        var prev = self.userListEl.querySelector('.mention-popup-item.active');
        if (prev) prev.classList.remove('active');
        item.classList.add('active');
        self.activeIndex = idx;
      }
    });
  };

  MentionPopup.prototype._loadUsers = function () {
    var self = this;
    var keyword = this.searchInput.value || '';
    this.loading = true;
    this._renderList();
    searchMentionUsers(keyword).then(function (users) {
      self.users = users || [];
      self.loading = false;
      self._renderList();
    }).catch(function () {
      self.users = [];
      self.loading = false;
      self._renderList();
    });
  };

  MentionPopup.prototype._renderList = function () {
    var self = this;
    var list = this.userListEl;
    if (this.loading) {
      list.innerHTML = '<div class="mention-popup-tip">\u641c\u7d22\u4e2d...</div>';
      return;
    }
    if (this.users.length === 0) {
      list.innerHTML = '<div class="mention-popup-tip">' +
        (this.searchInput.value ? '\u672a\u627e\u5230\u5339\u914d\u7684\u7528\u6237' : '\u6682\u65e0\u5173\u6ce8\u7684\u7528\u6237') + '</div>';
      return;
    }
    var currentId = getCurrentUserId();
    list.innerHTML = this.users.map(function (u, i) {
      var isSelf = u.id === currentId;
      var avatarHtml = u.avatar
        ? '<img class="mention-popup-avatar" src="' + escapeHtml(u.avatar) + '" alt="">'
        : '<div class="mention-popup-avatar mention-popup-avatar-default">' + escapeHtml((u.username || '?').charAt(0)) + '</div>';
      return '<div class="mention-popup-item' +
        (i === self.activeIndex ? ' active' : '') +
        (isSelf ? ' is-self' : '') +
        '" data-index="' + i + '">' +
        avatarHtml +
        '<span class="mention-popup-name">' + escapeHtml(u.username || '') + '</span>' +
        (isSelf ? '<span class="mention-popup-self">\u81ea\u5df1</span>' : '') +
        '</div>';
    }).join('');

  };

  MentionPopup.prototype._scrollToActive = function () {
    var item = this.userListEl.children[this.activeIndex];
    if (item) item.scrollIntoView({ block: 'nearest' });
  };

  MentionPopup.prototype._selectUser = function (user) {
    if (!user) return;
    var currentId = getCurrentUserId();
    if (user.id === currentId) {
      if (typeof global.showToast === 'function') global.showToast('\u4e0d\u80fd@\u81ea\u5df1', 'error');
      return;
    }
    this.onSelect(user);
    this.close();
  };

  MentionPopup.prototype.open = function (x, y, preKeyword) {
    this.searchInput.value = preKeyword || '';
    this.users = [];
    this.activeIndex = 0;
    this._renderList();

    this.el.style.left = x + 'px';
    this.el.style.top = y + 'px';
    this.el.style.display = 'block';

    var rect = this.el.getBoundingClientRect();
    if (rect.right > window.innerWidth - 10) {
      this.el.style.left = (window.innerWidth - rect.width - 10) + 'px';
    }
    if (rect.bottom > window.innerHeight - 10) {
      this.el.style.top = (window.innerHeight - rect.height - 10) + 'px';
    }
    if (rect.left < 10) this.el.style.left = '10px';
    if (rect.top < 10) this.el.style.top = '10px';

    var self = this;
    setTimeout(function () { self.searchInput.focus(); }, 50);
    this._loadUsers();
  };

  MentionPopup.prototype.close = function () {
    if (this.el) this.el.style.display = 'none';
    this.onClose();
  };

  MentionPopup.prototype.destroy = function () {
    if (this.el) {
      document.removeEventListener('mousedown', this._clickOutsideHandler);
      this.el.remove();
      this.el = null;
    }
  };

  MentionPopup.prototype.isOpen = function () {
    return this.el && this.el.style.display !== 'none';
  };

  function initMentionForTextarea(textarea, options) {
    options = options || {};
    var popup = new MentionPopup({
      onSelect: function (user) {
        insertMentionToTextarea(textarea, user);
        if (options.onSelect) options.onSelect(user);
      },
      onClose: options.onClose || function () {}
    });

    textarea.addEventListener('input', function (e) {
      var cursorPos = textarea.selectionStart;
      var textBeforeCursor = textarea.value.substring(0, cursorPos);
      if (textBeforeCursor.endsWith('@')) {
        var coords = getCaretCoordinates(textarea);
        var rect = textarea.getBoundingClientRect();
        popup.open(coords.left, coords.top - 350 < 10 ? coords.top + 30 : coords.top - 350);
      } else {
        var lastAt = textBeforeCursor.lastIndexOf('@');
        if (lastAt !== -1) {
          var after = textBeforeCursor.substring(lastAt + 1);
          if (after.indexOf(' ') === -1 && after.indexOf('\n') === -1 && !/@\[uid:/.test(after)) {
            var coords = getCaretCoordinates(textarea);
            popup.open(coords.left, coords.top - 350 < 10 ? coords.top + 30 : coords.top - 350, after);
            return;
          }
        }
        if (popup.isOpen()) popup.close();
      }
    });

    textarea.addEventListener('keydown', function (e) {
      if (!popup.isOpen()) return;
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'Enter' || e.key === 'Escape') {
        popup.searchInput.dispatchEvent(new KeyboardEvent('keydown', { key: e.key, bubbles: true }));
        e.preventDefault();
        e.stopPropagation();
      }
    }, true);

    return popup;
  }

  global.MentionUtils = {
    MentionPopup: MentionPopup,
    searchMentionUsers: searchMentionUsers,
    extractMentionUids: extractMentionUids,
    renderMentionContent: renderMentionContent,
    renderMentionTagsInElement: renderMentionTagsInElement,
    insertMentionToTextarea: insertMentionToTextarea,
    getCaretCoordinates: getCaretCoordinates,
    loadUserNames: loadUserNames,
    initMentionForTextarea: initMentionForTextarea,
    setCurrentUserId: setCurrentUserId,
    getCurrentUserId: getCurrentUserId,
    uidNameCache: _uidNameCache,
    MENTION_REGEX: MENTION_REGEX
  };
})(window);
