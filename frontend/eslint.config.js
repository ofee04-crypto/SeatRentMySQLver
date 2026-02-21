import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default [
  // 1. 設定要檢查的檔案範圍
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
  },

  // 2. 設定要忽略的資料夾
  {
    name: 'app/files-to-ignore',
    ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**'],
  },

  // 3. 載入 Vue 的基本規則 (Essential)
  ...pluginVue.configs['flat/essential'],

  // 4. 載入 Prettier 格式化規則 (避免 ESLint 跟 Prettier 打架)
  skipFormatting,

  // 5. 【關鍵】自定義寬鬆規則 (豁免金牌區域)
  {
    rules: {
      // -----------------------------------------------------------
      // 核心修正：解決 "The 'lang' attribute is missing"
      // -----------------------------------------------------------
      'vue/block-lang': 'off',

      // -----------------------------------------------------------
      // JS 寬鬆設定
      // -----------------------------------------------------------
      // 允許變數宣告了卻沒用到 (改成黃色警告，不要紅色報錯)
      'no-unused-vars': 'warn',

      // 允許使用未定義的變數 (避免因為沒寫 TS 定義而報錯)
      'no-undef': 'off',

      // Vue 寬鬆設定

      // 允許組件名稱只有一個單字
      'vue/multi-word-component-names': 'off',

      // 關閉 Props 必須定義型別的強制檢查
      'vue/require-prop-types': 'off',
    },
  },
]
