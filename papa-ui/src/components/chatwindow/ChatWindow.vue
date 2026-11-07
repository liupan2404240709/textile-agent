<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo-wrap">
          <img src="@/assets/papa.png" alt="" class="logo-img" width="48" height="48" />
        </div>
        <div class="brand-text">
          <span class="logo-title">纺织行业多模态大模型</span>
          <span class="logo-sub">纺织行业 · AI智能体平台</span>
        </div>
      </div>
      <el-button class="new-chat-button" @click="newChat" round>
        <el-icon class="btn-ico"><Plus /></el-icon>
        新会话
      </el-button>

      <div class="history-section">
        <div class="history-header">
          <span class="history-title">历史对话</span>
          <el-button
              class="refresh-btn"
              :icon="Refresh"
              circle
              size="small"
              @click="loadHistory"
              :loading="historyLoading"
          />
        </div>
        <div class="history-list" v-loading="historyLoading">
          <div
              v-for="item in historyList"
              :key="item.memoryId"
              class="history-item"
              :class="{ 'active': item.memoryId === uuid }"
              @click="loadChatHistory(item.memoryId)"
          >
            <div class="history-item-content">
              <div class="history-item-title" :title="getHistoryDisplayTitle(item)">{{ getHistoryDisplayTitle(item) }}</div>
              <div class="history-item-time">{{ formatTime(item.lastUpdateTime) }}</div>
            </div>
            <el-button
                class="delete-btn"
                :icon="Delete"
                circle
                size="small"
                @click.stop="deleteHistory(item.memoryId)"
            />
          </div>
          <el-empty v-if="!historyLoading && historyList.length === 0" description="暂无历史对话" :image-size="60" />
        </div>
      </div>

      <el-button class="logout-button" @click="handleLogout" round>
        <el-icon class="btn-ico"><SwitchButton /></el-icon>
        退出登录
      </el-button>
      <p class="sidebar-hint">面向纺织行业场景：支持面料开发、花型设计、印染工艺、检测标准、设计评审等知识问答、图像分析、语音输入与上下文记忆。</p>
    </aside>

    <div class="main-panel" :class="{ 'has-messages': messages.length > 0 }">
      <header class="chat-header" v-if="messages.length > 0">
        <div class="header-left">
          <h1 class="chat-title">纺织行业智能对话台</h1>
          <p class="chat-desc">可输入面料开发、花型设计、印染工艺、检测标准、设计评审等问题；支持文本、图片与语音多模态交互</p>
        </div>
        <el-button 
          class="rag-link-btn"
          type="primary" 
          plain
          @click="$router.push('/rag')"
        >
          <el-icon><Document /></el-icon>
          <span>RAG 文档管理</span>
        </el-button>
      </header>

      <!-- 欢迎页面 -->
      <div class="welcome-page" v-if="messages.length === 0">
        <div class="welcome-content">
          <div class="welcome-logo">
            <img src="@/assets/images/logo.png" alt="Logo" class="welcome-logo-img" />
          </div>
          <h1 class="welcome-title">你好，我是PaPa</h1>
          <p class="welcome-subtitle">纺织行业多模态智能助手，为你提供专业的纺织面料、花型、印染与检测知识服务</p>
        </div>
      </div>

      <div class="message-list" ref="messageListRef">
        <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['msg-row', message.isUser ? 'is-user' : 'is-assistant']"
        >
          <div class="msg-bubble">
            <div class="msg-content">
              <div v-html="renderMarkdown(message.content)"></div>
              <!-- MCP 图表显示区域 -->
              <div v-if="message.chartUrl" class="generated-image-container">
                <img 
                  :src="message.chartUrl" 
                  alt="MCP生成图表" 
                  class="generated-image"
                  @error="handleImageError(message)"
                  @load="handleImageLoad(message)"
                />
              </div>
              <!-- AI生成图片显示区域 -->
              <div v-if="message.imageUrl" class="generated-image-container">
                <img 
                  :src="message.imageUrl" 
                  alt="AI生成图片" 
                  class="generated-image"
                  @error="handleImageError(message)"
                  @load="handleImageLoad(message)"
                />
              </div>
              <span class="loading-dots" v-if="message.isThinking || message.isTyping">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
            <!-- AI回答的反馈栏 -->
            <div v-if="!message.isUser && !message.isThinking && !message.isTyping" class="message-feedback">
              <el-button class="feedback-btn" @click="copyMessage(message.content)" title="复制">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
              <el-button 
                class="feedback-btn feedback-icon-btn" 
                :class="{ 'is-active': message.feedback === 'like' }"
                @click="handleFeedback(index, 'like')" 
                title="赞"
              >
                <img :src="likeIcon" alt="赞" class="feedback-icon" />
              </el-button>
              <el-button 
                class="feedback-btn feedback-icon-btn" 
                :class="{ 'is-active': message.feedback === 'dislike' }"
                @click="handleFeedback(index, 'dislike')" 
                title="踩"
              >
                <img :src="dislikeIcon" alt="踩" class="feedback-icon" />
              </el-button>
              <el-button class="feedback-btn" @click="regenerateMessage(index)" title="重新生成">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <div ref="bottomAnchorRef" class="scroll-bottom-anchor" aria-hidden="true" />
      </div>

      <footer class="composer" :class="{ 'is-dragging': isDragOver }" ref="composerRef" @dragover="handleDragOver" @dragleave="handleDragLeave" @drop="handleDrop">
        <div class="composer-actions">
          <el-button
              class="generate-image-btn"
              :disabled="isSending || pendingImageFile"
              @click="handleGenerateImage"
              aria-label="AI生图"
              title="AI生图"
          >
            <el-icon><Picture /></el-icon>
            <span>AI生图</span>
          </el-button>
          <el-button
              class="upload-file-btn"
              :disabled="isSending || pendingImageFile"
              @click="triggerFileUpload"
              aria-label="上传文件"
              title="上传 Word、Excel、PDF 等文档"
          >
            <el-icon><Document /></el-icon>
            <span>上传文件</span>
          </el-button>
          <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/plain,text/markdown"
              class="file-input"
              @change="handleFileSelected"
          />
        </div>
        <div v-if="pendingImageFile" class="media-selected-preview">
          <img :src="pendingImagePreviewUrl" :alt="pendingImageName || '已选图片'" class="media-selected-image" />
          <div class="media-selected-meta">
            <p class="media-selected-name">{{ pendingImageName }}</p>
            <p class="media-selected-tip">点击发送后将与文本一起提交解析</p>
          </div>
          <el-button class="media-remove-btn" type="danger" plain size="small" @click="clearPendingImage">
            移除
          </el-button>
        </div>
        <div v-if="pendingFile" class="file-selected-preview" :class="{ 'is-drag-over': isDragOver }">
          <div class="file-icon">
            <img :src="getFileIconSrc(pendingFileName)" alt="文件图标" class="file-icon-img" />
          </div>
          <div class="file-selected-meta">
            <p class="file-selected-name">{{ pendingFileName }}</p>
            <p class="file-selected-size">{{ formatFileSize(pendingFileSize) }}</p>
            <p class="file-selected-status" :class="`status-${fileUploadStatus}`">{{ fileUploadStatus || '请输入问题后点击发送' }}</p>
          </div>
          <el-icon class="file-close-btn" @click="clearPendingFile"><Close /></el-icon>
        </div>
        <div class="composer-card">
          <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 8 }"
              :placeholder="inputPlaceholder"
              resize="none"
              class="composer-input"
              @keydown.enter.exact.prevent="sendMessage"
          />
          <input
              ref="imageInputRef"
              type="file"
              accept="image/*"
              class="image-input"
              @change="handleImageSelected"
          />
          <div class="web-search-switch">
            <span class="switch-label">联网搜素</span>
            <el-switch
                v-model="useWebSearch"
                :disabled="isSending || pendingImageFile"
                inline-prompt
                active-text="开"
                inactive-text="关"
            />
          </div>
          <el-button
              class="audio-btn"
              :class="{ 'is-recording': isRecording }"
              :disabled="isSending || pendingImageFile"
              circle
              @click="toggleSpeechRecognition"
              aria-label="语音输入"
              :title="isRecording ? '点击停止录音' : '点击开始语音输入'"
          >
            <el-icon><Microphone /></el-icon>
          </el-button>
          <el-button
              class="image-btn"
              :disabled="isSending"
              circle
              @click="triggerImageUpload"
              aria-label="上传图片"
          >
            <el-icon><Picture /></el-icon>
          </el-button>
          <el-button
              class="send-btn"
              :class="{ 'is-stop-btn': isSending }"
              :type="isSending ? 'danger' : 'primary'"
              circle
              @click="isSending ? stopStreaming() : sendMessage()"
              :aria-label="isSending ? '停止生成' : '发送'"
          >
            <el-icon v-if="isSending"><VideoPause /></el-icon>
            <el-icon v-else><Top /></el-icon>
          </el-button>
        </div>
        <p class="composer-foot">内容由 AI 生成，请结合实验数据、行业标准与设计规范进行复核。</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted,ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { Plus, Promotion, Picture, Microphone, SwitchButton, Refresh, Delete, Download, VideoPause, Document, Top, CopyDocument, Comment, CircleCloseFilled, RefreshRight, Close } from '@element-plus/icons-vue';
