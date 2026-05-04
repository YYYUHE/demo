let posts = [];
let currentCategory = '';
let currentSearch = '';
let currentUserId = null;
let currentPage = 1;
const pageSize = 10;
let hasMore = false;
let searchTimer = null;

// 页面加载时检查登录状态并获取收藏列表
document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth();
    await initUserBar();
    await initTopUserBar();
    await loadFavorites();
    
    // 初始化返回顶部按钮
    initBackToTop();
    
    // 初始化视图模式
    initViewMode();
    
    // 初始化搜索输入框（添加防抖）
    initSearchInput();
});

// 初始化顶部用户信息栏
async function initTopUserBar() {
    try {
        const response = await fetch('/api/profile/me');
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            const user = result.data;
            currentUserId = user.id;
            
            const avatarTop = document.getElementById('userAvatarTop');
            const placeholderTop = document.getElementById('userAvatarPlaceholderTop');
            
            if (user.avatar && user.avatar.trim() !== '') {
                placeholderTop.style.display = 'none';
                let img = avatarTop.querySelector('img');
                if (!img) {
                    img = document.createElement('img');
                    avatarTop.insertBefore(img, placeholderTop);
                }
                img.src = user.avatar;
                img.alt = user.username;
            } else {
                placeholderTop.style.display = 'flex';
                const firstLetter = user.username ? user.username.charAt(0).toUpperCase() : 'U';
                placeholderTop.textContent = firstLetter;
            }
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
    }
}

// 跳转到个人主页
function goToUserProfile() {
    window.location.href = '/index.html';
}

// 加载收藏的帖子列表
async function loadFavorites(category = '', keyword = '', reset = true) {
    if (reset) {
        currentPage = 1;
        posts = [];
    }
    
    const container = document.getElementById('posts-container');
    const emptyState = document.getElementById('empty-state');
    const loadMoreBtn = document.getElementById('loadMoreBtn');
    
    if (reset) {
        container.innerHTML = '<div class="loading-spinner">加载中...</div>';
    }
    
    try {
        // 构建请求URL - 使用专门的收藏API
        let url = `/api/posts/favorites?page=${currentPage}&size=${pageSize}`;
        
        if (category) {
            url += `&category=${encodeURIComponent(category)}`;
        }
        
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            const newPosts = result.data.content || [];
            
            if (reset) {
                posts = newPosts;
            } else {
                posts = [...posts, ...newPosts];
            }
            
            hasMore = !result.data.last;
            
            renderPosts();
            
            // 显示/隐藏加载更多按钮
            if (hasMore) {
                loadMoreBtn.style.display = 'inline-block';
            } else {
                loadMoreBtn.style.display = 'none';
            }
            
            // 显示/隐藏空状态
            if (posts.length === 0) {
                emptyState.style.display = 'block';
                container.innerHTML = '';
            } else {
                emptyState.style.display = 'none';
            }
        } else {
            showToast(result.message || '加载失败', 'error');
            container.innerHTML = '';
            emptyState.style.display = 'block';
        }
    } catch (error) {
        console.error('加载收藏列表失败:', error);
        showToast('网络错误，请稍后重试', 'error');
        container.innerHTML = '';
        emptyState.style.display = 'block';
    }
}

// 渲染帖子列表
function renderPosts() {
    const container = document.getElementById('posts-container');
    
    if (currentPage === 1) {
        container.innerHTML = '';
    }
    
    posts.forEach(post => {
        const postElement = createPostCard(post);
        container.appendChild(postElement);
    });
}

