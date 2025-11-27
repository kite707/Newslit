import { useState, useEffect } from "react";
import {
  Volume2,
  Check,
  Moon,
  Sun,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
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
import type { ArticleData, VocabularyItem } from "@/types";

export default function EnglishLearningApp() {
  const [darkMode, setDarkMode] = useState(false);
  const [showTranslation, setShowTranslation] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});
  const [articleData, setArticleData] =
    useState<ArticleData>(DEFAULT_ARTICLE_DATA);
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [completedDates, setCompletedDates] = useState<number[]>([]);
  const [availableDates, setAvailableDates] = useState<number[]>([]);
  const [vocabularies, setVocabularies] = useState<VocabularyItem[]>([]);
  const [hoveredWord, setHoveredWord] = useState<string | null>(null);

  // 현재 날짜의 기사 불러오기
  useEffect(() => {
    const loadArticle = async () => {
      setLoading(true);
      const data = await fetchArticle(currentDate);
      setArticleData(data);
      console.log("Fetched article data:", data);

      // 기사가 있으면 단어 불러오기
      if (data.sentences.length > 0 && data.sentences[0]?.articleId) {
        const vocabData = await fetchVocabulary(data.sentences[0].articleId);
        setVocabularies(vocabData);
        console.log("Fetched vocabularies:", vocabData);
      }

      setLoading(false);
    };
    loadArticle();
  }, [currentDate]);

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
    const loadData = async () => {
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;

      const [completed, available] = await Promise.all([
        fetchReadingHistory(1, year, month),
        fetchAvailableDates(year, month),
      ]);

      setCompletedDates(completed);
      setAvailableDates(available);
    };

    loadData();
  }, [currentMonth]);

  const playAudio = (): void => {
    alert("음성 재생 기능은 백엔드 연동 후 사용 가능합니다.");
  };

  // 완료 처리
  const handleComplete = async () => {
    if (
      !articleData ||
      articleData.sentences.length === 0 ||
      !articleData.sentences[0]?.articleId
    ) {
      alert("기사 정보가 없습니다.");
      return;
    }

    try {
      await markArticleAsComplete(articleData.sentences[0].articleId);
      setCompleted(!completed);
      alert("완료 처리되었습니다!");

      // 히스토리 다시 불러오기
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      const updatedCompleted = await fetchReadingHistory(1, year, month);
      setCompletedDates(updatedCompleted);
    } catch (error) {
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

  // 텍스트에서 단어 하이라이트
  const highlightVocabulary = (text: string) => {
    if (vocabularies.length === 0) return text;

    const words = vocabularies.map((v) => v.word.toLowerCase());
    const regex = new RegExp(`\\b(${words.join("|")})\\b`, "gi");

    const parts = text.split(regex);

    return parts.map((part, index) => {
      const vocab = vocabularies.find(
        (v) => v.word.toLowerCase() === part.toLowerCase()
      );

      if (vocab) {
        return (
          <span
            key={index}
            className={`relative inline-block cursor-pointer font-semibold ${
              darkMode
                ? "text-yellow-300 hover:text-yellow-200"
                : "text-blue-600 hover:text-blue-700"
            } border-b-2 ${darkMode ? "border-yellow-300" : "border-blue-600"}`}
            onMouseEnter={() => setHoveredWord(vocab.word)}
            onMouseLeave={() => setHoveredWord(null)}
          >
            {part}
            {hoveredWord === vocab.word && (
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
          <div className="mb-4">
            <h2 className="text-2xl font-bold mb-2">{articleData.title}</h2>
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

          {/* 모든 문장 표시 */}
          <div className="space-y-4 mb-4">
            {articleData.sentences.map((sentence, index) => (
              <div key={index}>
                <p className="text-lg leading-relaxed">
                  {highlightVocabulary(sentence.englishText)}
                </p>
                {showTranslation && (
                  <p
                    className={`mt-2 text-base ${
                      darkMode ? "text-gray-300" : "text-gray-600"
                    }`}
                  >
                    {sentence.koreanText}
                  </p>
                )}
              </div>
            ))}
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
                const allShown = vocabularies.every(
                  (_, idx) => showMeaning[idx]
                );
                const newState: Record<number, boolean> = {};
                vocabularies.forEach((_, idx) => {
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
              {vocabularies.every((_, idx) => showMeaning[idx])
                ? "뜻 숨기기"
                : "뜻 보기"}
            </button>
          </div>

          <div className="space-y-4">
            {vocabularies.map((item, idx) => (
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