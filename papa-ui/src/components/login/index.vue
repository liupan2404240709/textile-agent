<template>
  <div class="login-container">
    <el-alert
        v-show="false"
        :title="getEnvByName('VITE_APP_TITLE')"
        type="success"
        :closable="false"
        style="position: fixed"
    ></el-alert>
    <el-row class="login-row">
      <el-col :xs="24" :sm="24" :md="12" :lg="14" :xl="14">
        <div class="brand-panel">
          <div class="brand-tag">纺织行业 · AI智能体平台</div>
          <h1 class="brand-title">{{ getEnvByName('VITE_APP_TITLE') }}</h1>
          <p class="brand-desc">
            聚焦纺织行业垂直场景，支持文本、图像、语音融合理解，服务面料开发、花型设计、印染工艺、检测标准与设计评审全链路。
          </p>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="10" :xl="10">
        <el-form
            ref="ruleFormRef"
            :model="ruleForm"
            status-icon
            :rules="rules"
            class="login-form"
        >
          <div class="form-header">
            <div class="title">Hello</div>
            <div class="title-tips">
              纺织行业AI智能体登录入口
            </div>
          </div>
          <el-form-item prop="username">
            <el-input
                v-model.trim="ruleForm.username"
                :prefix-icon="User"
                autocomplete="off"
                placeholder="请输入用户名"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model.trim="ruleForm.password"
                type="password"
                show-password
                :prefix-icon="Lock"
                autocomplete="off"
                placeholder="请输入密码"
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <el-row class="captcha-row">
              <el-col :span="15">
                <el-input
                    v-model.trim="ruleForm.captchaCode"
                    autocomplete="off"
                    placeholder="请输入验证码"
                />
              </el-col>
              <el-col :span="8" :offset="1">
                <el-image
                    fit="contain"
                    class="pointer captcha-image"
                    :src="captcha.image"
                    @click="getCaptcha"
                />
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item>
            <el-button
                class="login-btn"
                type="primary"
                size="large"
                :loading="loading"
                @click="submitForm(ruleFormRef)"
            >
              登录
            </el-button>
          </el-form-item>
          <div class="form-footer">
            校内授权账号访问 · 支持验证码安全校验
          </div>
        </el-form>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { getCode, login } from '@/api/login'
import { useUserStore } from '@/store/user'
import { getEnvByName, timeFix } from '@/utils'
import { HOME_URL } from '@/constants'

const router = useRouter()
const route = useRoute()
const ruleFormRef = ref(null)
const userStore = useUserStore()

const ruleForm = reactive({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: '',
})

const loading = ref(false)

const validateUsername = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('用户名不能为空'))
  } else if (value.length < 4) {
    callback(new Error('用户名长度不能小于4位'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('密码不能为空'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能小于6位'))
  } else {
    callback()
  }
}

const validateCaptchaCode = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('验证码不能为空'))
  } else {
    callback()
  }
}

const rules = reactive({
  username: [{ required: true, validator: validateUsername }],
  password: [{ required: true, validator: validatePassword }],
  captchaCode: [{ required: true, validator: validateCaptchaCode }],
})

const captcha = ref({
  image: '',
  key: '',
})

const getCaptcha = async () => {
  try {
    const { data } = await getCode()
    captcha.value = data
    ruleForm.captchaKey = data.key
  } catch (error) {
    console.log('获取验证码失败:', error)
    ElNotification({
      title: '错误',
      message: '获取验证码失败，请重试',
      type: 'error',
    })
  }
}

const submitForm = (formEl) => {
  if (!formEl) return

  formEl.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true

      const { data } = await login(ruleForm)
      
      // 保存 token 到 store 和 localStorage
      userStore.setToken(data)
      
      console.log('登录成功，token 已保存')

      // 注意：/admin/info 接口已移除，如需用户信息可自行实现
      // const userInfo = await getUserInfo()
      // userStore.setUserInfo(userInfo.data)

      ElNotification({
        title: `hi,${timeFix()}!`,
        message: `欢迎回来`,
        type: 'success',
      })

      // 等待 DOM 更新和 token 写入完成
      await nextTick()
      
      // 直接跳转到聊天页面
      console.log('准备跳转到: /chat')
      router.replace('/chat').catch(err => {
        console.error('路由跳转失败:', err)
      })
    } catch (error) {
      console.log('登录失败:', error)
      ElNotification({
        title: '错误',
        message: error.message || '登录失败，请检查用户名和密码',
        type: 'error',
      })
      getCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  getCaptcha()
})
</script>

<style scoped lang="scss">
@import './index';
</style>
