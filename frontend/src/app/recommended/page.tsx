'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { analysisApi } from '@/lib/api';
import { PropertyCard } from '@/components/PropertyCard';
import { RECOMMEND_REASON_MAP } from '@/types';
import type { AnalysisItem } from '@/types';
import { Star, Calendar, Filter } from 'lucide-react';
import clsx from 'clsx';

type ReasonFilter = 'ALL' | 'REBOUND_AFTER_DROP' | 'STABLE_UPTREND' | 'HIGH_TRADE_VOLUME' | 'UNDERVALUED';

const reasonFilters: { key: ReasonFilter; label: string; color: string }[] = [
  { key: 'ALL', label: '전체', color: 'bg-gray-100 text-gray-700' },
  { key: 'REBOUND_AFTER_DROP', label: '하락 후 반등', color: 'bg-emerald-100 text-emerald-700' },
  { key: 'STABLE_UPTREND', label: '저변동 안정상승', color: 'bg-blue-100 text-blue-700' },
  { key: 'HIGH_TRADE_VOLUME', label: '거래량 급증', color: 'bg-amber-100 text-amber-700' },
  { key: 'UNDERVALUED', label: '저평가 매물', color: 'bg-purple-100 text-purple-700' },
];

function getCurrentYearMonth(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  return `${y}${m}`;
}

export default function RecommendedPage() {
  const [filter, setFilter] = useState<ReasonFilter>('ALL');
  const yearMonth = getCurrentYearMonth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['recommended', yearMonth],
    queryFn: () => analysisApi.getRecommended(yearMonth),
  });

  const filtered: AnalysisItem[] =
    filter === 'ALL'
      ? (data ?? [])
      : (data ?? []).filter(item => item.recommendReason === filter);

  // 이유별 카운트
  const countByReason = (data ?? []).reduce<Record<string, number>>((acc, item) => {
    if (item.recommendReason) {
      acc[item.recommendReason] = (acc[item.recommendReason] ?? 0) + 1;
    }
    return acc;
  }, {});

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">추천 매물</h1>
          <p className="text-sm text-gray-500 mt-1">AI 분석 기반 추천 아파트 목록</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500 bg-white border border-gray-100 rounded-lg px-3 py-2">
          <Calendar size={14} />
          <span>{yearMonth.slice(0, 4)}년 {yearMonth.slice(4)}월 기준</span>
        </div>
      </div>

      {/* 요약 배지 */}
      <div className="flex flex-wrap gap-3 mb-6">
        {Object.entries(RECOMMEND_REASON_MAP).map(([key, label]) => {
          const count = countByReason[key] ?? 0;
          const reasonFilter = reasonFilters.find(r => r.key === key);
          return (
            <div
              key={key}
              className={clsx(
                'flex items-center gap-2 px-3 py-2 rounded-lg text-sm',
                reasonFilter?.color ?? 'bg-gray-100 text-gray-700'
              )}
            >
              <Star size={13} />
              <span className="font-medium">{label}</span>
              <span className="font-bold">{count}건</span>
            </div>
          );
        })}
      </div>

      {/* 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6">
        <div className="flex items-center gap-2 flex-wrap">
          <Filter size={14} className="text-gray-400" />
          <span className="text-sm text-gray-500 mr-1">필터:</span>
          {reasonFilters.map(rf => (
            <button
              key={rf.key}
              onClick={() => setFilter(rf.key)}
              className={clsx(
                'px-3 py-1.5 rounded-full text-xs font-medium transition-colors border',
                filter === rf.key
                  ? `${rf.color} border-current`
                  : 'bg-white text-gray-500 border-gray-200 hover:border-gray-300'
              )}
            >
              {rf.label}
              {rf.key !== 'ALL' && (
                <span className="ml-1">({countByReason[rf.key] ?? 0})</span>
              )}
              {rf.key === 'ALL' && (
                <span className="ml-1">({(data ?? []).length})</span>
              )}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-700 text-sm">
          데이터를 불러오는 중 오류가 발생했습니다. 백엔드 서버 연결을 확인해주세요.
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16">
          <Star size={32} className="text-gray-300 mx-auto mb-3" />
          <p className="text-sm text-gray-400">추천 매물이 없습니다</p>
          <p className="text-xs text-gray-300 mt-1">분석을 실행하거나 필터를 변경해보세요</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((item, idx) => (
            <PropertyCard key={`${item.apartName}-${idx}`} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
