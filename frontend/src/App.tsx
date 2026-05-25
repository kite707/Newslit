import { useState, useEffect, useRef } from "react";
import { Volume2, Check, Pause } from "lucide-react";
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
import AuthPage from "./components/AuthPage";
import CalendarSection from "./components/CalendarSection";
import VocabularySection from "./components/VocabularySection";
import Header from "./components/Header";
import ArticleSection from "./components/ArticleSection";

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


  const getFilteredVocabularies = () => {
    const allText = articleData.sentences
      .map((s) => s.englishText)
      .join(" ")
      .toLowerCase();

    return vocabularies.filter((vocab) =>
      vocab.word && allText.includes(vocab.word.toLowerCase()),
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
      <div className="min-h-screen flex items-center justify-center bg-paper">
        <div className="news-eyebrow">Loading…</div>
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
          darkMode ? "bg-paper-dark text-text-dark" : "bg-paper text-ink"
        }`}
      >
        <div className="news-eyebrow">Loading article…</div>
      </div>
    );
  }

  const filteredVocabularies = getFilteredVocabularies();

  return (
    <div
      className={`min-h-screen ${
        darkMode ? "bg-paper-dark text-text-dark" : "bg-paper text-ink"
      }`}
    >
      <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
        <Header
          darkMode={darkMode}
          setDarkMode={setDarkMode}
          isAuthenticated={isAuthenticated}
          isGuest={isGuest}
          userNickname={userNickname}
          goToLogin={goToLogin}
          handleLogout={handleLogout}
        />

        <ArticleSection
          darkMode={darkMode}
          articleData={articleData}
          vocabularies={vocabularies}
          currentAudioUrl={currentAudioUrl}
          playingSentenceIndex={playingSentenceIndex}
          playSentenceAudio={playSentenceAudio}
        />

        <div
          className={`p-6 sm:p-8 mb-6 border ${
            darkMode ? "bg-card-dark border-edge-dark" : "bg-white border-newsedge"
          }`}
        >
          <VocabularySection
            darkMode={darkMode}
            vocabularies={vocabularies}
            filteredVocabularies={filteredVocabularies}
          />
        </div>

        <div className="flex gap-3 mb-6">
          <button
            onClick={playAudio}
            className={`flex items-center justify-center gap-2 px-6 py-3 text-sm font-semibold uppercase tracking-[1.5px] transition-colors border ${
              darkMode
                ? "border-edge-dark text-text-dark hover:border-text-dark"
                : "border-ink text-ink hover:bg-ink hover:text-paper"
            }`}
          >
            {isPlayingFullAudio ? (
              <Pause className="w-4 h-4" />
            ) : (
              <Volume2 className="w-4 h-4" />
            )}
            {isPlayingFullAudio ? "Pause Audio" : "Play Audio"}
          </button>
          <button
            onClick={handleComplete}
            className={`flex-1 flex items-center justify-center gap-2 py-3 text-sm font-semibold uppercase tracking-[1.5px] transition-colors ${
              completed
                ? darkMode
                  ? "bg-accent-dark text-ink"
                  : "bg-accent text-white"
                : darkMode
                  ? "bg-text-dark text-ink hover:bg-white"
                  : "bg-ink text-paper hover:bg-black"
            }`}
          >
            <Check className="w-4 h-4" />
            {completed ? "Completed" : "Mark as Complete"}
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
