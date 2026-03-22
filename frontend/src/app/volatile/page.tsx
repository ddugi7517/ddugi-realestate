'use client';
import { useQuery } from '@tanstack/react-query';
import { analysisApi } from '@/lib/api';
import { REGION_MAP } from '@/types';
import { Activity, Calendar, TrendingUp, TrendingDown } from 'lucide-react';
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

function VolatilityBadge({ index }: { index: number }) {
  const level = index >= 10 ? 'high' : index >= 5 ? 'medium' : 'low';
  const styles = {
    high: 'bg-red-100 text-red-700',
    medium: 'bg-amber-100 text-amber-700',
    low: 'bg-gray-100 text-gray-600',
  };
  const labels = { high: '매우높음', medium: '높음', low: '보통' };

  return (
    <span className={clsx('px-2 py-0.5 rounded-full text-xs font-medium', styles[level])}>
      {labels[level]}
    </span>
  );
}

export default function VolatilePage() {
  const yearMonth = getCurrentYearMonth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['highVolatility', yearMonth],
    queryFn: () => analysisApi.getHighVolatility(yearMonth, 'TRADE', 20),
  });

  const sorted = [...(data ?? [])].sort(
    (a, b) => (b.volatilityIndex ?? 0) - (a.volatilityIndex ?? 0)
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">고변동성 매물</h1>
          <p className="text-sm text-gray-500 mt-1">변동성 지수 기준 정렬된 아파트 목록</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500 bg-white border border-gray-100 rounded-lg px-3 py-2">
          <Calendar size={14} />
          <span>{yearMonth.slice(0, 4)}년 {yearMonth.slice(4)}월 기준</span>
        </div>
      </div>

      {/* 안내 */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 mb-6">
        <div className="flex items-start gap-3">
          <Activity size={16} className="text-amber-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-sm font-medium text-amber-800">변동성 지수 안내</p>
            <p className="text-xs text-amber-700 mt-0.5">
              변동성 지수는 가격 변동의 표준편차 기반으로 산출됩니다.
              높을수록 가격 변동이 크며, 투자 시 주의가 필요합니다.
            </p>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-700 text-sm">
          데이터를 불러오는 중 오류가 발생했습니다. 백엔드 서버 연결을 확인해주세요.
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : sorted.length === 0 ? (
          <div className="text-center py-16">
            <Activity size={32} className="text-gray-300 mx-auto mb-3" />
            <p className="text-sm text-gray-400">고변동성 데이터가 없습니다</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th className="text-left py-3 px-4 text-xs font-medium text-gray-500">순위</th>
                  <th className="text-left py-3 px-4 text-xs font-medium text-gray-500">아파트명</th>
                  <th className="text-left py-3 px-4 text-xs font-medium text-gray-500">지역</th>
                  <th className="text-right py-3 px-4 text-xs font-medium text-gray-500">평수</th>
                  <th className="text-right py-3 px-4 text-xs font-medium text-gray-500">현재가</th>
                  <th className="text-right py-3 px-4 text-xs font-medium text-gray-500">등락률</th>
                  <th className="text-right py-3 px-4 text-xs font-medium text-gray-500">변동성 지수</th>
                  <th className="text-center py-3 px-4 text-xs font-medium text-gray-500">위험도</th>
                  <th className="text-right py-3 px-4 text-xs font-medium text-gray-500">거래건수</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((item, idx) => {
                  const changeRate = item.changeRate ?? 0;
                  const isRising = changeRate > 0;
                  const isFalling = changeRate < 0;
                  return (
                    <tr
                      key={`${item.apartName}-${idx}`}
                      className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                    >
                      <td className="py-3 px-4 text-gray-400 font-medium">#{idx + 1}</td>
                      <td className="py-3 px-4">
                        <span className="font-medium text-gray-900 max-w-[160px] block truncate">
                          {item.apartName}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-500">
                        {REGION_MAP[item.regionCode] ?? item.regionCode}
                      </td>
                      <td className="py-3 px-4 text-right text-gray-600">
                        {item.exclusiveAreaPyeong}평
                      </td>
                      <td className="py-3 px-4 text-right font-medium text-gray-900">
                        {formatPrice(item.currentAvgPrice)}
                      </td>
                      <td className={clsx(
                        'py-3 px-4 text-right font-semibold',
                        isRising ? 'text-emerald-600' : isFalling ? 'text-red-500' : 'text-gray-500'
                      )}>
                        <span className="flex items-center justify-end gap-1">
                          {isRising ? <TrendingUp size={12} /> : isFalling ? <TrendingDown size={12} /> : null}
                          {changeRate > 0 ? '+' : ''}{changeRate.toFixed(2)}%
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-gray-900">
                        {item.volatilityIndex?.toFixed(2) ?? '-'}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {item.volatilityIndex !== null ? (
                          <VolatilityBadge index={item.volatilityIndex} />
                        ) : '-'}
                      </td>
                      <td className="py-3 px-4 text-right text-gray-600">
                        {item.tradeCount}건
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
