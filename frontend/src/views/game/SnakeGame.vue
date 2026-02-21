<template>
  <div class="snake-page">
    <div class="game-container">
      <!-- Header -->
      <div class="page-header">
        <router-link to="/mall" class="back-link">
          <i class="bi bi-arrow-left"></i>
          返回商城
        </router-link>

        <h1 class="page-title">
          <i class="bi bi-controller"></i>
          蛇蛇賺點數
        </h1>
            <span class="badge bg-warning text-dark px-3 py-2">
              <template v-if="memberId">
                <i class="bi bi-person-fill me-1"></i>{{ memName }} 
                <span class="mx-2 opacity-50">|</span>
                <i class="bi bi-coin me-1"></i>{{ currentPoints }} Pts
                <span class="mx-2 opacity-50">|</span>
                ID: {{ memberId }}
              </template>
              <template v-else>
                <router-link to="/login" class="text-dark text-decoration-none">⚠️ 未登入 (點我登入)</router-link>
              </template>
            </span>
      </div>

      <!-- KPI -->
      <div class="kpi-section">
        <div class="kpi-tile">
          <div class="kpi-label">目前得分</div>
          <!-- UI ONLY: displayScore 用於平滑遞增顯示 -->
          <div class="kpi-value">{{ displayScore }}</div>
        </div>

        <div class="kpi-tile" :class="{ redeemable: pointsEarned > 0 }">
          <div class="kpi-label">可換點數</div>
          <div class="kpi-value points">{{ pointsEarned }}</div>
          <div v-if="pointsEarned > 0" class="kpi-hint">
            <i class="bi bi-check-circle"></i>
            可立即兌換
          </div>
        </div>
      </div>

      <!-- Daily Play Limit Hint -->
      <div class="limit-hint" v-if="remainingPlays !== null">
        今日剩餘遊玩次數：<strong>{{ remainingPlays }}</strong> / {{ maxDailyPlays }}
      </div>

      <!-- Canvas -->
      <div class="canvas-section">
        <div class="canvas-wrapper" :class="{ playing: isGameRunning, shake: isShaking }">
          <canvas ref="gameCanvas" width="400" height="400"></canvas>

          <!-- Start Overlay -->
          <transition name="overlay-fade">
            <div v-if="!isGameRunning && !gameOver" class="game-overlay">
              <div class="overlay-card">
                <div class="overlay-icon">
                  <i class="bi bi-joystick"></i>
                </div>
                <div class="overlay-title">準備好了嗎？</div>
                <div class="overlay-desc">使用方向鍵控制蛇蛇移動</div>

                <button class="btn-primary" @click="startGame">
                  <i class="bi bi-play-fill"></i>
                  開始遊戲
                </button>
              </div>
            </div>
          </transition>

          <!-- Result Overlay -->
          <transition name="overlay-fade">
            <div v-if="gameOver" class="game-overlay">
              <div class="overlay-card">
                <div class="overlay-icon warn">
                  <i class="bi bi-flag-fill"></i>
                </div>
                <div class="overlay-title">遊戲結束</div>

                <div class="result-grid">
                  <div class="result-item">
                    <div class="result-label">本次得分</div>
                    <div class="result-value">{{ score }}</div>
                  </div>
                  <div class="divider"></div>
                  <div class="result-item">
                    <div class="result-label">可換點數</div>
                    <div class="result-value highlight">{{ pointsEarned }}</div>
                  </div>
                </div>

                <div v-if="!memberId" class="login-prompt">
                  <i class="bi bi-info-circle"></i>
                  登入後可兌換點數
                </div>

                <div class="actions">
                  <button
                    class="btn-warning"
                    @click="uploadScore"
                    :disabled="isUploaded || pointsEarned < 1 || isRedeeming"
                  >
                    <template v-if="isRedeeming">
                      <span class="spinner"></span>
                      兌換中…
                    </template>
                    <template v-else-if="isUploaded">
                      <i class="bi bi-check-circle"></i>
                      已入帳
                    </template>
                    <template v-else-if="pointsEarned < 1">
                      <i class="bi bi-x-circle"></i>
                      分數不足
                    </template>
                    <template v-else>
                      <i class="bi bi-gift"></i>
                      領取 {{ pointsEarned }} 點
                    </template>
                  </button>

                  <button class="btn-ghost" @click="startGame">
                    <i class="bi bi-arrow-clockwise"></i>
                    再玩一次
                  </button>
                </div>

                <div class="limit-hint overlay-limit" v-if="remainingPlays !== null">
                  今日剩餘遊玩次數：<strong>{{ remainingPlays }}</strong> / {{ maxDailyPlays }}
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <!-- Rules -->
      <div class="rules-section">
        <div class="rules-title">
          <i class="bi bi-info-circle"></i>
          遊戲說明
        </div>
        <ul class="rules-list">
          <li>方向鍵控制移動，撞牆或撞到自己即結束。</li>
          <li>吃到食物獲得 200 分，蛇身變長。</li>
          <li>每累積 <strong>100 分</strong> 可兌換 <strong>1 點(上限50點)</strong>（無條件捨去）。</li>
          <li>領取點數前請先登入會員帳號。</li>
          <li>
            每日遊玩次數上限：<strong>{{ maxDailyPlays }}</strong> 次（超過需隔天再來）。
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useMemberAuthStore } from '@/stores/memberAuth'; 

