(() => {
  const state = {
    post: window.__POST__ || null,
    postId: window.__POST__?.id || null,
    currentUserId: null,
    postType: "text",
    topics: [],
    comments: {
      sort: "time",
      page: 1,
      size: 20,
      total: 0,
      loading: false,
      items: []
    },
    composer: {
      parentId: null,
      replyToName: null,
      emojiOpen: false,
      sending: false
    },
    carousel: {
      images: [],
      index: 0,
      timer: null,
      touching: false,
      startX: 0,
      startY: 0,
      moved: false
    },
    viewer: {
      open: false,
      src: "",
      scale: 1,
      tx: 0,
      ty: 0,
      mode: null,
      startX: 0,
      startY: 0,
      startTx: 0,
      startTy: 0,
      startDist: 0,
      startScale: 1,
      lastTapTs: 0,
      moved: false
    },
    vote: {
      data: null,
      selectedOptionIds: new Set(),
      submitting: false
    },
    toastTimer: null,
    lastTouchEndTs: 0
  };

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  const IMG_PLACEHOLDER = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#f2f3f5"/>
          <stop offset="1" stop-color="#e6e8eb"/>
        </linearGradient>
      </defs>
      <rect width="640" height="360" rx="18" fill="url(#g)"/>
      <g fill="none" stroke="#909399" stroke-width="10" stroke-linecap="round" stroke-linejoin="round" opacity="0.75">
        <path d="M210 220l50-55 45 45 60-70 65 80"/>
        <rect x="190" y="120" width="260" height="160" rx="16"/>
        <path d="M248 158h0"/>
      </g>
      <text x="320" y="315" font-size="22" text-anchor="middle" fill="#606266" font-family="Arial, sans-serif">图片加载失败，点击重试</text>
    </svg>`
  )}`;

  function escapeHtml(str) {
    return String(str)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function showToast(message, type = "info") {
    const toast = $("#toast");
    if (!toast) return;
    toast.className = `toast show ${type}`;
    toast.textContent = message;
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => {
      toast.className = "toast";
    }, 2600);
  }

  function withRetry(url) {
    const s = String(url || "").trim();
    if (!s) return "";
    const sep = s.includes("?") ? "&" : "?";
    return `${s}${sep}retry=${Date.now()}`;
  }

  function bindImgFallback(img) {
    if (!img || img.dataset.fallbackBound === "1") return;
    img.dataset.fallbackBound = "1";
    img.addEventListener("error", () => {
      const original = img.dataset.originalSrc || img.getAttribute("data-src") || img.getAttribute("src") || "";
      img.dataset.originalSrc = original;
      img.classList.add("img-error");
      img.src = IMG_PLACEHOLDER;
    });
    img.addEventListener("load", () => {
      if (img.classList.contains("img-error") && img.src !== IMG_PLACEHOLDER) {
        img.classList.remove("img-error");
      }
    });
    img.addEventListener("click", (e) => {
      if (!img.classList.contains("img-error")) return;
      e.preventDefault();
      e.stopPropagation();
      const original = img.dataset.originalSrc || "";
      if (!original) return;
      img.classList.remove("img-error");
      img.src = withRetry(original);
    });
  }

  function sanitizeHtml(html) {
    const raw = String(html || "");
    const doc = new DOMParser().parseFromString(raw, "text/html");

    doc.querySelectorAll("script, style, iframe, object, embed, link, meta").forEach((n) => n.remove());

    Array.from(doc.body.querySelectorAll("*")).forEach((el) => {
      Array.from(el.attributes).forEach((attr) => {
        const name = attr.name.toLowerCase();
        const value = attr.value || "";
        if (name.startsWith("on")) el.removeAttribute(attr.name);
        if (name === "srcdoc") el.removeAttribute(attr.name);
        if ((name === "href" || name === "src" || name === "xlink:href") && /^\s*javascript:/i.test(value)) {
          el.removeAttribute(attr.name);
        }
      });
      if (el.tagName === "A") {
        el.setAttribute("rel", "noopener noreferrer");
        el.setAttribute("target", "_blank");
      }
    });

    return doc.body.innerHTML;
  }

  function normalizeTextWhitespace(root) {
    if (!root) return;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    while (walker.nextNode()) {
      const node = walker.currentNode;
      const val = node.nodeValue || "";
      if (!val) continue;
      const parent = node.parentElement;
      if (parent && parent.closest("pre, code, textarea")) continue;
      if (!/[ \t]{2,}|\t/.test(val)) continue;
      node.nodeValue = val
        .replace(/\t/g, "\u00A0\u00A0\u00A0\u00A0")
        .replace(/ {2,}/g, (m) => " " + "\u00A0".repeat(m.length - 1));
    }
  }

  function renderPostContent(container, content, { removeImages = false, removeTopics = false, removeDatingInfo = false } = {}) {
    if (!container) return;
    const raw = String(content || "");
    const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(raw);
    container.classList.remove("plaintext");

    if (!looksLikeHtml) {
      container.textContent = raw;
      container.classList.add("plaintext");
      return;
    }

    const div = document.createElement("div");
    div.innerHTML = sanitizeHtml(raw);
    if (removeImages) div.querySelectorAll("img").forEach((img) => img.remove());
    
    // 渲染@提及标签
    if (typeof MentionUtils !== 'undefined') {
      MentionUtils.renderMentionTagsInElement(div);
    }
    
    // 如果需要移除话题标签，删除包含#标签的段落
    if (removeTopics) {
      div.querySelectorAll("p").forEach((p) => {
        const text = (p.textContent || "").trim();
        // 匹配纯话题标签段落：以#开头，且去除所有#标签后没有实质内容
        if (/^#[^\s#]/.test(text)) {
          const withoutTopics = text.replace(/#[^\s#]+/g, '').trim();
          // 如果去除话题标签后只剩空格或空字符串，则删除该段落
          if (!withoutTopics) {
            p.remove();
          }
        }
      });
    }
    
    // 如果是交友帖子，移除性别信息和联系方式
    if (removeDatingInfo) {
      const paragraphs = Array.from(div.querySelectorAll("p"));
      const nodesToRemove = [];
      let isRemoving = false;
      
      const genderMarkers = ['我的性别：', '期望对方性别：'];
      const sectionMarkers = ['【自我介绍】', '【交友目标期望】', '【联系方式】'];
      const allMarkers = [...genderMarkers, ...sectionMarkers];
      
      for (let i = 0; i < paragraphs.length; i++) {
        const p = paragraphs[i];
        const text = (p.textContent || "").trim();
        
        if (!isRemoving) {
          // 检查是否进入需要移除的交友信息区域
          if (allMarkers.some(m => text.startsWith(m))) {
            isRemoving = true;
            nodesToRemove.push(p);
            
            // 如果是性别标记，同时移除接下来的2个段落（值 + 空行）
            if (genderMarkers.some(m => text.startsWith(m))) {
              if (i + 1 < paragraphs.length) {
                nodesToRemove.push(paragraphs[i + 1]);
                i++;
              }
              if (i + 1 < paragraphs.length) {
                nodesToRemove.push(paragraphs[i + 1]);
                i++;
              }
            }
          }
        } else {
          // 正在移除区域内
          nodesToRemove.push(p);
          
          const isContactFormat = /^[^：]+：.+/.test(text);
          const isNextMarker = allMarkers.some(m => text.startsWith(m));
          
          if (isNextMarker) {
            // 遇到下一个标记
            if (genderMarkers.some(m => text.startsWith(m))) {
              // 性别标记：额外移除接下来的2个段落
              if (i + 1 < paragraphs.length) {
                nodesToRemove.push(paragraphs[i + 1]);
                i++;
              }
              if (i + 1 < paragraphs.length) {
                nodesToRemove.push(paragraphs[i + 1]);
                i++;
              }
            }
            // 其他标记继续移除模式
          } else if (isContactFormat || text.length === 0) {
            // 联系方式格式或空段落，继续移除
          } else {
            // 遇到实际正文内容，停止移除
            nodesToRemove.pop();
            isRemoving = false;
          }
        }
      }
      
      nodesToRemove.forEach(node => node.remove());
    }
    
    normalizeTextWhitespace(div);
    container.innerHTML = div.innerHTML;
  }

  function formatDateTime(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, "0");
    return `发布于${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日 ${pad(
      d.getHours()
    )}:${pad(d.getMinutes())}`;
  }

  function stripHtml(html) {
    const div = document.createElement("div");
    div.innerHTML = html || "";
    return (div.textContent || "").replace(/\s+/g, " ").trim();
  }

  function extractImgSrcs(html) {
    const div = document.createElement("div");
    div.innerHTML = html || "";
    const imgs = Array.from(div.querySelectorAll("img"))
      .map((img) => img.getAttribute("src"))
      .filter(Boolean);
    return imgs;
  }

  function extractTopicsFromHtml(html) {
    const div = document.createElement("div");
    div.innerHTML = html || "";
    const found = new Set();
    
    // 只从包含话题标签的段落中提取
    div.querySelectorAll("p").forEach((p) => {
      const text = (p.textContent || "").trim();
      // 匹配纯话题标签段落（只包含#标签，可能用空格分隔）
      if (/^#[^\s#]/.test(text)) {
        const topicRe = /#([^\s#]{1,20})/g;
        let m;
        while ((m = topicRe.exec(text))) {
          found.add(m[1]);
        }
      }
    });
    
    return Array.from(found).slice(0, 12);
  }

  function getLayoutOverride() {
    const val = new URLSearchParams(window.location.search).get("layout");
    const t = String(val || "").trim().toLowerCase();
    if (t === "image" || t === "text") return t;
    return "";
  }

  function detectPostType(post) {
    const forced = getLayoutOverride();
    if (forced) return forced;

    const p = String(post?.postType || "").trim().toLowerCase();
    if (p === "image" || p === "text") return p;

    const html = post?.content || "";
    const imgs = extractImgSrcs(html);
    const textLen = stripHtml(html).length;
    const explicit = Array.isArray(post?.images) ? post.images.filter(Boolean).length : 0;
    if (explicit >= 1 || imgs.length >= 1) {
      if (textLen <= 2000) return "image";
    }
    return "text";
  }

  function renderAuthorPin(post) {
    const avatar = post?.authorAvatar || "";
    const username = post?.authorUsername || "用户";
    const pin = $("#authorPin");
    if (!pin) return;
    
    // 只更新头像和用户名，不覆盖整个innerHTML以保留按钮
    const avatarEl = pin.querySelector('.author-avatar');
    const nameEl = pin.querySelector('.author-name');
    
    if (avatarEl) {
      const firstLetter = username ? username.charAt(0).toUpperCase() : "U";
      const avatarHtml = avatar
        ? `<img loading="lazy" src="${escapeHtml(avatar)}" alt="${escapeHtml(username)}">`
        : `<span>${escapeHtml(firstLetter)}</span>`;
      avatarEl.innerHTML = avatarHtml;
    }
    
    if (nameEl) {
      nameEl.textContent = username;
    }
  }

  function renderMeta(post) {
    const meta = $("#postMeta");
    if (!meta) return;
    const time = formatDateTime(post?.createTime);
    const topicsWrap = $("#topicTags");
    const timeEl = $("#publishTime");
    if (timeEl) timeEl.textContent = time;

    if (topicsWrap) {
      topicsWrap.innerHTML = "";
      
      // 优先使用后端返回的topics数据
      const topics = post.topics && post.topics.length > 0 
        ? post.topics.map(t => t.name)
        : state.topics;
      
      if (topics.length === 0) {
        topicsWrap.style.display = "none";
      } else {
        topicsWrap.style.display = "flex";
        topics.forEach((t) => {
          const btn = document.createElement("button");
          btn.type = "button";
          btn.className = "topic-tag";
          btn.textContent = `#${t}`;
          btn.addEventListener("click", () => {
            // 跳转到帖子列表页，按话题搜索（带#前缀）
            window.location.href = `/posts.html?search=%23${encodeURIComponent(t)}`;
          });
          topicsWrap.appendChild(btn);
        });
      }
    }

    meta.style.display = "flex";
  }

  function setTopbarTitle(post) {
    const title = post?.title || "帖子详情";
    const el = $("#topbarTitle");
    if (el) el.textContent = title;
    document.title = title;
  }

  // 渲染交友帖子的性别信息和联系方式
  function renderDatingInfo(content) {
    if (!content) return;
    
    // 从 HTML 内容中提取性别信息和联系方式
    let gender = '';
    let targetGender = '';
    let contacts = [];
    let selfIntro = '';
    let friendshipGoal = '';
    let supplementContent = '';
    
    // 匹配【自我介绍】的位置
    const selfIntroMatch = content.match(/<p><strong>【自我介绍】<\/strong><\/p>/);
    const friendshipGoalMatch = content.match(/<p><strong>【交友目标期望】<\/strong><\/p>/);
    const contactSectionMatch = content.match(/<p><strong>【联系方式】<\/strong><\/p>/);
    
    // 提取我的性别（始终显示，包括"不公布"）
    const genderMatch = content.match(/<p><strong>我的性别：<\/strong><\/p>\s*<p>([^<]+)<\/p>/);
    if (genderMatch) {
      gender = genderMatch[1].trim();
    } else {
      gender = '不公布'; // 默认值
    }
    
    // 提取期望对方性别（始终显示，包括"不限"）
    const targetGenderMatch = content.match(/<p><strong>期望对方性别：<\/strong><\/p>\s*<p>([^<]+)<\/p>/);
    if (targetGenderMatch) {
      targetGender = targetGenderMatch[1].trim();
    } else {
      targetGender = '不限'; // 默认值
    }
    
    // 提取自我介绍内容
    if (selfIntroMatch && friendshipGoalMatch) {
      const startIdx = selfIntroMatch.index + selfIntroMatch[0].length;
      const endIdx = friendshipGoalMatch.index;
      const sectionHtml = content.substring(startIdx, endIdx);
      // 提取段落内容，过滤掉空段落
      const paragraphs = sectionHtml.match(/<p>([^<]*)<\/p>/g);
      if (paragraphs) {
        selfIntro = paragraphs
          .map(p => p.replace(/<p>([^<]*)<\/p>/, '$1').trim())
          .filter(text => text.length > 0)
          .join('\n');
      }
    }
    
    // 提取交友目标期望和补充内容
    if (friendshipGoalMatch) {
      let startIdx = friendshipGoalMatch.index + friendshipGoalMatch[0].length;
      let endIdx = contactSectionMatch ? contactSectionMatch.index : content.length;
      const sectionHtml = content.substring(startIdx, endIdx);
      
      // 使用更简单的方法：按<p>标签分割（支持多行内容）
      const paragraphRegex = /<p>([\s\S]*?)<\/p>/g;
      let match;
      const allTexts = [];
      
      while ((match = paragraphRegex.exec(sectionHtml)) !== null) {
        const text = match[1].trim();
        // 过滤空内容和换行符
        if (text && text !== '<br>' && text !== 'br' && !text.startsWith('#')) {
          allTexts.push(text);
        }
      }
      
      // 第一段是交友目标期望，其余是补充内容
      if (allTexts.length > 0) {
        friendshipGoal = allTexts[0];
        if (allTexts.length > 1) {
          supplementContent = allTexts.slice(1).join('\n');
        }
      }
    }
    
    // 提取联系方式
    if (contactSectionMatch) {
      const afterContact = content.substring(contactSectionMatch.index + contactSectionMatch[0].length);
      const contactEndMatch = afterContact.match(/(?=<p><br><\/p>|$)/);
      const contactEndIdx = contactEndMatch ? contactEndMatch.index : afterContact.length;
      const contactHtml = afterContact.substring(0, contactEndIdx);
      const contactLines = contactHtml.match(/<p>([^<]+)<\/p>/g);
      if (contactLines) {
        contactLines.forEach(line => {
          const match = line.match(/<p>([^：]+)：([^<]+)<\/p>/);
          if (match) {
            contacts.push({
              type: match[1].trim(),
              value: match[2].trim()
            });
          }
        });
      }
    }
    
    // 创建性别信息显示区域（在标题下方）- 始终显示
    const datingInfoDiv = document.createElement('div');
    datingInfoDiv.className = 'dating-gender-info';
    
    let html = '<div class="gender-text-row">';
    html += `<span class="gender-item"><span class="gender-label">我的性别：</span><span class="gender-value">${escapeHtml(gender)}</span></span>`;
    html += `<span class="gender-item"><span class="gender-label">期望对方：</span><span class="gender-value">${escapeHtml(targetGender)}</span></span>`;
    html += '</div>';
    datingInfoDiv.innerHTML = html;
    
    // 插入到标题下方
    const titleEl = $("#postTitle");
    if (titleEl && titleEl.parentNode) {
      titleEl.parentNode.insertBefore(datingInfoDiv, titleEl.nextSibling);
    }
    
    // 在帖子内容区域添加板块标签
    const postContentEl = $("#postContent");
    if (postContentEl) {
      let sectionsHtml = '';
      
      // 添加自我介绍板块
      if (selfIntro) {
        sectionsHtml += `
          <div class="dating-section">
            <div class="section-label">自我介绍</div>
            <div class="section-content">${escapeHtml(selfIntro).replace(/\n/g, '<br>')}</div>
          </div>
        `;
      }
      
      // 添加交友目标期望板块
      if (friendshipGoal) {
        sectionsHtml += `
          <div class="dating-section">
            <div class="section-label">交友目标期望</div>
            <div class="section-content">${escapeHtml(friendshipGoal).replace(/\n/g, '<br>')}</div>
          </div>
        `;
      }
      
      // 添加补充内容板块（如果有）
      if (supplementContent) {
        sectionsHtml += `
          <div class="dating-section">
            <div class="section-label">补充内容</div>
            <div class="section-content">${escapeHtml(supplementContent).replace(/\n/g, '<br>')}</div>
          </div>
        `;
      }
      
      if (sectionsHtml) {
        postContentEl.innerHTML = sectionsHtml;
      }
    }
    
    // 创建联系方式显示区域（在内容下方、评论区上方）
    if (contacts.length > 0) {
      const contactDiv = document.createElement('div');
      contactDiv.className = 'dating-contact-section';
      
      let contactHtml = '<div class="contact-header"><span class="contact-icon">📱</span><span class="contact-title">联系方式</span></div>';
      contactHtml += '<div class="contact-list">';
      
      contacts.forEach(contact => {
        let icon = '📧';
        if (contact.type === 'QQ') icon = '💬';
        else if (contact.type === '微信') icon = '💚';
        else if (contact.type === '邮箱') icon = '📧';
        
        contactHtml += `
          <div class="contact-item">
            <span class="contact-type-badge">${icon} ${escapeHtml(contact.type)}</span>
            <span class="contact-value">${escapeHtml(contact.value)}</span>
          </div>
        `;
      });
      
      contactHtml += '</div>';
      contactDiv.innerHTML = contactHtml;
      
      // 插入到帖子卡片之后、评论区之前（在父容器中插入）
      const commentCard = $(".comment-card");
      if (commentCard && commentCard.parentNode) {
        commentCard.parentNode.insertBefore(contactDiv, commentCard);
      }
    }
  }


  function initCarousel(images) {
    const wrap = $("#carouselWrap");
    const track = $("#carouselTrack");
    const dots = $("#carouselDots");
    if (!wrap || !track || !dots) return;

    state.carousel.images = images;
    state.carousel.index = 0;
    track.innerHTML = "";
    dots.innerHTML = "";

    images.forEach((src, idx) => {
      const slide = document.createElement("div");
      slide.className = "carousel-slide";
      const img = document.createElement("img");
      img.alt = `图片${idx + 1}`;
      if (idx === 0) {
        img.src = src;
        img.fetchPriority = "high";
      } else {
        img.setAttribute("data-src", src);
      }
      img.loading = "lazy";
      img.decoding = "async";
      img.draggable = false;
      bindImgFallback(img);
      img.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        openImageViewer(img.getAttribute("data-src") || img.dataset.originalSrc || img.src);
      });
      slide.appendChild(img);
      track.appendChild(slide);

      const dot = document.createElement("div");
      dot.className = "dot" + (idx === 0 ? " active" : "");
      dot.addEventListener("click", () => goToSlide(idx));
      dots.appendChild(dot);
    });

    $("#carouselLeft")?.addEventListener("click", prevSlide);
    $("#carouselRight")?.addEventListener("click", nextSlide);

    wrap.addEventListener(
      "touchstart",
      (e) => {
        if (!e.touches || e.touches.length !== 1) return;
        state.carousel.touching = true;
        state.carousel.moved = false;
        state.carousel.startX = e.touches[0].clientX;
        state.carousel.startY = e.touches[0].clientY;
        stopAutoplay();
      },
      { passive: true }
    );

    wrap.addEventListener(
      "touchmove",
      (e) => {
        if (!state.carousel.touching || !e.touches || e.touches.length !== 1) return;
        const dx = e.touches[0].clientX - state.carousel.startX;
        const dy = e.touches[0].clientY - state.carousel.startY;
        if (Math.abs(dx) > 10 && Math.abs(dx) > Math.abs(dy)) {
          state.carousel.moved = true;
          e.preventDefault();
        }
      },
      { passive: false }
    );

    wrap.addEventListener(
      "touchend",
      (e) => {
        if (!state.carousel.touching) return;
        state.carousel.touching = false;
        if (!state.carousel.moved) {
          const img = track.querySelectorAll("img")[state.carousel.index];
          if (img) openImageViewer(img.getAttribute("data-src") || img.dataset.originalSrc || img.src);
          startAutoplay();
          return;
        }
        const endX = (e.changedTouches && e.changedTouches[0]?.clientX) || state.carousel.startX;
        const dx = endX - state.carousel.startX;
        if (dx > 50) prevSlide();
        else if (dx < -50) nextSlide();
        startAutoplay();
      },
      { passive: true }
    );

    startAutoplay();
    updateCarouselUi();
  }

  function ensureCarouselAroundLoaded() {
    const track = $("#carouselTrack");
    if (!track) return;
    const imgs = Array.from(track.querySelectorAll("img"));
    const idx = state.carousel.index;
    [idx, idx - 1, idx + 1].forEach((i) => {
      const img = imgs[i];
      if (!img) return;
      const src = img.getAttribute("data-src");
      if (src) {
        img.src = src;
        img.removeAttribute("data-src");
      }
      bindImgFallback(img);
    });
  }

  function updateCarouselUi() {
    const track = $("#carouselTrack");
    const dots = $$("#carouselDots .dot");
    if (!track) return;
    track.style.transform = `translateX(-${state.carousel.index * 100}%)`;
    dots.forEach((d, idx) => {
      d.classList.toggle("active", idx === state.carousel.index);
    });
    prefetchAround(state.carousel.images, state.carousel.index);
    ensureCarouselAroundLoaded();
  }

  function goToSlide(idx) {
    state.carousel.index = idx;
    updateCarouselUi();
    lazyLoadImages();
  }

  function prevSlide() {
    const len = state.carousel.images.length;
    state.carousel.index = (state.carousel.index - 1 + len) % len;
    updateCarouselUi();
    lazyLoadImages();
  }

  function nextSlide() {
    const len = state.carousel.images.length;
    state.carousel.index = (state.carousel.index + 1) % len;
    updateCarouselUi();
    lazyLoadImages();
  }

  function startAutoplay() {
    stopAutoplay();
    if (state.carousel.images.length <= 1) return;
    state.carousel.timer = setInterval(() => {
      nextSlide();
    }, 3500);
  }

  function stopAutoplay() {
    if (state.carousel.timer) clearInterval(state.carousel.timer);
    state.carousel.timer = null;
  }

  function prefetchAround(images, idx) {
    const prefetch = (url) => {
      if (!url) return;
      const img = new Image();
      img.src = url;
    };
    prefetch(images[idx]);
    prefetch(images[idx + 1]);
    prefetch(images[idx - 1]);
  }

  function lazyLoadImages() {
    const imgs = $$("img[data-src]");
    if (imgs.length === 0) return;
    if (!("IntersectionObserver" in window)) {
      imgs.forEach((img) => {
        bindImgFallback(img);
        img.decoding = "async";
        img.src = img.getAttribute("data-src");
        img.removeAttribute("data-src");
      });
      return;
    }
    const ob = new IntersectionObserver(
      (entries) => {
        entries.forEach((en) => {
          if (!en.isIntersecting) return;
          const img = en.target;
          const src = img.getAttribute("data-src");
          bindImgFallback(img);
          img.decoding = "async";
          if (src) img.src = src;
          img.removeAttribute("data-src");
          ob.unobserve(img);
        });
      },
      { rootMargin: "200px" }
    );
    imgs.forEach((img) => ob.observe(img));
  }

  function openImageViewer(src) {
    const viewer = $("#imageViewer");
    const img = $("#imageViewerImg");
    if (!viewer || !img) return;
    const safe = String(src || "").trim();
    if (!safe) return;
    state.viewer.open = true;
    state.viewer.src = safe;
    state.viewer.scale = 1;
    state.viewer.tx = 0;
    state.viewer.ty = 0;
    state.viewer.mode = null;
    state.viewer.moved = false;
    img.dataset.originalSrc = safe;
    bindImgFallback(img);
    img.src = safe;
    img.style.transform = "translate(0px, 0px) scale(1)";
    viewer.classList.add("open");
    document.body.classList.add("viewer-open");
  }

  function closeImageViewer() {
    const viewer = $("#imageViewer");
    const img = $("#imageViewerImg");
    if (!viewer || !img) return;
    state.viewer.open = false;
    viewer.classList.remove("open");
    img.removeAttribute("src");
    img.style.transform = "translate(0px, 0px) scale(1)";
    document.body.classList.remove("viewer-open");
  }

  async function loadVote() {
    if (!state.postId) return;
    
    // 优先使用后端返回的投票数据
    if (state.post && state.post.vote) {
      state.vote.data = state.post.vote;
      renderVote();
      return;
    }
    
    // 如果没有，则通过API获取
    try {
      const { resp, json } = await apiJson(`/api/votes/post/${state.postId}`);
      if (resp.ok && json.code === 200) {
        state.vote.data = json.data;
        renderVote();
      }
    } catch (e) {
      console.error('加载投票信息失败:', e);
    }
  }

  function renderVote() {
    const container = $("#voteContainer");
    const vote = state.vote.data;
    if (!container || !vote) return;

    container.style.display = "block";
    const hasVoted = vote.hasVoted;
    const isEnded = vote.isEnded;
    const maxChoices = vote.maxChoices || 1;
    const totalVoters = vote.totalVoters || 0;

    let optionsHtml = "";
    vote.options.forEach(opt => {
      const isSelected = state.vote.selectedOptionIds.has(opt.id);
      const percentage = opt.percentage !== null ? opt.percentage.toFixed(1) : 0;
      const count = opt.voteCount !== null ? opt.voteCount : 0;
      
      let extraClass = "";
      if (hasVoted || isEnded) extraClass += " voted";
      if (isSelected) extraClass += " selected";

      optionsHtml += `
        <div class="vote-option${extraClass}" onclick="handleVoteOptionClick(${opt.id})">
          ${(hasVoted || isEnded) ? `<div class="vote-progress-bar" style="width: ${percentage}%"></div>` : ""}
          <div class="vote-option-content">
            <div class="vote-option-text">${escapeHtml(opt.content)}</div>
            ${(hasVoted || isEnded) ? `<div class="vote-option-count">${count}票 (${percentage}%)</div>` : ""}
          </div>
        </div>
      `;
    });

    let footerText = "";
    if (isEnded) {
      footerText = "投票已结束";
    } else if (hasVoted) {
      footerText = "你已投票";
    } else {
      footerText = maxChoices > 1 ? `最多可选 ${maxChoices} 项` : "单选";
    }

    const deadlineText = vote.deadline ? `<div class="vote-meta">截止时间：${formatDateTime(vote.deadline)}</div>` : "";

    container.innerHTML = `
      <div class="vote-header">
        <div class="vote-title">${escapeHtml(vote.title)}</div>
        ${deadlineText}
      </div>
      <div class="vote-options">
        ${optionsHtml}
      </div>
      ${(!hasVoted && !isEnded) ? `
        <button class="vote-submit-btn" id="voteSubmitBtn" onclick="submitVote()" ${state.vote.selectedOptionIds.size === 0 ? "disabled" : ""}>
          ${state.vote.submitting ? "提交中..." : "提交投票"}
        </button>
      ` : ""}
      <div class="vote-info-footer">
        ${totalVoters} 人参与 · ${footerText}
      </div>
    `;
  }

  window.handleVoteOptionClick = (optionId) => {
    const vote = state.vote.data;
    if (!vote || vote.hasVoted || vote.isEnded) return;

    const maxChoices = vote.maxChoices || 1;
    if (maxChoices === 1) {
      state.vote.selectedOptionIds.clear();
      state.vote.selectedOptionIds.add(optionId);
    } else {
      if (state.vote.selectedOptionIds.has(optionId)) {
        state.vote.selectedOptionIds.delete(optionId);
      } else {
        if (state.vote.selectedOptionIds.size < maxChoices) {
          state.vote.selectedOptionIds.add(optionId);
        } else {
          showToast(`最多只能选择 ${maxChoices} 项`, "info");
          return;
        }
      }
    }
    renderVote();
  };

  window.submitVote = async () => {
    if (state.vote.submitting || state.vote.selectedOptionIds.size === 0) return;

    state.vote.submitting = true;
    renderVote();

    try {
      const { resp, json } = await apiJson("/api/votes/cast", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          voteId: state.vote.data.id,
          optionIds: Array.from(state.vote.selectedOptionIds)
        })
      });

      if (resp.ok && json.code === 200) {
        showToast("投票成功", "success");
        // 延迟500毫秒后刷新页面，让用户看到成功提示
        setTimeout(() => {
          window.location.reload();
        }, 500);
      } else {
        showToast(json.message || "投票失败", "error");
        state.vote.submitting = false;
        renderVote();
      }
    } catch (e) {
      showToast("网络请求失败", "error");
      state.vote.submitting = false;
      renderVote();
    }
  };

  function initGestureGuards() {
    document.addEventListener("gesturestart", (e) => e.preventDefault(), { passive: false });
    document.addEventListener("gesturechange", (e) => e.preventDefault(), { passive: false });
    document.addEventListener("gestureend", (e) => e.preventDefault(), { passive: false });

    document.addEventListener(
      "touchmove",
      (e) => {
        if (e.touches && e.touches.length > 1) e.preventDefault();
      },
      { passive: false }
    );

    document.addEventListener(
      "touchend",
      (e) => {
        const now = Date.now();
        if (now - state.lastTouchEndTs <= 280) {
          e.preventDefault();
        }
        state.lastTouchEndTs = now;
      },
      { passive: false }
    );

    document.addEventListener(
      "contextmenu",
      (e) => {
        const t = e.target;
        if (t && t.tagName === "IMG") e.preventDefault();
      },
      true
    );

    document.addEventListener(
      "dblclick",
      (e) => {
        const t = e.target;
        if (t && t.tagName === "IMG" && t.id !== "imageViewerImg") {
          e.preventDefault();
          e.stopPropagation();
        }
      },
      true
    );
  }

  function renderPost() {
    const post = state.post;
    if (!post) return;

    setTopbarTitle(post);
    renderAuthorPin(post);

    const titleEl = $("#postTitle");
    if (titleEl) titleEl.textContent = post.title || "";

    const bodyEl = $("#postContent");
    state.postType = detectPostType(post);
    const isImagePost = state.postType === "image";
    document.body.classList.toggle("is-image-post", isImagePost);
    document.body.classList.toggle("post-type-text", !isImagePost);
    $("#carouselWrap").style.display = isImagePost ? "block" : "none";
    $("#postMeta").style.display = "flex";

    // 对于图文帖子，移除内容中的图片和话题标签（因为它们分别显示在轮播图和meta区域）
    // 对于交友帖子，移除内容中的性别信息和联系方式
    const isDatingPost = post.category === '交友';
    renderPostContent(bodyEl, post.content || "", { 
      removeImages: isImagePost, 
      removeTopics: isImagePost,
      removeDatingInfo: isDatingPost
    });

    // 优先使用后端返回的topics数据，如果没有则从内容中提取
    if (post.topics && post.topics.length > 0) {
      state.topics = post.topics.map(t => t.name);
    } else {
      state.topics = extractTopicsFromHtml(post.content || "");
    }
    renderMeta(post);

    if (isImagePost) {
      const imgs = post.images && Array.isArray(post.images) ? post.images : extractImgSrcs(post.content || "");
      const unique = Array.from(new Set(imgs)).filter(Boolean);
      if (unique.length > 0) {
        initCarousel(unique);
      } else {
        $("#carouselWrap").style.display = "none";
      }
    }

    bodyEl?.querySelectorAll("img").forEach((img) => {
      const src = img.getAttribute("src");
      if (!src) return;
      bindImgFallback(img);
      img.setAttribute("data-src", src);
      img.removeAttribute("src");
      img.setAttribute("loading", "lazy");
      img.decoding = "async";
      img.draggable = false;
      img.addEventListener("click", () => openImageViewer(src));
    });
    lazyLoadImages();
    
    // 如果是交友帖子，在renderPostContent之后渲染性别信息和板块标签
    if (isDatingPost) {
      renderDatingInfo(post.content);
    }
    
    // 交友帖子不加载投票功能
    if (!isDatingPost) {
      loadVote();
    }
  }

  function setComposerReply(parentId, replyToName, authorId) {
    state.composer.parentId = parentId;
    state.composer.replyToName = replyToName;
    const hint = $("#replyHint");
    const textarea = $("#commentInput");
    if (hint) {
      hint.textContent = replyToName ? `回复 @${replyToName}` : "发表一条友善的评论";
    }
    if (textarea) {
      textarea.focus();
      if (replyToName && authorId) {
        const val = textarea.value || "";
        const mentionTag = `@[uid:${authorId}]`;
        if (!val.includes(mentionTag)) {
          textarea.value = `${val}${val ? "\n" : ""}${mentionTag} `;
          updateComposerCount();
        }
      } else if (replyToName) {
        const val = textarea.value || "";
        if (!val.includes(`@${replyToName}`)) {
          textarea.value = `${val}${val ? "\n" : ""}@${replyToName} `;
          updateComposerCount();
        }
      }
    }
  }

  function clearComposerReply() {
    setComposerReply(null, null);
    // 关闭所有内联回复框
    $$('.inline-reply-box').forEach(box => {
      box.style.display = 'none';
    });
  }

  function showInlineReply(commentId, username, authorId) {
    // 先隐藏所有其他内联回复框
    $$('.inline-reply-box').forEach(box => {
      box.style.display = 'none';
    });
    $$('.inline-emoji-panel').forEach(panel => {
      panel.classList.remove('open');
    });
    
    // 显示当前评论的内联回复框
    const replyBox = $(`#inline-reply-${commentId}`);
    if (replyBox) {
      replyBox.style.display = 'block';
      const textarea = $(`#inline-reply-textarea-${commentId}`);
      if (textarea) {
        if (authorId) {
          textarea.value = `@[uid:${authorId}] `;
        } else {
          textarea.value = '';
        }
        textarea.focus();
        updateInlineReplyCount(commentId);
      }
    }
  }

  function hideInlineReply(commentId) {
    const replyBox = $(`#inline-reply-${commentId}`);
    if (replyBox) {
      replyBox.style.display = 'none';
    }
    const emojiPanel = $(`#inline-emoji-panel-${commentId}`);
    if (emojiPanel) {
      emojiPanel.classList.remove('open');
    }
  }

  function toggleInlineEmoji(commentId) {
    const emojiPanel = $(`#inline-emoji-panel-${commentId}`);
    if (emojiPanel) {
      emojiPanel.classList.toggle('open');
    }
  }

  function insertInlineEmoji(commentId, emoji) {
    const textarea = $(`#inline-reply-textarea-${commentId}`);
    if (!textarea) return;
    
    const start = textarea.selectionStart ?? textarea.value.length;
    const end = textarea.selectionEnd ?? textarea.value.length;
    const val = textarea.value || '';
    textarea.value = val.slice(0, start) + emoji + val.slice(end);
    
    const next = start + emoji.length;
    textarea.setSelectionRange(next, next);
    textarea.focus();
    
    updateInlineReplyCount(commentId);
  }

  function updateInlineReplyCount(commentId) {
    const textarea = $(`#inline-reply-textarea-${commentId}`);
    const countEl = $(`#inline-reply-count-${commentId}`);
    const sendBtn = document.querySelector(`button[data-action="send-inline-reply"][data-id="${commentId}"]`);
    
    if (!textarea || !countEl) return;
    
    const len = (textarea.value || '').length;
    countEl.textContent = `${len}/500`;
    
    // 根据字数改变颜色
    countEl.classList.remove('warning', 'danger');
    if (len > 450) {
      countEl.classList.add('danger');
    } else if (len > 400) {
      countEl.classList.add('warning');
    }
    
    if (sendBtn) {
      sendBtn.disabled = len === 0 || len > 500;
    }
  }

  async function sendInlineReply(commentId) {
    const textarea = $(`#inline-reply-textarea-${commentId}`);
    if (!textarea) return;
    
    const content = (textarea.value || '').trim();
    if (!content) return;
    if (content.length > 500) {
      showToast('评论最多500字', 'info');
      return;
    }

    // 获取评论的深度信息
    let targetComment = null;
    let depth = 0;
    const findComment = (list, id, d = 1) => {
      for (const c of list) {
        if (c.id === id) {
          targetComment = c;
          depth = d;
          return true;
        }
        if (Array.isArray(c.replies) && c.replies.length > 0) {
          if (findComment(c.replies, id, d + 1)) return true;
        }
      }
      return false;
    };
    findComment(state.comments.items, commentId);
    
    if (!targetComment || depth >= 3) {
      showToast('无法回复此评论', 'error');
      return;
    }

    try {
      const { resp, json } = await apiJson(`/api/posts/${state.postId}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ parentId: commentId, content })
      });
      
      if (!resp.ok || json.code !== 200) {
        throw new Error(json.message || '发送失败');
      }
      
      showToast('已发送', 'success');
      hideInlineReply(commentId);
      
      // 刷新评论列表
      state.comments.page = 1;
      await loadComments(true);
    } catch (e) {
      showToast(e.message || '发送失败', 'error');
    }
  }

  function updateComposerCount() {
    const textarea = $("#commentInput");
    const countEl = $("#commentCount");
    const sendBtn = $("#sendBtn");
    if (!textarea || !countEl || !sendBtn) return;
    const len = (textarea.value || "").length;
    countEl.textContent = `${len}/500`;
    sendBtn.disabled = state.composer.sending || len === 0 || len > 500;
  }

  function toggleEmojiPanel(force) {
    const panel = $("#emojiPanel");
    if (!panel) return;
    state.composer.emojiOpen = typeof force === "boolean" ? force : !state.composer.emojiOpen;
    panel.classList.toggle("open", state.composer.emojiOpen);
  }

  function insertEmoji(emoji) {
    const textarea = $("#commentInput");
    if (!textarea) return;
    const start = textarea.selectionStart ?? textarea.value.length;
    const end = textarea.selectionEnd ?? textarea.value.length;
    const val = textarea.value || "";
    textarea.value = val.slice(0, start) + emoji + val.slice(end);
    const next = start + emoji.length;
    textarea.setSelectionRange(next, next);
    textarea.focus();
    updateComposerCount();
  }

  async function apiJson(url, options) {
    const resp = await fetch(url, options);
    const json = await resp.json().catch(() => ({}));
    return { resp, json };
  }

  async function loadCurrentUserId() {
    try {
      const { resp, json } = await apiJson("/api/profile/me", {
        method: "GET",
        headers: { Accept: "application/json" }
      });
      if (resp.ok && json.code === 200 && json.data?.id) {
        state.currentUserId = Number(json.data.id);
      } else {
        state.currentUserId = null;
      }
    } catch (e) {
      state.currentUserId = null;
    }
  }

  function buildAvatarHtml(username, avatar) {
    const safeName = username || "用户";
    const firstLetter = safeName.charAt(0).toUpperCase();
    if (avatar) {
      return `<img src="${escapeHtml(avatar)}" alt="${escapeHtml(safeName)}">`;
    }
    return `<span>${escapeHtml(firstLetter)}</span>`;
  }

  function renderCommentItem(c, depth = 1) {
    const user = c.authorUsername || "用户";
    const avatar = c.authorAvatar || "";
    const time = formatDateTime(c.createTime);
    const likeCount = c.likeCount ?? 0;
    const liked = !!c.liked;
    const canReply = c.depth === 1 || c.depth === 2;
    const replyBtn = canReply
      ? `<button class="action-btn" data-action="reply" data-id="${c.id}" data-user="${escapeHtml(
          user
        )}" data-author-id="${c.authorId || ''}">回复</button>`
      : "";
    const likeBtn = `<button class="action-btn ${liked ? "liked" : ""}" data-action="like" data-id="${c.id}">${
      liked ? "已赞" : "赞"
    } ${likeCount}</button>`;

    const replies = Array.isArray(c.replies) ? c.replies : [];
    const replyCount = Number(c.replyCount || 0);

    let repliesHtml = "";
    if (replies.length > 0) {
      const isSecondLevel = c.depth === 2;
      const wrapClass = isSecondLevel ? "replies" : "replies";
      repliesHtml += `<div class="${wrapClass}" data-replies-of="${c.id}">`;
      replies.forEach((r) => {
        repliesHtml += renderCommentItem(r, depth + 1);
      });
      if (isSecondLevel && replyCount > replies.length) {
        repliesHtml += `<div><button class="action-btn" data-action="more-third" data-id="${c.id}">查看更多</button></div>`;
      }
      repliesHtml += `</div>`;
    } else if (c.depth === 2 && replyCount > 0) {
      repliesHtml += `<div class="replies" data-replies-of="${c.id}"><div><button class="action-btn" data-action="more-third" data-id="${c.id}">查看更多</button></div></div>`;
    }

    let toggleSecond = "";
    if (c.depth === 1 && replyCount > replies.length) {
      toggleSecond = `<div style="margin-top:10px;"><button class="action-btn" data-action="more-second" data-id="${c.id}">展开${replyCount}条回复</button></div>`;
    } else if (c.depth === 1 && replyCount > 0 && replyCount <= replies.length) {
      toggleSecond = `<div style="margin-top:10px;"><button class="action-btn" data-action="collapse-second" data-id="${c.id}">收起回复</button></div>`;
    }

    // 添加内联回复输入框容器（默认隐藏）
    const emojis = ["😀", "😄", "😂", "🥲", "😍", "😎", "🤔", "", "👍", "❤️", "🎉", "✨", "🌟", "🔥", "💯", "👏", "", "💪", "🤝", "✌️", "👌", "", "😊", "", "⭐", "", "☀️", "🌙", "🎵", ""];
      
    let emojiItemsHtml = emojis.filter(e => e.trim()).map(e => 
      `<button type="button" class="inline-emoji" data-emoji="${e}">${e}</button>`
    ).join('');
      
    const inlineReplyHtml = `
      <div class="inline-reply-box" id="inline-reply-${c.id}" style="display:none;">
        <div class="inline-reply-input-wrap">
          <textarea class="inline-reply-textarea" id="inline-reply-textarea-${c.id}" 
                    placeholder="回复 @${escapeHtml(user)} ..." maxlength="500"></textarea>
          <div class="inline-emoji-panel" id="inline-emoji-panel-${c.id}">
            <div class="inline-emoji-grid">
              ${emojiItemsHtml}
            </div>
          </div>
          <div class="inline-reply-actions">
            <div class="inline-reply-left">
              <button class="inline-emoji-btn" data-action="toggle-inline-emoji" data-id="${c.id}" type="button">
                表情
              </button>
              <button class="inline-emoji-btn" data-action="cancel-inline-reply" data-id="${c.id}" type="button">
                取消
              </button>
            </div>
            <div class="inline-reply-right">
              <div class="inline-reply-count" id="inline-reply-count-${c.id}">0/500</div>
              <button class="inline-reply-send-btn" data-action="send-inline-reply" data-id="${c.id}" type="button" disabled>发布</button>
            </div>
          </div>
        </div>
      </div>
    `;

    const indentClass = depth >= 2 ? "reply-item" : "";
    return `
      <div class="${depth === 1 ? "comment-item" : indentClass}" data-comment="${c.id}">
        <div class="comment-row">
          <div class="comment-avatar">${buildAvatarHtml(user, avatar)}</div>
          <div class="comment-main">
            <div class="comment-meta">
              <div class="comment-user">${escapeHtml(user)}</div>
              <div>${escapeHtml(time)}</div>
            </div>
            <div class="comment-content">${typeof MentionUtils !== 'undefined' ? MentionUtils.renderMentionContent(c.content || "") : escapeHtml(c.content || "")}</div>
            <div class="comment-actions">
              ${likeBtn}
              ${replyBtn}
            </div>
            ${inlineReplyHtml}
            ${repliesHtml}
            ${toggleSecond}
          </div>
        </div>
      </div>
    `;
  }

  function renderComments() {
    const list = $("#commentList");
    if (!list) return;
    list.innerHTML = state.comments.items.map((c) => renderCommentItem(c)).join("");

    function finishRender() {
      lazyLoadImages();
      const loadMore = $("#loadMoreBtn");
      if (loadMore) {
        const loaded = state.comments.items.length;
        loadMore.style.display = loaded < state.comments.total ? "inline-flex" : "none";
        loadMore.disabled = state.comments.loading;
      }
    }

    if (typeof MentionUtils !== 'undefined') {
      var allUids = [];
      function collectUids(items) {
        (items || []).forEach(function(c) {
          MentionUtils.extractMentionUids(c.content || '').forEach(function(id) { allUids.push(id); });
          if (Array.isArray(c.replies)) collectUids(c.replies);
        });
      }
      collectUids(state.comments.items);
      if (allUids.length > 0) {
        MentionUtils.loadUserNames(allUids).then(function() {
          list.innerHTML = state.comments.items.map((c) => renderCommentItem(c)).join("");
          finishRender();
        });
        return;
      }
    }
    finishRender();
  }

  async function loadComments(reset = false) {
    if (!state.postId || state.comments.loading) return;
    state.comments.loading = true;
    const sort = state.comments.sort;
    const page = reset ? 1 : state.comments.page;
    const size = state.comments.size;

    const loadingEl = $("#commentLoading");
    if (loadingEl) loadingEl.style.display = "block";

    try {
      const { resp, json } = await apiJson(
        `/api/posts/${state.postId}/comments?page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`,
        { method: "GET", headers: { Accept: "application/json" } }
      );
      if (!resp.ok || json.code !== 200) {
        throw new Error(json.message || "加载评论失败");
      }

      const data = json.data;
      state.comments.total = Number(data.total || 0);
      state.comments.page = Number(data.page || page);
      const items = Array.isArray(data.items) ? data.items : [];
      if (reset) state.comments.items = items;
      else state.comments.items = state.comments.items.concat(items);
      renderComments();
    } catch (e) {
      showToast(e.message || "加载评论失败", "error");
    } finally {
      state.comments.loading = false;
      if (loadingEl) loadingEl.style.display = "none";
    }
  }

  async function loadMoreSecond(commentId) {
    const target = state.comments.items.find((c) => c.id === commentId);
    if (!target) return;
    try {
      const { resp, json } = await apiJson(
        `/api/posts/${state.postId}/comments/${commentId}/replies?page=1&size=20`,
        { method: "GET", headers: { Accept: "application/json" } }
      );
      if (!resp.ok || json.code !== 200) throw new Error(json.message || "加载回复失败");
      const list = Array.isArray(json.data) ? json.data : [];
      target.replies = list.map((r) => {
        const thirdPreview = Array.isArray(r.replies) ? r.replies : [];
        return { ...r, replies: thirdPreview };
      });
      renderComments();
    } catch (e) {
      showToast(e.message || "加载回复失败", "error");
    }
  }

  async function loadMoreThird(commentId) {
    const findSecond = () => {
      for (const top of state.comments.items) {
        const seconds = Array.isArray(top.replies) ? top.replies : [];
        for (const sec of seconds) {
          if (sec.id === commentId) return sec;
        }
      }
      return null;
    };
    const second = findSecond();
    if (!second) return;
    try {
      const { resp, json } = await apiJson(
        `/api/posts/${state.postId}/comments/${commentId}/replies?page=1&size=20`,
        { method: "GET", headers: { Accept: "application/json" } }
      );
      if (!resp.ok || json.code !== 200) throw new Error(json.message || "加载回复失败");
      const list = Array.isArray(json.data) ? json.data : [];
      second.replies = list;
      renderComments();
    } catch (e) {
      showToast(e.message || "加载回复失败", "error");
    }
  }

  async function toggleLike(commentId) {
    try {
      const { resp, json } = await apiJson(`/api/comments/${commentId}/like`, {
        method: "POST",
        headers: { Accept: "application/json" }
      });
      if (!resp.ok || json.code !== 200) throw new Error(json.message || "点赞失败");
      const data = json.data || {};
      updateLikeInState(commentId, data.likeCount, data.liked);
      renderComments();
    } catch (e) {
      showToast(e.message || "点赞失败", "error");
    }
  }

  function updateLikeInState(commentId, likeCount, liked) {
    const walk = (list) => {
      for (const c of list) {
        if (c.id === commentId) {
          c.likeCount = likeCount;
          c.liked = liked;
          return true;
        }
        if (Array.isArray(c.replies) && c.replies.length > 0) {
          if (walk(c.replies)) return true;
        }
      }
      return false;
    };
    walk(state.comments.items);
  }

  async function sendComment() {
    const textarea = $("#commentInput");
    if (!textarea || state.composer.sending) return;
    const content = (textarea.value || "").trim();
    if (!content) return;
    if (content.length > 500) {
      showToast("评论最多500字", "info");
      return;
    }

    state.composer.sending = true;
    updateComposerCount();
    $("#sendBtn").textContent = "发送中...";
    try {
      const { resp, json } = await apiJson(`/api/posts/${state.postId}/comments`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ parentId: state.composer.parentId, content })
      });
      if (!resp.ok || json.code !== 200) throw new Error(json.message || "发送失败");
      showToast("已发送", "success");
      textarea.value = "";
      clearComposerReply();
      updateComposerCount();
      state.comments.page = 1;
      await loadComments(true);
    } catch (e) {
      showToast(e.message || "发送失败", "error");
    } finally {
      state.composer.sending = false;
      $("#sendBtn").textContent = "发送";
      updateComposerCount();
    }
  }

  function initComposer() {
    const textarea = $("#commentInput");
    const emojiBtn = $("#emojiBtn");
    const cancelReplyBtn = $("#cancelReplyBtn");
    const sendBtn = $("#sendBtn");
    const mentionBtn = $("#mentionBtn");

    if (textarea) {
      textarea.addEventListener("input", updateComposerCount);
      textarea.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
          e.preventDefault();
          sendComment();
        }
      });
      // 初始化@提及功能
      if (typeof MentionUtils !== 'undefined') {
        MentionUtils.initMentionForTextarea(textarea);
      }
    }
    if (mentionBtn) {
      mentionBtn.addEventListener("click", () => {
        if (textarea && typeof MentionUtils !== 'undefined') {
          var start = textarea.selectionStart;
          var val = textarea.value;
          textarea.value = val.substring(0, start) + '@' + val.substring(start);
          textarea.setSelectionRange(start + 1, start + 1);
          textarea.focus();
          textarea.dispatchEvent(new Event('input', { bubbles: true }));
        }
      });
    }
    if (emojiBtn) {
      emojiBtn.addEventListener("click", () => toggleEmojiPanel());
    }
    if (cancelReplyBtn) {
      cancelReplyBtn.addEventListener("click", () => clearComposerReply());
    }
    if (sendBtn) {
      sendBtn.addEventListener("click", () => sendComment());
    }

    const emojis = ["😀", "😄", "😂", "🥲", "😍", "😎", "🤔", "😭", "👍", "❤️", "🎉", "✨"];
    const grid = $("#emojiGrid");
    if (grid) {
      grid.innerHTML = "";
      emojis.forEach((e) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "emoji";
        btn.textContent = e;
        btn.addEventListener("click", () => insertEmoji(e));
        grid.appendChild(btn);
      });
    }

    updateComposerCount();
  }

  function initCommentEvents() {
    const list = $("#commentList");
    if (!list) return;
    
    list.addEventListener("click", (e) => {
      const btn = e.target.closest("button");
      if (!btn) return;
      const action = btn.getAttribute("data-action");
      const id = Number(btn.getAttribute("data-id") || "0");
      if (!action || !id) return;
      
      if (action === "reply") {
        const name = btn.getAttribute("data-user") || "用户";
        const authorId = btn.getAttribute("data-author-id") || null;
        showInlineReply(id, name, authorId);
      } else if (action === "like") {
        toggleLike(id);
      } else if (action === "more-second") {
        loadMoreSecond(id);
      } else if (action === "collapse-second") {
        const target = state.comments.items.find((c) => c.id === id);
        if (target) {
          target.replies = Array.isArray(target.replies) ? target.replies.slice(0, 0) : [];
          renderComments();
        }
      } else if (action === "more-third") {
        loadMoreThird(id);
      } else if (action === "cancel-inline-reply") {
        hideInlineReply(id);
      } else if (action === "send-inline-reply") {
        sendInlineReply(id);
      } else if (action === "toggle-inline-emoji") {
        toggleInlineEmoji(id);
      }
    });

    // 监听内联回复输入框的输入事件
    list.addEventListener("input", (e) => {
      if (e.target.classList.contains("inline-reply-textarea")) {
        const commentId = e.target.id.replace("inline-reply-textarea-", "");
        updateInlineReplyCount(commentId);
      }
    });

    // 为内联回复textarea初始化@提及功能
    if (typeof MentionUtils !== 'undefined') {
      list.addEventListener('focus', (e) => {
        if (e.target.classList.contains('inline-reply-textarea') && !e.target._mentionInit) {
          MentionUtils.initMentionForTextarea(e.target);
          e.target._mentionInit = true;
        }
      }, { once: false, capture: true });
    }

    // 监听Ctrl+Enter发送
    list.addEventListener("keydown", (e) => {
      if (e.target.classList.contains("inline-reply-textarea")) {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
          e.preventDefault();
          const commentId = e.target.id.replace("inline-reply-textarea-", "");
          sendInlineReply(Number(commentId));
        }
      }
    });
    
    // 监听表情点击事件
    list.addEventListener("click", (e) => {
      const emojiBtn = e.target.closest(".inline-emoji");
      if (emojiBtn) {
        e.stopPropagation();
        const emoji = emojiBtn.getAttribute("data-emoji");
        const replyBox = emojiBtn.closest(".inline-reply-box");
        if (replyBox && emoji) {
          const commentId = replyBox.id.replace("inline-reply-", "");
          insertInlineEmoji(commentId, emoji);
        }
      }
    });
  }

  function initSortButtons() {
    $("#sortTime")?.addEventListener("click", () => setSort("time"));
    $("#sortHot")?.addEventListener("click", () => setSort("hot"));
  }

  async function setSort(sort) {
    if (state.comments.sort === sort) return;
    state.comments.sort = sort;
    $("#sortTime")?.classList.toggle("active", sort === "time");
    $("#sortHot")?.classList.toggle("active", sort === "hot");
    state.comments.page = 1;
    await loadComments(true);
  }

  function initLoadMore() {
    $("#loadMoreBtn")?.addEventListener("click", async () => {
      if (state.comments.loading) return;
      const loaded = state.comments.items.length;
      if (loaded >= state.comments.total) return;
      state.comments.page += 1;
      await loadComments(false);
    });
  }

  function initTopbar() {
    $("#backBtn")?.addEventListener("click", () => history.length > 1 ? history.back() : (window.location.href = "/posts.html"));
  }

  function updateFavoriteButton() {
    const favorited = !!state.post?.favorited;
    
    // 更新左上角收藏按钮
    const favoriteBtnTop = $("#favoriteBtnTop");
    if (favoriteBtnTop) {
      favoriteBtnTop.classList.toggle("favorited", favorited);
      favoriteBtnTop.style.display = "flex";
    }
    
    // 更新中间位置收藏按钮
    const favoriteBtnMiddle = $("#favoriteBtnMiddle");
    if (favoriteBtnMiddle) {
      favoriteBtnMiddle.classList.toggle("favorited", favorited);
      favoriteBtnMiddle.style.display = "flex";
    }
    
    // 显示按钮容器
    const buttonsTop = $("#interactionButtonsTop");
    const buttonsMiddle = $("#interactionButtonsMiddle");
    if (buttonsTop) buttonsTop.style.display = "flex";
    if (buttonsMiddle) buttonsMiddle.style.display = "flex";
  }

  // 更新点赞按钮显示
  function updateLikeButton() {
    if (!state.post) return;
    const liked = !!state.post.liked;
    
    // 更新左上角点赞按钮
    const likeBtnTop = $("#likeBtnTop");
    if (likeBtnTop) {
      likeBtnTop.classList.toggle("liked", liked);
      likeBtnTop.style.display = "flex";
    }
    
    // 更新中间位置点赞按钮
    const likeBtnMiddle = $("#likeBtnMiddle");
    if (likeBtnMiddle) {
      likeBtnMiddle.classList.toggle("liked", liked);
      likeBtnMiddle.style.display = "flex";
    }
    
    // 显示按钮容器
    const buttonsTop = $("#interactionButtonsTop");
    const buttonsMiddle = $("#interactionButtonsMiddle");
    if (buttonsTop) buttonsTop.style.display = "flex";
    if (buttonsMiddle) buttonsMiddle.style.display = "flex";
  }

  async function initLikeButton() {
    if (!state.postId) return;
    await loadCurrentUserId();
    
    // 初始化左上角点赞按钮
    const likeBtnTop = $("#likeBtnTop");
    if (likeBtnTop) {
      updateLikeButton();
      
      likeBtnTop.addEventListener("click", async () => {
        if (!state.currentUserId) {
          showToast("请先登录", "error");
          return;
        }
        try {
          const { resp, json } = await apiJson(`/api/posts/${state.postId}/like`, {
            method: "POST",
            headers: { Accept: "application/json" }
          });
          if (!resp.ok || json.code !== 200) {
            throw new Error(json.message || "操作失败");
          }
          const data = json.data || {};
          state.post.liked = data.liked || false;
          state.post.likeCount = data.likeCount || 0;
          updateLikeButton();
          showToast(state.post.liked ? "点赞成功" : "取消点赞成功", "success");
        } catch (e) {
          showToast(e.message || "操作失败", "error");
        }
      });
    }
    
    // 初始化中间位置点赞按钮
    const likeBtnMiddle = $("#likeBtnMiddle");
    if (likeBtnMiddle) {
      updateLikeButton();
      
      likeBtnMiddle.addEventListener("click", async () => {
        if (!state.currentUserId) {
          showToast("请先登录", "error");
          return;
        }
        try {
          const { resp, json } = await apiJson(`/api/posts/${state.postId}/like`, {
            method: "POST",
            headers: { Accept: "application/json" }
          });
          if (!resp.ok || json.code !== 200) {
            throw new Error(json.message || "操作失败");
          }
          const data = json.data || {};
          state.post.liked = data.liked || false;
          state.post.likeCount = data.likeCount || 0;
          updateLikeButton();
          showToast(state.post.liked ? "点赞成功" : "取消点赞成功", "success");
        } catch (e) {
          showToast(e.message || "操作失败", "error");
        }
      });
    }
  }

  async function initFavoriteButton() {
    if (!state.postId) return;
    await loadCurrentUserId();
    
    // 初始化左上角收藏按钮
    const favoriteBtnTop = $("#favoriteBtnTop");
    if (favoriteBtnTop) {
      updateFavoriteButton();
      
      favoriteBtnTop.addEventListener("click", async () => {
        if (!state.currentUserId) {
          showToast("请先登录", "error");
          return;
        }
        try {
          const favorited = !!state.post?.favorited;
          const { resp, json } = await apiJson(`/api/posts/${state.postId}/favorite`, {
            method: favorited ? "DELETE" : "POST",
            headers: { Accept: "application/json" }
          });
          if (!resp.ok || json.code !== 200) {
            throw new Error(json.message || "操作失败");
          }
          state.post.favorited = !favorited;
          updateFavoriteButton();
          showToast(state.post.favorited ? "收藏成功" : "已取消收藏", "success");
        } catch (e) {
          showToast(e.message || "操作失败", "error");
        }
      });
    }
    
    // 初始化中间位置收藏按钮
    const favoriteBtnMiddle = $("#favoriteBtnMiddle");
    if (favoriteBtnMiddle) {
      updateFavoriteButton();
      
      favoriteBtnMiddle.addEventListener("click", async () => {
        if (!state.currentUserId) {
          showToast("请先登录", "error");
          return;
        }
        try {
          const favorited = !!state.post?.favorited;
          const { resp, json } = await apiJson(`/api/posts/${state.postId}/favorite`, {
            method: favorited ? "DELETE" : "POST",
            headers: { Accept: "application/json" }
          });
          if (!resp.ok || json.code !== 200) {
            throw new Error(json.message || "操作失败");
          }
          state.post.favorited = !favorited;
          updateFavoriteButton();
          showToast(state.post.favorited ? "收藏成功" : "已取消收藏", "success");
        } catch (e) {
          showToast(e.message || "操作失败", "error");
        }
      });
    }
  }

  // 初始化取消匿名按钮（仅作者可见）
  async function initCancelAnonymousButton() {
    const btn = $("#cancelAnonymousBtn");
    if (!btn || !state.postId || !state.post) return;
    const canCancelAnonymous = !!state.post.canCancelAnonymous;
    if (!canCancelAnonymous) {
      btn.style.display = "none";
      return;
    }

    btn.style.display = "block";
    btn.textContent = "取消匿名";
    btn.classList.remove("following");

    btn.addEventListener("click", async () => {
      if (!confirm("确定要取消匿名吗？取消后将公开作者信息。")) return;
      try {
        btn.disabled = true;
        const { resp, json } = await apiJson(`/api/posts/${state.postId}/anonymous/cancel`, {
          method: "POST",
          headers: { Accept: "application/json" }
        });
        if (!resp.ok || json.code !== 200) {
          throw new Error(json.message || "操作失败");
        }
        state.post.isAnonymous = false;
        showToast("已取消匿名", "success");
        // 取消匿名会影响作者显示，刷新页面以获取最新展示数据
        setTimeout(() => window.location.reload(), 600);
      } catch (e) {
        showToast(e.message || "操作失败", "error");
      } finally {
        btn.disabled = false;
      }
    });
  }

  // 初始化关注按钮
  async function initFollowButton() {
    const followBtn = $("#followBtn");
    if (!followBtn || !state.post) return;

    // 匿名帖子不展示关注按钮
    if (state.post.isAnonymous) {
      followBtn.style.display = "none";
      return;
    }

    const authorId = state.post.authorId;
    if (!authorId) {
      followBtn.style.display = "none";
      return;
    }

    await loadCurrentUserId();

    // 不能关注自己：直接不展示按钮
    if (state.currentUserId && Number(authorId) === Number(state.currentUserId)) {
      followBtn.style.display = "none";
      return;
    }

    // 检查当前用户关注状态
    try {
      const { resp, json } = await apiJson(`/api/follow/${authorId}/check`, {
        method: "GET",
        headers: { Accept: "application/json" }
      });

      if (resp.ok && json.code === 200) {
        // 如果返回了数据，说明已登录
        const data = json.data || {};
        const isFollowing = data.following || false;
        
        // 显示关注按钮
        followBtn.style.display = "block";
        updateFollowButton(isFollowing);

        // 绑定点击事件
        followBtn.addEventListener("click", async () => {
          await toggleFollow(authorId);
        });
      } else if (resp.status === 401) {
        // 未登录，显示默认的关注按钮
        followBtn.style.display = "block";
        updateFollowButton(false);
        
        // 点击时提示登录
        followBtn.addEventListener("click", () => {
          showToast("请先登录", "error");
        });
      }
    } catch (e) {
      console.error("检查关注状态失败:", e);
      // 出错时也显示按钮，但点击时提示登录
      followBtn.style.display = "block";
      updateFollowButton(false);
      followBtn.addEventListener("click", () => {
        showToast("请先登录", "error");
      });
    }
  }

  // 更新关注按钮显示
  function updateFollowButton(isFollowing) {
    const followBtn = $("#followBtn");
    if (!followBtn) return;

    if (isFollowing) {
      followBtn.textContent = "已关注";
      followBtn.classList.add("following");
    } else {
      followBtn.textContent = "关注";
      followBtn.classList.remove("following");
    }
  }

  // 切换关注状态
  async function toggleFollow(userId) {
    const followBtn = $("#followBtn");
    if (!followBtn) return;

    try {
      const { resp, json } = await apiJson(`/api/follow/${userId}/toggle`, {
        method: "POST",
        headers: { Accept: "application/json" }
      });

      if (!resp.ok || json.code !== 200) {
        throw new Error(json.message || "操作失败");
      }

      const data = json.data || {};
      const isFollowing = data.following || false;
      
      updateFollowButton(isFollowing);
      showToast(isFollowing ? "关注成功" : "已取消关注", "success");
    } catch (e) {
      showToast(e.message || "操作失败", "error");
    }
  }

  function initViewer() {
    const viewer = $("#imageViewer");
    const img = $("#imageViewerImg");
    if (!viewer || !img) return;

    const clamp = (n, min, max) => Math.max(min, Math.min(max, n));
    const dist = (t1, t2) => {
      const dx = t2.clientX - t1.clientX;
      const dy = t2.clientY - t1.clientY;
      return Math.hypot(dx, dy);
    };

    const apply = () => {
      const s = clamp(state.viewer.scale, 1, 4);
      state.viewer.scale = s;
      if (s === 1) {
        state.viewer.tx = 0;
        state.viewer.ty = 0;
      }
      img.style.transform = `translate(${state.viewer.tx}px, ${state.viewer.ty}px) scale(${s})`;
    };

    const toggleZoom = () => {
      state.viewer.scale = state.viewer.scale > 1 ? 1 : 2.5;
      state.viewer.tx = 0;
      state.viewer.ty = 0;
      apply();
    };

    viewer.addEventListener("click", (e) => {
      if (!state.viewer.open) return;
      if (e.target !== viewer && e.target !== img) return;
      if (state.viewer.scale > 1) {
        state.viewer.scale = 1;
        state.viewer.tx = 0;
        state.viewer.ty = 0;
        apply();
        return;
      }
      closeImageViewer();
    });

    img.addEventListener("dblclick", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!state.viewer.open) return;
      toggleZoom();
    });

    viewer.addEventListener(
      "touchstart",
      (e) => {
        if (!state.viewer.open) return;
        state.viewer.moved = false;
        if (e.touches.length === 1) {
          state.viewer.mode = state.viewer.scale > 1 ? "pan" : "tap";
          state.viewer.startX = e.touches[0].clientX;
          state.viewer.startY = e.touches[0].clientY;
          state.viewer.startTx = state.viewer.tx;
          state.viewer.startTy = state.viewer.ty;
        } else if (e.touches.length === 2) {
          state.viewer.mode = "pinch";
          state.viewer.startDist = dist(e.touches[0], e.touches[1]);
          state.viewer.startScale = state.viewer.scale;
        }
      },
      { passive: true }
    );

    viewer.addEventListener(
      "touchmove",
      (e) => {
        if (!state.viewer.open) return;
        if (state.viewer.mode === "pan" && e.touches.length === 1) {
          const dx = e.touches[0].clientX - state.viewer.startX;
          const dy = e.touches[0].clientY - state.viewer.startY;
          if (Math.abs(dx) > 2 || Math.abs(dy) > 2) state.viewer.moved = true;
          state.viewer.tx = state.viewer.startTx + dx;
          state.viewer.ty = state.viewer.startTy + dy;
          e.preventDefault();
          apply();
          return;
        }
        if (state.viewer.mode === "pinch" && e.touches.length === 2) {
          const d = dist(e.touches[0], e.touches[1]);
          const ratio = state.viewer.startDist ? d / state.viewer.startDist : 1;
          const next = clamp(state.viewer.startScale * ratio, 1, 4);
          if (Math.abs(next - state.viewer.scale) > 0.01) state.viewer.moved = true;
          state.viewer.scale = next;
          e.preventDefault();
          apply();
        }
      },
      { passive: false }
    );

    viewer.addEventListener(
      "touchend",
      () => {
        if (!state.viewer.open) return;
        const now = Date.now();
        const isTap = !state.viewer.moved;

        if (isTap) {
          if (now - state.viewer.lastTapTs <= 280) {
            state.viewer.lastTapTs = 0;
            toggleZoom();
            state.viewer.mode = null;
            return;
          }
          state.viewer.lastTapTs = now;
          if (state.viewer.scale === 1) {
            closeImageViewer();
          } else {
            state.viewer.scale = 1;
            state.viewer.tx = 0;
            state.viewer.ty = 0;
            apply();
          }
        }

        state.viewer.mode = null;
      },
      { passive: true }
    );

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeImageViewer();
    });
  }

  function hidePageSkeleton() {
    $("#pageSkeleton")?.classList.add("hidden");
  }

  function boot() {
    if (!state.post || !state.postId) {
      showToast("帖子不存在", "error");
      return;
    }

    initGestureGuards();
    initTopbar();
    initViewer();
    initSortButtons();
    initComposer();
    initCommentEvents();
    initLoadMore();
    renderPost();
    initLikeButton(); // 初始化点赞按钮
    initFollowButton(); // 初始化关注按钮
    initFavoriteButton();
    initCancelAnonymousButton();
    hidePageSkeleton();
    // 加载@提及用户名映射
    if (typeof MentionUtils !== 'undefined') {
      var uids = MentionUtils.extractMentionUids(state.post?.content || '');
      if (uids.length > 0) {
        MentionUtils.loadUserNames(uids).then(function() {
          renderPost();
        });
      }
    }
    loadComments(true);
  }

  document.addEventListener("DOMContentLoaded", boot);
})();