import likeIcon from '@/assets/images/点赞.png';
import dislikeIcon from '@/assets/images/不点赞.png';
import wordIcon from '@/assets/images/word.png';
import excelIcon from '@/assets/images/EXCEL.png';
import pdfIcon from '@/assets/images/Pdf.png';
import textIcon from '@/assets/images/Document.png';
import MarkdownIt from 'markdown-it';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/store/user.js';

const router = useRouter();
const userStore = useUserStore();
const messageListRef = ref(null);
const bottomAnchorRef = ref(null);
const isSending = ref(false);
const uuid = ref();
const inputMessage = ref('');
const messages = ref([]);
const abortController = ref(null);
const imageInputRef = ref();
const pendingImageFile = ref(null);
const pendingImageName = ref('');
const pendingImagePreviewUrl = ref('');
const historyList = ref([]);
const historyLoading = ref(false);
const historyTitleCache = ref({});
const useWebSearch = ref(false);
const inputPlaceholder = computed(() => pendingImageFile.value
    ? '输入图片解析问题（可选），Enter 发送…'
    : '输入纺织行业问题（如面料成分、花型设计、印染工艺、色牢度），Enter 发送…');

// 根据文件类型获取图标图片
const getFileIconSrc = (fileName) => {
  if (!fileName) return textIcon;
  const ext = fileName.toLowerCase().split('.').pop();
  
  const iconMap = {
    // Word
    'doc': wordIcon,
    'docx': wordIcon,
    // Excel
    'xls': excelIcon,
    'xlsx': excelIcon,
    // PDF
    'pdf': pdfIcon,
    // 文本文件
    'txt': textIcon,
    'md': textIcon,
    // 默认
    'default': textIcon
  };
  
  return iconMap[ext] || iconMap['default'];
};
const markdown = new MarkdownIt({
  breaks: true,
  linkify: true,
  html: true,
});

const normalizeMarkdownContent = (content) => String(content || '')
    .replace(/\r\n/g, '\n')
    .replace(/<br\s*\/?>/gi, '<br />');

const wrapTables = (html) => html
    .replace(/<table>/g, '<div class="table-scroll"><table>')
    .replace(/<\/table>/g, '</table></div>');

const renderMarkdown = (content) => wrapTables(markdown.render(normalizeMarkdownContent(content)));

const isRecording = ref(false);
let speechRecognition = null;

let scrollRafId = null;

