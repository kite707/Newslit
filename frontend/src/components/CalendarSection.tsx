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
    <section
      className={`p-6 sm:p-8 border ${
        darkMode ? "bg-card-dark border-edge-dark" : "bg-white border-newsedge"
      }`}
    >
      <div
        className={`flex justify-between items-end pb-2 mb-4 border-b ${
          darkMode ? "border-edge-dark" : "border-newsrule"
        }`}
      >
        <h2 className="font-masthead text-2xl font-black">Study Record</h2>
      </div>

      <div className="flex justify-between items-center mb-4">
        <button
          onClick={() => changeMonth(-1)}
          className={`p-2 border transition-colors ${
            darkMode
              ? "border-edge-dark hover:border-text-dark"
              : "border-newsedge hover:border-ink"
          }`}
        >
          <ChevronLeft className="w-4 h-4" />
        </button>
        <span className="font-masthead text-lg font-bold uppercase tracking-[1px]">
          {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
        </span>
        <button
          onClick={() => changeMonth(1)}
          className={`p-2 border transition-colors ${
            darkMode
              ? "border-edge-dark hover:border-text-dark"
              : "border-newsedge hover:border-ink"
          }`}
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1">
        {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
          <div
            key={day}
            className={`text-center text-[10px] font-semibold uppercase tracking-[1px] p-2 ${
              darkMode ? "text-muted-dark" : "text-newsmuted"
            }`}
          >
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
              className={`${
                hasArticle ? "cursor-pointer" : "cursor-not-allowed opacity-30"
              }`}
            >
              <div
                className={`text-center text-sm relative p-2 border transition-all ${
                  isCurrentSelected
                    ? darkMode
                      ? "bg-accent-dark text-ink border-accent-dark font-bold"
                      : "bg-accent text-white border-accent font-bold"
                    : isCompleted
                      ? darkMode
                        ? "border-transparent text-text-dark font-bold"
                        : "border-transparent text-ink font-bold"
                      : hasArticle
                        ? darkMode
                          ? "border-transparent hover:bg-edge-dark/40"
                          : "border-transparent hover:bg-newsedge/50"
                        : "border-transparent"
                }`}
              >
                {day}
                {day && isCompleted && !isCurrentSelected && (
                  <div
                    className={`absolute top-0.5 right-0.5 text-[9px] leading-none ${
                      darkMode ? "text-text-dark" : "text-ink"
                    }`}
                  >
                    ✓
                  </div>
                )}
                {day && !isCompleted && !isCurrentSelected && hasArticle && (
                  <div
                    className={`absolute bottom-0.5 left-1/2 transform -translate-x-1/2 w-1 h-1 ${
                      darkMode ? "bg-muted-dark" : "bg-newsmuted"
                    }`}
                  ></div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div
        className={`flex gap-5 mt-5 pt-4 text-xs border-t ${
          darkMode
            ? "border-edge-dark text-muted-dark"
            : "border-newsrule text-newsmuted"
        }`}
      >
        <div className="flex items-center gap-1.5">
          <span className={darkMode ? "text-text-dark" : "text-ink"}>✓</span>
          <span>Completed</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div
            className={`w-3 h-3 ${
              darkMode ? "bg-accent-dark" : "bg-accent"
            }`}
          ></div>
          <span>Selected</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div
            className={`w-1 h-1 ${
              darkMode ? "bg-muted-dark" : "bg-newsmuted"
            }`}
          ></div>
          <span>Available</span>
        </div>
      </div>
    </section>
  );
}
