/**
 * 分頁 Composable
 * 提供前端分頁邏輯，可在多個列表頁面共用
 */
import { ref, computed, watch } from 'vue'

/**
 * 建立分頁功能
 * @param {Ref} sourceList - 原始資料列表 (ref)
 * @param {Object} options - 配置選項
 * @param {number} options.defaultPageSize - 預設每頁筆數，預設 10
 * @param {Ref} options.searchText - 搜尋文字 (當變更時重置頁碼)
 * @returns {Object} 分頁相關的響應式資料和方法
 */
export function usePagination(sourceList, options = {}) {
  const { defaultPageSize = 10, searchText = null } = options

  // 響應式狀態
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)

  // 計算總頁數
  const totalPages = computed(() => {
    return Math.ceil(sourceList.value.length / pageSize.value)
  })

  // 計算分頁後的列表
  const paginatedList = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return sourceList.value.slice(start, end)
  })

  // 計算總筆數
  const total = computed(() => sourceList.value.length)

  // 是否有資料
  const hasData = computed(() => sourceList.value.length > 0)

  // 是否需要顯示分頁器
  const showPagination = computed(() => sourceList.value.length > pageSize.value)

  // 當搜尋文字變更時，重置到第一頁
  if (searchText) {
    watch(searchText, () => {
      currentPage.value = 1
    })
  }

  // 當原始列表變更時，確保當前頁碼有效
  watch(sourceList, () => {
    if (currentPage.value > totalPages.value && totalPages.value > 0) {
      currentPage.value = totalPages.value
    }
  })

  // ★ 修復：當 pageSize 變更時，強制回到第一頁
  watch(pageSize, () => {
    currentPage.value = 1
  })

  // 跳轉到指定頁
  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  // 上一頁
  const prevPage = () => {
    if (currentPage.value > 1) {
      currentPage.value--
    }
  }

  // 下一頁
  const nextPage = () => {
    if (currentPage.value < totalPages.value) {
      currentPage.value++
    }
  }

  // 重置分頁
  const resetPagination = () => {
    currentPage.value = 1
  }

  return {
    // 響應式狀態
    currentPage,
    pageSize,
    totalPages,
    total,

    // 計算屬性
    paginatedList,
    hasData,
    showPagination,

    // 方法
    goToPage,
    prevPage,
    nextPage,
    resetPagination,
  }
}

export default usePagination
