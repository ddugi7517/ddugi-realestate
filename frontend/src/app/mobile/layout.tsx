import type { Metadata, Viewport } from 'next';
import '../globals.css';
import { Providers } from '@/components/providers';

export const metadata: Metadata = {
  title: '뚜기세상',
  description: '서울·경기 아파트 실거래가 분석',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'black-translucent',
    title: '뚜기세상',
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  viewportFit: 'cover',
};

export default function MobileLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-gray-950 overscroll-none">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
