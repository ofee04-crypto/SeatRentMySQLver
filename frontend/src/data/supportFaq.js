/**
 * 客服支援 FAQ 資料來源
 * 結構：[{ category, items: [{ q, a, tags, keywords }] }]
 */
export const faqData = [
  {
    category: '租借相關',
    icon: 'fas fa-map-marker-alt',
    color: '#409eff',
    items: [
      {
        q: '如何租借座位？',
        a: `
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>前往「尋找租借點」頁面，選擇您想要的地點</li>
            <li>點擊地圖上的據點標記，查看可用座位數量</li>
            <li>點擊「租借」按鈕，選擇座位並確認</li>
            <li>完成付款後即可開始使用</li>
          </ol>
          <p style="color: #909399; font-size: 13px; margin-top: 10px;">
            <i class="fas fa-lightbulb"></i> 提示：建議先登入會員，可累積點數享有優惠！
          </p>
        `,
        tags: ['租借', '座位', '新手'],
        keywords: ['怎麼租', '如何使用', '租借流程'],
      },
      {
        q: '租借費率如何計算？',
        a: `
          <div style="background: #f0f9ff; padding: 15px; border-radius: 8px; border-left: 4px solid #409eff;">
            <p style="margin: 0 0 10px; font-weight: 600;">基本費率</p>
            <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
              <li>基本費：20 NTD（起租費）</li>
              <li>時段費：30 NTD / 每 30 分鐘</li>
              <li>會員點數折抵：100 點 = 1 NTD</li>
            </ul>
          </div>
          <p style="color: #67c23a; font-size: 13px; margin-top: 10px;">
            <i class="fas fa-gift"></i> 首次租借享 9 折優惠！
          </p>
        `,
        tags: ['費率', '計費', '價格'],
        keywords: ['多少錢', '費用', '收費', '計算'],
      },
      {
        q: '可以提前結束租借嗎？',
        a: `
          <p>可以的！您可以在租借期間隨時歸還座位，系統會依實際使用時間計費。</p>
          <div style="margin-top: 10px; padding: 10px; background: #fef0f0; border-radius: 6px;">
            <p style="margin: 0; color: #f56c6c; font-size: 13px;">
              <i class="fas fa-exclamation-triangle"></i> 注意：基本費 20 NTD 不退還
            </p>
          </div>
        `,
        tags: ['歸還', '提前結束'],
        keywords: ['提前還', '中途離開', '退費'],
      },
      {
        q: '找不到可用座位怎麼辦？',
        a: `
          <p>若目前據點無可用座位，您可以：</p>
          <ul style="line-height: 1.8; padding-left: 20px;">
            <li>點擊「附近據點」查看其他地點</li>
            <li>聯繫客服詢問座位釋出時間</li>
          </ul>
        `,
        tags: ['座位', '滿位'],
        keywords: ['沒有座位', '滿了', '額滿'],
      },
    ],
  },
  {
    category: '維修保養',
    icon: 'fas fa-tools',
    color: '#e6a23c',
    items: [
      {
        q: '遇到設備故障該如何回報？',
        a: `
          <p>請立即透過以下方式回報：</p>
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>點擊頁面底部「<b>我要回報問題</b>」按鈕</li>
            <li>選擇故障的機台或椅子</li>
            <li>填寫問題描述並上傳照片（必須）</li>
            <li>送出後會立即通知維修人員處理</li>
          </ol>
          <p style="color: #f56c6c; font-size: 13px; margin-top: 10px;">
            <i class="fas fa-exclamation-circle"></i> 緊急故障請直接聯繫現場人員！
          </p>
        `,
        tags: ['故障', '回報', '維修'],
        keywords: ['壞了', '不能用', '故障', '報修'],
      },
      {
        q: '設備維修需要多久時間？',
        a: `
          <div style="background: #fff7e6; padding: 15px; border-radius: 8px;">
            <p style="margin: 0 0 10px; font-weight: 600;">一般維修時程</p>
            <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
              <li><b style="color: #f56c6c;">緊急故障</b>：2 小時內到場</li>
              <li><b style="color: #e6a23c;">一般維修</b>：24 小時內處理</li>
              <li><b style="color: #67c23a;">定期保養</b>：每月排程執行</li>
            </ul>
          </div>
          <p style="color: #909399; font-size: 13px; margin-top: 10px;">
            您可以在「維修工單管理」查看處理進度
          </p>
        `,
        tags: ['維修', '時間', '進度'],
        keywords: ['多久', '要多少時間', '修理'],
      },
      {
        q: '維修期間可以使用其他座位嗎？',
        a: `
          <p>當然可以！系統會自動標示維修中的座位為「不可租借」，您可以：</p>
          <ul style="line-height: 1.8; padding-left: 20px;">
            <li>選擇同據點的其他可用座位</li>
            <li>切換到附近的其他據點</li>
            <li>享有維修補償點數（依情況而定）</li>
          </ul>
        `,
        tags: ['維修', '座位', '替代'],
        keywords: ['換座位', '其他位置'],
      },
    ],
  },
  {
    category: '付款相關',
    icon: 'fas fa-credit-card',
    color: '#67c23a',
    items: [
      {
        q: '支援哪些付款方式？',
        a: `
          <div style="background: #f0f9ff; padding: 15px; border-radius: 8px;">
            <p style="margin: 0 0 10px; font-weight: 600;">支援付款方式</p>
            <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
              <li><i class="fas fa-credit-card" style="color: #409eff;"></i> 信用卡 / 金融卡</li>
              <li><i class="fas fa-mobile-alt" style="color: #67c23a;"></i> LINE Pay / Apple Pay</li>
              <li><i class="fas fa-university" style="color: #e6a23c;"></i> ATM 轉帳</li>
              <li><i class="fas fa-store" style="color: #f56c6c;"></i> 超商代碼繳費</li>
            </ul>
          </div>
          <p style="color: #909399; font-size: 13px; margin-top: 10px;">
            所有付款均透過 ECPay 綠界金流，安全有保障
          </p>
        `,
        tags: ['付款', '支付', '方式'],
        keywords: ['怎麼付', '信用卡', '行動支付', 'LINE Pay'],
      },
      {
        q: '付款失敗怎麼辦？',
        a: `
          <p>若遇到付款失敗，請依以下步驟處理：</p>
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>確認信用卡額度是否充足</li>
            <li>檢查網路連線是否穩定</li>
            <li>嘗試更換付款方式</li>
            <li>仍無法解決請聯繫客服（附上訂單編號）</li>
          </ol>
          <div style="margin-top: 10px; padding: 10px; background: #fef0f0; border-radius: 6px;">
            <p style="margin: 0; color: #f56c6c; font-size: 13px;">
              <i class="fas fa-info-circle"></i> 付款失敗不會扣款，請放心重試
            </p>
          </div>
        `,
        tags: ['付款', '失敗', '錯誤'],
        keywords: ['付不了', '扣款失敗', '無法付款'],
      },
      {
        q: '如何申請退款？',
        a: `
          <p>退款申請條件：</p>
          <ul style="line-height: 1.8; padding-left: 20px;">
            <li>設備故障導致無法使用</li>
            <li>系統錯誤重複扣款</li>
            <li>其他非使用者因素</li>
          </ul>
          <p style="margin-top: 10px;">申請方式：</p>
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>前往「訂單管理」找到該筆訂單</li>
            <li>點擊「申請退款」並填寫原因</li>
            <li>客服審核通過後 3-7 工作天退款</li>
          </ol>
        `,
        tags: ['退款', '退費'],
        keywords: ['退錢', '申請退款', '取消訂單'],
      },
    ],
  },
  {
    category: '帳號與其他',
    icon: 'fas fa-user-circle',
    color: '#909399',
    items: [
      {
        q: '如何修改個人資料？',
        a: `
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>點擊右上角「會員中心」</li>
            <li>選擇「個人資料」</li>
            <li>修改完成後點擊「儲存」</li>
          </ol>
          <p style="color: #909399; font-size: 13px; margin-top: 10px;">
            <i class="fas fa-shield-alt"></i> 修改手機號碼需要簡訊驗證
          </p>
        `,
        tags: ['帳號', '個人資料'],
        keywords: ['修改資料', '更改', '編輯'],
      },
      {
        q: '忘記密碼怎麼辦？',
        a: `
          <ol style="line-height: 1.8; padding-left: 20px;">
            <li>點擊登入頁面的「忘記密碼」</li>
            <li>輸入註冊時的 Email</li>
            <li>前往信箱點擊重設密碼連結</li>
            <li>設定新密碼並重新登入</li>
          </ol>
          <div style="margin-top: 10px; padding: 10px; background: #fef0f0; border-radius: 6px;">
            <p style="margin: 0; color: #f56c6c; font-size: 13px;">
              <i class="fas fa-exclamation-triangle"></i> 重設連結 30 分鐘內有效
            </p>
          </div>
        `,
        tags: ['密碼', '登入'],
        keywords: ['忘記密碼', '重設', '登不進去'],
      },
      {
        q: '點數如何累積與使用？',
        a: `
          <div style="background: #f0f9ff; padding: 15px; border-radius: 8px;">
            <p style="margin: 0 0 10px; font-weight: 600;">點數累積方式</p>
            <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
              <li>每消費 1 NTD = 1 點</li>
              <li>首次租借贈送 500 點</li>
              <li>推薦好友註冊贈送 200 點</li>
              <li>每月生日贈送 100 點</li>
            </ul>
          </div>
          <div style="margin-top: 10px; background: #f0f9eb; padding: 15px; border-radius: 8px;">
            <p style="margin: 0 0 10px; font-weight: 600;">點數使用方式</p>
            <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
              <li>100 點 = 1 NTD 折抵</li>
              <li>兌換商家優惠券</li>
              <li>參加會員專屬活動</li>
            </ul>
          </div>
        `,
        tags: ['點數', '會員', '優惠'],
        keywords: ['積分', '紅利', '折扣'],
      },
      {
        q: '如何聯繫客服？',
        a: `
          <p>我們提供多種聯繫方式：</p>
          <ul style="line-height: 1.8; padding-left: 20px;">
            <li><i class="fas fa-headset" style="color: #409eff;"></i> 客服專線：0968-179-091（24 小時）</li>
            <li><i class="fas fa-envelope" style="color: #67c23a;"></i> Email：support@seatrentsys.com</li>
            <li><i class="fas fa-comment-dots" style="color: #e6a23c;"></i> LINE 官方帳號：@seatrent</li>
            <li><i class="fas fa-exclamation-circle" style="color: #f56c6c;"></i> 緊急回報：點擊下方「我要回報問題」</li>
          </ul>
          <p style="color: #909399; font-size: 13px; margin-top: 10px;">
            平均回覆時間：1 小時內（尖峰時段可能較長）
          </p>
        `,
        tags: ['客服', '聯繫', '幫助'],
        keywords: ['客服', '聯絡', '電話', '信箱'],
      },
    ],
  },
]
