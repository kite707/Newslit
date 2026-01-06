// src/services/historyService.ts

import type { HistoryData } from "@/types";
import { API_BASE_URL, API_ENDPOINTS } from "@/constants/api.constants";


/**
 * 특정 사용자의 특정 월 읽기 기록을 가져옵니다
 * @param userId 사용자 ID
 * @param year 연도
 * @param month 월 (1-12)
 * @returns 완료한 날짜 배열 (일자만)
 */
export async function fetchReadingHistory(
  year: number,
  month: number
): Promise<number[]> {
  try {
    const monthString = String(month).padStart(2, "0");
    const dateString = `${year}${monthString}`;
    const url = `${API_BASE_URL}${API_ENDPOINTS.READING_HISTORY}?&date=${dateString}`;

    const response = await fetch(url, {
      method: "GET",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      console.warn(`Failed to fetch reading history for user`);
      return [];
    }

    const data: HistoryData[] = await response.json();

    // readDate에서 일자만 추출
    const completedDates = data.map((history) =>
      new Date(history.displayDate).getDate()
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
  dailyId: number
): Promise<boolean> {
  try {
    const url = `${API_BASE_URL}${API_ENDPOINTS.READING_HISTORY}?dailyId=${dailyId}`;

    const response = await fetch(url, {
      method: "POST",
      credentials: "include", // 쿠키 포함
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error("UNAUTHORIZED");
      }

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
