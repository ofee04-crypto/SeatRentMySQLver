//本地網址開放測試
window.APP_CONFIG = {
  // 👇 每次 Tunnel 重開，只要將後端Tunnel放進來這一行
  API_URL: 'https://transaction-shell-routines-colored.trycloudflare.com',
}

//第一次 必須在終端機 輸入winget install --id Cloudflare.cloudflared 輸入Y
//Tunnel網址為隨機生成，每次需重新複製
//cloudflared tunnel --url http://localhost:8080