const stripRagPrefix = (text) => {
  if (typeof text !== 'string' || !text.trim()) {
    return '';
  }

  let cleaned = text;
  const ragPrefixRegex =
      /^\s*[“"']?\s*(Answer using the following information:?|Use the following information to answer:?|请基于以下信息回答[:：]?|请根据以下信息回答[:：]?)/i;

  if (!ragPrefixRegex.test(cleaned)) {
    return cleaned.trim();
  }

  // Remove common RAG scaffold blocks before the real answer.
  cleaned = cleaned
      .replace(/^\s*[“"']?\s*(Answer using the following information:?|Use the following information to answer:?|请基于以下信息回答[:：]?|请根据以下信息回答[:：]?)\s*/i, '')
      .replace(/^\s*(Context|Information|参考信息|检索信息|背景信息)\s*[:：]\s*/i, '')
      .replace(/^\s*[-*]\s*/gm, '')
      .trim();

  // If a clear "final answer" marker exists, keep content after marker.
  const markers = [
    /(?:^|\n)\s*(Final Answer|Answer|回答)\s*[:：]\s*/i,
    /(?:^|\n)\s*(结论|最终回答)\s*[:：]\s*/i,
  ];
  for (const marker of markers) {
    const match = cleaned.match(marker);
    if (match && typeof match.index === 'number') {
      const start = match.index + match[0].length;
      const tail = cleaned.slice(start).trim();
      if (tail) {
        return tail;
      }
    }
  }

  // Fallback: keep text after first blank line if present.
  const splitByBlankLine = cleaned.split(/\n\s*\n/);
  if (splitByBlankLine.length > 1) {
    const tail = splitByBlankLine.slice(1).join('\n\n').trim();
    if (tail) {
      return tail;
    }
  }

  return cleaned.trim();
};

const stripRagAttachmentFromUserMessage = (text) => {
  if (typeof text !== 'string' || !text.trim()) {
    return '';
  }

  const markers = [
    /(?:^|\n)\s*[""']?\s*Answer using the following information:?/i,
    /(?:^|\n)\s*[""']?\s*Use the following information to answer:?/i,
    /(?:^|\n)\s*[""']?\s*请基于以下信息回答[:：]?/i,
    /(?:^|\n)\s*[""']?\s*请根据以下信息回答[:：]?/i,
  ];

  let cutIndex = -1;
  for (const marker of markers) {
    const match = marker.exec(text);
    if (match && typeof match.index === 'number') {
      if (cutIndex === -1 || match.index < cutIndex) {
        cutIndex = match.index;
      }
    }
  }

  if (cutIndex === -1) {
    return text.trim();
  }

  const kept = text.slice(0, cutIndex).trim();
  return kept || text.trim();
};

// 清理文件上传消息，只保留文件基本信息，移除文件内容
const stripFileContentFromUserMessage = (text) => {
  if (typeof text !== 'string' || !text.trim()) {
    return '';
  }

  // 检测是否是文件上传消息
  if (!text.includes('用户上传了一个文件') && !text.includes('【文件信息】')) {
    return text;
  }

  // 提取文件信息部分（文件名和大小）
  const fileInfoMatch = text.match(/【文件信息】([\s\S]*?)(?=【文件内容】|$)/i);
  
  if (fileInfoMatch) {
    // 只返回文件信息部分
    const fileInfo = fileInfoMatch[1].trim();
    return `用户上传了一个文件，请分析并回答用户的问题：\n【文件信息】\n${fileInfo}`;
  }

  return text;
};

function applyScrollToBottom(behavior = 'auto') {
  const list = messageListRef.value;
  const anchor = bottomAnchorRef.value;
  if (anchor) {
    anchor.scrollIntoView({ block: 'end', behavior });
  } else if (list) {
    list.scrollTo({ top: list.scrollHeight, behavior });
  }
}

function scrollToBottom() {
  nextTick(() => {
    requestAnimationFrame(() => applyScrollToBottom('smooth'));
  });
}

function scheduleScrollToBottom() {
  if (scrollRafId != null) {
    return;
  }
  scrollRafId = requestAnimationFrame(() => {
    scrollRafId = null;
    nextTick(() => applyScrollToBottom('auto'));
  });
}

watch(
    messages,
    () => {
      scheduleScrollToBottom();
    },
    { deep: true, flush: 'post' },
);

onMounted(() => {
  initUUID();
  // hello();  // 已禁用自动发送问候语
  initSpeechRecognition();
  registerPasteHandler();
  loadHistory();
});

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  if (year === now.getFullYear()) {
    return `${month}-${day} ${hours}:${minutes}`;
  }
  return `${year}-${month}-${day}`;
};

const loadHistory = async () => {
  historyLoading.value = true;
  try {
    const response = await axios.get('/api/papa/history');
    if (response.data.code === 200) {
      historyList.value = response.data.data || [];
      enrichHistoryTitles(historyList.value);
    }
  } catch (error) {
    console.error('加载历史对话失败:', error);
    ElMessage.error('加载历史对话失败');
  } finally {
    historyLoading.value = false;
  }
};

const HISTORY_PLACEHOLDER_TITLE = '新对话';
const HISTORY_TITLE_MAX_LENGTH = 30;
const HISTORY_GREETING_REGEX = /^(你好|您好|hi|hello)\s*[!！?？。.]?$/i;

const shortenHistoryTitle = (text) => {
  const compact = String(text || '').replace(/\s+/g, ' ').trim();
  if (!compact) return HISTORY_PLACEHOLDER_TITLE;
  return compact.length > HISTORY_TITLE_MAX_LENGTH
      ? `${compact.slice(0, HISTORY_TITLE_MAX_LENGTH)}...`
      : compact;
};

const isGreetingMessage = (text) => {
  const compact = String(text || '').replace(/\s+/g, ' ').trim();
  if (!compact) return false;
  return HISTORY_GREETING_REGEX.test(compact);
};

const getHistoryDisplayTitle = (item) => {
  const cached = historyTitleCache.value[item.memoryId];
  if (cached) return cached;
  const firstMessage = String(item?.firstMessage || '').trim();
  if (isGreetingMessage(firstMessage)) {
    return HISTORY_PLACEHOLDER_TITLE;
  }
  const serverTitle = shortenHistoryTitle(firstMessage);
  return serverTitle || HISTORY_PLACEHOLDER_TITLE;
};

const shouldEnrichHistoryTitle = (item) => {
  const raw = String(item?.firstMessage || '').trim();
  return !raw || raw === HISTORY_PLACEHOLDER_TITLE || isGreetingMessage(raw);
};

const buildHistoryTitleFromMessages = (chatMessages) => {
  if (!Array.isArray(chatMessages)) return '';
  const mapped = mapHistoryMessages(chatMessages);
  const meaningfulMessages = mapped.filter((msg) => {
    const content = String(msg?.content || '').trim();
    return content && !isGreetingMessage(content);
  });
  const firstMeaningfulUser = meaningfulMessages.find((msg) => msg.isUser);
  if (firstMeaningfulUser) return shortenHistoryTitle(firstMeaningfulUser.content);
  const firstMeaningfulAssistant = meaningfulMessages.find((msg) => !msg.isUser);
  if (firstMeaningfulAssistant) return shortenHistoryTitle(firstMeaningfulAssistant.content);
  const firstUser = mapped.find((msg) => msg.isUser && msg.content?.trim());
  if (firstUser) return shortenHistoryTitle(firstUser.content);
  const firstAssistant = mapped.find((msg) => !msg.isUser && msg.content?.trim());
  if (firstAssistant) return shortenHistoryTitle(firstAssistant.content);
  return '';
};

const enrichHistoryTitles = async (list) => {
  if (!Array.isArray(list) || list.length === 0) return;
  const needEnrich = list
      .filter((item) => shouldEnrichHistoryTitle(item))
      .filter((item) => !historyTitleCache.value[item.memoryId])
      .slice(0, 20);
  if (needEnrich.length === 0) return;

  const settled = await Promise.allSettled(
      needEnrich.map(async (item) => {
        const resp = await axios.get(`/api/papa/history/${item.memoryId}/content`);
        if (resp?.data?.code !== 200 || !resp?.data?.data) return;
        const chatMessages = JSON.parse(resp.data.data);
        const derivedTitle = buildHistoryTitleFromMessages(chatMessages);
        if (derivedTitle) {
          historyTitleCache.value[item.memoryId] = derivedTitle;
        }
      })
  );

  const hasUpdate = settled.some((r) => r.status === 'fulfilled');
  if (hasUpdate) {
    historyList.value = [...historyList.value];
  }
};

const extractMessageText = (msg) => {
  if (!msg || typeof msg !== 'object') {
    return '';
  }

  if (typeof msg.singleText === 'string') {
    return msg.singleText;
  }

  if (typeof msg.text === 'string') {
    return msg.text;
  }

  if (typeof msg.content === 'string') {
    return msg.content;
  }

  if (Array.isArray(msg.contents)) {
    return msg.contents
        .map((item) => {
          if (typeof item === 'string') return item;
          if (!item || typeof item !== 'object') return '';
          if (typeof item.text === 'string') return item.text;
          if (typeof item.content === 'string') return item.content;
          if (typeof item.value === 'string') return item.value;
          return '';
        })
        .filter(Boolean)
        .join('\n')
        .trim();
  }

  if (Array.isArray(msg.content)) {
    return msg.content
        .map((item) => {
          if (typeof item === 'string') return item;
          if (item && typeof item.text === 'string') return item.text;
          if (item && typeof item.content === 'string') return item.content;
          return '';
        })
        .filter(Boolean)
        .join('\n')
        .trim();
  }

  return '';
};

const normalizeRole = (msg) => {
  const rawRole = String(msg?.type || msg?.role || msg?.messageType || msg?.kind || '').toUpperCase();

  if (rawRole.includes('SYSTEM')) return 'SYSTEM';
  if (rawRole.includes('USER') || rawRole.includes('HUMAN')) return 'USER';
  if (rawRole.includes('AI') || rawRole.includes('ASSISTANT')) return 'ASSISTANT';
  if (rawRole.includes('TOOL')) return 'TOOL';

  return 'UNKNOWN';
};

const mapHistoryMessages = (chatMessages) => {
  if (!Array.isArray(chatMessages)) {
    return [];
  }

  return chatMessages
      .map((msg) => {
        const role = normalizeRole(msg);
        const text = extractMessageText(msg).trim();
        if (!text || role === 'SYSTEM' || role === 'TOOL' || role === 'UNKNOWN') {
          return null;
        }
        let displayText = role === 'USER'
            ? stripRagAttachmentFromUserMessage(text)
            : stripRagPrefix(text);
        
        // 对于用户消息，进一步清理文件内容
        if (role === 'USER') {
          displayText = stripFileContentFromUserMessage(displayText);
        }
        
        return {
          isUser: role === 'USER',
          content: displayText || text,
          isTyping: false,
          isThinking: false,
        };
      })
      .filter(Boolean);
};

const loadChatHistory = async (memoryId) => {
  if (memoryId === uuid.value) {
    ElMessage.info('当前已在此对话中');
    return;
  }

  try {
    const response = await axios.get(`/api/papa/history/${memoryId}/content`);
    if (response.data.code === 200 && response.data.data) {
      const chatMessages = JSON.parse(response.data.data);
      messages.value = mapHistoryMessages(chatMessages);

      uuid.value = memoryId;
      localStorage.setItem('user_uuid', memoryId);

      if (messages.value.length === 0) {
        ElMessage.warning('该对话没有可展示的用户/助手消息');
        return;
      }

      ElMessage.success('已加载历史对话');
      scrollToBottom();
    } else {
      ElMessage.warning('该对话暂无历史记录');
    }
  } catch (error) {
    console.error('加载历史对话失败:', error);
    ElMessage.error('加载历史对话失败');
  }
};


const deleteHistory = async (memoryId) => {
  try {
    await ElMessageBox.confirm(
        '确定要删除这条历史对话吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    );

    await axios.delete(`/api/papa/history/${memoryId}`);
    ElMessage.success('删除成功');
    loadHistory();

    if (memoryId === uuid.value) {
      newChat();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除历史对话失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

// ... existing code ...


const registerPasteHandler = () => {
  window.addEventListener('paste', handlePaste);
};

const clearPendingImage = () => {
  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value);
  }
  pendingImageFile.value = null;
  pendingImageName.value = '';
  pendingImagePreviewUrl.value = '';
  if (imageInputRef.value) {
    imageInputRef.value.value = '';
  }
};

const setPendingImage = (file, name) => {
  clearPendingImage();
  pendingImageFile.value = file;
  pendingImageName.value = name || file.name;
  pendingImagePreviewUrl.value = URL.createObjectURL(file);
};

const handlePaste = (event) => {
  const items = event.clipboardData?.items;
  if (!items) {
    return;
  }

  for (let i = 0; i < items.length; i++) {
    const item = items[i];

    if (item.kind === 'file' && item.type.startsWith('image/')) {
      event.preventDefault();

      const file = item.getAsFile();
      if (!file) {
        ElMessage.warning('无法读取剪贴板图片');
        return;
      }

      if (isSending.value) {
        ElMessage.warning('请等待当前请求完成');
        return;
      }

      const pastedName = `剪贴板图片_${new Date().getTime()}.png`;
      setPendingImage(file, pastedName);

      ElMessage.success({
        message: '📋 已从剪贴板粘贴图片，点击发送即可解析',
        duration: 2500
      });

      break;
    }
  }
};

// 拖拽相关
const isDragOver = ref(false);
const composerRef = ref(null);

const handleDragOver = (event) => {
  event.preventDefault();
  event.stopPropagation();
  if (!isSending.value && !pendingImageFile.value) {
    isDragOver.value = true;
  }
};

const handleDragLeave = (event) => {
  event.preventDefault();
  event.stopPropagation();
  isDragOver.value = false;
};

const handleDrop = (event) => {
  event.preventDefault();
  event.stopPropagation();
  isDragOver.value = false;
  
  if (isSending.value || pendingImageFile.value) {
    ElMessage.warning('请等待当前请求完成');
    return;
  }
  
  const files = event.dataTransfer?.files;
  if (!files || files.length === 0) {
    return;
  }
  
  // 处理第一个文件
  const file = files[0];
  processFile(file);
};

const hello = () => {
  sendRequest('你好');
};

const sendMessage = () => {
  const messageText = inputMessage.value.trim();
  
  // 优先处理图片
  if (pendingImageFile.value) {
    sendImageRequest(messageText);
    return;
  }
  
  // 处理文件上传
  if (pendingFile.value) {
    sendFileRequest(messageText);
    return;
  }
  
  // 普通文本对话
  if (messageText) {
    sendRequest(messageText);
    inputMessage.value = '';
  }
};

const sendRequest = (message) => {
  isSending.value = true;
  const userMsg = {
    isUser: true,
    content: message,
    isTyping: false,
    isThinking: false,
  };
  messages.value.push(userMsg);

  const botMsg = {
    isUser: false,
    content: '',
    rawContent: '',
    isTyping: true,
    isThinking: false,
  };
  messages.value.push(botMsg);
  const lastMsg = messages.value[messages.value.length - 1];
  scrollToBottom();

  // 创建 AbortController 用于取消请求
  abortController.value = new AbortController();

  // DEBUG: 打印 webSearchEnabled 值
  console.log('🌐 webSearchEnabled:', useWebSearch.value, 'type:', typeof useWebSearch.value);
  console.log('📦 发送数据:', { memoryId: uuid.value, message, webSearchEnabled: useWebSearch.value });

  axios
      .post('/api/papa/chat', {
        memoryId: uuid.value,
        message,
        webSearchEnabled: useWebSearch.value,
      }, {
        responseType: 'stream',
        signal: abortController.value.signal,
        onDownloadProgress: (e) => {
          const fullText = e.event.target.responseText;
          const currentRaw = lastMsg.rawContent || '';
          const newText = fullText.substring(currentRaw.length);
          lastMsg.rawContent = currentRaw + newText;
          lastMsg.content = stripRagPrefix(lastMsg.rawContent);
          
          // 检测并提取 MCP 工具生成的图表 URL
          // 匹配格式：https://开头的图片链接
          const imageUrlMatch = lastMsg.content.match(/(https?:\/\/[^\s"'<>]+\.(?:png|jpg|jpeg|gif|webp|svg)|https?:\/\/mdn\.alipayobjects\.com\/[^\s"'<>]+)/i);
          if (imageUrlMatch && !lastMsg.chartUrl) {
            lastMsg.chartUrl = imageUrlMatch[0];
            console.log('✅ 检测到MCP图表URL:', lastMsg.chartUrl);
            
            // 从 Markdown 内容中移除图片标签，避免重复显示
            // 移除 ![alt](url) 格式的图片
            lastMsg.content = lastMsg.content.replace(/!\[.*?\]\(.*?\)/g, '');
            // 移除单独的 URL（如果 AI 直接返回了 URL）
            lastMsg.content = lastMsg.content.replace(/https?:\/\/[^\s"'<>]+\.(?:png|jpg|jpeg|gif|webp|svg)/gi, '');
            lastMsg.content = lastMsg.content.replace(/https?:\/\/mdn\.alipayobjects\.com\/[^\s"'<>]+/gi, '');
            // 清理多余的空行
            lastMsg.content = lastMsg.content.replace(/\n{3,}/g, '\n\n');
          }
          
          scheduleScrollToBottom();
        },
      })
      .then(() => {
        messages.value.at(-1).isTyping = false;
        isSending.value = false;
        abortController.value = null;
        scheduleScrollToBottom();
      })
      .catch((error) => {
        // 如果是用户主动取消，不显示错误
        if (axios.isCancel(error)) {
          console.log('用户已停止流式输出');
          messages.value.at(-1).content += '\n\n*[已停止]*';
          messages.value.at(-1).isTyping = false;
          isSending.value = false;
          abortController.value = null;
          return;
        }
        console.error('流式错误:', error);
        messages.value.at(-1).content = '请求失败，请重试';
        messages.value.at(-1).isTyping = false;
        isSending.value = false;
        abortController.value = null;
        scheduleScrollToBottom();
      });
};

const stopStreaming = () => {
  if (abortController.value) {
    abortController.value.abort();
    abortController.value = null;
    isSending.value = false;
    ElMessage.info('已停止生成');
  }
};

// 复制消息内容
const copyMessage = async (content) => {
  try {
    // 移除 Markdown 标记的纯文本内容
    const plainText = content
      .replace(/\*\*(.+?)\*\*/g, '$1') // 粗体
      .replace(/\*(.+?)\*/g, '$1') // 斜体
      .replace(/`{3}[\s\S]*?`{3}/g, '') // 代码块
      .replace(/`(.+?)`/g, '$1') // 行内代码
      .replace(/#{1,6}\s/g, '') // 标题
      .replace(/\[(.+?)\]\(.+?\)/g, '$1') // 链接
      .replace(/![.*?]\(.*?\)/g, '') // 图片
      .replace(/^\s*[-*+]\s/gm, '') // 列表
      .replace(/^\s*\d+\.\s/gm, '') // 有序列表
      .trim();
    
    await navigator.clipboard.writeText(plainText || content);
    ElMessage.success('已复制到剪贴板');
  } catch (error) {
    console.error('复制失败:', error);
    ElMessage.error('复制失败，请手动复制');
  }
};

// 处理点赞/点踩
const handleFeedback = (index, type) => {
  const message = messages.value[index];
  if (!message) return;
  
  // 切换反馈状态
  if (message.feedback === type) {
    message.feedback = null; // 取消反馈
  } else {
    message.feedback = type; // 设置反馈
  }
  
  // TODO: 调用后端 API 记录反馈
  console.log(`消息反馈: index=${index}, type=${type}`);
};

// 重新生成消息
const regenerateMessage = (index) => {
  if (isSending.value) {
    ElMessage.warning('请等待当前生成完成');
    return;
  }
  
  // 找到对应的用户问题
  let userQuestionIndex = -1;
  for (let i = index - 1; i >= 0; i--) {
    if (messages.value[i].isUser) {
      userQuestionIndex = i;
      break;
    }
  }
  
  if (userQuestionIndex === -1) {
    ElMessage.warning('未找到对应的用户问题');
    return;
  }
  
  const userQuestion = messages.value[userQuestionIndex].content;
  
  // 删除当前 AI 回答
  messages.value.splice(index, 1);
  
  // 重新发送请求
  sendRequest(userQuestion);
};

const sendGenerateImageRequest = async (promptText) => {
  isSending.value = true;

  messages.value.push({
    isUser: true,
    content: `【AI生图】${promptText}`,
    isTyping: false,
    isThinking: false,
  });
  messages.value.push({
    isUser: false,
    content: '正在根据提示词生成图片，请稍候...',
    isTyping: true,
    isThinking: false,
  });
  scrollToBottom();

  try {
    const resp = await axios.post('/api/papa/generateImage', {
      prompt: promptText,
      size: '1024*1024',
    });

    const lastMsg = messages.value.at(-1);
    if (resp?.data?.code !== 200) {
      lastMsg.content = resp?.data?.message || '图片生成失败，请重试';
      lastMsg.isTyping = false;
      return;
    }

    const data = resp.data.data || {};
    if (data.imageUrl) {
      lastMsg.content = `图片生成完成（尺寸：${data.size || '1024*1024'}）`;
      lastMsg.imageUrl = data.imageUrl;
      lastMsg.imagePrompt = promptText;
    } else if (data.taskId) {
      lastMsg.content = `图片生成任务已提交成功。\n\n- 任务ID：${data.taskId}\n- 状态：${data.taskStatus || 'UNKNOWN'}\n\n当前接口尚未拿到最终图片地址，可后续补充任务查询接口。`;
    } else {
      lastMsg.content = '图片生成请求已完成，但暂未返回图片地址';
    }
    lastMsg.isTyping = false;
  } catch (error) {
    console.error('图片生成错误:', error);
    const lastMsg = messages.value.at(-1);
    lastMsg.content = '图片生成失败，请重试';
    lastMsg.isTyping = false;
  } finally {
    isSending.value = false;
    scrollToBottom();
  }
};

const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

  if (!SpeechRecognition) {
    console.warn('当前浏览器不支持语音识别功能');
    return;
  }

  speechRecognition = new SpeechRecognition();
  speechRecognition.lang = 'zh-CN';
  speechRecognition.continuous = false;
  speechRecognition.interimResults = false;
  speechRecognition.maxAlternatives = 1;

  speechRecognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    const confidence = event.results[0][0].confidence;

    isRecording.value = false;
    ElMessage.success({
      message: `✅ 识别成功（准确度: ${(confidence * 100).toFixed(0)}%）`,
      duration: 2000
    });

    if (transcript && transcript.trim()) {
      setTimeout(() => {
        sendRequest(transcript.trim());
      }, 300);
    }
  };

  speechRecognition.onerror = (event) => {
    isRecording.value = false;
    console.error('语音识别错误:', event.error);

    let errorMsg = '语音识别失败';
    switch(event.error) {
      case 'no-speech':
        errorMsg = '未检测到语音，请重试';
        break;
      case 'audio-capture':
        errorMsg = '无法访问麦克风，请检查设备';
        break;
      case 'not-allowed':
        errorMsg = '麦克风权限被拒绝，请在浏览器设置中允许';
        break;
      case 'network':
        errorMsg = '网络错误，请检查网络连接';
        break;
      case 'aborted':
        errorMsg = '录音已取消';
        break;
      default:
        errorMsg = `识别失败: ${event.error}`;
    }
    ElMessage.error(errorMsg);
  };

  speechRecognition.onend = () => {
    isRecording.value = false;
  };
};

const toggleSpeechRecognition = () => {
  if (!speechRecognition) {
    ElMessage.warning('您的浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器');
    return;
  }

  if (isRecording.value) {
    speechRecognition.stop();
    isRecording.value = false;
    ElMessage.info('录音已停止');
  } else {
    try {
      speechRecognition.start();
      isRecording.value = true;
      ElMessage.info({
        message: '🎤 正在聆听...请说话',
        duration: 3000
      });
    } catch (error) {
      console.error('启动语音识别失败:', error);
      ElMessage.error('启动语音识别失败，请重试');
      isRecording.value = false;
    }
  }
};

// 图片加载成功处理
const handleImageLoad = (message) => {
  console.log('✅ 图片加载成功:', message.imageUrl);
  message.imageLoadError = false;
};

// 图片加载错误处理
const handleImageError = (message) => {
  console.error('❌ 图片加载失败:', message.imageUrl);
  message.imageLoadError = true;
  ElMessage.warning('图片加载失败，请尝试右键点击图片选择“图片另存为”');
};

const handleGenerateImage = () => {
  const promptText = inputMessage.value.trim();
  if (!promptText) {
    ElMessage.warning('请输入图片描述提示词');
    return;
  }
  if (pendingImageFile.value) {
    ElMessage.warning('生图功能不支持上传参考图片，请先移除图片');
    return;
  }
  sendGenerateImageRequest(promptText);
  inputMessage.value = '';
};

const downloadImage = async (imageUrl, prompt) => {
  try {
    ElMessage.info('正在准备下载...');
    
    // 方法1：尝试使用fetch获取blob（处理跨域）
    let blob;
    try {
      const response = await fetch(imageUrl, {
        mode: 'cors',
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      blob = await response.blob();
    } catch (fetchError) {
      console.warn('fetch方式失败，尝试直接下载:', fetchError);
      
      // 方法2：直接使用a标签下载（适用于同域或允许跨域的资源）
      const link = document.createElement('a');
      link.href = imageUrl;
      link.target = '_blank';
      
      const timestamp = new Date().getTime();
      const safePrompt = prompt ? prompt.substring(0, 20).replace(/[\\/:*?"<>|]/g, '_') : 'AI生成图片';
      link.download = `${safePrompt}_${timestamp}.png`;
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      ElMessage.success('图片已开始下载！');
      return;
    }
    
    // 创建blob URL并下载
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    
    const timestamp = new Date().getTime();
    const safePrompt = prompt ? prompt.substring(0, 20).replace(/[\\/:*?"<>|]/g, '_') : 'AI生成图片';
    link.download = `${safePrompt}_${timestamp}.png`;
    
    document.body.appendChild(link);
    link.click();
    
    // 清理
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);
    
    ElMessage.success('图片下载成功！');
  } catch (error) {
    console.error('下载图片失败:', error);
    
    // 最后的降级方案：在新标签页打开图片，让用户手动保存
    try {
      window.open(imageUrl, '_blank');
      ElMessage.warning({
        message: '已在新标签页打开图片，请右键选择“图片另存为”',
        duration: 5000,
        showClose: true,
      });
    } catch (openError) {
      ElMessage.error({
        message: '下载失败，请右键点击图片选择“图片另存为”',
        duration: 5000,
        showClose: true,
      });
    }
  }
};

const triggerImageUpload = () => {
  if (!isSending.value) {
    imageInputRef.value?.click();
  }
};

const handleImageSelected = (event) => {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持上传图片文件');
    return;
  }
  setPendingImage(file, file.name);
};

const sendImageRequest = async (messageText) => {
  const file = pendingImageFile.value;
  if (!file) {
    return;
  }
  isSending.value = true;
  const promptText = messageText || '请从纺织专业角度解析这张图片，重点识别面料材质与组织结构（机织/针织/非织造）、图案花型与色彩、印染与后整理工艺、款式与版型特征、应用场景（服装/家纺/产业用纺织品）';
  const previewUrl = URL.createObjectURL(file);

  messages.value.push({
    isUser: true,
    content: `${promptText}\n\n![用户上传图片](${previewUrl})`,
    isTyping: false,
    isThinking: false,
  });
  messages.value.push({
    isUser: false,
    content: '',
    isTyping: true,
    isThinking: false,
  });
  scrollToBottom();

  const formData = new FormData();
  formData.append('image', file);
  if (messageText) {
    formData.append('message', messageText);
  }

  try {
    const resp = await axios.post('/api/papa/readImageContent', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    const lastMsg = messages.value.at(-1);
    lastMsg.content = resp.data || '图片解析完成，但未返回内容';
    lastMsg.isTyping = false;
  } catch (error) {
    console.error('图片解析错误:', error);
    const lastMsg = messages.value.at(-1);
    lastMsg.content = '图片解析失败，请重试';
    lastMsg.isTyping = false;
  } finally {
    isSending.value = false;
    inputMessage.value = '';
    clearPendingImage();
    URL.revokeObjectURL(previewUrl);
    scrollToBottom();
  }
};

// ========== 文件上传相关 ==========
const fileInputRef = ref(null);
const pendingFile = ref(null);
const pendingFileName = ref('');
const pendingFileSize = ref(0);
const fileUploadStatus = ref(''); // 上传中, 解析中, 已完成解析

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const clearPendingFile = () => {
  pendingFile.value = null;
  pendingFileName.value = '';
  pendingFileSize.value = 0;
  fileUploadStatus.value = '';
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const triggerFileUpload = () => {
  if (!isSending.value && !pendingImageFile.value) {
    fileInputRef.value?.click();
  }
};

const handleFileSelected = (event) => {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }
  processFile(file);
};

// 处理文件（验证和设置）
const processFile = (file) => {
  // 验证文件类型
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain',
    'text/markdown'
  ];
  
  const fileName = file.name.toLowerCase();
  const allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt', '.md'];
  const hasValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext));
  
  if (!hasValidExtension && !allowedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 Word、Excel、PDF、TXT、Markdown 格式的文件');
    return;
  }
  
  // 验证文件大小（最大 50MB）
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB');
    return;
  }
  
  pendingFile.value = file;
  pendingFileName.value = file.name;
  pendingFileSize.value = file.size;
  fileUploadStatus.value = '已选择文件';
  
  ElMessage.success({
    message: `📄 已选择文件：${file.name}，请输入问题后点击发送`,
    duration: 3000
  });
};

const sendFileRequest = async (messageText) => {
  const file = pendingFile.value;
  if (!file) {
    return;
  }
  
  isSending.value = true;
  fileUploadStatus.value = '上传中';
  const userQuestion = messageText || '请分析这个文件的内容';
  
  // 显示用户消息
  messages.value.push({
    isUser: true,
    content: `📄 **上传文件**: ${file.name}\n\n**问题**: ${userQuestion}`,
    isTyping: false,
    isThinking: false,
  });
  
  // 创建 AI 回复占位
  messages.value.push({
    isUser: false,
    content: '',
    isTyping: true,
    isThinking: false,
  });
  
  scrollToBottom();

  const formData = new FormData();
  formData.append('file', file);
  formData.append('message', userQuestion);
  formData.append('memoryId', uuid.value);
  formData.append('webSearchEnabled', useWebSearch.value);

  try {
    fileUploadStatus.value = '解析中';
    // 使用 axios 发送请求，自动携带 token
    const response = await axios.post('/api/papa/uploadFile', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      responseType: 'stream',
      onDownloadProgress: (e) => {
        const fullText = e.event.target.responseText;
        const lastMsg = messages.value.at(-1);
        const currentRaw = lastMsg.rawContent || '';
        const newText = fullText.substring(currentRaw.length);
        lastMsg.rawContent = currentRaw + newText;
        lastMsg.content = stripRagPrefix(lastMsg.rawContent);
        scheduleScrollToBottom();
      },
    });

    fileUploadStatus.value = '已完成解析';
    const lastMsg = messages.value.at(-1);
    lastMsg.isTyping = false;
    
    if (!lastMsg.content.trim()) {
      lastMsg.content = '文件处理完成，但未返回内容';
    }
  } catch (error) {
    console.error('文件处理错误:', error);
    fileUploadStatus.value = '解析失败';
    const lastMsg = messages.value.at(-1);
    lastMsg.content = '❌ 文件处理失败，请重试';
    lastMsg.isTyping = false;
  } finally {
    isSending.value = false;
    inputMessage.value = '';
    clearPendingFile();
    scrollToBottom();
  }
};

const initUUID = () => {
  let storedUUID = localStorage.getItem('user_uuid');
  if (!storedUUID) {
    storedUUID = uuidToNumber(uuidv4());
    localStorage.setItem('user_uuid', storedUUID);
  }
  uuid.value = storedUUID;
};

const uuidToNumber = (uuid) => {
  let number = 0;
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i];
    number = number * 16 + (parseInt(hexValue, 16) || 0);
  }
  return number % 1000000;
};

const newChat = () => {
  console.log('开始新会话');
  localStorage.removeItem('user_uuid');
  window.location.reload();
};

const handleLogout = () => {
  ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        userStore.clearUserInfo();
        ElMessage.success('已退出登录');
        router.push('/login');
      })
      .catch(() => {
      });
};

onUnmounted(() => {
  window.removeEventListener('paste', handlePaste);
  clearPendingImage();
});
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--zoe-bg, #f7f8fa);
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--zoe-sidebar, #0f1419);
  color: #e5e7eb;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px 20px;
}

.logo-wrap {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  display: block;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.logo-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #f9fafb;
  line-height: 1.3;
}

.logo-sub {
  font-size: 11px;
  color: var(--zoe-sidebar-muted, #8b949e);
  line-height: 1.35;
}

.new-chat-button {
  width: 100%;
  height: 40px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: #f3f4f6;
  transition: background 0.2s, border-color 0.2s;
}

.new-chat-button:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.btn-ico {
  margin-right: 6px;
  font-size: 16px;
}

.sidebar-hint {
  margin: auto 0 0;
  padding-top: 24px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--zoe-sidebar-muted, #8b949e);
}

.history-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 14px 0 12px;
  min-height: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(148, 163, 184, 0.16);
  padding: 10px 8px 8px;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 8px;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  color: #c8d0dc;
  letter-spacing: 0.02em;
}

.refresh-btn {
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  background: rgba(255, 255, 255, 0.06);
  color: #9aa3b2;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #e5e7eb;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 3px;
}

.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.16);
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.24);
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  margin-bottom: 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid transparent;
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.22);
}

.history-item.active {
  background: rgba(37, 99, 235, 0.22);
  border-color: rgba(96, 165, 250, 0.4);
}

.history-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.history-item-title {
  font-size: 13px;
  color: #e5e7eb;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-time {
  font-size: 11px;
  color: #8f98a6;
  line-height: 1.25;
}

.delete-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s, color 0.2s;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #9aa3b2;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.22);
  color: #f87171;
}

.logout-button {
  width: 100%;
  height: 40px;
  margin-top: 8px;
  font-weight: 500;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
  transition: all 0.2s;
}

.logout-button:hover {
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.16);
  color: #fff;
}

.main-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--zoe-bg, #f7f8fa);
  position: relative;
}

/* 欢迎页面样式 */
.welcome-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  /* 添加柔和渐变背景，让白色 logo 更自然融入 */
  background: linear-gradient(180deg, #ffffff 0%, #f7f8fa 100%);
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-logo {
  margin-bottom: 24px;
}

.welcome-logo-img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  border-radius: 24px;
  /* 保持 logo 原样，去除多余装饰 */
  background: transparent;
  box-shadow: none;
  /* 添加轻微的阴影让 logo 更立体 */
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.06));
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px;
  letter-spacing: -0.02em;
}