const router = useRouter();
const memberAuthStore = useMemberAuthStore();

const memberId = computed(() => memberAuthStore.member?.memId);
const memName = computed(() => memberAuthStore.member?.memName || '訪客');
const currentPoints = computed(() => memberAuthStore.member?.memPoints || 0);

// =============================
// 每日遊玩次數限制（整合自組員邏輯，但不破壞你原本流程）
// =============================
const maxDailyPlays = 10 // 你可自行調整
const remainingPlays = ref(null)

const getTodayKey = () => {
  // 使用本機 locale 的日期字串即可（同一天判斷一致即可）
  return new Date().toLocaleDateString()
}

const readPlayRecord = () => {
  try {
    return JSON.parse(localStorage.getItem('snake_game_record') || '{}')
  } catch {
    return {}
  }
}

const writePlayRecord = (record) => {
  localStorage.setItem('snake_game_record', JSON.stringify(record))
}

const refreshPlayLimit = () => {
  const today = getTodayKey()
  const record = readPlayRecord()

  // 若不是今天，重置
  if (record.date !== today) {
    const newRecord = { date: today, count: 0 }
    writePlayRecord(newRecord)
    remainingPlays.value = maxDailyPlays
    return { ok: true, record: newRecord }
  }

  // 是今天
  const count = Number.isFinite(record.count) ? record.count : 0
  remainingPlays.value = Math.max(0, maxDailyPlays - count)
  return { ok: count < maxDailyPlays, record: { date: today, count } }
}

const consumeOnePlay = () => {
  const { ok, record } = refreshPlayLimit()
  if (!ok) return false

  const next = { ...record, count: (record.count || 0) + 1 }
  writePlayRecord(next)
  remainingPlays.value = Math.max(0, maxDailyPlays - next.count)
  return true
}

// ===== 核心遊戲狀態（維持原規則） =====
const gameCanvas = ref(null)
const score = ref(0)
const gameSpeed = ref(150)
const isGameRunning = ref(false)
const gameOver = ref(false)
const isUploaded = ref(false)



// ===== UI ONLY（不影響核心規則） =====
const displayScore = ref(0) // UI ONLY: 分數平滑顯示
const isRedeeming = ref(false) // UI ONLY: 兌點 loading
const isShaking = ref(false) // UI ONLY: Game Over shake
let foodPulse = 0 // CANVAS VISUAL ONLY: 食物呼吸

const pointsEarned = computed(() => Math.floor(score.value / 100))

