export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  nickname: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  nickname?: string;
  email?: string;
}
