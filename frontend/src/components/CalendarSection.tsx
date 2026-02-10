import { ChevronLeft, ChevronRight } from "lucide-react";

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

// props 타입 정의 (백엔드의 DTO라고 생각하면 됨)
interface CalendarSectionProps {
  darkMode: boolean;
  currentMonth: Date;
  currentDate: Date;
  availableDates: number[];
  completedDates: number[];
  changeMonth: (delta: number) => void;
  handleDateClick: (day: number | null) => void;
}

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

export default function CalendarSection({
  darkMode,
  currentMonth,
  currentDate,
  availableDates,
  completedDates,
  changeMonth,
  handleDateClick,
}: CalendarSectionProps) {
  return (
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
                {day && !isCompleted && !isCurrentSelected && hasArticle && (
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
  );
}
