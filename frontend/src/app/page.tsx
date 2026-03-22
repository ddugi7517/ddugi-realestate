'use client';
import { useQuery } from '@tanstack/react-query';
import { analysisApi } from '@/lib/api';
import { StatCard } from '@/components/StatCard';
import { PriceChart } from '@/components/PriceChart';
import { REGION_MAP } from '@/types';
import type { AnalysisItem } from '@/types';
import { TrendingUp, TrendingDown, Building2, Calendar } from 'lucide-react';
import clsx from 'clsx';

function formatPrice(price: number): string {
  if (price >= 10000) {
    const eok = Math.floor(price / 10000);
    const man = price % 10000;
    return man > 0 ? `${eok}억 ${man.toLocaleString()}만` : `${eok}억`;
  }
  return `${price.toLocaleString()}만`;
}

function getCurrentYearMonth(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  return `${y}${m}`;
}

function PropertyTable({ items, type }: { items: AnalysisItem[]; type: 'rising' | 'falling' }) {
  const isRising = type === 'rising';
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100">
            <th className="text-left py-2 px-3 text-xs font-medium text-gray-500">아파트명</th>
            <th className="text-left py-2 px-3 text-xs font-medium text-gray-500">지역</th>
            <th className="text-right py-2 px-3 text-xs font-medium text-gray-500">평수</th>
            <th className="text-right py-2 px-3 text-xs font-medium text-gray-500">현재가</th>
            <th className="text-right py-2 px-3 text-xs font-medium text-gray-500">등락률</th>
          </tr>
        </thead>
        <tbody>
          {items.slice(0, 5).map((item, idx) => (
            <tr key={`${item.apartName}-${idx}`} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
              <td className="py-2.5 px-3 font-medium text-gray-900 max-w-[160px] truncate">
                {item.apartName}
              </td>
              <td className="py-2.5 px-3 text-gray-500">
                {REGION_MAP[item.regionCode] ?? item.regionCode}
              </td>
              <td className="py-2.5 px-3 text-right text-gray-600">
                {item.exclusiveAreaPyeong}평
              </td>
              <td className="py-2.5 px-3 text-right text-gray-900 font-medium">
                {formatPrice(item.currentAvgPrice)}
              </td>
              <td className={clsx(
                'py-2.5 px-3 text-right font-semibold',
                isRising ? 'text-emerald-600' : 'text-red-500'
              )}>
                {(item.changeRate ?? 0) > 0 ? '+' : ''}{(item.changeRate ?? 0).toFixed(2)}%
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function DashboardPage() {
  const yearMonth = getCurrentYearMonth();

  const { data: rising, isLoading: risingLoading, error: risingError } = useQuery({
    queryKey: ['topRising', yearMonth],
    queryFn: () => analysisApi.getTopRising(yearMonth),
  });

  const { data: falling, isLoading: fallingLoading, error: fallingError } = useQuery({
    queryKey: ['topFalling', yearMonth],
    queryFn: () => analysisApi.getTopFalling(yearMonth),
  });

  const isLoading = risingLoading || fallingLoading;
  const hasError = risingError || fallingError;

  const allItems = [...(rising ?? []), ...(falling ?? [])];
  const totalCount = allItems.length;
  const risingCount = (rising ?? []).length;
  const fallingCount = (falling ?? []).length;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
          <p className="text-sm text-gray-500 mt-1">서울 아파트 실거래가 분석 현황</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500 bg-white border border-gray-100 rounded-lg px-3 py-2">
          <Calendar size={14} />
          <span>{yearMonth.slice(0, 4)}년 {yearMonth.slice(4)}월 기준</span>
        </div>
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard
          title="전체 분석 단지"
          value={isLoading ? '-' : `${totalCount}개`}
          subtitle="급등+급락 합산"
          icon={Building2}
          color="blue"
        />
        <StatCard
          title="상승 단지"
          value={isLoading ? '-' : `${risingCount}개`}
          subtitle="전월 대비 상승"
          icon={TrendingUp}
          color="emerald"
        />
        <StatCard
          title="하락 단지"
          value={isLoading ? '-' : `${fallingCount}개`}
          subtitle="전월 대비 하락"
          icon={TrendingDown}
          color="red"
        />
        <StatCard
          title="이번달 기준"
          value={isLoading ? '-' : `${yearMonth.slice(0, 4)}.${yearMonth.slice(4)}`}
          subtitle="분석 기준 년월"
          icon={Calendar}
          color="amber"
        />
      </div>

      {hasError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-700 text-sm">
          데이터를 불러오는 중 오류가 발생했습니다. 백엔드 서버 연결을 확인해주세요.
        </div>
      )}

      {/* 테이블 영역 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={16} className="text-emerald-500" />
            <h2 className="text-sm font-semibold text-gray-700">급등 TOP 5</h2>
          </div>
          {isLoading ? (
            <div className="flex items-center justify-center h-32">
              <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (rising ?? []).length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">데이터가 없습니다</p>
          ) : (
            <PropertyTable items={rising ?? []} type="rising" />
          )}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center gap-2 mb-4">
            <TrendingDown size={16} className="text-red-500" />
            <h2 className="text-sm font-semibold text-gray-700">급락 TOP 5</h2>
          </div>
          {isLoading ? (
            <div className="flex items-center justify-center h-32">
              <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (falling ?? []).length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">데이터가 없습니다</p>
          ) : (
            <PropertyTable items={falling ?? []} type="falling" />
          )}
        </div>
      </div>

      {/* 차트 */}
      {!isLoading && allItems.length > 0 && (
        <PriceChart
          data={allItems.slice(0, 15)}
          title="등락률 분포 (급등/급락 TOP 15)"
        />
      )}
    </div>
  );
}
