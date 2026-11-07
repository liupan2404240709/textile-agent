import { reactive } from 'vue'
import { setToken, getToken, removeToken, setUserInfo, getUserInfo, removeUserInfo } from '@/utils'

const userStore = reactive({
    token: getToken() || '',
    userInfo: getUserInfo() || {},

    setToken(token) {
        this.token = token
        setToken(token)
    },

    setUserInfo(userInfo) {
        this.userInfo = userInfo
        setUserInfo(userInfo)
    },

    clearUserInfo() {
        this.token = ''
        this.userInfo = {}
        removeToken()
        removeUserInfo()
    }
})

export function useUserStore() {
    return userStore
}
