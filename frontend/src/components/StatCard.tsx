import clsx from 'clsx';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: LucideIcon;
  color?: 'default' | 'emerald' | 'red' | 'amber' | 'blue';
}

const colorMap = {
  default: { bg: 'bg-gray-50', icon: 'text-gray-500', value: 'text-gray-900' },
  emerald: { bg: 'bg-emerald-50', icon: 'text-emerald-500', value: 'text-emerald-700' },
  red: { bg: 'bg-red-50', icon: 'text-red-500', value: 'text-red-700' },
  amber: { bg: 'bg-amber-50', icon: 'text-amber-500', value: 'text-amber-700' },
  blue: { bg: 'bg-blue-50', icon: 'text-blue-500', value: 'text-blue-700' },
};

export function StatCard({ title, value, subtitle, icon: Icon, color = 'default' }: StatCardProps) {
  const colors = colorMap[color];

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm text-gray-500 font-medium">{title}</p>
          <p className={clsx('text-2xl font-bold mt-1', colors.value)}>{value}</p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        {Icon && (
          <div className={clsx('p-2 rounded-lg', colors.bg)}>
            <Icon size={20} className={colors.icon} />
          </div>
        )}
      </div>
    </div>
  );
}
