export interface AnalysisItem {
  apartName: string;
  regionCode: string;
  exclusiveArea: number;
  exclusiveAreaPyeong: number;
  tradeType: 'TRADE' | 'JEONSE' | 'MONTHLY';
  currentAvgPrice: number;
  prevAvgPrice: number | null;
  changeRate: number | null;
  volatilityIndex: number | null;
  tradeCount: number;
  recommended: boolean;
  recommendReason: 'REBOUND_AFTER_DROP' | 'STABLE_UPTREND' | 'HIGH_TRADE_VOLUME' | 'UNDERVALUED' | null;
}

export interface RegionSummary {
  regionCode: string;
  yearMonth: string;
  tradeType: string;
  totalCount: number;
  risingCount: number;
  fallingCount: number;
  stableCount: number;
  avgChangeRate: number;
  topRising: AnalysisItem[];
  topFalling: AnalysisItem[];
  highVolatility: AnalysisItem[];
  recommended: AnalysisItem[];
}

export const REGION_MAP: Record<string, string> = {
  '11110': '종로구', '11140': '중구',    '11170': '용산구', '11200': '성동구',
  '11215': '광진구', '11230': '동대문구', '11260': '성북구', '11290': '강북구',
  '11305': '도봉구', '11320': '노원구',  '11350': '은평구', '11380': '서대문구',
  '11410': '마포구', '11440': '양천구',  '11470': '강서구', '11500': '구로구',
  '11530': '금천구', '11545': '영등포구', '11560': '동작구', '11590': '관악구',
  '11620': '서초구', '11650': '강남구',  '11680': '송파구', '11710': '강동구',
  '41150': '의정부시', '41630': '양주시', '41360': '남양주시',
  '41281': '고양 덕양구', '41285': '고양 일산동구', '41287': '고양 일산서구',
  '41480': '파주시',
};

export const CITY_REGION_MAP: Record<string, { label: string; codes: string[] }> = {
  seoul: {
    label: '서울',
    codes: ['11110','11140','11170','11200','11215','11230','11260','11290',
            '11305','11320','11350','11380','11410','11440','11470','11500',
            '11530','11545','11560','11590','11620','11650','11680','11710'],
  },
  gyeonggi: {
    label: '경기',
    codes: ['41150','41630','41360','41281','41285','41287','41480'],
  },
};

export const RECOMMEND_REASON_MAP: Record<string, string> = {
  REBOUND_AFTER_DROP: '하락 후 반등',
  STABLE_UPTREND: '저변동 안정상승',
  HIGH_TRADE_VOLUME: '거래량 급증',
  UNDERVALUED: '저평가 매물',
};
