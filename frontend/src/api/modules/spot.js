import http from '../http';

/**
 * @description 獲取所有站點資料
 * @returns {Promise}
 */
export const getAllSpots = () => {
  // 假設的後端API端點為 /api/v1/spots
  // 如果您的端點不同，請修改此處的 URL
  return http.get('/api/v1/spots');
};
