import { defineStore } from 'pinia'
import axios from 'axios'

export const useMemberAuthStore = defineStore('memberAuth', {
  state: () => {
    // ✅ 保留 HEAD：登入狀態還原
    const savedMember = localStorage.getItem('member_user')
    const savedLogin = localStorage.getItem('isLogin') === 'true'

    return {
      /* =====================
       * 類型 1：登入狀態（核心）
       * ===================== */
      isLogin: savedLogin,

      /* =====================
       * 類型 2：會員業務資料（前台會用）
       * ===================== */
      member: savedMember
        ? JSON.parse(savedMember)
        : {
            memId: null,
            memUsername: '',
            memName: '',
            memPoints: 0,
            memInvoice: '',
          },

      /* =====================
       * 類型 3：使用者操作暫存（rec 需要）
       * ===================== */
      selectedSpotId: null,
    }
  },

  actions: {
    /* ========= 登入 ========= */
    setMemberLogin(memberData) {
      this.isLogin = true
      this.member = {
        memId: memberData.memId,
        memUsername: memberData.memUsername,
        memName: memberData.memName,
        memPoints: memberData.memPoints,
        memInvoice: memberData.memInvoice,
      }

      localStorage.setItem('member_user', JSON.stringify(this.member))
      localStorage.setItem('isLogin', 'true')
    },

    /* ========= 點數同步========= */
    async refreshPoints() {
      if (!this.member.memId) return

      try {
        const res = await axios.get('http://localhost:8080/api/members/find', {
          params: { memId: this.member.memId },
        })

        if (res.data) {
          this.member = {
            ...this.member,
            memPoints: res.data.memPoints,
            memName: res.data.memName || this.member.memName,
          }

          localStorage.setItem('member_user', JSON.stringify(this.member))
        }
      } catch (err) {
        console.warn('點數同步失敗', err)
      }
    },

    /* ========= 登出 ========= */
    async clearMemberLogin() {
      try {
        await axios.post('http://localhost:8080/api/auth/logout', {}, { withCredentials: true })
      } catch (_) {}

      this.isLogin = false
      this.member = {
        memId: null,
        memUsername: '',
        memName: '',
        memPoints: 0,
        memInvoice: '',
      }
      this.selectedSpotId = null

      localStorage.removeItem('member_user')
      localStorage.removeItem('isLogin')
      localStorage.removeItem('token')
    },

    /* ========= rec 專用 ========= */
    setSpotId(spotId) {
      this.selectedSpotId = spotId
    },
    clearSpotId() {
      this.selectedSpotId = null
    },
  },
})
