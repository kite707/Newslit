export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const API_ENDPOINTS = {
  DAILY: "/daily",
  DAILY_AVAILABLE: "/daily/available",
  READING_HISTORY: "/reading-history",
  VOCABULARY: "/vocabulary",
} as const;