watch(score, (newScore) => {
  // UI ONLY: 如果分數被重置（例如重新開始），同步顯示
  if (newScore <= displayScore.value) {
    displayScore.value = newScore
    return
  }
  const start = displayScore.value
  const delta = newScore - start
  const steps = 10
  let n = 0
  const tick = () => {
    n += 1
    displayScore.value = Math.round(start + (delta * n) / steps)
    if (n < steps) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
})

// ===== 遊戲邏輯變數（核心） =====
let ctx = null
let snake = []
let food = { x: 5, y: 5 }
let direction = { x: 1, y: 0 }
let nextDirection = { x: 1, y: 0 }
const gridSize = 20

const checkLoginStatus = () => {
  memberId.value = localStorage.getItem('memberId') || localStorage.getItem('memId')
}

// 內部：真正啟動遊戲（保持你原本遊戲初始化與流程不變）
const startGameCore = () => {
  score.value = 0
  displayScore.value = 0 // UI ONLY
  gameSpeed.value = 150
  direction = { x: 1, y: 0 }
  nextDirection = { x: 1, y: 0 }
  snake = [
    { x: 10, y: 10 },
    { x: 9, y: 10 },
    { x: 8, y: 10 },
  ]
  gameOver.value = false
  isUploaded.value = false
  isShaking.value = false // UI ONLY
  isGameRunning.value = true
  spawnFood()
  gameLoop()
}

// 外部：你畫面所有按鈕呼叫的 startGame（加入每日次數限制，但不破壞你的 UX/UI 流程）
const startGame = async () => {
  // 若正在遊戲中，不做任何事（避免重複扣次數/重啟）
  if (isGameRunning.value) return

  // 先刷新顯示（確保 UI 顯示正確）
  const { ok } = refreshPlayLimit()
  if (!ok) {
    await Swal.fire({
      icon: 'error',
      title: '今日次數已用盡',
      text: `今天已經玩滿 ${maxDailyPlays} 次囉，請明天再來！`,
      confirmButtonText: '回商城逛逛',
    })
    router.push('/mall')
    return
  }

  // 扣一次
  const consumed = consumeOnePlay()
  if (!consumed) {
    await Swal.fire({
      icon: 'error',
      title: '今日次數已用盡',
      text: `今天已經玩滿 ${maxDailyPlays} 次囉，請明天再來！`,
      confirmButtonText: '回商城逛逛',
    })
    router.push('/mall')
    return
  }

  startGameCore()
}

const gameLoop = () => {
  if (!isGameRunning.value) return
  setTimeout(() => {
    update()
    draw()
    gameLoop()
  }, gameSpeed.value)
}

const update = () => {
  direction = nextDirection
  const head = { x: snake[0].x + direction.x, y: snake[0].y + direction.y }

  // 碰撞偵測（核心規則不變）
  if (
    head.x < 0 ||
    head.x >= 20 ||
    head.y < 0 ||
    head.y >= 20 ||
    snake.some((s) => s.x === head.x && s.y === head.y)
  ) {
    isGameRunning.value = false
    gameOver.value = true

    // UI ONLY: 輕微震動回饋
    isShaking.value = true
    setTimeout(() => (isShaking.value = false), 350)
    return
  }

  snake.unshift(head)

  if (head.x === food.x && head.y === food.y) {
    score.value += 200
    // 每 100 分加速一次（核心規則不變）
    if (score.value % 100 === 0 && gameSpeed.value > 60) gameSpeed.value -= 15
    spawnFood()
  } else {
    snake.pop()
  }
}

const draw = () => {
  if (!ctx) return

  // CANVAS VISUAL ONLY: 背景（柔和深色）
  const bg = ctx.createLinearGradient(0, 0, 0, 400)
  bg.addColorStop(0, '#24313c')
  bg.addColorStop(1, '#171f27')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, 400, 400)

  // CANVAS VISUAL ONLY: 淡格線
  ctx.strokeStyle = 'rgba(255,255,255,0.06)'
  ctx.lineWidth = 1
  for (let i = 0; i <= 20; i++) {
    ctx.beginPath()
    ctx.moveTo(i * gridSize, 0)
    ctx.lineTo(i * gridSize, 400)
    ctx.stroke()

    ctx.beginPath()
    ctx.moveTo(0, i * gridSize)
    ctx.lineTo(400, i * gridSize)
    ctx.stroke()
  }

  // CANVAS VISUAL ONLY: 食物呼吸 + 光暈
  foodPulse += 0.12
  const pulse = 8 + Math.sin(foodPulse) * 1.2

  const fx = food.x * gridSize + 10
  const fy = food.y * gridSize + 10
  const glow = ctx.createRadialGradient(fx, fy, 0, fx, fy, pulse + 6)
  glow.addColorStop(0, 'rgba(230,162,60,0.9)')
  glow.addColorStop(1, 'rgba(230,162,60,0)')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(fx, fy, pulse + 6, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#e6a23c'
  ctx.beginPath()
  ctx.arc(fx, fy, pulse, 0, Math.PI * 2)
  ctx.fill()

  // CANVAS VISUAL ONLY: 蛇（頭部更亮 + 小眼睛）
  snake.forEach((p, i) => {
    const x = p.x * gridSize
    const y = p.y * gridSize

    if (i === 0) {
      ctx.fillStyle = '#f5f7fa'
      ctx.fillRect(x + 1, y + 1, gridSize - 2, gridSize - 2)

      // 眼睛（簡單辨識）
      ctx.fillStyle = 'rgba(30,30,30,0.7)'
      ctx.beginPath()
      ctx.arc(x + 7, y + 8, 1.6, 0, Math.PI * 2)
      ctx.arc(x + 13, y + 8, 1.6, 0, Math.PI * 2)
      ctx.fill()
    } else {
      ctx.fillStyle = '#67c23a'
      ctx.fillRect(x + 2, y + 2, gridSize - 4, gridSize - 4)
    }
  })
}

const spawnFood = () => {
  food = { x: Math.floor(Math.random() * 20), y: Math.floor(Math.random() * 20) }
  if (snake.some((s) => s.x === food.x && s.y === food.y)) spawnFood()
}

const handleKeyDown = (e) => {
  const key = e.key
  if (key === 'ArrowUp' && direction.y === 0) nextDirection = { x: 0, y: -1 }
  if (key === 'ArrowDown' && direction.y === 0) nextDirection = { x: 0, y: 1 }
  if (key === 'ArrowLeft' && direction.x === 0) nextDirection = { x: -1, y: 0 }
  if (key === 'ArrowRight' && direction.x === 0) nextDirection = { x: 1, y: 0 }
}

const uploadScore = async () => {
  checkLoginStatus();

  if (!memberId.value) {
    // 儲存目前分數到 Session，登入後可恢復
    sessionStorage.setItem('pendingSnakeScore', score.value);
    
    const result = await Swal.fire({
      title: '請先登入',
      text: `您獲得了 ${score.value} 分，登入後即可換取點數！`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '去登入',
      cancelButtonText: '下次再說'
    });

    if (result.isConfirmed) {
      router.push({ path: '/login', query: { redirect: '/snake' } });
    }
    return;
  }

  const points = Math.floor(score.value / 100);
  
  try {
    const res = await axios.post('http://localhost:8080/api/game/add-points', {
      memberId: memberId.value,
      points: points
    });

    isUploaded.value = true;
    sessionStorage.removeItem('pendingSnakeScore');

    
    await Swal.fire({
      title: '兌換成功！',
      text: `已入帳 ${res.data.addPoints} 點，目前帳戶總點數：${res.data.currentPoints}`,  
      icon: 'success'
    });

    // ✅ 自動刷新全站點數
    if (typeof memberAuthStore.refreshPoints === 'function') {
      await memberAuthStore.refreshPoints();
    }
  } catch (err) {
    console.error("上傳失敗", err);
    Swal.fire('系統錯誤', '無法連接到伺服器，請稍後再試', 'error');
   
  }
};

onMounted(() => {
  ctx = gameCanvas.value?.getContext('2d')
  window.addEventListener('keydown', handleKeyDown)

  // 會員狀態（保留你原本邏輯）
  checkLoginStatus()

  // 每日次數顯示初始化
  refreshPlayLimit()

  // 登入後回來可承接暫存分數（核心行為不變）
  const savedScore = sessionStorage.getItem('pendingSnakeScore')
  if (savedScore && memberId.value) {
    score.value = parseInt(savedScore, 10)
    displayScore.value = score.value // UI ONLY
    gameOver.value = true
    isGameRunning.value = false
    Swal.fire({
      title: '歡迎回來',
      text: `已偵測到您剛才獲得的 ${score.value} 分，快領取點數吧！`,
      icon: 'info',
      confirmButtonText: '確定',
    })
  }

  draw()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
/* 基底：不花俏的商城風格 + 遊戲區淡霓虹 */
.snake-page {
  --bg1: #f3f6fb;
  --bg2: #e9eef6;
  --text: #303133;
  --muted: #606266;
  --border: #dcdfe6;
  --primary: #409eff;
  --success: #67c23a;
  --warning: #e6a23c;

  min-height: 100vh;
  background: linear-gradient(180deg, var(--bg1) 0%, var(--bg2) 100%);
  padding: 20px 14px;
  color: var(--text);
}

.game-container {
  max-width: 640px;
  margin: 0 auto;
}

/* Header */
.page-header {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.back-link {
  text-decoration: none;
  color: var(--muted);
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.back-link:hover {
  color: var(--primary);
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.page-title i {
  color: var(--primary);
}

.login-status {
  justify-self: end;
}

.status-badge {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  gap: 6px;
  align-items: center;
  border: 1px solid var(--border);
}
.status-badge.logged-in {
  background: #f0f9eb;
  color: var(--success);
  border-color: rgba(103, 194, 58, 0.35);
}
.status-badge.not-logged {
  background: #fef0f0;
  color: #f56c6c;
  border-color: rgba(245, 108, 108, 0.35);
}
.status-badge.not-logged:hover {
  background: #f56c6c;
  color: #fff;
}

/* KPI */
.kpi-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 10px;
}

.kpi-tile {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  text-align: center;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 600;
}

.kpi-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
}

.kpi-value.points {
  color: var(--success);
}

.kpi-tile.redeemable {
  border-color: rgba(103, 194, 58, 0.55);
  box-shadow:
    0 2px 10px rgba(0, 0, 0, 0.04),
    0 0 18px rgba(103, 194, 58, 0.12);
}

.kpi-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--success);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* Daily Limit Hint */
.limit-hint {
  margin: 0 2px 12px;
  font-size: 13px;
  color: var(--muted);
}
.overlay-limit {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.82);
}

/* Canvas */
.canvas-section {
  margin-bottom: 14px;
}

.canvas-wrapper {
  position: relative;
  width: fit-content;
  margin: 0 auto;
  border-radius: 14px;
  overflow: hidden;
  border: 2px solid var(--border);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.canvas-wrapper.playing {
  border-color: rgba(64, 158, 255, 0.55);
  box-shadow:
    0 6px 18px rgba(0, 0, 0, 0.08),
    0 0 18px rgba(64, 158, 255, 0.12);
}

.canvas-wrapper.shake {
  animation: shake 0.35s;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-4px);
  }
  40% {
    transform: translateX(4px);
  }
  60% {
    transform: translateX(-3px);
  }
  80% {
    transform: translateX(3px);
  }
}

canvas {
  display: block;
}

/* Overlay */
.game-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 20, 26, 0.82);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  padding: 16px;
}

