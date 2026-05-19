// 公共前端工具：登录态、统一 fetch、Toast、导航栏渲染
// 浏览器原生 ES module，无需打包。所有页面通过 <script type="module"> 引入。

const USER_KEY = 'cleaning_user';

/** 读取当前登录用户 { id, role } */
export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
  } catch {
    return null;
  }
}

/** 保存登录态（演示用，生产应使用 JWT） */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/** 清除登录态 */
export function clearUser() {
  localStorage.removeItem(USER_KEY);
}

/** 没登录就跳回登录页 */
export function requireLogin(allowedRoles) {
  const user = getUser();
  if (!user) {
    location.href = 'index.html';
    return null;
  }
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    toast('无权访问该页面', 'error');
    setTimeout(() => (location.href = 'index.html'), 800);
    return null;
  }
  return user;
}

/** 带鉴权头的 JSON 请求 */
export async function api(path, { method = 'GET', body, headers = {} } = {}) {
  const user = getUser() || {};
  const opts = {
    method,
    headers: {
      'X-User-Id': user.id || 'anonymous',
      'X-User-Role': user.role || '',
      ...headers
    }
  };
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const msg = (data && data.error) || res.statusText || '请求失败';
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return data;
}

/** 上传文件（multipart/form-data） */
export async function upload(path, file, formFields = {}) {
  const user = getUser() || {};
  const fd = new FormData();
  fd.append('photo', file);
  for (const [k, v] of Object.entries(formFields)) fd.append(k, v);
  const res = await fetch(path, {
    method: 'POST',
    headers: {
      'X-User-Id': user.id || 'anonymous',
      'X-User-Role': user.role || ''
    },
    body: fd
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error((data && data.error) || res.statusText);
    err.status = res.status;
    throw err;
  }
  return data;
}

/** 浮动提示 */
export function toast(message, kind = '') {
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.textContent = message;
  document.body.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => {
    el.classList.remove('show');
    setTimeout(() => el.remove(), 300);
  }, 2000);
}

/** 渲染顶部导航栏，根据角色显示对应入口 */
export function renderNavbar(activeName) {
  const user = getUser();
  if (!user) return;
  const nav = document.createElement('nav');
  nav.className = 'navbar';
  // 学生可访问 upload，老师额外能访问 review，管理员还能看 stats
  const allLinks = [
    { name: 'upload', label: '上传', href: 'upload.html', roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
    { name: 'review', label: '复核', href: 'review.html', roles: ['TEACHER', 'ADMIN'] },
    { name: 'stats',  label: '统计', href: 'stats.html',  roles: ['ADMIN'] }
  ];
  const links = allLinks
    .filter(l => l.roles.includes(user.role))
    .map(l => `<a href="${l.href}" class="${l.name === activeName ? 'active' : ''}">${l.label}</a>`)
    .join('');
  nav.innerHTML = `
    <div class="brand">教室卫生 AI 巡查</div>
    ${links}
    <div class="spacer"></div>
    <div class="user">${user.id} · ${user.role}</div>
    <button class="logout" id="logout-btn">退出</button>
  `;
  document.body.prepend(nav);
  document.getElementById('logout-btn').onclick = () => {
    clearUser();
    location.href = 'index.html';
  };
}

/** 把 ISO 时间格式化为本地易读字符串 */
export function fmtTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return isNaN(d) ? iso : d.toLocaleString();
}
