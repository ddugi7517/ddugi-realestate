'use client';
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { notificationApi, analysisApi } from '@/lib/api';
import { Bell, Play, Send, CheckCircle, XCircle } from 'lucide-react';

export default function NotificationPage() {
  const [yearMonth, setYearMonth] = useState('');
  const [testResult, setTestResult] = useState<'success' | 'error' | null>(null);
  const [analysisResult, setAnalysisResult] = useState<'success' | 'error' | null>(null);
  const [sendResult, setSendResult] = useState<'success' | 'error' | null>(null);

  const testMutation = useMutation({
    mutationFn: notificationApi.test,
    onSuccess: () => setTestResult('success'),
    onError: () => setTestResult('error'),
  });

  const analysisMutation = useMutation({
    mutationFn: () => analysisApi.runAnalysis(yearMonth || undefined),
    onSuccess: () => setAnalysisResult('success'),
    onError: () => setAnalysisResult('error'),
  });

  const sendMutation = useMutation({
    mutationFn: () => notificationApi.sendAll(yearMonth || undefined),
    onSuccess: () => setSendResult('success'),
    onError: () => setSendResult('error'),
  });

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">알림 설정</h1>
        <p className="text-sm text-gray-500 mt-1">분석 실행 및 알림 발송 관리</p>
      </div>

      {/* 기준 년월 입력 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">기준 년월 설정</h3>
        <div className="flex items-center gap-3">
          <input
            type="text"
            value={yearMonth}
            onChange={e => setYearMonth(e.target.value)}
            placeholder="예: 202503 (비워두면 현재 월)"
            className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-64"
          />
          <span className="text-xs text-gray-400">YYYYMM 형식으로 입력하세요</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* 분석 실행 */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 bg-blue-50 rounded-lg">
              <Play size={16} className="text-blue-500" />
            </div>
            <h3 className="text-sm font-semibold text-gray-700">분석 실행</h3>
          </div>
          <p className="text-xs text-gray-500 mb-4">
            선택한 기준 년월의 실거래가 데이터를 분석합니다. 분석 완료까지 수 분이 소요될 수 있습니다.
          </p>
          {analysisResult === 'success' && (
            <div className="flex items-center gap-2 text-emerald-600 text-xs mb-3">
              <CheckCircle size={14} />
              <span>분석이 성공적으로 실행되었습니다</span>
            </div>
          )}
          {analysisResult === 'error' && (
            <div className="flex items-center gap-2 text-red-500 text-xs mb-3">
              <XCircle size={14} />
              <span>분석 실행 중 오류가 발생했습니다</span>
            </div>
          )}
          <button
            onClick={() => { setAnalysisResult(null); analysisMutation.mutate(); }}
            disabled={analysisMutation.isPending}
            className="w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white text-sm font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {analysisMutation.isPending ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                실행 중...
              </>
            ) : (
              <>
                <Play size={14} />
                분석 실행
              </>
            )}
          </button>
        </div>

        {/* 테스트 알림 */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 bg-amber-50 rounded-lg">
              <Bell size={16} className="text-amber-500" />
            </div>
            <h3 className="text-sm font-semibold text-gray-700">테스트 알림</h3>
          </div>
          <p className="text-xs text-gray-500 mb-4">
            알림 연결 상태를 확인하기 위한 테스트 메시지를 발송합니다.
          </p>
          {testResult === 'success' && (
            <div className="flex items-center gap-2 text-emerald-600 text-xs mb-3">
              <CheckCircle size={14} />
              <span>테스트 알림이 발송되었습니다</span>
            </div>
          )}
          {testResult === 'error' && (
            <div className="flex items-center gap-2 text-red-500 text-xs mb-3">
              <XCircle size={14} />
              <span>알림 발송 중 오류가 발생했습니다</span>
            </div>
          )}
          <button
            onClick={() => { setTestResult(null); testMutation.mutate(); }}
            disabled={testMutation.isPending}
            className="w-full py-2 px-4 bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white text-sm font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {testMutation.isPending ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                발송 중...
              </>
            ) : (
              <>
                <Bell size={14} />
                테스트 발송
              </>
            )}
          </button>
        </div>

        {/* 전체 알림 발송 */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 bg-emerald-50 rounded-lg">
              <Send size={16} className="text-emerald-500" />
            </div>
            <h3 className="text-sm font-semibold text-gray-700">전체 알림 발송</h3>
          </div>
          <p className="text-xs text-gray-500 mb-4">
            분석 결과를 기반으로 추천 매물 및 급등/급락 알림을 전체 발송합니다.
          </p>
          {sendResult === 'success' && (
            <div className="flex items-center gap-2 text-emerald-600 text-xs mb-3">
              <CheckCircle size={14} />
              <span>전체 알림이 발송되었습니다</span>
            </div>
          )}
          {sendResult === 'error' && (
            <div className="flex items-center gap-2 text-red-500 text-xs mb-3">
              <XCircle size={14} />
              <span>알림 발송 중 오류가 발생했습니다</span>
            </div>
          )}
          <button
            onClick={() => { setSendResult(null); sendMutation.mutate(); }}
            disabled={sendMutation.isPending}
            className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-300 text-white text-sm font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {sendMutation.isPending ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                발송 중...
              </>
            ) : (
              <>
                <Send size={14} />
                전체 발송
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
