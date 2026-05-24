import type { AuthResponse, LoginRequest, SignupRequest, SendCodeRequest, SendCodeResponse, VerifyCodeRequest } from "@/types";
import { API_BASE_URL, API_ENDPOINTS } from "@/constants/api.constants";

export class AuthService {
  private static TOKEN_KEY = "jwt_token";
  private static NICKNAME_KEY = "user_nickname";
  private static EMAIL_KEY = "user_email";

  static async login(email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.LOGIN}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, password } as LoginRequest),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "로그인에 실패했습니다.");
    }

    const data: AuthResponse = await response.json();

    // 토큰 저장
    if (data.token) {
      this.saveToken(data.token);
    }

    // 사용자 정보 저장
    if (data.nickname) {
      localStorage.setItem(this.NICKNAME_KEY, data.nickname);
    }
    if (data.email) {
      localStorage.setItem(this.EMAIL_KEY, data.email);
    }

    return data;
  }

  static async signup(
    email: string,
    nickname: string,
    password: string
  ): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.SIGNUP}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, nickname, password } as SignupRequest),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "회원가입에 실패했습니다.");
    }

    const data: AuthResponse = await response.json();
    

    // 토큰 저장
    if (data.token) {
      this.saveToken(data.token);
    }

    // 사용자 정보 저장
    localStorage.setItem(this.NICKNAME_KEY, nickname);
    localStorage.setItem(this.EMAIL_KEY, email);

    return data;
  }

  static async sendCode(email: string): Promise<SendCodeResponse> {
    const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.EMAIL_SEND}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email } as SendCodeRequest),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "인증코드 발송에 실패했습니다.");
    }

    return response.json();
  }

  static async verifyCode(email: string, code: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.EMAIL_VERIFY}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, code } as VerifyCodeRequest),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "인증코드 검증에 실패했습니다.");
    }
  }

  static async logout(): Promise<void> {
  try {
    const response = await fetch(
      `${API_BASE_URL}${API_ENDPOINTS.LOGOUT}`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      console.warn('Logout request failed, but clearing local state');
    }
  } catch (error) {
    console.error('Failed to call logout API:', error);
    // API 호출 실패해도 로컬 상태는 정리
  }
}

  static saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  static getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  static getNickname(): string | null {
    return localStorage.getItem(this.NICKNAME_KEY);
  }

  static getEmail(): string | null {
    return localStorage.getItem(this.EMAIL_KEY);
  }

  static isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  static getAuthHeader(): HeadersInit {
    const token = this.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
}
