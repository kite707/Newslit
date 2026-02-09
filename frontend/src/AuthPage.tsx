import { useState } from "react";
import { AuthService } from "./services/authService";
import type { AuthResponse } from "@/types";
import { Moon, Sun } from "lucide-react";

export default function AuthPage({
  onLogin,
  onGuestMode,
}: {
  onLogin: (email: string, nickname: string) => void;
  onGuestMode: () => void;
}) {
  const [darkMode, setDarkMode] = useState(false);
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email || !password) {
      alert("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    if (!isLogin) {
      if (!nickname) {
        alert("닉네임을 입력해주세요.");
        return;
      }
      if (password !== confirmPassword) {
        alert("비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    setLoading(true);

    try {
      let data: AuthResponse;

      if (isLogin) {
        data = await AuthService.login(email, password);
      } else {
        data = await AuthService.signup(email, nickname, password);
      }

      const displayName = data.nickname || nickname || email;
      onLogin(email, displayName);
    } catch (error) {
      alert(
        error instanceof Error ? error.message : "서버 연결에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className={`min-h-screen flex items-center justify-center ${
        darkMode
          ? "bg-gray-900"
          : "bg-gradient-to-br from-blue-50 to-indigo-100"
      }`}
    >
      <button
        onClick={() => setDarkMode(!darkMode)}
        className={`absolute top-6 right-6 p-3 rounded-full transition-all shadow-lg ${
          darkMode
            ? "bg-gray-800 hover:bg-gray-700 text-yellow-400"
            : "bg-white hover:bg-gray-50 text-gray-700"
        }`}
      >
        {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
      </button>

      <div
        className={`w-full max-w-md p-8 rounded-2xl shadow-2xl ${
          darkMode ? "bg-gray-800" : "bg-white"
        }`}
      >
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-2">🗞️ Newslit</h1>
          <p
            className={`text-sm ${
              darkMode ? "text-gray-400" : "text-gray-600"
            }`}
          >
            매일 영어 기사로 배우는 영어 학습
          </p>
        </div>

        <div className="flex gap-2 mb-6">
          <button
            onClick={() => setIsLogin(true)}
            className={`flex-1 py-2.5 rounded-lg font-medium transition-all ${
              isLogin
                ? darkMode
                  ? "bg-blue-600 text-white shadow-lg"
                  : "bg-blue-500 text-white shadow-lg"
                : darkMode
                  ? "bg-gray-700 text-gray-300 hover:bg-gray-600"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            로그인
          </button>
          <button
            onClick={() => setIsLogin(false)}
            className={`flex-1 py-2.5 rounded-lg font-medium transition-all ${
              !isLogin
                ? darkMode
                  ? "bg-blue-600 text-white shadow-lg"
                  : "bg-blue-500 text-white shadow-lg"
                : darkMode
                  ? "bg-gray-700 text-gray-300 hover:bg-gray-600"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            회원가입
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              className={`block text-sm font-medium mb-2 ${
                darkMode ? "text-gray-300" : "text-gray-700"
              }`}
            >
              이메일
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="이메일을 입력하세요"
              disabled={loading}
              className={`w-full px-4 py-3 rounded-lg border transition-all ${
                darkMode
                  ? "bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:border-blue-500"
                  : "bg-white border-gray-300 text-gray-900 placeholder-gray-400 focus:border-blue-500"
              } focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:opacity-50`}
            />
          </div>

          {!isLogin && (
            <div>
              <label
                className={`block text-sm font-medium mb-2 ${
                  darkMode ? "text-gray-300" : "text-gray-700"
                }`}
              >
                닉네임
              </label>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="닉네임을 입력하세요"
                disabled={loading}
                className={`w-full px-4 py-3 rounded-lg border transition-all ${
                  darkMode
                    ? "bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:border-blue-500"
                    : "bg-white border-gray-300 text-gray-900 placeholder-gray-400 focus:border-blue-500"
                } focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:opacity-50`}
              />
            </div>
          )}

          <div>
            <label
              className={`block text-sm font-medium mb-2 ${
                darkMode ? "text-gray-300" : "text-gray-700"
              }`}
            >
              비밀번호
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              disabled={loading}
              className={`w-full px-4 py-3 rounded-lg border transition-all ${
                darkMode
                  ? "bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:border-blue-500"
                  : "bg-white border-gray-300 text-gray-900 placeholder-gray-400 focus:border-blue-500"
              } focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:opacity-50`}
            />
          </div>

          {!isLogin && (
            <div>
              <label
                className={`block text-sm font-medium mb-2 ${
                  darkMode ? "text-gray-300" : "text-gray-700"
                }`}
              >
                비밀번호 확인
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="비밀번호를 다시 입력하세요"
                disabled={loading}
                className={`w-full px-4 py-3 rounded-lg border transition-all ${
                  darkMode
                    ? "bg-gray-700 border-gray-600 text-white placeholder-gray-400 focus:border-blue-500"
                    : "bg-white border-gray-300 text-gray-900 placeholder-gray-400 focus:border-blue-500"
                } focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:opacity-50`}
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className={`w-full py-3 rounded-lg font-semibold transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 ${
              darkMode
                ? "bg-blue-600 hover:bg-blue-700 text-white"
                : "bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white"
            } disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none`}
          >
            {loading ? "처리 중..." : isLogin ? "로그인" : "회원가입"}
          </button>
        </form>

        <div className="mt-6">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div
                className={`w-full border-t ${
                  darkMode ? "border-gray-700" : "border-gray-300"
                }`}
              ></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span
                className={`px-2 ${
                  darkMode
                    ? "bg-gray-800 text-gray-400"
                    : "bg-white text-gray-500"
                }`}
              >
                또는
              </span>
            </div>
          </div>

          <button
            onClick={onGuestMode}
            className={`w-full mt-4 py-3 rounded-lg font-medium transition-all border-2 ${
              darkMode
                ? "border-gray-600 text-gray-300 hover:bg-gray-700"
                : "border-gray-300 text-gray-700 hover:bg-gray-50"
            }`}
          >
            비회원으로 시작하기
          </button>
        </div>

        {isLogin && (
          <div className="mt-6 text-center">
            <a
              href="#"
              className={`text-sm ${
                darkMode
                  ? "text-blue-400 hover:text-blue-300"
                  : "text-blue-600 hover:text-blue-700"
              }`}
            >
              비밀번호를 잊으셨나요?
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
