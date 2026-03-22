'use client';
import { REGION_MAP } from '@/types';

interface RegionSelectorProps {
  value: string;
  onChange: (regionCode: string) => void;
  className?: string;
}

export function RegionSelector({ value, onChange, className }: RegionSelectorProps) {
  return (
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      className={`
        px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-700
        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
        cursor-pointer ${className ?? ''}
      `}
    >
      {Object.entries(REGION_MAP).map(([code, name]) => (
        <option key={code} value={code}>
          {name}
        </option>
      ))}
    </select>
  );
}
