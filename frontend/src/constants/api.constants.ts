export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const API_ENDPOINTS = {
  // Daily
  DAILY: "/daily",
  DAILY_AVAILABLE: "/daily/available",
  // History
  READING_HISTORY: "/reading-history",
  // Vocabulary
  VOCABULARY: "/vocabulary",
  // Auth
  LOGIN: "/user/login",
  SIGNUP: "/user/signup",
  LOGOUT: "/user/logout",
  EMAIL_SEND: "/user/email/send",
  EMAIL_VERIFY: "/user/email/verify",
} as const;