.welcome-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  line-height: 1.6;
}

.chat-header {
  flex-shrink: 0;
  padding: 16px 24px 12px;
  border-bottom: 1px solid var(--zoe-border, #e8eaed);
  background: var(--zoe-surface, #fff);
  box-shadow: var(--zoe-shadow-sm, 0 1px 2px rgba(15, 23, 42, 0.06));
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  .header-left {
    flex: 1;
    min-width: 0;
  }

  .rag-link-btn {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    font-size: 14px;

    :deep(.el-icon) {
      font-size: 16px;
    }
  }
}

.chat-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--zoe-text, #1f2937);
}

.chat-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--zoe-text-secondary, #6b7280);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  scroll-behavior: auto;
}

.scroll-bottom-anchor {
  flex-shrink: 0;
  height: 1px;
  width: 100%;
  pointer-events: none;
  visibility: hidden;
}

.msg-row {
  display: flex;
  max-width: 1100px;
  width: 100%;
  align-items: flex-start;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
}

.msg-row.is-user {
  justify-content: flex-end;
}

.msg-row.is-assistant {
  justify-content: flex-start;
}

.msg-bubble {
  border-radius: var(--zoe-radius-md, 12px);
  padding: 16px 20px;
  box-shadow: var(--zoe-shadow-sm, 0 1px 2px rgba(15, 23, 42, 0.06));
  max-width: 100%;
  width: 100%;
  box-sizing: border-box;
}

