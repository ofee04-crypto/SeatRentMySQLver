import axios from 'axios';

const tdxApi = axios.create({
  baseURL: 'https://tdx.transportdata.tw/api/basic',
});

const authApi = axios.create({
  baseURL: 'https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect',
});

// 提示: 請務必將以下預留位置替換為您真實的 TDX API 憑證
const clientId = 'YOUR_CLIENT_ID'; // <-- 請填入您的 Client ID
const clientSecret = 'YOUR_CLIENT_SECRET'; // <-- 請填入您的 Client Secret

let accessToken = null;
let tokenExpiryTime = 0;

/**
 * 從 TDX 認證伺服器獲取 access token。
 * 此函數會快取 token，避免不必要的重複認證。
 * @returns {Promise<string>} The access token.
 */
async function getAccessToken() {
  // 如果 token 仍然有效，直接返回快取的 token
  if (accessToken && Date.now() < tokenExpiryTime) {
    return accessToken;
  }

  // 檢查是否提供了憑證
  if (clientId === 'YOUR_CLIENT_ID' || clientSecret === 'YOUR_CLIENT_SECRET') {
    const errorMessage = "TDX API 憑證未設定。請在 'frontend/src/api/modules/tdx.js' 檔案中設定您的 Client ID 和 Client Secret。";
    console.error(errorMessage);
    // 直接拋出錯誤，讓呼叫者可以捕捉到這個明確的問題
    throw new Error(errorMessage);
  }

  try {
    const params = new URLSearchParams();
    params.append('grant_type', 'client_credentials');
    params.append('client_id', clientId);
    params.append('client_secret', clientSecret);

    const response = await authApi.post('/token', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });

    accessToken = response.data.access_token;
    // 將 token 的過期時間設定為比實際過期時間提早 20 秒，以增加緩衝
    tokenExpiryTime = Date.now() + (response.data.expires_in - 20) * 1000;
    
    return accessToken;
  } catch (error) {
    console.error('從 TDX 獲取 access token 失敗:', error);
    // 將原始錯誤向上拋出
    throw error;
  }
}

/**
 * 獲取所有觀光景點資料。
 * @returns {Promise<Array>} 景點列表。
 */
export async function getScenicSpots() {
  const token = await getAccessToken();
  const response = await tdxApi.get('/v2/Tourism/ScenicSpot', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      '$format': 'JSON',
    }
  });
  return response.data;
}

/**
 * 獲取所有餐飲資料。
 * @returns {Promise<Array>} 餐廳列表。
 */
export async function getRestaurants() {
  const token = await getAccessToken();
  const response = await tdxApi.get('/v2/Tourism/Restaurant', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    params: {
      '$format': 'JSON',
    }
  });
  return response.data;
}
