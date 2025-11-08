import React, { useState, useEffect } from "react";
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
  displayDate: string;
  source: string;
  title: string;
  originalText: string;
  translatedText: string;
  vocabularies: Vocabulary[];
}

export default function EnglishLearningApp() {
  const [darkMode, setDarkMode] = useState<boolean>(false);
  const [showTranslation, setShowTranslation] = useState<boolean>(false);
  const [completed, setCompleted] = useState<boolean>(false);
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date());
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});
  const [articleData, setArticleData] = useState<ArticleData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [currentDate, setCurrentDate] = useState<Date>(new Date());

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
          throw new Error("Failed to fetch article");
        }
        const data: ArticleData = await response.json();
        setArticleData(data);
        setError(null);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    fetchArticle();
  }, [currentDate]);

  const completedDates: number[] = [1, 3, 5, 7, 10, 12, 15, 18, 20, 23, 25, 28];

  const playAudio = (): void => {
    alert("음성 재생 기능은 백엔드 연동 후 사용 가능합니다.");
  };

  const handleComplete = (): void => {
    setCompleted(!completed);
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

  const changeDate = (day: number | null): void => {
    if (day === null) return;
    setCurrentDate(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day)
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading article...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600 mb-4">Error: {error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!articleData) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-600">No article data available</p>
      </div>
    );
  }

  return (
    <div
      className={`min-h-screen transition-colors duration-300 ${
        darkMode ? "bg-gray-900 text-white" : "bg-gray-50 text-gray-900"
      }`}
    >
      <header
        className={`sticky top-0 z-10 ${
          darkMode ? "bg-gray-800 border-gray-700" : "bg-white border-gray-200"
        } border-b`}
      >
        <div className="max-w-4xl mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold">🗞️ Newslit</h1>
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
      </header>

      <main className="max-w-4xl mx-auto px-4 py-8 space-y-8">
        {/* === Article Section === */}
        <section
          className={`rounded-xl shadow-lg overflow-hidden ${
            darkMode ? "bg-gray-800" : "bg-white"
          }`}
        >
          <div className="p-6 space-y-6">
            <div className="flex justify-between items-start">
              <div>
                <h2 className="text-xl font-bold mb-1 text-blue-600">
                  Today's Paragraph
                </h2>
                <p
                  className={`text-sm ${
                    darkMode ? "text-gray-400" : "text-gray-500"
                  }`}
                >
                  {new Date(articleData.displayDate).toLocaleDateString(
                    "en-US",
                    {
                      weekday: "long",
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                    }
                  )}
                </p>
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-medium ${
                  darkMode
                    ? "bg-blue-900 text-blue-200"
                    : "bg-blue-100 text-blue-800"
                }`}
              >
                {articleData.source}
              </span>
            </div>

            <div
              className={`border-l-4 ${
                darkMode
                  ? "border-yellow-500 bg-gray-750"
                  : "border-yellow-500 bg-yellow-50"
              } pl-4 py-2`}
            >
              <h3 className="text-2xl font-bold">{articleData.title}</h3>
            </div>

            <div
              className={`p-4 rounded-lg ${
                darkMode ? "bg-gray-700" : "bg-gray-50"
              }`}
            >
              <p className="text-lg leading-relaxed">
                {articleData.originalText}
              </p>
            </div>

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
                className={`p-4 rounded-lg border-l-4 ${
                  darkMode
                    ? "bg-gray-700 border-blue-500"
                    : "bg-blue-50 border-blue-400"
                }`}
              >
                <p className="leading-relaxed">{articleData.translatedText}</p>
              </div>
            )}

            {/* === Vocabulary Section === */}
            <div>
              <div className="flex justify-between items-center mb-3">
                <h3 className="font-semibold">Key Words</h3>
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

              <div className="grid gap-3">
                {articleData.vocabularies.map((item, idx) => (
                  <div
                    key={item.id}
                    className={`p-4 rounded-lg transition-all ${
                      darkMode ? "bg-gray-700" : "bg-gray-50"
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-lg">
                          {item.word}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded text-xs font-medium ${getPosColor(
                            item.partOfSpeech
                          )}`}
                        >
                          {getShortPos(item.partOfSpeech)}
                        </span>
                      </div>
                    </div>

                    {showMeaning[idx] && (
                      <div
                        className={`text-sm mb-2 ${
                          darkMode ? "text-gray-300" : "text-gray-600"
                        }`}
                      >
                        {item.meaning}
                      </div>
                    )}

                    {item.exampleSentence && showMeaning[idx] && (
                      <div
                        className={`mt-3 pt-3 border-t space-y-1 ${
                          darkMode ? "border-gray-600" : "border-gray-200"
                        }`}
                      >
                        <div
                          className={`text-sm italic ${
                            darkMode ? "text-gray-300" : "text-gray-600"
                          }`}
                        >
                          "{item.exampleSentence}"
                        </div>
                        {item.exampleTranslation && (
                          <div
                            className={`text-sm ${
                              darkMode ? "text-gray-400" : "text-gray-500"
                            }`}
                          >
                            {item.exampleTranslation}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* === Buttons === */}
            <div className="flex gap-3">
              <button
                onClick={playAudio}
                className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg font-medium transition-colors ${
                  darkMode
                    ? "bg-gray-700 hover:bg-gray-600"
                    : "bg-gray-100 hover:bg-gray-200"
                }`}
              >
                <Volume2 className="w-5 h-5" />
                Play Audio
              </button>
              <button
                onClick={handleComplete}
                className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg font-medium transition-colors ${
                  completed
                    ? darkMode
                      ? "bg-green-900 text-green-100"
                      : "bg-green-100 text-green-800"
                    : "bg-blue-600 hover:bg-blue-700 text-white"
                }`}
              >
                <Check className="w-5 h-5" />
                {completed ? "Completed!" : "Mark as Complete"}
              </button>
            </div>
          </div>
        </section>

        {/* === Calendar Section === */}
        <section
          className={`rounded-xl shadow-lg p-6 ${
            darkMode ? "bg-gray-800" : "bg-white"
          }`}
        >
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold">Study Record</h2>
            <div className="flex items-center gap-2">
              <button
                onClick={() => changeMonth(-1)}
                className={`p-2 rounded-lg transition-colors ${
                  darkMode ? "hover:bg-gray-700" : "hover:bg-gray-100"
                }`}
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <span className="font-medium min-w-[140px] text-center">
                {monthNames[currentMonth.getMonth()]}{" "}
                {currentMonth.getFullYear()}
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
          </div>

          <div className="grid grid-cols-7 gap-2">
            {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
              <div
                key={day}
                className={`text-center text-sm font-medium py-2 ${
                  darkMode ? "text-gray-400" : "text-gray-500"
                }`}
              >
                {day}
              </div>
            ))}

            {getDaysInMonth(currentMonth).map((day, idx) => (
              <button
                key={idx}
                onClick={() => changeDate(day)}
                className={`p-2 rounded-lg transition-colors ${
                  darkMode ? "hover:bg-gray-600" : "hover:bg-gray-100"
                }`}
              >
                <div
                  className={`aspect-square flex items-center justify-center rounded-lg text-sm ${
                    day === null
                      ? ""
                      : completedDates.includes(day)
                      ? darkMode
                        ? "bg-green-900 text-green-100 font-bold"
                        : "bg-green-100 text-green-800 font-bold"
                      : darkMode
                      ? "bg-gray-700 hover:bg-gray-600"
                      : "bg-gray-50 hover:bg-gray-100"
                  }`}
                >
                  {day}
                </div>
              </button>
            ))}
          </div>

          <div className="flex gap-4 mt-6 text-sm">
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-green-900" : "bg-green-100"
                }`}
              ></div>
              <span>Completed</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className={`w-4 h-4 rounded ${
                  darkMode ? "bg-gray-700" : "bg-gray-50"
                }`}
              ></div>
              <span>Not Yet</span>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