.overlay-card {
  width: min(420px, 92%);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 14px;
  padding: 18px 16px;
  text-align: center;
}

.overlay-icon {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 8px;
}
.overlay-icon.warn {
  color: rgba(230, 162, 60, 0.95);
}

.overlay-title {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 6px;
}
.overlay-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
  margin-bottom: 14px;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  margin: 12px 0 14px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.result-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 6px;
}
.result-value {
  font-size: 26px;
  font-weight: 900;
  color: #fff;
}
.result-value.highlight {
  color: var(--warning);
}

.divider {
  width: 1px;
  height: 46px;
  background: rgba(255, 255, 255, 0.16);
}

.login-prompt {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(230, 162, 60, 0.45);
  background: rgba(230, 162, 60, 0.14);
  color: rgba(230, 162, 60, 0.95);
  font-size: 13px;
  margin-bottom: 12px;
}

.actions {
  display: grid;
  gap: 10px;
}

/* Buttons */
.btn-primary,
.btn-warning,
.btn-ghost {
  border: none;
  border-radius: 12px;
  padding: 12px 14px;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  transition:
    transform 120ms ease,
    filter 120ms ease,
    opacity 120ms ease;
}

.btn-primary {
  background: var(--success);
  color: #fff;
}
.btn-primary:hover {
  filter: brightness(1.03);
  transform: translateY(-1px);
}

.btn-warning {
  background: var(--warning);
  color: #fff;
}
.btn-warning:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.btn-ghost {
  background: transparent;
  color: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.28);
}
.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.08);
}

.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: rgba(255, 255, 255, 0.95);
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition:
    opacity 200ms ease,
    transform 200ms ease;
}
.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* Rules */
.rules-section {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.rules-title {
  font-weight: 800;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.rules-title i {
  color: var(--primary);
}

.rules-list {
  margin: 0;
  padding-left: 18px;
  color: var(--muted);
  font-size: 13px;
  display: grid;
  gap: 8px;
}

@media (max-width: 520px) {
  .page-header {
    grid-template-columns: 1fr;
    text-align: center;
  }
  .login-status {
    justify-self: center;
  }
  .kpi-section {
    grid-template-columns: 1fr;
  }
}
</style>
