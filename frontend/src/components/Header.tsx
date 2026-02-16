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
  return (
    <div className="flex justify-between items-center mb-8">
      <div>
        <h1 className="text-3xl font-bold">🗞️ Newslit</h1>
        {isAuthenticated && (
          <p className="text-sm text-gray-500 mt-1">
            안녕하세요, {userNickname}님!
          </p>
        )}
        {isGuest && (
          <p className="text-sm text-gray-500 mt-1">비회원 모드</p>
        )}
      </div>
      <div className="flex items-center gap-3">
        {isGuest && (
          <button
            onClick={goToLogin}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${
              darkMode
                ? "bg-blue-600 hover:bg-blue-700 text-white"
                : "bg-blue-500 hover:bg-blue-600 text-white"
            }`}
          >
            <LogIn className="w-4 h-4" />
            로그인
          </button>
        )}
        {isAuthenticated && (
          <button
            onClick={handleLogout}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              darkMode
                ? "bg-red-600 hover:bg-red-700 text-white"
                : "bg-red-500 hover:bg-red-600 text-white"
            }`}
          >
            로그아웃
          </button>
        )}
        <button
          onClick={() => setDarkMode(!darkMode)}
          className={`p-2 rounded-lg transition-colors ${
            darkMode
              ? "bg-gray-700 hover:bg-gray-600"
              : "bg-gray-100 hover:bg-gray-200"
          }`}
        >
          {darkMode ? (
            <Sun className="w-5 h-5" />
          ) : (
            <Moon className="w-5 h-5" />
          )}
        </button>
      </div>
    </div>
  );
}
