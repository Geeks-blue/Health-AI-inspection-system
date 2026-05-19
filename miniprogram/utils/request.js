// 统一封装 wx.request / wx.uploadFile，自动带上鉴权请求头
const app = getApp();

/** 拼接完整 URL */
function fullUrl(path) {
  return app.globalData.baseUrl + path;
}

/** 统一请求头：X-User-Id 与 X-User-Role 与后端约定一致 */
function authHeader() {
  const u = app.globalData.user || {};
  return {
    'X-User-Id': u.id || 'anonymous',
    'X-User-Role': u.role || 'STUDENT'
  };
}

/** 普通 JSON 请求 */
function request({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl(url),
      method,
      data,
      header: { 'content-type': 'application/json', ...authHeader() },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 文件上传（multipart/form-data） */
function uploadFile({ url, filePath, name, formData }) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: fullUrl(url),
      filePath,
      name,
      formData,
      header: authHeader(),
      success: (res) => {
        // 微信返回的 res.data 是字符串，需要手动解析 JSON
        try {
          const body = res.data ? JSON.parse(res.data) : {};
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(body);
          } else {
            reject({ statusCode: res.statusCode, body });
          }
        } catch (e) {
          reject(e);
        }
      },
      fail: reject
    });
  });
}

module.exports = { request, uploadFile };
