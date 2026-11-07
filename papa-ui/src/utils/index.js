export function getEnvByName(name) {
    // 优先读取真实的环境变量（.env / .env.development / .env.production），
    // 未配置时回退到硬编码默认值，避免出现"改了 env 却不生效"的问题。
    const defaultMap = {
        'VITE_APP_TITLE': '纺织行业大模型'
    }
    const fromEnv = import.meta.env?.[name]
    return fromEnv || defaultMap[name] || ''
}

export function timeFix() {
    const time = new Date()
    const hour = time.getHours()
    if (hour < 9) {
        return '早上好'
    } else if (hour <= 11) {
        return '上午好'
    } else if (hour <= 13) {
        return '中午好'
    } else if (hour <= 20) {
        return '下午好'
    } else {
        return '晚上好'
    }
}

export function setToken(token) {
    localStorage.setItem('token', token)
}

export function getToken() {
    return localStorage.getItem('token')
}

export function removeToken() {
    localStorage.removeItem('token')
}

export function setUserInfo(userInfo) {
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
}

export function getUserInfo() {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
}

export function removeUserInfo() {
    localStorage.removeItem('userInfo')
}
