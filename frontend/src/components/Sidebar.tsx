'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, MapPin, Star, TrendingUp, Bell, Home } from 'lucide-react';
import clsx from 'clsx';

const navItems = [
  { href: '/ddugiMain1', label: '메인', icon: Home },
  { href: '/', label: '대시보드', icon: LayoutDashboard },
  { href: '/analysis', label: '지역분석', icon: MapPin },
  { href: '/recommended', label: '추천매물', icon: Star },
  { href: '/volatile', label: '고변동성', icon: TrendingUp },
  { href: '/notification', label: '알림설정', icon: Bell },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 min-h-screen bg-gray-900 text-white flex flex-col">
      <div className="px-6 py-6 border-b border-gray-700">
        <h1 className="text-lg font-bold text-white leading-tight">
          부동산 분석
        </h1>
        <p className="text-xs text-gray-400 mt-1">Real Estate Dashboard</p>
      </div>
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ href, label, icon: Icon }) => {
          const isActive = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-800 hover:text-white'
              )}
            >
              <Icon size={18} />
              {label}
            </Link>
          );
        })}
      </nav>
      <div className="px-6 py-4 border-t border-gray-700">
        <p className="text-xs text-gray-500">서울 부동산 분석 시스템</p>
      </div>
    </aside>
  );
}
