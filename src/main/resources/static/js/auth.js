/**
 * 通用认证工具
 */

// 检查登录状态
async function checkAuth() {
    try {
        const response = await fetch('/api/auth/check');
        const data = await response.json();
        
        if (data.code !== 200) {
            // 未登录，跳转到登录页
            window.location.href = '/auth.html';
            return null;
        }
        
        return data.data;
    } catch (error) {
        console.error('检查登录状态失败:', error);
        window.location.href = '/auth.html';
        return null;
    }
}

// 获取当前用户信息
async function getCurrentUser() {
    try {
        const response = await fetch('/api/auth/check');
        const data = await response.json();
        
        if (data.code === 200) {
            return data.data;
        }
        return null;
    } catch (error) {
        console.error('获取用户信息失败:', error);
        return null;
    }
}

// 处理登出
async function handleLogout() {
    if (!confirm('确定要退出登录吗？')) {
        return;
    }
    
    try {
        const response = await fetch('/api/auth/logout', {
            method: 'POST'
        });
        
        const data = await response.json();
        
        if (data.code === 200) {
            window.location.href = '/auth.html';
        }
    } catch (error) {
        alert('登出失败，请重试');
    }
}

// 创建用户信息栏 HTML
function createUserBar() {
    return `
        <div class="user-bar">
            <span id="currentUsername">欢迎，用户</span>
            <button class="btn-logout" onclick="handleLogout()">退出登录</button>
            <a href="/index.html" class="btn-home">返回首页</a>
        </div>
    `;
}

// 初始化用户信息栏
async function initUserBar() {
    const user = await getCurrentUser();
    if (user) {
        const usernameEl = document.getElementById('currentUsername');
        if (usernameEl) {
            usernameEl.textContent = `欢迎，${user.username}`;
        }
    }
}

// 获取当前用户完整信息（包含头像）
async function getCurrentUserFullProfile() {
    try {
        const response = await fetch('/api/profile/me');
        const data = await response.json();
        
        if (data.code === 200) {
            return data.data;
        }
        return null;
    } catch (error) {
        console.error('获取用户完整信息失败:', error);
        return null;
    }
}
