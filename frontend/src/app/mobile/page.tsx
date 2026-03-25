'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { analysisApi } from '@/lib/api';
import { REGION_MAP, CITY_REGION_MAP, RECOMMEND_REASON_MAP } from '@/types';
import type { AnalysisItem } from '@/types';
import {
  Home, MapPin, Star, TrendingUp, TrendingDown,
  ChevronRight, Building2, BarChart3, Minus, RefreshCw,
} from 'lucide-react';
import clsx from 'clsx';

// ── helpers ──────────────────────────────────────────────────────────────────

function formatPrice(price: number): string {
  if (price >= 10000) {
    const eok = Math.floor(price / 10000);
    const man = price % 10000;
    return man > 0 ? `${eok}억 ${man.toLocaleString()}만` : `${eok}억`;
  }
  return `${price.toLocaleString()}만`;
}

function getAvailableMonths() {
  const months = [];
  const start = new Date(2025, 0);
  const now = new Date();
  const end = new Date(now.getFullYear(), now.getMonth());
  for (let d = new Date(start); d <= end; d.setMonth(d.getMonth() + 1)) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    months.push({ value: `${y}${m}`, label: `${y}.${m}` });
  }
  return months.reverse();
}

function currentYM() {
  const now = new Date();
  return `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`;
}

// ── sub-components ────────────────────────────────────────────────────────────

function ChangeChip({ rate }: { rate: number | null }) {
  if (rate === null) return <span className="text-gray-500 text-xs">-</span>;
  const up = rate > 0;
  const dn = rate < 0;
  return (
    <span className={clsx(
      'text-xs font-bold flex items-center gap-0.5',
      up ? 'text-red-400' : dn ? 'text-blue-400' : 'text-gray-400'
    )}>
      {up ? <TrendingUp size={11} /> : dn ? <TrendingDown size={11} /> : <Minus size={11} />}
      {rate > 0 ? '+' : ''}{rate.toFixed(2)}%
    </span>
  );
}

