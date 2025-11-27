import type { ArticleData, AvailableDatesResponse } from "@/types";
import { API_BASE_URL, API_ENDPOINTS } from "@/constants/api.constants";

function formatDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}${month}${day}`;
}

export const DEFAULT_ARTICLE_DATA: ArticleData = {
  title: "No Article Available",
  publishedDate: "2024-11-27",
  source: "Newslit",
  sourceUrl: "https://newslit.com",
  sentences: [
    {
      articleId: 0,
      orderIndex: 1,
      englishText:
        "There is no article available for today. Newslit provides a daily paragraph to help you learn English. Please check back tomorrow!",
      koreanText:
        "오늘은 제공되는 기사가 없습니다. Newslit은 매일 한 단락의 영어 문장을 제공하여 영어 학습을 돕습니다. 내일 다시 확인해주세요!",
      status: "SUCCESS",
    },
  ],
};

/**
 * 특정 날짜의 기사를 가져옵니다
 * @param date 조회할 날짜
 * @returns 기사 데이터 (없으면 기본 데이터 반환)
 */
export async function fetchArticle(date: Date): Promise<ArticleData> {
  try {
    const dateString = formatDateString(date);
    const url = `${API_BASE_URL}${API_ENDPOINTS.ARTICLE}?date=${dateString}`;

    const response = await fetch(url);
    if (!response.ok) {
      console.warn(`No article found for date: ${dateString}`);
      return DEFAULT_ARTICLE_DATA;
    }

    const data: ArticleData = await response.json();
    console.log("Fetched article data:", data);
    return data;
  } catch (error) {
    console.error("Failed to fetch article:", error);
    return DEFAULT_ARTICLE_DATA;
  }
}

/**
 * 특정 월의 기사가 있는 날짜 목록을 가져옵니다
 * @param year 연도
 * @param month 월 (1-12)
 * @returns 기사가 있는 날짜 배열 (일자만)
 */
export async function fetchAvailableDates(
  year: number,
  month: number
): Promise<number[]> {
  try {
    const monthString = String(month).padStart(2, "0");
    const dateString = `${year}${monthString}`;
    const url = `${API_BASE_URL}${API_ENDPOINTS.ARTICLE_AVAILABLE}?date=${dateString}`;

    const response = await fetch(url);

    if (!response.ok) {
      console.warn(`Failed to fetch available dates for: ${dateString}`);
      return [];
    }

    const data: AvailableDatesResponse = await response.json();
    return data.map((item) => {
      const day = parseInt(item.displayDate.slice(-2), 10);
      return day;
    });
  } catch (error) {
    console.error("Failed to fetch available dates:", error);
    return [];
  }
}