// 创建帖子卡片
function createPostCard(post) {
    const itemDiv = document.createElement('div');
    itemDiv.className = 'post-item';
    itemDiv.onclick = () => viewPostDetail(post.id);
    
    const cardDiv = document.createElement('div');
    cardDiv.className = 'post-card';
    
    // 封面区域
    const coverContainer = document.createElement('div');
    coverContainer.className = 'post-cover-container';
    
    if (post.coverImage) {
        const img = document.createElement('img');
        img.className = 'post-cover';
        img.src = post.coverImage;
        img.alt = post.title || '帖子封面';
        img.loading = 'lazy';
        coverContainer.appendChild(img);
        
        // 如果有多个图片，显示数量标识
        if (post.imageCount && post.imageCount > 1) {
            const countBadge = document.createElement('div');
            countBadge.className = 'cover-image-count';
            countBadge.textContent = `${post.imageCount} 图`;
            coverContainer.appendChild(countBadge);
        }
    } else {
        // 无图片时使用标题作为封面
        const titleCover = document.createElement('div');
        titleCover.className = 'post-title-cover';
        titleCover.textContent = post.title || '无标题';
        coverContainer.appendChild(titleCover);
    }
    
    cardDiv.appendChild(coverContainer);
    
    // 内容区域
    const contentDiv = document.createElement('div');
    contentDiv.className = 'post-card-content';
    
    // 标题
    const titleDiv = document.createElement('div');
    titleDiv.className = 'post-title';
    titleDiv.textContent = post.title || '无标题';
    contentDiv.appendChild(titleDiv);
    
    // 话题标签
    if (post.topics && post.topics.length > 0) {
        const topicsDiv = document.createElement('div');
        topicsDiv.className = 'post-topics-card';
        
        post.topics.slice(0, 3).forEach(topic => {
            const topicBtn = document.createElement('span');
            topicBtn.className = 'post-topic-card-btn';
            topicBtn.textContent = topic.name || topic;
            topicBtn.onclick = (e) => {
                e.stopPropagation();
                searchByTopic(topic.name || topic);
            };
            topicsDiv.appendChild(topicBtn);
        });
        
        contentDiv.appendChild(topicsDiv);
    }
    
    // 预览文本
    if (post.content) {
        const previewDiv = document.createElement('div');
        previewDiv.className = 'post-preview-text';
        const plainText = post.content.replace(/<[^>]*>/g, '').substring(0, 100);
        previewDiv.textContent = plainText + (post.content.length > 100 ? '...' : '');
        contentDiv.appendChild(previewDiv);
    }
    
    // 底部信息
    const footerDiv = document.createElement('div');
    footerDiv.className = 'post-footer-info';
    
    // 作者信息
    const authorDiv = document.createElement('div');
    authorDiv.className = 'post-author-info';
    
    const avatarDiv = document.createElement('div');
    avatarDiv.className = 'post-author-avatar-small';
    
    if (post.authorAvatar) {
        const avatarImg = document.createElement('img');
        avatarImg.src = post.authorAvatar;
        avatarImg.alt = post.authorUsername || '作者';
        avatarDiv.appendChild(avatarImg);
    } else {
        const avatarPlaceholder = document.createElement('div');
        avatarPlaceholder.className = 'post-author-avatar-placeholder-small';
        const firstLetter = post.authorUsername ? post.authorUsername.charAt(0).toUpperCase() : 'U';
        avatarPlaceholder.textContent = firstLetter;
        avatarDiv.appendChild(avatarPlaceholder);
    }
    
    const authorDetails = document.createElement('div');
    authorDetails.className = 'post-author-details';
    
    const authorName = document.createElement('div');
    authorName.className = 'post-author-name';
    authorName.textContent = post.authorUsername || '匿名用户';
    
    const postTime = document.createElement('div');
    postTime.className = 'post-time';
    postTime.textContent = formatTime(post.createTime);
    
    authorDetails.appendChild(authorName);
    authorDetails.appendChild(postTime);
    
    authorDiv.appendChild(avatarDiv);
    authorDiv.appendChild(authorDetails);
    
    // 统计信息
    const statsDiv = document.createElement('div');
    statsDiv.className = 'post-stats';
    
    // 点赞数
    const likeStat = document.createElement('div');
    likeStat.className = 'post-stat-item';
    likeStat.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
        </svg>
        <span>${post.likeCount || 0}</span>
    `;
    statsDiv.appendChild(likeStat);
    
    footerDiv.appendChild(authorDiv);
    footerDiv.appendChild(statsDiv);
    
    contentDiv.appendChild(footerDiv);
    cardDiv.appendChild(contentDiv);
    itemDiv.appendChild(cardDiv);
    
    return itemDiv;
}

// 按分类筛选
async function filterByCategory(category) {
    currentCategory = category;
    
    // 更新按钮状态
    document.querySelectorAll('.category-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 加载对应分类的收藏
    currentPage = 1;
    await loadFavorites(category, currentSearch, true);
}

// 处理搜索
function handleSearch() {
    performSearch();
}

// 执行搜索
async function performSearch() {
    const searchInput = document.getElementById('searchInput');
    const searchType = document.getElementById('searchType');
    let keyword = searchInput.value.trim();
    
    // 如果是按话题搜索，自动添加#前缀
    if (searchType && searchType.value === 'topic' && keyword && !keyword.startsWith('#')) {
        keyword = '#' + keyword;
    }
    
    currentSearch = keyword;
    
    // 显示/隐藏清除图标
    const clearIcon = document.getElementById('clearSearchIcon');
    if (keyword) {
        clearIcon.classList.add('visible');
    } else {
        clearIcon.classList.remove('visible');
    }
    
    // 重新加载收藏列表
    currentPage = 1;
    await loadFavorites(currentCategory, keyword, true);
}

// 初始化搜索输入框
function initSearchInput() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchIcon');
    
    if (searchInput) {
        // 回车搜索
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
        
        // 输入时防抖搜索
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                performSearch();
            }, 300);
            
            // 显示/隐藏清除按钮
            if (searchInput.value.trim()) {
                clearBtn.classList.add('visible');
            } else {
                clearBtn.classList.remove('visible');
            }
        });
    }
    
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            searchInput.value = '';
            currentSearch = '';
            clearBtn.classList.remove('visible');
            performSearch();
        });
    }
}

// 处理搜索框回车键
function handleSearchKeyPress(event) {
    if (event.key === 'Enter') {
        performSearch();
    }
}

// 清除搜索
function clearSearch() {
    const searchInput = document.getElementById('searchInput');
    const clearIcon = document.getElementById('clearSearchIcon');
    
    if (searchInput) {
        searchInput.value = '';
        currentSearch = '';
        clearIcon.classList.remove('visible');
        performSearch();
    }
}

// 更新搜索框占位符
function updateSearchPlaceholder() {
    const searchType = document.getElementById('searchType');
    const searchInput = document.getElementById('searchInput');
    
    if (searchType && searchInput) {
        if (searchType.value === 'topic') {
            searchInput.placeholder = '搜索话题...';
        } else {
            searchInput.placeholder = '搜索收藏的帖子...';
        }
    }
}

// 按话题搜索
function searchByTopic(topicName) {
    const searchInput = document.getElementById('searchInput');
    searchInput.value = '#' + topicName;
    performSearch();
}

// 查看帖子详情
function viewPostDetail(postId) {
    window.location.href = `/post/${postId}`;
}

// 加载更多
async function loadMorePosts() {
    if (!hasMore) return;
    
    currentPage++;
    const loadMoreBtn = document.getElementById('loadMoreBtn');
    loadMoreBtn.textContent = '加载中...';
    loadMoreBtn.disabled = true;
    
    await loadFavorites(currentCategory, currentSearch, false);
    
    loadMoreBtn.textContent = '加载更多';
    loadMoreBtn.disabled = false;
}

// 格式化时间
function formatTime(timeStr) {
    if (!timeStr) return '';
    
    const now = new Date();
    const time = new Date(timeStr);
    const diff = now - time;
    
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;
    
    if (diff < minute) {
        return '刚刚';
    } else if (diff < hour) {
        return Math.floor(diff / minute) + '分钟前';
    } else if (diff < day) {
        return Math.floor(diff / hour) + '小时前';
    } else if (diff < 7 * day) {
        return Math.floor(diff / day) + '天前';
    } else {
        const year = time.getFullYear();
        const month = String(time.getMonth() + 1).padStart(2, '0');
        const day = String(time.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
}

// 初始化返回顶部按钮
function initBackToTop() {
    const backToTopBtn = document.getElementById('backToTop');
    
    window.addEventListener('scroll', () => {
        if (window.pageYOffset > 300) {
            backToTopBtn.classList.add('visible');
        } else {
            backToTopBtn.classList.remove('visible');
        }
    });
}

// 返回顶部
function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

// 显示Toast提示
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 2000);
}

// 显示关注列表
async function showFollowingList() {
    const modal = document.getElementById('followingModal');
    const listContainer = document.getElementById('followingList');
    
    modal.style.display = 'flex';
    listContainer.innerHTML = '<div class="loading-spinner">加载中...</div>';
    
    try {
        const response = await fetch('/api/follows/my-following');
        const result = await response.json();
        
        if (result.code === 200 && result.data && result.data.length > 0) {
            listContainer.innerHTML = '';
            
            result.data.forEach(user => {
                const userDiv = document.createElement('div');
                userDiv.style.cssText = 'display:flex;align-items:center;gap:12px;padding:12px;border-bottom:1px solid #f0f0f0;';
                
                const avatarDiv = document.createElement('div');
                avatarDiv.style.cssText = 'width:40px;height:40px;border-radius:50%;overflow:hidden;flex-shrink:0;background:#f5f7fa;';
                
                if (user.avatar) {
                    const img = document.createElement('img');
                    img.src = user.avatar;
                    img.alt = user.username;
                    img.style.cssText = 'width:100%;height:100%;object-fit:cover;';
                    avatarDiv.appendChild(img);
                } else {
                    const placeholder = document.createElement('div');
                    placeholder.style.cssText = 'width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;font-size:16px;font-weight:bold;';
                    const firstLetter = user.username ? user.username.charAt(0).toUpperCase() : 'U';
                    placeholder.textContent = firstLetter;
                    avatarDiv.appendChild(placeholder);
                }
                
                const infoDiv = document.createElement('div');
                infoDiv.style.cssText = 'flex:1;min-width:0;';
                
                const nameDiv = document.createElement('div');
                nameDiv.style.cssText = 'font-size:14px;color:#303133;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;';
                nameDiv.textContent = user.username;
                
                const bioDiv = document.createElement('div');
                bioDiv.style.cssText = 'font-size:12px;color:#909399;margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;';
                bioDiv.textContent = user.bio || '这个人很神秘，什么都没有写';
                
                infoDiv.appendChild(nameDiv);
                infoDiv.appendChild(bioDiv);
                
                userDiv.appendChild(avatarDiv);
                userDiv.appendChild(infoDiv);
                
                listContainer.appendChild(userDiv);
            });
        } else {
            listContainer.innerHTML = '<div style="text-align:center;padding:40px;color:#909399;">暂无关注</div>';
        }
    } catch (error) {
        console.error('加载关注列表失败:', error);
        listContainer.innerHTML = '<div style="text-align:center;padding:40px;color:#f56c6c;">加载失败</div>';
    }
}

// 关闭关注列表模态框
function closeFollowingModal(event) {
    if (!event || event.target.id === 'followingModal') {
        const modal = document.getElementById('followingModal');
        modal.style.display = 'none';
    }
}

// ==================== 视图模式切换 ====================

// 初始化视图模式
function initViewMode() {
    const savedViewMode = localStorage.getItem('favorites_view_mode') || 'waterfall';
    applyViewMode(savedViewMode);
}

// 切换视图模式
function toggleViewMode() {
    const currentMode = localStorage.getItem('favorites_view_mode') || 'waterfall';
    const newMode = currentMode === 'waterfall' ? 'compact' : 'waterfall';
    localStorage.setItem('favorites_view_mode', newMode);
    applyViewMode(newMode);
}

// 应用视图模式
function applyViewMode(mode) {
    const postsList = document.getElementById('posts-container');
    const viewToggleBtn = document.getElementById('viewToggleBtn');
    const viewToggleText = document.getElementById('viewToggleText');
    
    if (mode === 'compact') {
        postsList.classList.add('compact-view');
        if (viewToggleBtn) viewToggleBtn.classList.add('active');
        if (viewToggleText) viewToggleText.textContent = '瀑布流';
    } else {
        postsList.classList.remove('compact-view');
        if (viewToggleBtn) viewToggleBtn.classList.remove('active');
        if (viewToggleText) viewToggleText.textContent = '仅展示列表';
    }
}
