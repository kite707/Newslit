import { useState } from "react";
import { AuthService } from "../services/authService";
import type { AuthResponse } from "@/types";
import { Moon, Sun } from "lucide-react";

type SignupStep = 1 | 2 | 3;

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
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [signupStep, setSignupStep] = useState<SignupStep>(1);
  const [verificationCode, setVerificationCode] = useState("");

  const resetSignupState = () => {
    setSignupStep(1);
    setVerificationCode("");
    setEmail("");
    setNickname("");
    setPassword("");
    setConfirmPassword("");
    setErrors({});
  };

  const handleTabSwitch = (toLogin: boolean) => {
    setIsLogin(toLogin);
    resetSignupState();
    setPassword("");
    setErrors({});
  };

  const handleSendCode = async () => {
    if (!email) {
      setErrors({ email: "이메일을 입력해주세요." });
      return;
    }
    setErrors({});
    setLoading(true);
    try {
      await AuthService.sendCode(email);
      setSignupStep(2);
    } catch (error) {
      setErrors({
        form: error instanceof Error ? error.message : "인증코드 발송에 실패했습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!verificationCode) {
      setErrors({ code: "인증코드를 입력해주세요." });
      return;
    }
    setErrors({});
    setLoading(true);
    try {
      await AuthService.verifyCode(email, verificationCode);
      setSignupStep(3);
    } catch (error) {
      setErrors({
        form: error instanceof Error ? error.message : "인증코드 검증에 실패했습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    setErrors({});
    setLoading(true);
    try {
      await AuthService.sendCode(email);
      setVerificationCode("");
      setErrors({ form: "인증코드가 재발송되었습니다." });
    } catch (error) {
      setErrors({
        form: error instanceof Error ? error.message : "인증코드 재발송에 실패했습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSignup = async () => {
    const newErrors: Record<string, string> = {};
    if (!nickname) newErrors.nickname = "닉네임을 입력해주세요.";
    if (!password) newErrors.password = "비밀번호를 입력해주세요.";
    if (password && confirmPassword && password !== confirmPassword) {
      newErrors.confirmPassword = "비밀번호가 일치하지 않습니다.";
    } else if (!confirmPassword) {
      newErrors.confirmPassword = "비밀번호 확인을 입력해주세요.";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    setLoading(true);
    try {
      const data: AuthResponse = await AuthService.signup(email, nickname, password);
      const displayName = data.nickname || nickname || email;
      onLogin(email, displayName);
    } catch (error) {
      setErrors({
        form: error instanceof Error ? error.message : "회원가입에 실패했습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async () => {
    const newErrors: Record<string, string> = {};
    if (!email) newErrors.email = "이메일을 입력해주세요.";
    if (!password) newErrors.password = "비밀번호를 입력해주세요.";

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    setLoading(true);
    try {
      const data: AuthResponse = await AuthService.login(email, password);
      const displayName = data.nickname || email;
      onLogin(email, displayName);
    } catch (error) {
      setErrors({
        form: error instanceof Error ? error.message : "로그인에 실패했습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isLogin) {
      await handleLogin();
    } else if (signupStep === 1) {
      await handleSendCode();
    } else if (signupStep === 2) {
      await handleVerifyCode();
    } else {
      await handleSignup();
    }
  };

  const inputClass = (fieldError?: string) =>
    `w-full px-4 py-3 rounded-lg border transition-all ${
      fieldError
        ? "border-red-500 focus:border-red-500 focus:ring-red-500"
        : darkMode
          ? "border-gray-600 focus:border-blue-500"
          : "border-gray-300 focus:border-blue-500"
    } ${
      darkMode
        ? "bg-gray-700 text-white placeholder-gray-400"
        : "bg-white text-gray-900 placeholder-gray-400"
    } focus:outline-none focus:ring-2 focus:ring-opacity-50 disabled:opacity-50`;

  const labelClass = `block text-sm font-medium mb-2 ${
    darkMode ? "text-gray-300" : "text-gray-700"
  }`;

  const stepIndicator = () => {
    if (isLogin) return null;
    const steps = ["이메일 인증", "코드 입력", "정보 입력"];
    return (
      <div className="flex items-center justify-center gap-2 mb-6">
        {steps.map((label, i) => {
          const stepNum = (i + 1) as SignupStep;
          const isActive = signupStep === stepNum;
          const isCompleted = signupStep > stepNum;
          return (
            <div key={label} className="flex items-center gap-2">
              {i > 0 && (
                <div
                  className={`w-6 h-0.5 ${
                    isCompleted
                      ? "bg-blue-500"
                      : darkMode
                        ? "bg-gray-600"
                        : "bg-gray-300"
                  }`}
                />
              )}
              <div className="flex flex-col items-center">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold ${
                    isActive
                      ? "bg-blue-500 text-white"
                      : isCompleted
                        ? "bg-blue-500 text-white"
                        : darkMode
                          ? "bg-gray-700 text-gray-400 border border-gray-600"
                          : "bg-gray-200 text-gray-500"
                  }`}
                >
                  {isCompleted ? "✓" : stepNum}
                </div>
                <span
                  className={`text-xs mt-1 ${
                    isActive
                      ? darkMode
                        ? "text-blue-400"
                        : "text-blue-600"
                      : darkMode
                        ? "text-gray-500"
                        : "text-gray-400"
                  }`}
                >
                  {label}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  const renderSignupFields = () => {
    if (signupStep === 1) {
      return (
        <div>
          <label className={labelClass}>이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setErrors((prev) => { const { email: _, ...rest } = prev; return rest; });
            }}
            placeholder="이메일을 입력하세요"
            disabled={loading}
            className={inputClass(errors.email)}
          />
          {errors.email && (
            <p className="mt-1 text-sm text-red-500">{errors.email}</p>
          )}
        </div>
      );
    }

    if (signupStep === 2) {
      return (
        <>
          <div>
            <label className={labelClass}>인증코드</label>
            <p className={`text-sm mb-2 ${darkMode ? "text-gray-400" : "text-gray-500"}`}>
              {email}로 발송된 6자리 코드를 입력하세요
            </p>
            <input
              type="text"
              value={verificationCode}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                setVerificationCode(val);
                setErrors((prev) => { const { code: _, ...rest } = prev; return rest; });
              }}
              placeholder="000000"
              maxLength={6}
              disabled={loading}
              className={`${inputClass(errors.code)} text-center text-2xl tracking-[0.5em]`}
            />
            {errors.code && (
              <p className="mt-1 text-sm text-red-500">{errors.code}</p>
            )}
          </div>
          <div className="text-center">
            <button
              type="button"
              onClick={handleResendCode}
              disabled={loading}
              className={`text-sm ${
                darkMode
                  ? "text-blue-400 hover:text-blue-300"
                  : "text-blue-600 hover:text-blue-700"
              } disabled:opacity-50`}
            >
              인증코드 재발송
            </button>
          </div>
        </>
      );
    }

    return (
      <>
        <div>
          <label className={labelClass}>닉네임</label>
          <input
            type="text"
            value={nickname}
            onChange={(e) => {
              setNickname(e.target.value);
              setErrors((prev) => { const { nickname: _, ...rest } = prev; return rest; });
            }}
            placeholder="닉네임을 입력하세요"
            disabled={loading}
            className={inputClass(errors.nickname)}
          />
          {errors.nickname && (
            <p className="mt-1 text-sm text-red-500">{errors.nickname}</p>
          )}
        </div>
        <div>
          <label className={labelClass}>비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setErrors((prev) => { const { password: _, ...rest } = prev; return rest; });
            }}
            placeholder="비밀번호를 입력하세요"
            disabled={loading}
            className={inputClass(errors.password)}
          />
          {errors.password && (
            <p className="mt-1 text-sm text-red-500">{errors.password}</p>
          )}
        </div>
        <div>
          <label className={labelClass}>비밀번호 확인</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              setErrors((prev) => { const { confirmPassword: _, ...rest } = prev; return rest; });
            }}
            placeholder="비밀번호를 다시 입력하세요"
            disabled={loading}
            className={inputClass(errors.confirmPassword)}
          />
          {errors.confirmPassword && (
            <p className="mt-1 text-sm text-red-500">{errors.confirmPassword}</p>
          )}
        </div>
      </>
    );
  };

  const getSubmitLabel = () => {
    if (loading) return "처리 중...";
    if (isLogin) return "로그인";
    if (signupStep === 1) return "인증코드 발송";
    if (signupStep === 2) return "인증하기";
    return "회원가입";
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
            onClick={() => handleTabSwitch(true)}
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
            onClick={() => handleTabSwitch(false)}
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

        {stepIndicator()}

        <form onSubmit={handleSubmit} className="space-y-4">
          {errors.form && (
            <div className={`p-3 rounded-lg text-sm ${
              errors.form.includes("재발송")
                ? "bg-blue-100 text-blue-700"
                : "bg-red-100 text-red-700"
            }`}>
              {errors.form}
            </div>
          )}

          {isLogin ? (
            <>
              <div>
                <label className={labelClass}>이메일</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setErrors((prev) => { const { email: _, ...rest } = prev; return rest; });
                  }}
                  placeholder="이메일을 입력하세요"
                  disabled={loading}
                  className={inputClass(errors.email)}
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-500">{errors.email}</p>
                )}
              </div>
              <div>
                <label className={labelClass}>비밀번호</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setErrors((prev) => { const { password: _, ...rest } = prev; return rest; });
                  }}
                  placeholder="비밀번호를 입력하세요"
                  disabled={loading}
                  className={inputClass(errors.password)}
                />
                {errors.password && (
                  <p className="mt-1 text-sm text-red-500">{errors.password}</p>
                )}
              </div>
            </>
          ) : (
            renderSignupFields()
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
            {getSubmitLabel()}
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
            <button
              type="button"
              onClick={() => setErrors({ form: "준비 중인 기능입니다." })}
              className={`text-sm ${
                darkMode
                  ? "text-blue-400 hover:text-blue-300"
                  : "text-blue-600 hover:text-blue-700"
              }`}
            >
              비밀번호를 잊으셨나요?
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
