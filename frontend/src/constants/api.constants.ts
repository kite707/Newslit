export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const API_ENDPOINTS = {
  ARTICLE: "/daily",
  ARTICLE_AVAILABLE: "/daily/available",
  READING_HISTORY: "/reading-history",
  VOCABULARY: "/vocabulary",
} as const;
