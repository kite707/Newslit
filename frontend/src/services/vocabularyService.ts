import type { VocabularyItem } from "@/types";
import { API_BASE_URL, API_ENDPOINTS } from "@/constants/api.constants";


export async function fetchVocabulary(
  articleId: number
): Promise<VocabularyItem[]> {
  try {
    const url = `${API_BASE_URL}${API_ENDPOINTS.VOCABULARY}?articleId=${articleId}`;
    const response = await fetch(url);

    if (!response.ok) {
      console.warn(`Failed to fetch vocabulary for article: ${articleId}`);
      return [];
    }

    const data: VocabularyItem[] = await response.json();
    return data;
  } catch (error) {
    console.error("Failed to fetch vocabulary:", error);
    return [];
  }
}
