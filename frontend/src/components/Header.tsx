import { useMemo } from "react";
import { Moon, Sun, LogIn } from "lucide-react";

interface HeaderProps {
  darkMode: boolean;
  setDarkMode: (value: boolean) => void;
  isAuthenticated: boolean;
  isGuest: boolean;
  userNickname: string;
  goToLogin: () => void;
  handleLogout: () => void;
}

export default function Header({
  darkMode,
  setDarkMode,
  isAuthenticated,
  isGuest,
  userNickname,
  goToLogin,
  handleLogout,
}: HeaderProps) {
  // The displayed date is stable for the session, so compute it only once.
  const todayLabel = useMemo(
    () =>
      new Date().toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long",
      }),
    [],
  );

  return (
    <header className="mb-8">
      {/* Top bar: date + actions */}
      <div className="flex justify-between items-center mb-3">
        <span className="text-[11px] uppercase tracking-[0.5px] text-newsmuted">
          {todayLabel}
        </span>
        <div className="flex items-center gap-2">
          {isGuest && (
            <button
              onClick={goToLogin}
              className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-[1.5px] border transition-colors flex items-center gap-1.5 ${
                darkMode
                  ? "border-text-dark text-text-dark hover:bg-text-dark hover:text-ink"
                  : "border-ink text-ink hover:bg-ink hover:text-paper"
              }`}
            >
              <LogIn className="w-3.5 h-3.5" />
              로그인
            </button>
          )}
          {isAuthenticated && (
            <button
              onClick={handleLogout}
              className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-[1.5px] border transition-colors ${
                darkMode
                  ? "border-muted-dark text-muted-dark hover:border-text-dark hover:text-text-dark"
                  : "border-newsmuted text-newsmuted hover:border-ink hover:text-ink"
              }`}
            >
              로그아웃
            </button>
          )}
          <button
            onClick={() => setDarkMode(!darkMode)}
            aria-label="Toggle theme"
            className={`p-2 border transition-colors ${
              darkMode
                ? "border-edge-dark text-text-dark hover:border-text-dark"
                : "border-newsedge text-ink hover:border-ink"
            }`}
          >
            {darkMode ? (
              <Sun className="w-4 h-4" />
            ) : (
              <Moon className="w-4 h-4" />
            )}
          </button>
        </div>
      </div>

      {/* Masthead */}
      <div className="news-rule-double" />
      <div className="text-center py-5">
        <h1 className="font-masthead text-5xl sm:text-6xl font-black tracking-[2px] leading-none">
          NEWSLIT
        </h1>
        <p className="news-eyebrow mt-2">The English Learning Daily</p>
      </div>
      <div
        className={`border-b-[3px] border-double ${
          darkMode ? "border-text-dark" : "border-ink"
        }`}
      />

      {/* Greeting line */}
      {(isAuthenticated || isGuest) && (
        <p
          className={`text-center text-xs italic mt-3 ${
            darkMode ? "text-muted-dark" : "text-newsmuted"
          }`}
        >
          {isAuthenticated
            ? `${userNickname}님, 환영합니다.`
            : "비회원으로 이용 중"}
        </p>
      )}
    </header>
  );
}