.is-user .msg-bubble {
  max-width: 60%;
  width: auto;
  background: #eaf3ff;
  color: var(--zoe-text, #1f2937);
  border: 1px solid #cfe2ff;
}

.is-assistant .msg-bubble {
  background: var(--zoe-surface, #fff);
  border: 1px solid var(--zoe-border, #e8eaed);
}



.msg-content {
  font-size: 17px;
  line-height: 1.6;
  word-break: break-word;
}

.is-user .msg-content {
  color: var(--zoe-text, #1f2937);
}

.msg-content :deep(p) {
  margin: 0 0 0.3em;
  line-height: 1.6;
}

.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}

.msg-content :deep(h1),
.msg-content :deep(h2),
.msg-content :deep(h3),
.msg-content :deep(h4),
.msg-content :deep(h5),
.msg-content :deep(h6) {
  margin: 0.8em 0 0.4em;
  font-weight: 600;
  line-height: 1.35;
  color: #1f2937;
}

.msg-content :deep(h1) { font-size: 1.5em; }
.msg-content :deep(h2) { font-size: 1.3em; }
.msg-content :deep(h3) { font-size: 1.15em; }
.msg-content :deep(h4),
.msg-content :deep(h5),
.msg-content :deep(h6) { font-size: 1em; }

.msg-content :deep(pre) {
  margin: 0.5em 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.06);
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 14px;
  line-height: 1.55;
}

.msg-content :deep(pre code) {
  padding: 0;
  background: transparent;
  font-size: inherit;
  white-space: inherit;
}

.msg-content :deep(code) {
  font-size: 0.9em;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.msg-content :deep(ul),
.msg-content :deep(ol) {
  margin: 0.2em 0;
  padding-left: 1.5em;
}

.msg-content :deep(li) {
  margin: 0.15em 0;
  line-height: 1.55;
}

.msg-content :deep(ol > li) {
  padding-left: 0.3em;
}

.msg-content :deep(ul > li) {
  padding-left: 0.2em;
}

.msg-content :deep(li + li) {
  margin-top: 0.1em;
}

.msg-content :deep(li > p) {
  margin: 0.15em 0;
}

.msg-content :deep(blockquote) {
  margin: 0.4em 0;
  padding: 0.5em 1em;
  border-left: 3px solid #e5e7eb;
  background: rgba(0, 0, 0, 0.02);
}

.msg-content :deep(blockquote p) {
  margin: 0.15em 0;
}

.msg-content :deep(.table-scroll) {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  margin: 0.6em 0;
  padding-bottom: 4px;
}

.msg-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8em 0;
  table-layout: auto;
  border: 1px solid var(--zoe-border, #e8eaed);
  background: rgba(255, 255, 255, 0.96);
  font-size: 14px;
}

.msg-content :deep(thead) {
  background: rgba(15, 23, 42, 0.04);
}

.msg-content :deep(th),
.msg-content :deep(td) {
  border: 1px solid var(--zoe-border, #e8eaed);
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.msg-content :deep(th) {
  background: rgba(0, 0, 0, 0.04);
  font-weight: 600;
}

.msg-content :deep(img) {
  max-width: 100%;
  width: auto;
  height: auto;
  border-radius: 8px;
  display: block;
  margin-top: 0.4em;
}

.generated-image-container {
  margin-top: 16px;
  margin-left: -12px;
  margin-right: -12px;
  padding: 0;
  background: transparent;
  border-radius: 0;
  border: none;
  display: flex;
  flex-direction: column;
  gap: 0;
  align-items: stretch;
  width: calc(100% + 24px);
  position: relative;
}

.generated-image-container::after {
  content: '💡 提示：如下载失败，可右键点击图片选择“图片另存为”';
  position: absolute;
  bottom: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--zoe-text-secondary, #6b7280);
  white-space: nowrap;
  opacity: 0.8;
}

/* 图片加载失败占位样式 */
.image-error-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: #f5f7fa;
  border-radius: 0;
  border: none;
  min-height: 200px;
  width: 100%;
}

.image-error-placeholder p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.image-error-placeholder .error-tip {
  font-size: 12px;
  color: #909399;
}

.image-error-placeholder .image-link {
  margin-top: 12px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #409eff;
  font-size: 12px;
  word-break: break-all;
  max-width: 100%;
  text-decoration: none;
}

.image-error-placeholder .image-link:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.generated-image {
  width: 100%;
  height: auto;
  border-radius: 0;
  box-shadow: none;
  display: block;
  transition: transform 0.2s ease;
}

.generated-image:hover {
  transform: scale(1.02);
}

.download-image-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.download-image-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.is-user .msg-content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.is-user .msg-content :deep(pre) {
  background: rgba(255, 255, 255, 0.55);
}

.is-user .msg-content :deep(th),
.is-user .msg-content :deep(td) {
  border-color: #bdd6ff;
}

.is-user .msg-content :deep(th) {
  background: #dbeafe;
}

.loading-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 6px;
  vertical-align: middle;
}

/* 消息反馈栏样式 */
.message-feedback {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  /* 按钮始终显示，不再隐藏 */
  opacity: 1;
}

.feedback-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: #6b7280;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 细线风格图标 */
  font-size: 18px;
}

.feedback-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 自定义图标按钮样式 */
.feedback-icon-btn {
  padding: 4px;
}

.feedback-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  filter: grayscale(100%) opacity(0.6);
  transition: all 0.2s ease;
}

.feedback-icon-btn:hover .feedback-icon {
  filter: grayscale(0%) opacity(1);
}

.feedback-icon-btn.is-active .feedback-icon {
  filter: grayscale(0%) opacity(1);
}

.feedback-btn.is-active {
  color: #2563eb;
}

.feedback-btn.is-active:hover {
  background: #eff6ff;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.35;
  animation: pulse 1.2s infinite ease-in-out both;
}

.dot:nth-child(2) {
  animation-delay: -0.25s;
}

.dot:nth-child(3) {
  animation-delay: -0.5s;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.65);
    opacity: 0.35;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.composer {
  flex-shrink: 0;
  padding: 16px 0 24px;
  background: linear-gradient(to top, var(--zoe-bg, #f7f8fa) 70%, transparent);
  position: relative;
}

/* 拖拽提示遮罩 */
.composer::before {
  content: '📄 释放文件以上传';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 500;
  color: #2563eb;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 10;
  border: 3px dashed #2563eb;
  border-radius: 16px;
}

.composer.is-dragging::before {
  opacity: 1;
}

/* 当没有消息时，composer 居中显示 */
.main-panel:not(.has-messages) .composer {
  position: absolute;
  bottom: 50%;
  left: 0;
  right: 0;
  transform: translateY(60%);
  padding: 24px 0;
  background: transparent;
}

.composer-actions {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  box-sizing: border-box;
}

.generate-image-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  height: 36px;
  border-radius: 18px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.generate-image-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.generate-image-btn:active:not(:disabled) {
  transform: translateY(0);
}

.generate-image-btn .el-icon {
  font-size: 18px;
}

.composer-card {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  min-height: 92px;
  background: var(--zoe-surface, #fff);
  border: none;
  border-radius: var(--zoe-radius-lg, 16px);
  box-shadow: var(--zoe-shadow-md, 0 4px 24px rgba(15, 23, 42, 0.08));
  box-sizing: border-box;
}

.composer-input {
  flex: 1;
  display: flex;
  align-items: center;
}


.image-input,
.file-input {
  display: none;
}

.audio-btn,
.image-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  color: #374151;
  background: #f3f4f6;
  box-shadow: none;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.web-search-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
  flex-shrink: 0;
}

.switch-label {
  font-size: 12px;
  color: var(--zoe-text-secondary, #6b7280);
  user-select: none;
}

.audio-btn:hover:not(:disabled),
.image-btn:hover:not(:disabled) {
  color: #111827;
  background: #e5e7eb;
}

.audio-btn.is-recording {
  background: #111827;
  border-color: #111827;
  color: #ffffff;
  animation: pulse-recording 1.5s infinite;
}

@keyframes pulse-recording {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(239, 68, 68, 0);
  }
}

.media-selected-preview {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto 10px;
  padding: 10px 20px;
  border-radius: 12px;
  border: 1px solid var(--zoe-border, #e8eaed);
  background: var(--zoe-surface, #fff);
  display: flex;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
}

.media-selected-image {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--zoe-border, #e8eaed);
  flex-shrink: 0;
}

.media-selected-meta {
  min-width: 0;
  flex: 1;
}

.media-selected-name {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--zoe-text, #1f2937);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.media-selected-tip {
  margin: 0;
  font-size: 12px;
  color: var(--zoe-text-secondary, #6b7280);
}

.media-remove-btn {
  flex-shrink: 0;
}

.file-selected-preview {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto 10px;
  padding: 12px 20px;
  border-radius: 12px;
  border: 2px dashed transparent;
  background: var(--zoe-surface, #fff);
  display: flex;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
  position: relative;
  transition: all 0.3s ease;
}

.file-selected-preview.is-drag-over {
  border-color: #2563eb;
  background: #eff6ff;
}

.file-selected-preview:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.file-icon {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.file-icon-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  display: block;
}

.file-selected-meta {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-selected-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--zoe-text, #1f2937);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-selected-size {
  margin: 0;
  font-size: 12px;
  color: var(--zoe-text-secondary, #6b7280);
}

.file-selected-status {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  transition: color 0.3s ease;
}

.file-selected-status.status-上传中 {
  color: #f59e0b;
  font-weight: 500;
}

.file-selected-status.status-解析中 {
  color: #3b82f6;
  font-weight: 500;
}

.file-selected-status.status-已完成解析 {
  color: #10b981;
  font-weight: 500;
}

.file-selected-status.status-解析失败 {
  color: #ef4444;
  font-weight: 500;
}

.file-close-btn {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  cursor: pointer;
  color: #9ca3af;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  position: absolute;
  top: 8px;
  right: 8px;
}

.file-close-btn:hover {
  color: #ef4444;
  background: #fee2e2;
}

.composer-input :deep(.el-textarea__inner) {
  box-shadow: none;
  border: none;
  padding: 10px 6px;
  font-size: 16px;
  line-height: 1.5;
  min-height: 56px !important;
  background: transparent;
  overflow: hidden;
}

/* 隐藏滚动条 */
.composer-input :deep(.el-textarea__inner)::-webkit-scrollbar {
  display: none;
}

.composer-input :deep(.el-textarea__inner) {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.composer-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}


.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  padding: 0;
  background: #d1d5db;
  border: none;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  background: #9ca3af;
}

.composer-foot {
  max-width: 1100px;
  width: 100%;
  margin: 10px auto 0;
  font-size: 11px;
  color: var(--zoe-text-secondary, #9ca3af);
  text-align: center;
  padding: 0;
  box-sizing: border-box;
}

.upload-file-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #374151;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.upload-file-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.upload-file-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.upload-file-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-file-btn .el-icon {
  font-size: 18px;
}

@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
    height: 100vh;
    height: 100dvh;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    padding: 12px 14px;
    gap: 10px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  .brand {
    flex: 1;
    min-width: 0;
    padding: 0;
  }

  .new-chat-button {
    width: 100%;
    height: 40px;
    font-weight: 500;
    border: 1px solid rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.06);
    color: #f3f4f6;
    transition: background 0.2s, border-color 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .sidebar-hint {
    display: none;
  }

  .history-section {
    margin: 8px 0 0;
    max-height: 220px;
  }

  .chat-header {
    padding: 12px 16px;
  }

  .message-list {
    padding: 16px 20px;
  }

  .composer {
    padding: 12px 20px 18px;
  }

  .media-selected-preview {
    width: 100%;
  }

  .msg-row.is-user,
  .msg-row.is-assistant {
    width: 100%;
  }

  .msg-row {
    width: 100%;
  }

  .composer-card {
    width: 100%;
  }

  .composer-mode-bar {
    width: 100%;
  }
  .new-chat-button:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.2);
    color: #fff;
  }

  .logout-button {
    width: 100%;
    height: 40px;
    margin-top: 10px;
    font-weight: 500;
    border: 1px solid rgba(239, 68, 68, 0.3);
    background: rgba(239, 68, 68, 0.1);
    color: #fca5a5;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .logout-button:hover {
    background: rgba(239, 68, 68, 0.2);
    border-color: rgba(239, 68, 68, 0.5);
    color: #fff;
  }

  .btn-ico {
    margin-right: 6px;
    font-size: 16px;
  }

}
</style>
