import  { useState, useEffect } from "react";
import {
  Volume2,
  Check,
  Moon,
  Sun,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

interface Vocabulary {
  id: number;
  word: string;
  partOfSpeech: string;
  meaning: string;
  exampleSentence?: string;
  exampleTranslation?: string;
}

interface ArticleData {
  id?: number;
  displayDate: string;
  source: string;
  title: string;
  originalText: string;
  translatedText: string;
  vocabularies: Vocabulary[];
}

interface HistoryData {
  id: number;
  userId: number;
  articleId: number;
  readDate: string;
  createdAt: string;
}

interface AvailableDatesResponse {
  dates: number[];
}

const defaultArticleData: ArticleData = {
  id: 0,
  displayDate: new Date().toISOString(),
  source: "Newslit",
  title: "No Article Available Today",
  originalText:
    "There is no article available for today. Newslit provides a daily paragraph to help you learn English. Please check back tomorrow!",
  translatedText:
    "오늘은 제공되는 기사가 없습니다. Newslit은 매일 한 단락의 영어 문장을 제공하여 영어 학습을 돕습니다. 내일 다시 확인해주세요!",
  vocabularies: [
    {
      id: 1,
      word: "available",
      partOfSpeech: "형용사",
      meaning: "이용 가능한, 제공되는",
      exampleSentence: "There is no article available for today.",
      exampleTranslation: "오늘 제공되는 기사가 없습니다.",
    },
  ],
};

export default function EnglishLearningApp() {
  const [darkMode, setDarkMode] = useState(false);
  const [showTranslation, setShowTranslation] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});
  const [articleData, setArticleData] =
    useState<ArticleData>(defaultArticleData);
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [completedDates, setCompletedDates] = useState<number[]>([]);
  const [availableDates, setAvailableDates] = useState<number[]>([]);

  useEffect(() => {
    const fetchArticle = async () => {
      try {
        setLoading(true);
        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, "0");
        const day = String(currentDate.getDate()).padStart(2, "0");
        const dateString = `${year}${month}${day}`;

        const response = await fetch(
          `http://localhost:8080/api/article?date=${dateString}`
        );

        if (!response.ok) {
          setArticleData(defaultArticleData);
        } else {
          const data: ArticleData = await response.json();
          setArticleData(data);
        }
      } catch (err) {
        console.warn("No article available, using default data.");
        setArticleData(defaultArticleData);
      } finally {
        setLoading(false);
      }
    };

    fetchArticle();
  }, [currentDate]);

  const handleDateClick = async (day: number | null) => {
    if (day === null) return;

    // 기사가 없는 날짜는 클릭 불가
    if (!availableDates.includes(day)) return;

    try {
      const selectedDate = new Date(
        currentMonth.getFullYear(),
        currentMonth.getMonth(),
        day
      );
      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
      const date = String(selectedDate.getDate()).padStart(2, "0");
      const dateString = `${year}${month}${date}`;

      const response = await fetch(
        `http://localhost:8080/api/article?date=${dateString}`
      );

      if (!response.ok) {
        setArticleData(defaultArticleData);
      } else {
        const data: ArticleData = await response.json();
        setArticleData(data);
      }

      setCurrentDate(selectedDate);
    } catch (err) {
      console.warn("Failed to fetch article for selected date");
      setArticleData(defaultArticleData);
    }
  };

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        const year = currentMonth.getFullYear();
        const month = String(currentMonth.getMonth() + 1).padStart(2, "0");
        const dateString = `${year}${month}`;

        const response = await fetch(
          `http://localhost:8080/api/reading-history?userId=1&date=${dateString}`
        );

        if (response.ok) {
          const data = await response.json();
          setCompletedDates(
            data.histories.map((h: HistoryData) =>
              new Date(h.readDate).getDate()
            )
          );
        }
      } catch (err) {
        console.warn("Failed to fetch reading history");
      }
    };

    const fetchAvailableDates = async () => {
      try {
        const year = currentMonth.getFullYear();
        const month = String(currentMonth.getMonth() + 1).padStart(2, "0");
        const dateString = `${year}${month}`;

        const response = await fetch(
          `http://localhost:8080/api/article/available?date=${dateString}`
        );

        if (response.ok) {
          const data: AvailableDatesResponse = await response.json();
          setAvailableDates(data.dates || []);
        } else {
          setAvailableDates([]);
        }
      } catch (err) {
        console.warn("Failed to fetch available dates");
        setAvailableDates([]);
      }
    };

    fetchHistory();
    fetchAvailableDates();
  }, [currentMonth]);

  const playAudio = (): void => {
    alert("음성 재생 기능은 백엔드 연동 후 사용 가능합니다.");
  };

  const handleComplete = async (): Promise<void> => {
    if (!articleData?.id) {
      alert("기사 정보가 없습니다.");
      return;
    }

    try {
      // userId를 쿠키에 설정
      document.cookie = "userId=1; path=/";

      const response = await fetch(
        `http://localhost:8080/api/reading-history?articleId=${articleData.id}`,
        {
          method: "POST",
          credentials: "include", // 쿠키를 포함하여 요청
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      if (response.ok) {
        setCompleted(!completed);
        alert("완료 처리되었습니다!");

        // 성공 시 완료 날짜 목록 다시 불러오기
        const year = currentMonth.getFullYear();
        const month = String(currentMonth.getMonth() + 1).padStart(2, "0");
        const dateString = `${year}${month}`;

        const historyResponse = await fetch(
          `http://localhost:8080/api/reading-history?userId=1&date=${dateString}`
        );

        if (historyResponse.ok) {
          const data = await historyResponse.json();
          if (Array.isArray(data)) {
            setCompletedDates(
              data.map((h: HistoryData) => new Date(h.readDate).getDate())
            );
          }
        }
      } else {
        const errorData = await response
          .json()
          .catch(() => ({ message: "알 수 없는 오류" }));
        console.error("Error response:", errorData);
        alert(
          `완료 처리에 실패했습니다.\nStatus: ${response.status}\nMessage: ${
            errorData.message || response.statusText
          }`
        );
      }
    } catch (err) {
      console.error("Failed to mark as complete:", err);
      alert(
        `완료 처리 중 오류가 발생했습니다.\n${
          err instanceof Error ? err.message : String(err)
        }`
      );
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

  return (
    <div
      className={`min-h-screen ${
        darkMode ? "bg-gray-900 text-white" : "bg-gray-50 text-gray-900"
      }`}
    >
      <div className="max-w-4xl mx-auto p-6">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold">🗞️ Newslit</h1>
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

        <div
          className={`rounded-xl p-6 mb-6 ${
            darkMode ? "bg-gray-800" : "bg-white"
          } shadow-lg`}
        >
          <h2 className="text-2xl font-bold mb-4">Today's Paragraph</h2>
          <div className="text-sm text-gray-500 mb-4">
            {new Date(articleData.displayDate).toLocaleDateString("en-US", {
              weekday: "long",
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </div>

          <div
            className={`inline-block px-3 py-1 rounded-full text-xs font-medium mb-4 ${
              darkMode
                ? "bg-blue-900 text-blue-200"
                : "bg-blue-100 text-blue-700"
            }`}
          >
            {articleData.source}
          </div>

          <h3 className="text-xl font-semibold mb-4">{articleData.title}</h3>
          <p className="text-lg leading-relaxed mb-4">
            {articleData.originalText}
          </p>

          <button
            onClick={() => setShowTranslation(!showTranslation)}
            className={`text-sm font-medium ${
              darkMode
                ? "text-blue-400 hover:text-blue-300"
                : "text-blue-600 hover:text-blue-700"
            }`}
          >
            {showTranslation ? "번역 숨기기 ▲" : "번역 보기 ▼"}
          </button>

          {showTranslation && (
            <div
              className={`mt-4 p-4 rounded-lg ${
                darkMode ? "bg-gray-700" : "bg-gray-50"
              }`}
            >
              {articleData.translatedText}
            </div>
          )}
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
                const allShown = articleData.vocabularies.every(
                  (_, idx) => showMeaning[idx]
                );
                const newState: Record<number, boolean> = {};
                articleData.vocabularies.forEach((_, idx) => {
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
              {articleData.vocabularies.every((_, idx) => showMeaning[idx])
                ? "뜻 숨기기"
                : "뜻 보기"}
            </button>
          </div>

          <div className="space-y-4">
            {articleData.vocabularies.map((item, idx) => (
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
            ))}
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

              return (
                <div
                  key={idx}
                  onClick={() => handleDateClick(day)}
                  className={`p-2 rounded-lg transition-colors ${
                    hasArticle
                      ? darkMode
                        ? "hover:bg-gray-600 cursor-pointer"
                        : "hover:bg-gray-100 cursor-pointer"
                      : "cursor-not-allowed opacity-50"
                  }`}
                >
                  <div
                    className={`text-center text-sm relative ${
                      isCompleted
                        ? darkMode
                          ? "bg-green-600 text-white"
                          : "bg-green-500 text-white"
                        : ""
                    } rounded p-1`}
                  >
                    {day}
                    {day && !isCompleted && hasArticle && (
                      <div
                        className={`absolute bottom-0 left-1/2 transform -translate-x-1/2 w-1.5 h-1.5 rounded-full ${
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
                  darkMode ? "bg-blue-400" : "bg-blue-500"
                } relative flex items-end justify-center`}
              >
                <div className="w-1.5 h-1.5 rounded-full bg-white mb-0.5"></div>
              </div>
              <span>Article Available</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-gray-700" : "bg-gray-200"
                }`}
              ></div>
              <span>No Article</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
