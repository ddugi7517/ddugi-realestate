'use client';
import { useState, useEffect } from 'react';
import { REGION_MAP, CITY_REGION_MAP } from '@/types';

interface RegionSelectorProps {
  value: string;
  onChange: (regionCode: string) => void;
  className?: string;
}

export function RegionSelector({ value, onChange, className }: RegionSelectorProps) {
  const initialCity = Object.entries(CITY_REGION_MAP).find(([, v]) =>
    v.codes.includes(value)
  )?.[0] ?? 'seoul';

  const [city, setCity] = useState(initialCity);

  useEffect(() => {
    const found = Object.entries(CITY_REGION_MAP).find(([, v]) => v.codes.includes(value))?.[0];
    if (found && found !== city) setCity(found);
  }, [value]);

  const handleCityChange = (newCity: string) => {
    setCity(newCity);
    const firstCode = CITY_REGION_MAP[newCity].codes[0];
    onChange(firstCode);
  };

  const districtCodes = CITY_REGION_MAP[city]?.codes ?? [];

  const selectCls = `text-sm border border-gray-200 rounded-lg px-3 py-1.5 text-gray-700 bg-white
    focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent cursor-pointer ${className ?? ''}`;

  return (
    <div className="flex items-center gap-2">
      {/* 도시 선택 */}
      <select value={city} onChange={e => handleCityChange(e.target.value)} className={selectCls}>
        {Object.entries(CITY_REGION_MAP).map(([key, { label }]) => (
          <option key={key} value={key}>{label}</option>
        ))}
      </select>

      {/* 구/시 선택 */}
      <select value={value} onChange={e => onChange(e.target.value)} className={selectCls}>
        {districtCodes.map(code => (
          <option key={code} value={code}>
            {REGION_MAP[code] ?? code}
          </option>
        ))}
      </select>
    </div>
  );
}
