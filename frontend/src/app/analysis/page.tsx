'use client';
import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { analysisApi } from '@/lib/api';
import { RegionSelector } from '@/components/RegionSelector';
import { PropertyCard } from '@/components/PropertyCard';
import { StatCard } from '@/components/StatCard';
import { REGION_MAP } from '@/types';
import type { AnalysisItem } from '@/types';
import { MapPin, TrendingUp, TrendingDown, Star, Calendar, Ruler } from 'lucide-react';
import clsx from 'clsx';

type TabKey = 'rising' | 'falling' | 'recommended';

const tabs: { key: TabKey; label: string }[] = [
  { key: 'rising', label: '급등' },
  { key: 'falling', label: '급락' },
  { key: 'recommended', label: '추천' },
];

const PYEONG_OPTIONS = [
  { label: '전체', min: 0, max: Infinity },
  { label: '10평대', min: 10, max: 19 },
  { label: '20평대', min: 20, max: 29 },
  { label: '30평대', min: 30, max: 39 },
  { label: '40평대', min: 40, max: 49 },
  { label: '50평 이상', min: 50, max: Infinity },
];

const PIE_COLORS = ['#10b981', '#ef4444', '#9ca3af'];

function getAvailableMonths(): { value: string; label: string }[] {
  const months = [];
  const start = new Date(2025, 0); // 2025-01
  const now = new Date();
  const end = new Date(now.getFullYear(), now.getMonth());

  for (let d = new Date(start); d <= end; d.setMonth(d.getMonth() + 1)) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    months.push({ value: `${y}${m}`, label: `${y}년 ${m}월` });
  }
  return months.reverse(); // 최신순
}

function getCurrentYearMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export default function AnalysisPage() {
  const [regionCode, setRegionCode] = useState('11650');
  const [activeTab, setActiveTab] = useState<TabKey>('rising');
  const [yearMonth, setYearMonth] = useState(getCurrentYearMonth);
  const [pyeongIdx, setPyeongIdx] = useState(0);

  const months = useMemo(() => getAvailableMonths(), []);

  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['regionSummary', regionCode, yearMonth],
    queryFn: () => analysisApi.getRegionSummary(regionCode, yearMonth),
  });

  const filterByPyeong = (items: AnalysisItem[]) => {
    const { min, max } = PYEONG_OPTIONS[pyeongIdx];
    if (min === 0 && max === Infinity) return items;
    return items.filter(item => item.exclusiveAreaPyeong >= min && item.exclusiveAreaPyeong <= max);
  };

  const pieData = summary
    ? [
        { name: '상승', value: summary.risingCount },
        { name: '하락', value: summary.fallingCount },
        { name: '보합', value: summary.stableCount },
      ]
    : [];

  const allTabItems: Record<TabKey, AnalysisItem[]> = {
    rising: summary?.topRising ?? [],
    falling: summary?.topFalling ?? [],
    recommended: summary?.recommended ?? [],
  };

  const tabItems: Record<TabKey, AnalysisItem[]> = {
    rising: filterByPyeong(allTabItems.rising),
    falling: filterByPyeong(allTabItems.falling),
    recommended: filterByPyeong(allTabItems.recommended),
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">지역별 분석</h1>
          <p className="text-sm text-gray-500 mt-1">구별 아파트 시세 현황</p>
        </div>
      </div>

      {/* 필터 영역 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          {/* 지역 선택 */}
          <div className="flex items-center gap-2">
            <MapPin size={15} className="text-blue-500 shrink-0" />
            <span className="text-sm font-medium text-gray-600 whitespace-nowrap">지역</span>
            <RegionSelector value={regionCode} onChange={setRegionCode} />
          </div>

          <div className="h-5 w-px bg-gray-200" />

          {/* 월 선택 */}
          <div className="flex items-center gap-2">
            <Calendar size={15} className="text-blue-500 shrink-0" />
            <span className="text-sm font-medium text-gray-600 whitespace-nowrap">기준월</span>
            <select
              value={yearMonth}
              onChange={e => setYearMonth(e.target.value)}
              className="text-sm border border-gray-200 rounded-lg px-3 py-1.5 text-gray-700 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent cursor-pointer"
            >
              {months.map(m => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </div>

          <div className="h-5 w-px bg-gray-200" />

          {/* 평형 선택 */}
          <div className="flex items-center gap-2">
            <Ruler size={15} className="text-blue-500 shrink-0" />
            <span className="text-sm font-medium text-gray-600 whitespace-nowrap">평형</span>
            <div className="flex gap-1">
              {PYEONG_OPTIONS.map((opt, i) => (
                <button
                  key={opt.label}
                  onClick={() => setPyeongIdx(i)}
                  className={clsx(
                    'px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
                    pyeongIdx === i
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  )}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
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
      ) : summary ? (
        <>
          {/* 통계 카드 */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatCard title="전체 단지" value={`${summary.totalCount}개`} icon={MapPin} color="blue" />
            <StatCard title="상승 단지" value={`${summary.risingCount}개`} icon={TrendingUp} color="emerald" />
            <StatCard title="하락 단지" value={`${summary.fallingCount}개`} icon={TrendingDown} color="red" />
            <StatCard
              title="평균 등락률"
              value={`${summary.avgChangeRate >= 0 ? '+' : ''}${summary.avgChangeRate.toFixed(2)}%`}
              icon={Star}
              color={summary.avgChangeRate >= 0 ? 'emerald' : 'red'}
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            {/* 파이차트 */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-4">상승/하락/보합 비율</h3>
              {pieData.every(d => d.value === 0) ? (
                <p className="text-sm text-gray-400 text-center py-12">데이터가 없습니다</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={80}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {pieData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={PIE_COLORS[index]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value: number) => [`${value}개`, '']} />
                    <Legend formatter={(value: string) => <span className="text-xs text-gray-600">{value}</span>} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>

            {/* 요약 정보 */}
            <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-100 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-4">{REGION_MAP[regionCode]} 요약</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-3">
                  <div>
                    <p className="text-xs text-gray-500">분석 기준월</p>
                    <p className="text-sm font-medium text-gray-900">
                      {summary.yearMonth.slice(0, 4)}년 {summary.yearMonth.slice(4)}월
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">거래 유형</p>
                    <p className="text-sm font-medium text-gray-900">
                      {{ TRADE: '매매', JEONSE: '전세', MONTHLY: '월세' }[summary.tradeType] ?? summary.tradeType}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">보합 단지</p>
                    <p className="text-sm font-medium text-gray-900">{summary.stableCount}개</p>
                  </div>
                </div>
                <div className="space-y-3">
                  <div>
                    <p className="text-xs text-gray-500">고변동성 단지</p>
                    <p className="text-sm font-medium text-gray-900">{summary.highVolatility.length}개</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">추천 매물</p>
                    <p className="text-sm font-medium text-amber-600">{summary.recommended.length}개</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">상승률</p>
                    <p className="text-sm font-medium text-emerald-600">
                      {summary.totalCount > 0
                        ? `${((summary.risingCount / summary.totalCount) * 100).toFixed(1)}%`
                        : '-'}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 탭 */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
            <div className="flex items-center justify-between mb-5">
              <div className="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
                {tabs.map(tab => (
                  <button
                    key={tab.key}
                    onClick={() => setActiveTab(tab.key)}
                    className={clsx(
                      'px-4 py-1.5 rounded-md text-sm font-medium transition-colors',
                      activeTab === tab.key
                        ? 'bg-white text-gray-900 shadow-sm'
                        : 'text-gray-500 hover:text-gray-700'
                    )}
                  >
                    {tab.label}
                    <span className="ml-1.5 text-xs text-gray-400">({tabItems[tab.key].length})</span>
                  </button>
                ))}
              </div>
              {pyeongIdx > 0 && (
                <span className="text-xs text-blue-600 bg-blue-50 border border-blue-100 rounded-full px-2.5 py-1">
                  {PYEONG_OPTIONS[pyeongIdx].label} 필터 적용 중
                </span>
              )}
            </div>

            {tabItems[activeTab].length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-8">해당 데이터가 없습니다</p>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {tabItems[activeTab].map((item, idx) => (
                  <PropertyCard key={`${item.apartName}-${idx}`} item={item} />
                ))}
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="text-sm text-gray-400 text-center py-16">
          지역을 선택하면 분석 결과가 표시됩니다
        </div>
      )}
    </div>
  );
}