function MobileCard({ item, rank }: { item: AnalysisItem; rank?: number }) {
  const region = REGION_MAP[item.regionCode] ?? item.regionCode;
  return (
    <div className="flex items-center gap-3 py-3 px-4 border-b border-white/5 last:border-0 active:bg-white/5 transition-colors">
      {rank && (
        <span className={clsx(
          'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
          rank === 1 ? 'bg-yellow-500/20 text-yellow-400' :
          rank === 2 ? 'bg-gray-400/20 text-gray-300' :
          rank === 3 ? 'bg-amber-700/20 text-amber-600' :
          'bg-white/5 text-gray-500'
        )}>{rank}</span>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white truncate">{item.apartName}</p>
        <p className="text-xs text-gray-400 mt-0.5">{region} · {item.exclusiveAreaPyeong}평 · {item.tradeCount}건</p>
      </div>
      <div className="text-right shrink-0">
        <p className="text-sm font-bold text-white">{formatPrice(item.currentAvgPrice)}</p>
        <ChangeChip rate={item.changeRate} />
      </div>
    </div>
  );
}

const REASON_STYLE: Record<string, { bg: string; text: string; dot: string }> = {
  REBOUND_AFTER_DROP: { bg: 'bg-emerald-500/10', text: 'text-emerald-400', dot: 'bg-emerald-400' },
  STABLE_UPTREND:     { bg: 'bg-blue-500/10',    text: 'text-blue-400',    dot: 'bg-blue-400' },
  HIGH_TRADE_VOLUME:  { bg: 'bg-amber-500/10',   text: 'text-amber-400',   dot: 'bg-amber-400' },
  UNDERVALUED:        { bg: 'bg-purple-500/10',  text: 'text-purple-400',  dot: 'bg-purple-400' },
};

function RecommendCard({ item }: { item: AnalysisItem }) {
  const region = REGION_MAP[item.regionCode] ?? item.regionCode;
  const style = item.recommendReason ? REASON_STYLE[item.recommendReason] : null;
  return (
    <div className="bg-white/5 rounded-2xl p-4 border border-white/8 active:bg-white/10 transition-colors">
      <div className="flex items-start justify-between mb-2">
        <div className="flex-1 min-w-0 pr-2">
          <p className="text-sm font-bold text-white truncate">{item.apartName}</p>
          <p className="text-xs text-gray-400 mt-0.5">{region} · {item.exclusiveAreaPyeong}평</p>
        </div>
        <ChangeChip rate={item.changeRate} />
      </div>
      <div className="flex items-center justify-between mt-3">
        <div>
          <p className="text-xs text-gray-500">현재 평균가</p>
          <p className="text-base font-extrabold text-white">{formatPrice(item.currentAvgPrice)}</p>
        </div>
        {style && item.recommendReason && (
          <span className={clsx('flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold', style.bg, style.text)}>
            <span className={clsx('w-1.5 h-1.5 rounded-full', style.dot)} />
            {RECOMMEND_REASON_MAP[item.recommendReason]}
          </span>
        )}
      </div>
    </div>
  );
}

// ── tabs ─────────────────────────────────────────────────────────────────────

type Tab = 'home' | 'analysis' | 'recommend' | 'volatile';

// ── Home Tab ─────────────────────────────────────────────────────────────────

function HomeTab({ yearMonth }: { yearMonth: string }) {

  const { data: rising, isLoading: r1 } = useQuery({
    queryKey: ['rising', yearMonth],
    queryFn: () => analysisApi.getTopRising(yearMonth, 'TRADE', 5),
  });
  const { data: falling, isLoading: r2 } = useQuery({
    queryKey: ['falling', yearMonth],
    queryFn: () => analysisApi.getTopFalling(yearMonth, 'TRADE', 5),
  });
  const isLoading = r1 || r2;

  return (
    <div className="pb-6">
      {/* Hero */}
      <div className="px-5 pt-2 pb-6">
        <div className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-blue-600 via-indigo-700 to-violet-800 p-5">
          <div className="absolute top-0 right-0 w-40 h-40 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/4" />
          <div className="absolute bottom-0 left-0 w-32 h-32 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/4" />
          <div className="relative">
            <div className="flex items-center gap-2 mb-3">
              <Building2 size={16} className="text-blue-200" />
              <span className="text-xs font-semibold text-blue-200 tracking-wider uppercase">뚜기세상</span>
            </div>
            <p className="text-2xl font-extrabold text-white leading-tight mb-1">서울·경기<br />아파트 분석</p>
            <p className="text-xs text-blue-200">실거래가 기반 AI 시세 분석</p>
            <div className="mt-4">
              <span className="text-xs text-blue-200">{yearMonth.slice(0,4)}년 {yearMonth.slice(4)}월 기준</span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="px-5 mb-6">
        <div className="grid grid-cols-2 gap-3">
          {[
            { label: '급등 단지', value: rising?.length ?? '-', icon: TrendingUp, color: 'text-red-400', bg: 'bg-red-500/10' },
            { label: '급락 단지', value: falling?.length ?? '-', icon: TrendingDown, color: 'text-blue-400', bg: 'bg-blue-500/10' },
          ].map(({ label, value, icon: Icon, color, bg }) => (
            <div key={label} className={clsx('rounded-2xl p-4', bg, 'border border-white/5')}>
              <Icon size={18} className={clsx(color, 'mb-2')} />
              <p className="text-2xl font-extrabold text-white">{isLoading ? '…' : value}<span className="text-sm font-normal text-gray-400 ml-1">개</span></p>
              <p className="text-xs text-gray-400 mt-0.5">{label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Rising */}
      <div className="px-5 mb-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <TrendingUp size={15} className="text-red-400" />
            <span className="text-sm font-bold text-white">급등 TOP 5</span>
          </div>
          <span className="text-xs text-gray-500">전월 대비</span>
        </div>
        <div className="bg-white/5 rounded-2xl border border-white/8 overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center h-32"><div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
          ) : (rising ?? []).length === 0 ? (
            <p className="text-xs text-gray-500 text-center py-8">데이터 없음</p>
          ) : (rising ?? []).map((item, i) => <MobileCard key={i} item={item} rank={i + 1} />)}
        </div>
      </div>

      {/* Falling */}
      <div className="px-5">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <TrendingDown size={15} className="text-blue-400" />
            <span className="text-sm font-bold text-white">급락 TOP 5</span>
          </div>
          <span className="text-xs text-gray-500">전월 대비</span>
        </div>
        <div className="bg-white/5 rounded-2xl border border-white/8 overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center h-32"><div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
          ) : (falling ?? []).length === 0 ? (
            <p className="text-xs text-gray-500 text-center py-8">데이터 없음</p>
          ) : (falling ?? []).map((item, i) => <MobileCard key={i} item={item} rank={i + 1} />)}
        </div>
      </div>
    </div>
  );
}

// ── Analysis Tab ──────────────────────────────────────────────────────────────

function AnalysisTab({ yearMonth }: { yearMonth: string }) {
  const [city, setCity] = useState('seoul');
  const [regionCode, setRegionCode] = useState('11650');

  const { data: summary, isLoading } = useQuery({
    queryKey: ['regionSummary', regionCode, yearMonth],
    queryFn: () => analysisApi.getRegionSummary(regionCode, yearMonth),
  });

  const districtCodes = CITY_REGION_MAP[city]?.codes ?? [];

  const tabs = [
    { key: 'topRising', label: '급등', items: summary?.topRising ?? [] },
    { key: 'topFalling', label: '급락', items: summary?.topFalling ?? [] },
    { key: 'recommended', label: '추천', items: summary?.recommended ?? [] },
  ] as const;
  const [subTab, setSubTab] = useState<'topRising' | 'topFalling' | 'recommended'>('topRising');

  return (
    <div className="pb-6">
      {/* Region Selector */}
      <div className="px-5 pt-2 pb-4">
        <div className="flex gap-2">
          <select
            value={city}
            onChange={e => { setCity(e.target.value); setRegionCode(CITY_REGION_MAP[e.target.value].codes[0]); }}
            className="flex-1 bg-gray-900 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50 appearance-none"
          >
            {Object.entries(CITY_REGION_MAP).map(([k, v]) => (
              <option key={k} value={k} className="bg-gray-900 text-white">{v.label}</option>
            ))}
          </select>
          <select
            value={regionCode}
            onChange={e => setRegionCode(e.target.value)}
            className="flex-[2] bg-gray-900 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50 appearance-none"
          >
            {districtCodes.map(code => (
              <option key={code} value={code} className="bg-gray-900 text-white">{REGION_MAP[code] ?? code}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Stats */}
      {summary && (
        <div className="px-5 mb-4">
          <div className="bg-gradient-to-r from-blue-600/20 to-indigo-600/20 rounded-2xl border border-white/8 p-4">
            <p className="text-xs text-gray-400 mb-3">{REGION_MAP[regionCode]} · {yearMonth.slice(0, 4)}.{yearMonth.slice(4)} 기준</p>
            <div className="grid grid-cols-4 gap-2 text-center">
              {[
                { label: '전체', value: summary.totalCount },
                { label: '상승', value: summary.risingCount, color: 'text-red-400' },
                { label: '하락', value: summary.fallingCount, color: 'text-blue-400' },
                { label: '추천', value: summary.recommended.length, color: 'text-amber-400' },
              ].map(({ label, value, color }) => (
                <div key={label}>
                  <p className={clsx('text-lg font-extrabold', color ?? 'text-white')}>{value}</p>
                  <p className="text-xs text-gray-500">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Sub Tabs */}
      <div className="px-5 mb-3">
        <div className="flex bg-white/5 rounded-xl p-1 gap-1">
          {tabs.map(t => (
            <button
              key={t.key}
              onClick={() => setSubTab(t.key)}
              className={clsx(
                'flex-1 py-2 rounded-lg text-xs font-semibold transition-colors',
                subTab === t.key ? 'bg-blue-600 text-white shadow' : 'text-gray-400'
              )}
            >
              {t.label} ({t.items.length})
            </button>
          ))}
        </div>
      </div>

      <div className="px-5">
        <div className="bg-white/5 rounded-2xl border border-white/8 overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center h-32"><div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
          ) : tabs.find(t => t.key === subTab)?.items.length === 0 ? (
            <p className="text-xs text-gray-500 text-center py-10">데이터 없음</p>
          ) : (
            tabs.find(t => t.key === subTab)?.items.map((item, i) => <MobileCard key={i} item={item} rank={i + 1} />)
          )}
        </div>
      </div>
    </div>
  );
}

// ── Recommend Tab ─────────────────────────────────────────────────────────────

function RecommendTab({ yearMonth }: { yearMonth: string }) {
  const [city, setCity] = useState<string>('all');
  const [regionCode, setRegionCode] = useState<string>('all');

  const { data, isLoading } = useQuery({
    queryKey: ['recommended', yearMonth],
    queryFn: () => analysisApi.getRecommended(yearMonth),
  });

  const districtCodes = city !== 'all' ? (CITY_REGION_MAP[city]?.codes ?? []) : [];

  const filtered = (data ?? []).filter(item => {
    if (city === 'all') return true;
    if (regionCode !== 'all') return item.regionCode === regionCode;
    return CITY_REGION_MAP[city]?.codes.includes(item.regionCode);
  });

  const countByReason = filtered.reduce<Record<string, number>>((acc, item) => {
    if (item.recommendReason) acc[item.recommendReason] = (acc[item.recommendReason] ?? 0) + 1;
    return acc;
  }, {});

  function handleCityChange(val: string) {
    setCity(val);
    setRegionCode('all');
  }

  return (
    <div className="pb-6">
      {/* Region Selector */}
      <div className="px-5 pt-2 pb-3">
        <div className="flex gap-2">
          <select
            value={city}
            onChange={e => handleCityChange(e.target.value)}
            className="flex-1 bg-gray-900 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50 appearance-none"
          >
            <option value="all" className="bg-gray-900 text-white">전체</option>
            {Object.entries(CITY_REGION_MAP).map(([k, v]) => (
              <option key={k} value={k} className="bg-gray-900 text-white">{v.label}</option>
            ))}
          </select>
          {city !== 'all' && (
            <select
              value={regionCode}
              onChange={e => setRegionCode(e.target.value)}
              className="flex-[2] bg-gray-900 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50 appearance-none"
            >
              <option value="all" className="bg-gray-900 text-white">전체</option>
              {districtCodes.map(code => (
                <option key={code} value={code} className="bg-gray-900 text-white">{REGION_MAP[code] ?? code}</option>
              ))}
            </select>
          )}
        </div>
      </div>

      {/* Summary chips */}
      <div className="px-5 pb-4">
        <div className="flex gap-2 flex-wrap">
          {Object.entries(REASON_STYLE).map(([key, style]) => (
            <div key={key} className={clsx('flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border border-white/5', style.bg, style.text)}>
              <span className={clsx('w-1.5 h-1.5 rounded-full', style.dot)} />
              {RECOMMEND_REASON_MAP[key]}
              <span className="font-bold">{countByReason[key] ?? 0}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="px-5">
        {isLoading ? (
          <div className="flex items-center justify-center h-48"><div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16">
            <Star size={32} className="text-gray-700 mx-auto mb-3" />
            <p className="text-sm text-gray-500">추천 매물 없음</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3">
            {filtered.map((item, i) => <RecommendCard key={i} item={item} />)}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Volatile Tab ──────────────────────────────────────────────────────────────

function VolatileTab({ yearMonth }: { yearMonth: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['volatile', yearMonth],
    queryFn: () => analysisApi.getHighVolatility(yearMonth, 'TRADE', 20),
  });

  return (
    <div className="pb-6">
      <div className="px-5 pt-2 pb-4">
        <div className="bg-orange-500/10 border border-orange-500/20 rounded-2xl px-4 py-3 flex items-center gap-3">
          <BarChart3 size={16} className="text-orange-400 shrink-0" />
          <p className="text-xs text-orange-300">6개월 변동계수 기준 상위 단지입니다. 단기 가격 변동이 크므로 주의가 필요합니다.</p>
        </div>
      </div>
      <div className="px-5">
        <div className="bg-white/5 rounded-2xl border border-white/8 overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center h-48"><div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin" /></div>
          ) : (data ?? []).length === 0 ? (
            <p className="text-xs text-gray-500 text-center py-10">데이터 없음</p>
          ) : (data ?? []).map((item, i) => (
            <div key={i} className="flex items-center gap-3 py-3 px-4 border-b border-white/5 last:border-0">
              <span className="text-xs font-bold text-orange-400 w-5 shrink-0">{i + 1}</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-white truncate">{item.apartName}</p>
                <p className="text-xs text-gray-400">{REGION_MAP[item.regionCode] ?? item.regionCode} · {item.exclusiveAreaPyeong}평</p>
              </div>
              <div className="text-right shrink-0">
                <p className="text-xs font-bold text-orange-400">변동 {item.volatilityIndex?.toFixed(1) ?? '-'}%</p>
                <ChangeChip rate={item.changeRate} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

const NAV_ITEMS = [
  { key: 'home',      label: '홈',     icon: Home },
  { key: 'analysis',  label: '지역분석', icon: MapPin },
  { key: 'recommend', label: '추천',   icon: Star },
  { key: 'volatile',  label: '고변동',  icon: BarChart3 },
] as const;

export default function MobilePage() {
  const [tab, setTab] = useState<Tab>('home');
  const [yearMonth, setYearMonth] = useState(currentYM);

  const TAB_TITLES: Record<Tab, string> = {
    home:      '뚜기세상',
    analysis:  '지역별 분석',
    recommend: '추천 매물',
    volatile:  '고변동성',
  };

  return (
    <div
      className="flex flex-col bg-gray-950 text-white"
      style={{ height: '100dvh' }}
    >
      {/* Status bar spacer */}
      <div style={{ height: 'env(safe-area-inset-top)' }} />

      {/* Header */}
      <header className="shrink-0 px-5 py-3 flex items-center justify-between border-b border-white/5 backdrop-blur-xl bg-gray-950/80 sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 bg-blue-600 rounded-lg flex items-center justify-center">
            <Building2 size={14} className="text-white" />
          </div>
          <h1 className="text-base font-bold text-white">{TAB_TITLES[tab]}</h1>
        </div>
        <div className="flex items-center gap-1 bg-white/8 rounded-xl px-2.5 py-1.5 border border-white/8">
          <RefreshCw size={11} className="text-gray-400 shrink-0" />
          <select
            value={yearMonth}
            onChange={e => setYearMonth(e.target.value)}
            className="bg-transparent text-xs text-gray-300 focus:outline-none appearance-none cursor-pointer"
          >
            {getAvailableMonths().map(m => (
              <option key={m.value} value={m.value} className="text-gray-900 bg-white">{m.label}</option>
            ))}
          </select>
        </div>
      </header>

      {/* Scrollable content */}
      <main className="flex-1 overflow-y-auto overscroll-none">
        {tab === 'home'      && <HomeTab yearMonth={yearMonth} />}
        {tab === 'analysis'  && <AnalysisTab yearMonth={yearMonth} />}
        {tab === 'recommend' && <RecommendTab yearMonth={yearMonth} />}
        {tab === 'volatile'  && <VolatileTab yearMonth={yearMonth} />}
      </main>

      {/* Bottom Nav */}
      <nav
        className="shrink-0 border-t border-white/8 bg-gray-950/90 backdrop-blur-xl"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
      >
        <div className="flex">
          {NAV_ITEMS.map(({ key, label, icon: Icon }) => {
            const active = tab === key;
            return (
              <button
                key={key}
                onClick={() => setTab(key as Tab)}
                className="flex-1 flex flex-col items-center gap-1 py-2.5 transition-colors"
              >
                <div className={clsx(
                  'w-10 h-6 rounded-full flex items-center justify-center transition-all',
                  active ? 'bg-blue-600' : 'bg-transparent'
                )}>
                  <Icon size={16} className={active ? 'text-white' : 'text-gray-500'} />
                </div>
                <span className={clsx(
                  'text-[10px] font-medium transition-colors',
                  active ? 'text-blue-400' : 'text-gray-500'
                )}>{label}</span>
              </button>
            );
          })}
        </div>
      </nav>
    </div>
  );
}
