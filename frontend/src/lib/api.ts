import axios from 'axios';
import { AnalysisItem, RegionSummary } from '@/types';

const api = axios.create({ baseURL: '/api' });

export const analysisApi = {
  getRegionSummary: (regionCode: string, yearMonth?: string, tradeType = 'TRADE') =>
    api.get<RegionSummary>(`/analysis/summary/region/${regionCode}`, {
      params: { yearMonth, tradeType }
    }).then(r => r.data),

  getTopRising: (yearMonth?: string, tradeType = 'TRADE', limit = 10) =>
    api.get<AnalysisItem[]>('/analysis/top/rising', { params: { yearMonth, tradeType, limit } }).then(r => r.data),

  getTopFalling: (yearMonth?: string, tradeType = 'TRADE', limit = 10) =>
    api.get<AnalysisItem[]>('/analysis/top/falling', { params: { yearMonth, tradeType, limit } }).then(r => r.data),

  getHighVolatility: (yearMonth?: string, tradeType = 'TRADE', limit = 10) =>
    api.get<AnalysisItem[]>('/analysis/top/volatile', { params: { yearMonth, tradeType, limit } }).then(r => r.data),

  getRecommended: (yearMonth?: string) =>
    api.get<AnalysisItem[]>('/analysis/recommended', { params: { yearMonth } }).then(r => r.data),

  runAnalysis: (yearMonth?: string) =>
    api.post('/analysis/run', null, { params: { yearMonth } }).then(r => r.data),
};

export const notificationApi = {
  test: () => api.post('/notification/test').then(r => r.data),
  sendAll: (yearMonth?: string) => api.post('/notification/all', null, { params: { yearMonth } }).then(r => r.data),
};
