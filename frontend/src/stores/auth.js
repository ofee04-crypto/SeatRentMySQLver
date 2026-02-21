import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLogin: false,
    role: null, // 'member' | 'admin'
    user: null, // { id, username, name ... }
  }),

  actions: {
    login(user, role) {
      this.isLogin = true
      this.user = user
      this.role = role
    },

    logout() {
      this.isLogin = false
      this.user = null
      this.role = null
    },
  },
})