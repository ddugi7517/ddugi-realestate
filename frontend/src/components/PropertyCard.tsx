import clsx from 'clsx';
import { AnalysisItem, REGION_MAP, RECOMMEND_REASON_MAP } from '@/types';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface PropertyCardProps {
  item: AnalysisItem;
}

const reasonColorMap: Record<string, string> = {
  REBOUND_AFTER_DROP: 'bg-emerald-100 text-emerald-700',
  STABLE_UPTREND: 'bg-blue-100 text-blue-700',
  HIGH_TRADE_VOLUME: 'bg-amber-100 text-amber-700',
  UNDERVALUED: 'bg-purple-100 text-purple-700',
};

function formatPrice(price: number): string {
  if (price >= 10000) {
    const eok = Math.floor(price / 10000);
    const man = price % 10000;
    return man > 0 ? `${eok}억 ${man.toLocaleString()}만` : `${eok}억`;
  }
  return `${price.toLocaleString()}만`;
}

export function PropertyCard({ item }: PropertyCardProps) {
  const changeRate = item.changeRate ?? 0;
  const isRising = changeRate > 0;
  const isFalling = changeRate < 0;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-gray-900 truncate text-sm">{item.apartName}</h3>
          <p className="text-xs text-gray-500 mt-0.5">
            {REGION_MAP[item.regionCode] ?? item.regionCode} · {item.exclusiveAreaPyeong}평
          </p>
        </div>
        <div className={clsx(
          'flex items-center gap-1 text-sm font-semibold ml-2 flex-shrink-0',
          isRising ? 'text-emerald-600' : isFalling ? 'text-red-500' : 'text-gray-500'
        )}>
          {isRising ? <TrendingUp size={14} /> : isFalling ? <TrendingDown size={14} /> : <Minus size={14} />}
          {changeRate > 0 ? '+' : ''}{changeRate.toFixed(2)}%
        </div>
      </div>

      <div className="space-y-1.5">
        <div className="flex justify-between items-center text-sm">
          <span className="text-gray-500">현재 평균가</span>
          <span className="font-semibold text-gray-900">{formatPrice(item.currentAvgPrice)}</span>
        </div>
        {item.prevAvgPrice !== null && (
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-500">이전 평균가</span>
            <span className="text-gray-600">{formatPrice(item.prevAvgPrice)}</span>
          </div>
        )}
        <div className="flex justify-between items-center text-sm">
          <span className="text-gray-500">거래 건수</span>
          <span className="text-gray-600">{item.tradeCount}건</span>
        </div>
        {item.volatilityIndex !== null && (
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-500">변동성 지수</span>
            <span className="text-gray-600">{item.volatilityIndex.toFixed(2)}</span>
          </div>
        )}
      </div>

      {item.recommendReason && (
        <div className="mt-3 pt-3 border-t border-gray-50">
          <span className={clsx(
            'inline-block px-2.5 py-1 rounded-full text-xs font-medium',
            reasonColorMap[item.recommendReason] ?? 'bg-gray-100 text-gray-600'
          )}>
            {RECOMMEND_REASON_MAP[item.recommendReason] ?? item.recommendReason}
          </span>
        </div>
      )}
    </div>
  );
}
