import { useState, useEffect, useRef } from "react";
import {
  Volume2,
  Check,
  ExternalLink,
  Play,
  Pause,
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
import type { DailyData, VocabularyItem } from "@/types";
import { AuthService } from "./services/authService";
import AuthPage from "./AuthPage";
import CalendarSection from "./CalendarSection";
import VocabularySection from "./VocabularySection";
import Header from "./Header";

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

  // 오디오 관련 상태 추가
  const [currentAudioUrl, setCurrentAudioUrl] = useState<string>("");
  const [playingSentenceIndex, setPlayingSentenceIndex] = useState<
    number | null
  >(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [isPlayingFullAudio, setIsPlayingFullAudio] = useState(false);

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

  // 현재 날짜의 기사 불러오기 - 오디오 URL도 함께 저장
  useEffect(() => {
    if (!isAuthenticated && !isGuest) return;

    const loadArticle = async () => {
      setLoading(true);
      const data = await fetchArticle(currentDate);
      setArticleData(data);

      if (data.sentences.length > 0 && data.sentences[0]?.articleId) {
        const vocabData = await fetchVocabulary(data.sentences[0].articleId);
        setVocabularies(vocabData);

        // 해당 날짜의 오디오 URL 찾기
        const year = currentDate.getFullYear();
        const month = currentDate.getMonth() + 1;
        const availableData = await fetchAvailableDates(year, month);

        const dayString = currentDate.toISOString().slice(0, 10);
        const audioData = availableData.find(
          (item) =>
            item.displayDate === dayString &&
            item.articleId === data.sentences[0].articleId,
        );

        if (audioData) {
          setCurrentAudioUrl(audioData.audioUrl);
        }
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
      day,
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

      // availableData에서 날짜(day)만 추출하여 중복 제거
      const days = [
        ...new Set(
          available.map((item) => parseInt(item.displayDate.slice(-2), 10)),
        ),
      ].sort((a, b) => a - b);

      setAvailableDates(days);
    };

    loadData();
  }, [currentMonth, isAuthenticated, isGuest]);

  // 오디오 재생 함수
  const playSentenceAudio = (index: number) => {
    const sentence = articleData.sentences[index];

    if (!currentAudioUrl || !sentence) {
      alert("오디오를 불러올 수 없습니다.");
      return;
    }

    // 이미 재생 중인 같은 문장이면 일시정지
    if (
      playingSentenceIndex === index &&
      audioRef.current &&
      !audioRef.current.paused
    ) {
      audioRef.current.pause();
      setPlayingSentenceIndex(null);
      return;
    }

    // 새로운 오디오 객체 생성 또는 기존 것 사용
    if (!audioRef.current) {
      audioRef.current = new Audio(currentAudioUrl);
    } else if (audioRef.current.src !== currentAudioUrl) {
      audioRef.current.src = currentAudioUrl;
    }

    const audio = audioRef.current;

    // 시작 시간 설정
    audio.currentTime = sentence.startTime;
    setPlayingSentenceIndex(index);

    // 재생
    audio.play().catch((err) => {
      console.error("Audio play failed:", err);
      alert("오디오 재생에 실패했습니다.");
      setPlayingSentenceIndex(null);
    });

    // 종료 시간 0.3초 전에 자동 정지
    const checkTime = () => {
      if (audio.currentTime >= sentence.endTime - 0.3) {
        audio.pause();
        setPlayingSentenceIndex(null);
        audio.removeEventListener("timeupdate", checkTime);
      }
    };

    audio.addEventListener("timeupdate", checkTime);

    // 오디오가 끝나거나 에러 발생 시
    audio.onended = () => {
      setPlayingSentenceIndex(null);
    };

    audio.onerror = () => {
      alert("오디오 재생 중 오류가 발생했습니다.");
      setPlayingSentenceIndex(null);
    };
  };

  // 컴포넌트 언마운트 시 오디오 정리
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
    };
  }, []);

  const playAudio = (): void => {
    if (!currentAudioUrl || articleData.sentences.length === 0) {
      alert("오디오를 불러올 수 없습니다.");
      return;
    }

    // 오디오 객체 생성 또는 재사용
    if (!audioRef.current) {
      audioRef.current = new Audio(currentAudioUrl);
    } else if (audioRef.current.src !== currentAudioUrl) {
      audioRef.current.src = currentAudioUrl;
    }

    const audio = audioRef.current;

    // 이미 재생 중이면 일시정지
    if (isPlayingFullAudio) {
      audio.pause();
      setIsPlayingFullAudio(false);
      return;
    }

    // 첫 번째 문장의 시작 시간부터 재생
    const firstSentence = articleData.sentences[0];
    const lastSentence =
      articleData.sentences[articleData.sentences.length - 1];

    audio.currentTime = firstSentence.startTime;
    setIsPlayingFullAudio(true);

    audio.play().catch((err) => {
      console.error("Audio play failed:", err);
      alert("오디오 재생에 실패했습니다.");
      setIsPlayingFullAudio(false);
    });

    // 마지막 문장 종료 시간에 도달하면 자동 정지
    const checkTime = () => {
      if (audio.currentTime >= lastSentence.endTime - 0.3) {
        audio.pause();
        setIsPlayingFullAudio(false);
        audio.removeEventListener("timeupdate", checkTime);
      }
    };

    audio.addEventListener("timeupdate", checkTime);

    // 오디오 종료 시
    audio.onended = () => {
      setIsPlayingFullAudio(false);
    };
  };

  // 완료 처리
  const handleComplete = async () => {
    if (!isAuthenticated) {
      const confirmLogin = window.confirm(
        "로그인이 필요한 기능입니다.\n로그인 페이지로 이동하시겠습니까?",
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
      if (completed) {
        alert("미완료 처리 되었습니다");
      } else {
        alert("완료 처리 되었습니다.");
      }
      setCompleted(!completed);

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
        (v) => v.word.toLowerCase() === part.toLowerCase(),
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
                    vocab.partOfSpeech,
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
      allText.includes(vocab.word.toLowerCase()),
    );
  };

  const changeMonth = (delta: number): void => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + delta, 1),
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
        <Header
          darkMode={darkMode}
          setDarkMode={setDarkMode}
          isAuthenticated={isAuthenticated}
          isGuest={isGuest}
          userNickname={userNickname}
          goToLogin={goToLogin}
          handleLogout={handleLogout}
        />

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
                className={`transition-all duration-200 ${
                  darkMode ? "hover:bg-gray-700/30" : "hover:bg-gray-50"
                } rounded-lg p-4 -mx-4`}
              >
                <div className="flex items-start gap-3">
                  {/* 오디오 재생 버튼 */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      playSentenceAudio(index);
                    }}
                    className={`flex-shrink-0 p-2 rounded-full transition-all ${
                      playingSentenceIndex === index
                        ? darkMode
                          ? "bg-blue-600 text-white"
                          : "bg-blue-500 text-white"
                        : darkMode
                          ? "bg-gray-700 hover:bg-gray-600 text-gray-300"
                          : "bg-gray-100 hover:bg-gray-200 text-gray-600"
                    }`}
                    disabled={!currentAudioUrl}
                  >
                    {playingSentenceIndex === index ? (
                      <Pause className="w-4 h-4" />
                    ) : (
                      <Play className="w-4 h-4" />
                    )}
                  </button>

                  {/* 문장 텍스트 */}
                  <div
                    className="flex-1 cursor-pointer"
                    onClick={() =>
                      setExpandedSentences((prev) => ({
                        ...prev,
                        [index]: !prev[index],
                      }))
                    }
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
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-center pt-2">
            <button
              onClick={(e) => {
                e.stopPropagation();
                const allExpanded = articleData.sentences.every(
                  (_, i) => expandedSentences[i],
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
          <VocabularySection
            darkMode={darkMode}
            vocabularies={vocabularies}
            filteredVocabularies={filteredVocabularies}
          />
        </div>

        <div className="flex gap-4 mb-6">
          <button
            onClick={playAudio}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg font-medium transition-colors ${
              isPlayingFullAudio
                ? darkMode
                  ? "bg-orange-600 hover:bg-orange-700"
                  : "bg-orange-500 hover:bg-orange-600"
                : darkMode
                  ? "bg-blue-600 hover:bg-blue-700"
                  : "bg-blue-500 hover:bg-blue-600"
            } text-white`}
          >
            {isPlayingFullAudio ? (
              <Pause className="w-5 h-5" />
            ) : (
              <Volume2 className="w-5 h-5" />
            )}
            {isPlayingFullAudio ? "Pause Audio" : "Play Audio"}
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

        <CalendarSection
          darkMode={darkMode}
          currentMonth={currentMonth}
          currentDate={currentDate}
          availableDates={availableDates}
          completedDates={completedDates}
          changeMonth={changeMonth}
          handleDateClick={handleDateClick}
        />
      </div>
    </div>
  );
}
