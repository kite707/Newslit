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
  const [message, setMessage] = useState("");

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
    setMessage("");
  };

  const handleTabSwitch = (toLogin: boolean) => {
    setIsLogin(toLogin);
    resetSignupState();
    setPassword("");
    setErrors({});
    setMessage("");
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
    setMessage("");
    setLoading(true);
    try {
      await AuthService.sendCode(email);
      setVerificationCode("");
      setMessage("인증코드가 재발송되었습니다.");
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
    `w-full px-4 py-3 border transition-all ${
      fieldError
        ? "border-red-600 focus:border-red-600"
        : darkMode
          ? "border-edge-dark focus:border-text-dark"
          : "border-newsedge focus:border-ink"
    } ${
      darkMode
        ? "bg-paper-dark text-text-dark placeholder-muted-dark"
        : "bg-white text-ink placeholder-newsfaint"
    } focus:outline-none disabled:opacity-50`;

  const labelClass = `block text-[11px] font-semibold uppercase tracking-[2px] mb-2 ${
    darkMode ? "text-muted-dark" : "text-newsmuted"
  }`;

  const stepIndicator = () => {
    if (isLogin) return null;
    const steps = ["Email", "Verify", "Details"];
    return (
      <div className="flex items-center justify-center gap-2 mb-6">
        {steps.map((label, i) => {
          const stepNum = (i + 1) as SignupStep;
          const isActive = signupStep === stepNum;
          const isCompleted = signupStep > stepNum;
          const onColor = darkMode
            ? "bg-text-dark text-ink border-text-dark"
            : "bg-ink text-paper border-ink";
          const offColor = darkMode
            ? "text-muted-dark border-edge-dark"
            : "text-newsmuted border-newsedge";
          return (
            <div key={label} className="flex items-center gap-2">
              {i > 0 && (
                <div
                  className={`w-6 h-px ${
                    isCompleted
                      ? darkMode
                        ? "bg-text-dark"
                        : "bg-ink"
                      : darkMode
                        ? "bg-edge-dark"
                        : "bg-newsedge"
                  }`}
                />
              )}
              <div className="flex flex-col items-center">
                <div
                  className={`w-7 h-7 flex items-center justify-center text-xs font-bold border ${
                    isActive || isCompleted ? onColor : offColor
                  }`}
                >
                  {isCompleted ? "✓" : stepNum}
                </div>
                <span
                  className={`text-[10px] uppercase tracking-[1px] mt-1 ${
                    isActive
                      ? darkMode
                        ? "text-text-dark"
                        : "text-ink"
                      : darkMode
                        ? "text-muted-dark"
                        : "text-newsfaint"
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
            <p className="mt-1 text-sm text-red-600">{errors.email}</p>
          )}
        </div>
      );
    }

    if (signupStep === 2) {
      return (
        <>
          <div
            className={`border-2 p-6 text-center ${
              darkMode ? "border-text-dark" : "border-ink"
            }`}
          >
            <div className="news-eyebrow mb-1">Your Code</div>
            <p
              className={`text-xs mb-4 ${
                darkMode ? "text-muted-dark" : "text-newsmuted"
              }`}
            >
              {email}로 발송된 6자리 코드를 입력하세요
            </p>
            <input
              type="text"
              inputMode="numeric"
              value={verificationCode}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, "").slice(0, 6);
                setVerificationCode(val);
                setErrors((prev) => { const { code: _, ...rest } = prev; return rest; });
              }}
              placeholder="000000"
              maxLength={6}
              disabled={loading}
              className={`w-full bg-transparent text-center font-mono text-4xl font-bold tracking-[0.4em] focus:outline-none disabled:opacity-50 ${
                darkMode
                  ? "text-text-dark placeholder-muted-dark"
                  : "text-ink placeholder-newsfaint"
              }`}
            />
            <p
              className={`mt-3 text-xs italic ${
                darkMode ? "text-muted-dark" : "text-newsfaint"
              }`}
            >
              Expires in 10 minutes
            </p>
            {errors.code && (
              <p className="mt-2 text-sm text-red-600">{errors.code}</p>
            )}
          </div>
          <div className="text-center">
            <button
              type="button"
              onClick={handleResendCode}
              disabled={loading}
              className={`text-xs font-semibold uppercase tracking-[1.5px] underline underline-offset-2 ${
                darkMode
                  ? "text-muted-dark hover:text-text-dark"
                  : "text-newsmuted hover:text-ink"
              } disabled:opacity-50`}
            >
              Resend code
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
            <p className="mt-1 text-sm text-red-600">{errors.nickname}</p>
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
            <p className="mt-1 text-sm text-red-600">{errors.password}</p>
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
            <p className="mt-1 text-sm text-red-600">{errors.confirmPassword}</p>
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
      className={`min-h-screen flex items-center justify-center p-4 ${
        darkMode ? "bg-paper-dark" : "bg-paper"
      }`}
    >
      <button
        onClick={() => setDarkMode(!darkMode)}
        aria-label="Toggle theme"
        className={`absolute top-6 right-6 p-2.5 border transition-all ${
          darkMode
            ? "border-edge-dark text-text-dark hover:border-text-dark"
            : "border-newsedge text-ink hover:border-ink"
        }`}
      >
        {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
      </button>

      <div
        className={`w-full max-w-md border overflow-hidden ${
          darkMode ? "bg-card-dark border-edge-dark" : "bg-white border-newsedge"
        }`}
      >
        {/* Masthead bar (matches verification email) */}
        <div className="bg-ink text-center px-8 py-7">
          <div className="font-masthead text-3xl font-black tracking-[4px] text-white">
            NEWSLIT
          </div>
          <div className="text-[11px] uppercase tracking-[2px] text-newsmuted mt-1.5">
            The English Learning Daily
          </div>
        </div>

        <div className="p-8">
          <div
            className={`border-b-[3px] border-double mb-6 ${
              darkMode ? "border-text-dark" : "border-ink"
            }`}
          />

          <div
            className={`flex mb-6 border-b ${
              darkMode ? "border-edge-dark" : "border-newsrule"
            }`}
          >
            <button
              onClick={() => handleTabSwitch(true)}
              className={`flex-1 py-2.5 text-sm font-semibold uppercase tracking-[1.5px] transition-all border-b-2 -mb-px ${
                isLogin
                  ? darkMode
                    ? "border-text-dark text-text-dark"
                    : "border-ink text-ink"
                  : darkMode
                    ? "border-transparent text-muted-dark hover:text-text-dark"
                    : "border-transparent text-newsmuted hover:text-ink"
              }`}
            >
              Sign In
            </button>
            <button
              onClick={() => handleTabSwitch(false)}
              className={`flex-1 py-2.5 text-sm font-semibold uppercase tracking-[1.5px] transition-all border-b-2 -mb-px ${
                !isLogin
                  ? darkMode
                    ? "border-text-dark text-text-dark"
                    : "border-ink text-ink"
                  : darkMode
                    ? "border-transparent text-muted-dark hover:text-text-dark"
                    : "border-transparent text-newsmuted hover:text-ink"
              }`}
            >
              Register
            </button>
          </div>

          {stepIndicator()}

          <form onSubmit={handleSubmit} className="space-y-4">
            {message && (
              <div
                className={`p-3 text-sm border-l-2 ${
                  darkMode
                    ? "border-text-dark bg-paper-dark text-text-dark"
                    : "border-ink bg-paper text-newsbody"
                }`}
              >
                {message}
              </div>
            )}
            {errors.form && (
              <div className="p-3 text-sm border-l-2 border-red-600 bg-red-50 text-red-700">
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
                  <p className="mt-1 text-sm text-red-600">{errors.email}</p>
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
                  <p className="mt-1 text-sm text-red-600">{errors.password}</p>
                )}
              </div>
            </>
          ) : (
            renderSignupFields()
          )}

            <button
              type="submit"
              disabled={loading}
              className={`w-full py-3 text-sm font-semibold uppercase tracking-[2px] transition-all ${
                darkMode
                  ? "bg-text-dark text-ink hover:bg-white"
                  : "bg-ink text-paper hover:bg-black"
              } disabled:opacity-50 disabled:cursor-not-allowed`}
            >
              {getSubmitLabel()}
            </button>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div
                  className={`w-full border-t ${
                    darkMode ? "border-edge-dark" : "border-newsrule"
                  }`}
                ></div>
              </div>
              <div className="relative flex justify-center">
                <span
                  className={`px-3 text-[11px] uppercase tracking-[2px] ${
                    darkMode
                      ? "bg-card-dark text-muted-dark"
                      : "bg-white text-newsmuted"
                  }`}
                >
                  or
                </span>
              </div>
            </div>

            <button
              onClick={onGuestMode}
              className={`w-full mt-4 py-3 text-sm font-semibold uppercase tracking-[1.5px] transition-all border ${
                darkMode
                  ? "border-edge-dark text-text-dark hover:border-text-dark"
                  : "border-ink text-ink hover:bg-ink hover:text-paper"
              }`}
            >
              Continue as Guest
            </button>
          </div>

          {isLogin && (
            <div className="mt-6 text-center">
              <button
                type="button"
                onClick={() => { setErrors({}); setMessage("준비 중인 기능입니다."); }}
                className={`text-xs italic underline underline-offset-2 ${
                  darkMode
                    ? "text-muted-dark hover:text-text-dark"
                    : "text-newsmuted hover:text-ink"
                }`}
              >
                비밀번호를 잊으셨나요?
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
