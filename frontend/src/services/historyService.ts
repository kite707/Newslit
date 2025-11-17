// src/services/historyService.ts

import type { HistoryData } from "@/types";
import { API_BASE_URL, API_ENDPOINTS } from "@/constants/api.constants";

/**
 * 읽기 기록 API 응답 타입
 */
interface HistoryResponse {
  histories: HistoryData[];
}

/**
 * 특정 사용자의 특정 월 읽기 기록을 가져옵니다
 * @param userId 사용자 ID
 * @param year 연도
 * @param month 월 (1-12)
 * @returns 완료한 날짜 배열 (일자만)
 */
export async function fetchReadingHistory(
  userId: number,
  year: number,
  month: number
): Promise<number[]> {
  try {
    const monthString = String(month).padStart(2, "0");
    const dateString = `${year}${monthString}`;
    const url = `${API_BASE_URL}${API_ENDPOINTS.READING_HISTORY}?userId=${userId}&date=${dateString}`;

    const response = await fetch(url);

    if (!response.ok) {
      console.warn(`Failed to fetch reading history for user ${userId}`);
      return [];
    }

    const data: HistoryResponse = await response.json();

    // readDate에서 일자만 추출
    const completedDates = data.histories.map((history) =>
      new Date(history.readDate).getDate()
    );

    return completedDates;
  } catch (error) {
    console.error("Failed to fetch reading history:", error);
    return [];
  }
}

/**
 * 기사를 완료 처리합니다
 * @param articleId 기사 ID
 * @returns 성공 여부
 */
export async function markArticleAsComplete(
  articleId: number
): Promise<boolean> {
  try {
    // userId를 쿠키에 설정 (백엔드에서 읽음)
    document.cookie = "userId=1; path=/";

    const url = `${API_BASE_URL}${API_ENDPOINTS.READING_HISTORY}?articleId=${articleId}`;

    const response = await fetch(url, {
      method: "POST",
      credentials: "include", // 쿠키 포함
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: "알 수 없는 오류" }));

      console.error("Failed to mark as complete:", errorData);
      throw new Error(errorData.message || response.statusText);
    }

    return true;
  } catch (error) {
    console.error("Error marking article as complete:", error);
    throw error; // 에러를 다시 던져서 컴포넌트에서 처리하게 함
  }
}
