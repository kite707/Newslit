import { useState, useEffect } from "react";
import {
  Volume2,
  Check,
  Moon,
  Sun,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  LogIn,
} from "lucide-react";
import {
  fetchArticle,
  fetchAvailableDates,
  DEFAULT_ARTICLE_DATA,
} from "./services/articleService";
import {
  markArticleAsComplete,
  fetchReadingHistory,
} from "./services/historyService";
import { fetchVocabulary } from "./services/vocabularyService";
import type { DailyData, AuthResponse, VocabularyItem } from "@/types";
import { AuthService } from "./services/authService";

function AuthPage({
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
        error instanceof Error ? error.message : "서버 연결에 실패했습니다."
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

export default function EnglishLearningApp() {
  // 초기 로딩 상태 추가
  const [isInitializing, setIsInitializing] = useState(true);

  // 인증 상태
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isGuest, setIsGuest] = useState(false);
  const [_userEmail, setUserEmail] = useState("");
  const [userNickname, setUserNickname] = useState("");

  // UI 상태
  const [darkMode, setDarkMode] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});
  const [hoveredWordId, setHoveredWordId] = useState<string | null>(null);
  const [expandedSentences, setExpandedSentences] = useState<
    Record<number, boolean>
  >({});

  // 데이터 상태
  const [articleData, setArticleData] =
    useState<DailyData>(DEFAULT_ARTICLE_DATA);
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [completedDates, setCompletedDates] = useState<number[]>([]);
  const [availableDates, setAvailableDates] = useState<number[]>([]);
  const [vocabularies, setVocabularies] = useState<VocabularyItem[]>([]);

  // 로그인 핸들러
  const handleLogin = (email: string, nickname: string) => {
    setIsAuthenticated(true);
    setIsGuest(false);
    setUserEmail(email);
    setUserNickname(nickname);

    localStorage.setItem("isAuthenticated", "true");
    localStorage.setItem("userEmail", email);
    localStorage.setItem("userNickname", nickname);
  };

  // 비회원 모드
  const handleGuestMode = () => {
    setIsGuest(true);
    setIsAuthenticated(false);
    localStorage.setItem("isGuest", "true");
  };

  // 로그아웃 핸들러
  const handleLogout = async () => {
    await AuthService.logout();
    setIsAuthenticated(false);
    setIsGuest(false);
    setUserEmail("");
    setUserNickname("");
    localStorage.removeItem("isAuthenticated");
    localStorage.removeItem("isGuest");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userNickname");
  };

  // 로그인 페이지로 이동
  const goToLogin = () => {
    setIsGuest(false);
    localStorage.removeItem("isGuest");
  };

  // 페이지 로드 시 인증 상태 확인 - 초기화 단계에서 처리
  useEffect(() => {
    const savedAuth = localStorage.getItem("isAuthenticated");
    const savedGuest = localStorage.getItem("isGuest");
    const savedEmail = localStorage.getItem("userEmail");
    const savedNickname = localStorage.getItem("userNickname");

    if (savedAuth === "true" && savedEmail && savedNickname) {
      setIsAuthenticated(true);
      setUserEmail(savedEmail);
      setUserNickname(savedNickname);
    } else if (savedGuest === "true") {
      setIsGuest(true);
    }

    // 초기화 완료
    setIsInitializing(false);
  }, []);

  // 현재 날짜의 기사 불러오기
  useEffect(() => {
    if (!isAuthenticated && !isGuest) return;

    const loadArticle = async () => {
      setLoading(true);
      const data = await fetchArticle(currentDate);
      setArticleData(data);

      if (data.sentences.length > 0 && data.sentences[0]?.articleId) {
        const vocabData = await fetchVocabulary(data.sentences[0].articleId);
        setVocabularies(vocabData);
      }

      setLoading(false);
    };
    loadArticle();
  }, [currentDate, isAuthenticated, isGuest]);

  // 날짜 클릭 핸들러
  const handleDateClick = async (day: number | null) => {
    if (day === null || !availableDates.includes(day)) return;

    const selectedDate = new Date(
      currentMonth.getFullYear(),
      currentMonth.getMonth(),
      day
    );
    setCurrentDate(selectedDate);
  };

  // 월별 완료 기록 & 사용 가능한 날짜 불러오기
  useEffect(() => {
    if (!isAuthenticated && !isGuest) return;

    const loadData = async () => {
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;

      const [completed, available] = await Promise.all([
        isAuthenticated
          ? fetchReadingHistory(year, month)
          : Promise.resolve([]),
        fetchAvailableDates(year, month),
      ]);

      setCompletedDates(completed);
      setAvailableDates(available);
    };

    loadData();
  }, [currentMonth, isAuthenticated, isGuest]);

  const playAudio = (): void => {
    alert("음성 재생 기능은 백엔드 연동 후 사용 가능합니다.");
  };

  // 완료 처리
  const handleComplete = async () => {
    if (!isAuthenticated) {
      const confirmLogin = window.confirm(
        "로그인이 필요한 기능입니다.\n로그인 페이지로 이동하시겠습니까?"
      );
      if (confirmLogin) {
        goToLogin();
      }
      return;
    }

    if (
      !articleData ||
      articleData.sentences.length === 0 ||
      !articleData.sentences[0]?.articleId
    ) {
      alert("기사 정보가 없습니다.");
      return;
    }

    try {
      await markArticleAsComplete(articleData.dailyId);
      setCompleted(!completed);
      alert("완료 처리되었습니다!");

      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      const updatedCompleted = await fetchReadingHistory(year, month);
      setCompletedDates(updatedCompleted);
    } catch (error) {
      if (error instanceof Error && error.message === "UNAUTHORIZED") {
        alert(`로그인 정보가 만료되었습니다. 로그인 페이지로 이동합니다.`);
        await handleLogout();
        return;
      }
      alert(`완료 처리에 실패했습니다.\n${error}`);
    }
  };

  const getShortPos = (pos: string): string => {
    const posMap: Record<string, string> = {
      명사: "n",
      동사: "v",
      형용사: "a",
      부사: "adv",
    };
    return posMap[pos] || pos;
  };

  const getPosColor = (pos: string): string => {
    const shortPos = getShortPos(pos);
    const colors: Record<string, string> = {
      n: darkMode ? "bg-blue-900 text-blue-200" : "bg-blue-100 text-blue-700",
      v: darkMode
        ? "bg-green-900 text-green-200"
        : "bg-green-100 text-green-700",
      a: darkMode
        ? "bg-purple-900 text-purple-200"
        : "bg-purple-100 text-purple-700",
      adv: darkMode
        ? "bg-orange-900 text-orange-200"
        : "bg-orange-100 text-orange-700",
    };
    return (
      colors[shortPos] ||
      (darkMode ? "bg-gray-700 text-gray-200" : "bg-gray-100 text-gray-700")
    );
  };

  const highlightVocabulary = (text: string, sentenceIndex: number) => {
    if (vocabularies.length === 0) return text;

    const words = vocabularies.map((v) => v.word.toLowerCase());
    const regex = new RegExp(`\\b(${words.join("|")})\\b`, "gi");

    const parts = text.split(regex);
    let wordCounter = 0;

    return parts.map((part, index) => {
      const vocab = vocabularies.find(
        (v) => v.word.toLowerCase() === part.toLowerCase()
      );

      if (vocab) {
        const uniqueId = `${sentenceIndex}-${wordCounter}`;
        wordCounter++;

        return (
          <span
            key={index}
            className={`relative inline-block cursor-pointer font-semibold ${
              darkMode
                ? "text-yellow-300 hover:text-yellow-200"
                : "text-blue-600 hover:text-blue-700"
            } border-b-2 ${darkMode ? "border-yellow-300" : "border-blue-600"}`}
            onMouseEnter={() => setHoveredWordId(uniqueId)}
            onMouseLeave={() => setHoveredWordId(null)}
          >
            {part}
            {hoveredWordId === uniqueId && (
              <span
                className={`absolute z-10 bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 rounded-lg shadow-lg whitespace-nowrap ${
                  darkMode
                    ? "bg-gray-700 text-white"
                    : "bg-white text-gray-900 border border-gray-200"
                }`}
              >
                <span className="block font-bold">{vocab.word}</span>
                <span
                  className={`text-xs px-2 py-0.5 rounded ${getPosColor(
                    vocab.partOfSpeech
                  )} inline-block my-1`}
                >
                  {getShortPos(vocab.partOfSpeech)}
                </span>
                <span className="block text-sm">{vocab.meaning}</span>
              </span>
            )}
          </span>
        );
      }

      return part;
    });
  };

  const getFilteredVocabularies = () => {
    const allText = articleData.sentences
      .map((s) => s.englishText)
      .join(" ")
      .toLowerCase();

    return vocabularies.filter((vocab) =>
      allText.includes(vocab.word.toLowerCase())
    );
  };

  const getDaysInMonth = (date: Date): (number | null)[] => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) days.push(null);
    for (let i = 1; i <= daysInMonth; i++) days.push(i);
    return days;
  };

  const monthNames: string[] = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];

  const changeMonth = (delta: number): void => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + delta, 1)
    );
  };

  // 초기화 중일 때 로딩 화면
  if (isInitializing) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  // 로그인/비회원 체크
  if (!isAuthenticated && !isGuest) {
    return <AuthPage onLogin={handleLogin} onGuestMode={handleGuestMode} />;
  }

  if (loading) {
    return (
      <div
        className={`min-h-screen flex items-center justify-center ${
          darkMode ? "bg-gray-900 text-white" : "bg-gray-50 text-gray-900"
        }`}
      >
        <div className="text-xl">Loading article...</div>
      </div>
    );
  }

  const filteredVocabularies = getFilteredVocabularies();

  return (
    <div
      className={`min-h-screen ${
        darkMode ? "bg-gray-900 text-white" : "bg-gray-50 text-gray-900"
      }`}
    >
      <div className="max-w-4xl mx-auto p-6">
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

        <div
          className={`rounded-xl p-6 mb-6 ${
            darkMode ? "bg-gray-800" : "bg-white"
          } shadow-lg`}
        >
          <div className="mb-4">
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h2 className="text-2xl font-bold">{articleData.title}</h2>
              <span
                className={`text-sm font-medium px-3 py-1 rounded-full ${
                  darkMode
                    ? "bg-gray-700 text-gray-300"
                    : "bg-gray-200 text-gray-600"
                }`}
              >
                {articleData.currentPages} / {articleData.totalPages}
              </span>
            </div>
            <div className="flex items-center gap-3 flex-wrap">
              <div
                className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${
                  darkMode
                    ? "bg-blue-900 text-blue-200"
                    : "bg-blue-100 text-blue-700"
                }`}
              >
                {articleData.source}
              </div>
              <span
                className={`text-sm ${
                  darkMode ? "text-gray-400" : "text-gray-600"
                }`}
              >
                {articleData.publishedDate}
              </span>
              {articleData.sourceUrl && (
                <a
                  href={articleData.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`text-sm flex items-center gap-1 ${
                    darkMode
                      ? "text-blue-400 hover:text-blue-300"
                      : "text-blue-600 hover:text-blue-700"
                  }`}
                >
                  원문 보기 <ExternalLink className="w-3 h-3" />
                </a>
              )}
            </div>
          </div>

          <div className="space-y-6 mb-4">
            {articleData.sentences.map((sentence, index) => (
              <div
                key={index}
                onClick={() =>
                  setExpandedSentences((prev) => ({
                    ...prev,
                    [index]: !prev[index],
                  }))
                }
                className={`cursor-pointer transition-all duration-200 ${
                  darkMode ? "hover:bg-gray-700/30" : "hover:bg-gray-50"
                } rounded-lg p-4 -mx-4`}
              >
                <p className="text-lg leading-relaxed">
                  {highlightVocabulary(sentence.englishText, index)}
                </p>
                <div
                  className={`overflow-hidden transition-all duration-300 ${
                    expandedSentences[index]
                      ? "max-h-40 opacity-100 mt-3"
                      : "max-h-0 opacity-0"
                  }`}
                >
                  <p
                    className={`text-base border-l-2 pl-3 ${
                      darkMode
                        ? "text-gray-300 border-blue-500"
                        : "text-gray-600 border-blue-400"
                    }`}
                  >
                    {sentence.koreanText}
                  </p>
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-center pt-2">
            <button
              onClick={(e) => {
                e.stopPropagation();
                const allExpanded = articleData.sentences.every(
                  (_, i) => expandedSentences[i]
                );
                const newState: Record<number, boolean> = {};
                articleData.sentences.forEach((_, i) => {
                  newState[i] = !allExpanded;
                });
                setExpandedSentences(newState);
              }}
              className={`text-sm font-medium transition-colors ${
                darkMode
                  ? "text-blue-400 hover:text-blue-300"
                  : "text-blue-600 hover:text-blue-700"
              }`}
            >
              {articleData.sentences.every((_, i) => expandedSentences[i])
                ? "모든 번역 숨기기 ▲"
                : "모든 번역 보기 ▼"}
            </button>
          </div>
        </div>

        <div
          className={`rounded-xl p-6 mb-6 ${
            darkMode ? "bg-gray-800" : "bg-white"
          } shadow-lg`}
        >
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-2xl font-bold">Key Words</h2>
            <button
              onClick={() => {
                const allShown = filteredVocabularies.every((vocab) => {
                  const idx = vocabularies.indexOf(vocab);
                  return showMeaning[idx];
                });
                const newState: Record<number, boolean> = {};
                filteredVocabularies.forEach((vocab) => {
                  const idx = vocabularies.indexOf(vocab);
                  newState[idx] = !allShown;
                });
                setShowMeaning(newState);
              }}
              className={`text-xs font-medium px-3 py-1 rounded-full transition-colors ${
                darkMode
                  ? "bg-gray-600 hover:bg-gray-500 text-gray-300"
                  : "bg-gray-200 hover:bg-gray-300 text-gray-700"
              }`}
            >
              {filteredVocabularies.every((vocab) => {
                const idx = vocabularies.indexOf(vocab);
                return showMeaning[idx];
              })
                ? "뜻 숨기기"
                : "뜻 보기"}
            </button>
          </div>

          <div className="space-y-4">
            {filteredVocabularies.map((item) => {
              const idx = vocabularies.indexOf(item);
              return (
                <div
                  key={item.id}
                  onClick={() =>
                    setShowMeaning({ ...showMeaning, [idx]: !showMeaning[idx] })
                  }
                  className={`p-4 rounded-lg cursor-pointer transition-colors ${
                    darkMode
                      ? "bg-gray-700 hover:bg-gray-600"
                      : "bg-gray-50 hover:bg-gray-100"
                  }`}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <span className="font-bold text-lg">{item.word}</span>
                    <span
                      className={`text-xs px-2 py-1 rounded ${getPosColor(
                        item.partOfSpeech
                      )}`}
                    >
                      {getShortPos(item.partOfSpeech)}
                    </span>
                  </div>

                  {showMeaning[idx] && (
                    <div className="mt-2">
                      <p className="text-sm mb-2">{item.meaning}</p>
                    </div>
                  )}

                  {item.exampleSentence && showMeaning[idx] && (
                    <div
                      className={`mt-2 p-3 rounded ${
                        darkMode ? "bg-gray-600" : "bg-white"
                      }`}
                    >
                      <p className="text-sm italic mb-1">
                        "{item.exampleSentence}"
                      </p>
                      {item.exampleTranslation && (
                        <p className="text-xs text-gray-500">
                          {item.exampleTranslation}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex gap-4 mb-6">
          <button
            onClick={playAudio}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg font-medium transition-colors ${
              darkMode
                ? "bg-blue-600 hover:bg-blue-700"
                : "bg-blue-500 hover:bg-blue-600"
            } text-white`}
          >
            <Volume2 className="w-5 h-5" />
            Play Audio
          </button>
          <button
            onClick={handleComplete}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg font-medium transition-colors ${
              completed
                ? darkMode
                  ? "bg-green-600 hover:bg-green-700"
                  : "bg-green-500 hover:bg-green-600"
                : darkMode
                ? "bg-gray-700 hover:bg-gray-600"
                : "bg-gray-200 hover:bg-gray-300"
            } ${completed ? "text-white" : ""}`}
          >
            <Check className="w-5 h-5" />
            {completed ? "Completed!" : "Mark as Complete"}
          </button>
        </div>

        <div
          className={`rounded-xl p-6 ${
            darkMode ? "bg-gray-800" : "bg-white"
          } shadow-lg`}
        >
          <h2 className="text-2xl font-bold mb-4">Study Record</h2>
          <div className="flex justify-between items-center mb-4">
            <button
              onClick={() => changeMonth(-1)}
              className={`p-2 rounded-lg transition-colors ${
                darkMode ? "hover:bg-gray-700" : "hover:bg-gray-100"
              }`}
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <span className="font-semibold">
              {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
            </span>
            <button
              onClick={() => changeMonth(1)}
              className={`p-2 rounded-lg transition-colors ${
                darkMode ? "hover:bg-gray-700" : "hover:bg-gray-100"
              }`}
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-2">
            {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
              <div key={day} className="text-center text-sm font-semibold p-2">
                {day}
              </div>
            ))}
            {getDaysInMonth(currentMonth).map((day, idx) => {
              const hasArticle = day && availableDates.includes(day);
              const isCompleted = day && completedDates.includes(day);
              const isCurrentSelected =
                day &&
                currentDate.getDate() === day &&
                currentDate.getMonth() === currentMonth.getMonth() &&
                currentDate.getFullYear() === currentMonth.getFullYear();

              return (
                <div
                  key={idx}
                  onClick={() => handleDateClick(day)}
                  className={`p-1 rounded-lg transition-all ${
                    hasArticle
                      ? "cursor-pointer hover:bg-opacity-10"
                      : "cursor-not-allowed opacity-40"
                  }`}
                >
                  <div
                    className={`text-center text-sm relative rounded-lg p-2 transition-all ${
                      isCompleted
                        ? darkMode
                          ? "bg-green-600 text-white shadow-md"
                          : "bg-green-500 text-white shadow-md"
                        : isCurrentSelected
                        ? darkMode
                          ? "bg-blue-600 text-white shadow-lg scale-105"
                          : "bg-blue-500 text-white shadow-lg scale-105"
                        : ""
                    }`}
                  >
                    {day}
                    {day &&
                      !isCompleted &&
                      !isCurrentSelected &&
                      hasArticle && (
                        <div
                          className={`absolute -bottom-0.5 left-1/2 transform -translate-x-1/2 w-1 h-1 rounded-full ${
                            darkMode ? "bg-blue-400" : "bg-blue-500"
                          }`}
                        ></div>
                      )}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="flex gap-4 mt-4 text-sm">
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-green-600" : "bg-green-500"
                }`}
              ></div>
              <span>Completed</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-blue-600" : "bg-blue-500"
                }`}
              ></div>
              <span>Selected</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-gray-700" : "bg-gray-100"
                } relative flex items-end justify-center`}
              >
                <div className="w-1 h-1 rounded-full bg-blue-500 mb-0.5"></div>
              </div>
              <span>Article Available</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
