import Link from 'next/link';
import { LayoutDashboard, MapPin, Star, TrendingUp, Bell, ArrowRight, Building2 } from 'lucide-react';

const pages = [
  {
    href: '/',
    label: '대시보드',
    description: '서울 아파트 급등/급락 TOP 5 및 전체 현황을 한눈에 확인합니다.',
    icon: LayoutDashboard,
    color: 'bg-blue-500',
    lightColor: 'bg-blue-50',
    textColor: 'text-blue-600',
  },
  {
    href: '/analysis',
    label: '지역별 분석',
    description: '구별 상승/하락/보합 비율과 추천 매물을 분석합니다.',
    icon: MapPin,
    color: 'bg-emerald-500',
    lightColor: 'bg-emerald-50',
    textColor: 'text-emerald-600',
  },
  {
    href: '/recommended',
    label: '추천 매물',
    description: '조건에 맞는 추천 아파트 매물을 확인합니다.',
    icon: Star,
    color: 'bg-amber-500',
    lightColor: 'bg-amber-50',
    textColor: 'text-amber-600',
  },
  {
    href: '/volatile',
    label: '고변동성',
    description: '단기간 급격한 가격 변동이 있는 단지를 모니터링합니다.',
    icon: TrendingUp,
    color: 'bg-orange-500',
    lightColor: 'bg-orange-50',
    textColor: 'text-orange-600',
  },
  {
    href: '/notification',
    label: '알림 설정',
    description: '텔레그램을 통한 급등/급락 알림 조건을 설정합니다.',
    icon: Bell,
    color: 'bg-purple-500',
    lightColor: 'bg-purple-50',
    textColor: 'text-purple-600',
  },
];

export default function DdugiMain1Page() {
  return (
    <div>
      {/* 헤더 */}
      <div className="mb-10">
        <div className="flex items-center gap-3 mb-3">
          <div className="p-2.5 bg-blue-600 rounded-xl">
            <Building2 size={24} className="text-white" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">뚜기 부동산 분석</h1>
            <p className="text-sm text-gray-500 mt-0.5">서울 아파트 실거래가 분석 시스템</p>
          </div>
        </div>
        <p className="text-gray-600 mt-4 max-w-xl">
          국토교통부 실거래가 데이터를 기반으로 서울 아파트 시세를 분석하고,
          급등·급락 단지 및 추천 매물 정보를 제공합니다.
        </p>
      </div>

      {/* 페이지 카드 목록 */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {pages.map(({ href, label, description, icon: Icon, color, lightColor, textColor }) => (
          <Link
            key={href}
            href={href}
            className="group bg-white rounded-xl shadow-sm border border-gray-100 p-5 hover:shadow-md hover:border-gray-200 transition-all"
          >
            <div className="flex items-start justify-between mb-4">
              <div className={`p-2.5 rounded-lg ${lightColor}`}>
                <Icon size={20} className={textColor} />
              </div>
              <ArrowRight
                size={16}
                className="text-gray-300 group-hover:text-gray-500 group-hover:translate-x-0.5 transition-all mt-1"
              />
            </div>
            <h2 className="text-base font-semibold text-gray-900 mb-1.5">{label}</h2>
            <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
            <div className={`mt-4 h-0.5 w-8 rounded-full ${color} opacity-60 group-hover:w-16 transition-all`} />
          </Link>
        ))}
      </div>

      {/* 하단 안내 */}
      <div className="mt-10 bg-gray-50 rounded-xl border border-gray-100 p-5">
        <p className="text-sm text-gray-500">
          데이터 출처: 국토교통부 실거래가 공개시스템 (data.go.kr) &nbsp;·&nbsp; 매일 오전 6시 자동 업데이트
        </p>
      </div>
    </div>
  );
}