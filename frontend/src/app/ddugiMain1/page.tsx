import Link from 'next/link';
import { LayoutDashboard, MapPin, Star, TrendingUp, Bell, ArrowRight, Building2, Database, RefreshCw, BarChart3, ChevronRight } from 'lucide-react';

const features = [
  {
    href: '/analysis',
    label: '지역별 분석',
    description: '구별 상승·하락·보합 비율과 추천 매물 분석',
    icon: MapPin,
    gradient: 'from-emerald-500 to-teal-600',
    bg: 'bg-emerald-50',
    text: 'text-emerald-600',
    border: 'border-emerald-100',
  },
  {
    href: '/recommended',
    label: '추천 매물',
    description: '반등·안정상승·저평가 매물 자동 선별',
    icon: Star,
    gradient: 'from-amber-500 to-orange-500',
    bg: 'bg-amber-50',
    text: 'text-amber-600',
    border: 'border-amber-100',
  },
  {
    href: '/volatile',
    label: '고변동성 단지',
    description: '단기 급변 단지 모니터링 및 리스크 파악',
    icon: TrendingUp,
    gradient: 'from-orange-500 to-red-500',
    bg: 'bg-orange-50',
    text: 'text-orange-600',
    border: 'border-orange-100',
  },
  {
    href: '/notification',
    label: '텔레그램 알림',
    description: '급등·급락 조건 감지 즉시 알림 발송',
    icon: Bell,
    gradient: 'from-purple-500 to-violet-600',
    bg: 'bg-purple-50',
    text: 'text-purple-600',
    border: 'border-purple-100',
  },
];

const stats = [
  { label: '서울 분석 지역', value: '24개 구', icon: MapPin },
  { label: '경기 분석 지역', value: '5개 시', icon: Database },
  { label: '데이터 업데이트', value: '매일 06:00', icon: RefreshCw },
  { label: '분석 지표', value: '4가지', icon: BarChart3 },
];

export default function DdugiMain1Page() {
  return (
    <div className="flex gap-8 min-h-[calc(100vh-4rem)]">

      {/* 왼쪽 패널 */}
      <aside className="w-64 shrink-0">
        <div className="sticky top-8 space-y-3">
          {/* 부동산분석 메인 링크 */}
          <Link
            href="/"
            className="group flex items-center gap-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl px-4 py-4 transition-all shadow-md shadow-blue-200 hover:shadow-lg hover:shadow-blue-300"
          >
            <div className="p-1.5 bg-blue-500 rounded-lg">
              <LayoutDashboard size={18} />
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold">부동산분석 메인</p>
              <p className="text-xs text-blue-200 mt-0.5">대시보드 바로가기</p>
            </div>
            <ArrowRight size={16} className="text-blue-300 group-hover:translate-x-0.5 transition-transform" />
          </Link>

          {/* 구분선 */}
          <div className="h-px bg-gray-200 my-1" />

          {/* 기능 바로가기 */}
          <p className="text-xs font-medium text-gray-400 px-1">기능 바로가기</p>
          {features.map(({ href, label, icon: Icon, text, bg }) => (
            <Link
              key={href}
              href={href}
              className="group flex items-center gap-3 bg-white hover:bg-gray-50 border border-gray-100 hover:border-gray-200 rounded-xl px-4 py-3 transition-all"
            >
              <div className={`p-1.5 ${bg} rounded-lg`}>
                <Icon size={15} className={text} />
              </div>
              <span className="text-sm font-medium text-gray-700 group-hover:text-gray-900 flex-1">{label}</span>
              <ChevronRight size={14} className="text-gray-300 group-hover:text-gray-400 transition-colors" />
            </Link>
          ))}

          {/* 데이터 출처 */}
          <div className="mt-4 bg-gray-50 rounded-xl border border-gray-100 px-4 py-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              데이터 출처<br />
              <span className="text-gray-500">국토교통부 실거래가<br />공개시스템</span>
            </p>
          </div>
        </div>
      </aside>

      {/* 메인 콘텐츠 */}
      <div className="flex-1 min-w-0">
        {/* 히어로 */}
        <div className="relative bg-gradient-to-br from-gray-900 via-blue-950 to-gray-900 rounded-2xl overflow-hidden mb-6 px-8 py-10">
          {/* 배경 장식 */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-blue-500 opacity-10 rounded-full -translate-y-1/2 translate-x-1/4 blur-3xl" />
          <div className="absolute bottom-0 left-1/3 w-48 h-48 bg-indigo-500 opacity-10 rounded-full translate-y-1/2 blur-2xl" />

          <div className="relative">
            <div className="flex items-center gap-2 mb-4">
              <div className="p-2 bg-blue-600 rounded-xl">
                <Building2 size={22} className="text-white" />
              </div>
              <span className="text-xs font-semibold text-blue-400 tracking-widest uppercase">Real Estate Analytics</span>
            </div>
            <h1 className="text-4xl font-extrabold text-white leading-tight mb-3">
              뚜기세상
            </h1>
            <p className="text-blue-200 text-base max-w-lg leading-relaxed mb-6">
              국토교통부 실거래가 데이터를 기반으로 서울·경기 아파트 시세를 분석하고,
              급등·급락 단지와 추천 매물 정보를 제공합니다.
            </p>
            <Link
              href="/"
              className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold px-5 py-2.5 rounded-xl transition-colors shadow-lg shadow-blue-900/40"
            >
              <LayoutDashboard size={16} />
              대시보드 시작하기
              <ArrowRight size={15} />
            </Link>
          </div>
        </div>

        {/* 통계 */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {stats.map(({ label, value, icon: Icon }) => (
            <div key={label} className="bg-white border border-gray-100 rounded-xl px-4 py-4 shadow-sm">
              <Icon size={15} className="text-blue-400 mb-2" />
              <p className="text-xl font-bold text-gray-900">{value}</p>
              <p className="text-xs text-gray-400 mt-0.5">{label}</p>
            </div>
          ))}
        </div>

        {/* 기능 카드 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {features.map(({ href, label, description, icon: Icon, gradient, bg, text, border }) => (
            <Link
              key={href}
              href={href}
              className={`group bg-white rounded-xl border ${border} hover:shadow-md transition-all overflow-hidden`}
            >
              <div className={`h-1.5 bg-gradient-to-r ${gradient}`} />
              <div className="p-5">
                <div className="flex items-start justify-between mb-3">
                  <div className={`p-2.5 ${bg} rounded-xl`}>
                    <Icon size={20} className={text} />
                  </div>
                  <ArrowRight
                    size={16}
                    className="text-gray-200 group-hover:text-gray-400 group-hover:translate-x-0.5 transition-all mt-1"
                  />
                </div>
                <h2 className="text-base font-semibold text-gray-900 mb-1">{label}</h2>
                <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
