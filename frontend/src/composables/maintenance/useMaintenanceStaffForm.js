import { ref, reactive, computed, unref } from 'vue'
import Swal from 'sweetalert2'

export function useMaintenanceStaffForm({ staffId, router, maintenanceApi }) {
  // 1. 優化編輯模式判斷：確保它是數字且大於 0
  const isEdit = computed(() => {
    const id = unref(staffId) // 以防萬一傳入的是 ref
    return !isNaN(id) && id > 0
  })

  const formRef = ref(null)
  const loading = ref(false)
  const submitting = ref(false)
  const formVisible = ref(false)

  const form = reactive({
    staffName: '',
    staffCompany: '',
    staffPhone: '',
    staffEmail: '',
    staffNote: '',
    isActive: true,
  })

  const rules = {
    staffName: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
    staffEmail: [{ type: 'email', message: 'Email 格式不正確', trigger: 'blur' }],
  }

  const init = async () => {
    console.log('Init called. isEdit:', isEdit.value) // 除錯 Log

    // 觸發進場動畫
    setTimeout(() => {
      formVisible.value = true
      console.log('Form Visible set to true') // 除錯 Log
    }, 100)

    if (!isEdit.value) {
      console.log('Create Mode - Stopping init')
      return
    }

    loading.value = true
    try {
      // 這裡原本直接用 staffId，建議確保它是數值
      const res = await maintenanceApi.getStaffById(staffId)
      Object.assign(form, res.data)
    } catch (error) {
      console.error('Fetch Error:', error) // 捕捉錯誤
      router.push('/admin/staff-list')
    } finally {
      loading.value = false
    }
  }

  const submitForm = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
      if (!valid) {
        await Swal.fire({
          icon: 'warning',
          title: '表單驗證失敗',
          text: '請檢查必填欄位與格式',
          confirmButtonText: '確認',
          showClass: { popup: 'animate__animated animate__shakeX' },
        })
        return
      }

      // 確認彈窗
      const confirmResult = await Swal.fire({
        title: isEdit.value ? '確認更新？' : '確認新增？',
        text: isEdit.value ? '即將更新此人員資料' : '即將新增維護人員',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#409eff',
        cancelButtonColor: '#909399',
        confirmButtonText: '確認送出',
        cancelButtonText: '取消',
        showClass: { popup: 'animate__animated animate__fadeInDown animate__faster' },
        hideClass: { popup: 'animate__animated animate__fadeOutUp animate__faster' },
      })

      if (!confirmResult.isConfirmed) return

      submitting.value = true
      try {
        if (isEdit.value) {
          await maintenanceApi.updateStaff(staffId, form)
          await Swal.fire({
            icon: 'success',
            title: '更新成功！',
            html: `<span class="text-success">人員 <b>${form.staffName}</b> 資料已更新</span>`,
            timer: 2000,
            timerProgressBar: true,
            showConfirmButton: false,
            showClass: { popup: 'animate__animated animate__bounceIn' },
          })
        } else {
          await maintenanceApi.createStaff(form)
          await Swal.fire({
            icon: 'success',
            title: '新增成功！',
            html: `<span class="text-success">加入新成員 <b>${form.staffName}</b> 加入團隊</span>`,
            timer: 2000,
            timerProgressBar: true,
            showConfirmButton: false,
            showClass: { popup: 'animate__animated animate__tada' },
          })
        }

        router.push('/admin/staff-list')
      } catch {
        // 錯誤已由 http.js 攔截器處理
      } finally {
        submitting.value = false
      }
    })
  }

  const handleCancel = async () => {
    // 保持你原本的「判斷有無填寫」條件
    if (form.staffName || form.staffCompany || form.staffPhone) {
      const result = await Swal.fire({
        title: '確定要離開嗎？',
        text: '您填寫的資料將不會被保存',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#e6a23c',
        cancelButtonColor: '#909399',
        confirmButtonText: '離開',
        cancelButtonText: '繼續編輯',
        showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
      })
      if (!result.isConfirmed) return
    }
    router.push('/admin/staff-list')
  }

  return {
    // state
    isEdit,
    formRef,
    loading,
    submitting,
    formVisible,
    form,
    rules,

    // actions
    init,
    submitForm,
    handleCancel,
  }
}
