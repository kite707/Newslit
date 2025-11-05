import React, { useEffect, useState } from "react";
import {
  Volume2,
  Check,
  Moon,
  Sun,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

export default function EnglishLearningApp() {
  const [darkMode, setDarkMode] = useState(false);
  const [showTranslation, setShowTranslation] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());

  // 샘플 데이터
  const todayParagraph = {
    text: "Climate change is one of the most pressing issues of our time. Scientists worldwide are working to understand its effects on our planet. Rising temperatures, melting ice caps, and extreme weather events are becoming more common. It is crucial that we take action now to protect our environment for future generations.",
    translation:
      "기후 변화는 우리 시대의 가장 시급한 문제 중 하나입니다. 전 세계의 과학자들은 지구에 미치는 영향을 이해하기 위해 노력하고 있습니다. 기온 상승, 빙하 융해, 극한 기상 현상이 점점 더 흔해지고 있습니다. 미래 세대를 위해 환경을 보호하기 위한 조치를 지금 취하는 것이 중요합니다.",
    words: [
      { word: "pressing", meaning: "긴급한, 시급한" },
      { word: "crucial", meaning: "결정적인, 중대한" },
      { word: "extreme", meaning: "극단적인, 극심한" },
    ],
    source: "VOA Learning English",
  };

  // 학습 완료한 날짜들 (샘플)
  const completedDates = [1, 3, 5, 7, 10, 12, 15, 18, 20, 23, 25, 28];

  const playAudio = () => {
    // TTS 기능은 백엔드 연동 후 구현
    alert("음성 재생 기능은 백엔드 연동 후 사용 가능합니다.");
  };

  const handleComplete = () => {
    setCompleted(!completed);
  };

  // 달력 생성 로직
  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days = [];
    // 빈 칸 추가
    for (let i = 0; i < firstDay; i++) {
      days.push(null);
    }
    // 날짜 추가
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    return days;
  };

  const monthNames = [
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

  const changeMonth = (delta: number) => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + delta, 1)
    );
  };

  return (
    <div
      className={`min-h-screen transition-colors duration-300 ${
        darkMode ? "bg-gray-900 text-white" : "bg-gray-50 text-gray-900"
      }`}
    >
      {/* Header */}
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
        {/* 오늘의 문단 카드 */}
        <section
          className={`rounded-xl shadow-lg overflow-hidden ${
            darkMode ? "bg-gray-800" : "bg-white"
          }`}
        >
          <div className="p-6 space-y-6">
            <div className="flex justify-between items-start">
              <div>
                <h2 className="text-2xl font-bold mb-1">Today's Paragraph</h2>
                <p
                  className={`text-sm ${
                    darkMode ? "text-gray-400" : "text-gray-500"
                  }`}
                >
                  {new Date().toLocaleDateString("en-US", {
                    weekday: "long",
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                </p>
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-medium ${
                  darkMode
                    ? "bg-blue-900 text-blue-200"
                    : "bg-blue-100 text-blue-800"
                }`}
              >
                {todayParagraph.source}
              </span>
            </div>

            {/* 영어 문단 */}
            <div
              className={`p-4 rounded-lg ${
                darkMode ? "bg-gray-700" : "bg-gray-50"
              }`}
            >
              <p className="text-lg leading-relaxed">{todayParagraph.text}</p>
            </div>

            {/* 번역 토글 */}
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
                <p className="leading-relaxed">{todayParagraph.translation}</p>
              </div>
            )}

            {/* 주요 단어 */}
            <div>
              <h3 className="font-semibold mb-3">Key Words</h3>
              <div className="grid gap-2">
                {todayParagraph.words.map((item, idx) => (
                  <div
                    key={idx}
                    className={`flex justify-between items-center p-3 rounded-lg ${
                      darkMode ? "bg-gray-700" : "bg-gray-50"
                    }`}
                  >
                    <span className="font-medium">{item.word}</span>
                    <span
                      className={darkMode ? "text-gray-400" : "text-gray-600"}
                    >
                      {item.meaning}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* 액션 버튼들 */}
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
                    : darkMode
                    ? "bg-blue-600 hover:bg-blue-700 text-white"
                    : "bg-blue-600 hover:bg-blue-700 text-white"
                }`}
              >
                <Check className="w-5 h-5" />
                {completed ? "Completed!" : "Mark as Complete"}
              </button>
            </div>
          </div>
        </section>

        {/* 학습 기록 달력 */}
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

          {/* 달력 그리드 */}
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
              <div
                key={idx}
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
            ))}
          </div>

          {/* 범례 */}
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
