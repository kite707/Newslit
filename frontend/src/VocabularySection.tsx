import { useState } from "react";
import type { VocabularyItem } from "@/types";

interface VocabularySectionProps {
  darkMode: boolean;
  vocabularies: VocabularyItem[];
  filteredVocabularies: VocabularyItem[];
}

const getShortPos = (pos: string): string => {
  const posMap: Record<string, string> = {
    명사: "n",
    동사: "v",
    형용사: "a",
    부사: "adv",
  };
  return posMap[pos] || pos;
};

const getPosColor = (pos: string, darkMode: boolean): string => {
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

export default function VocabularySection({
  darkMode,
  vocabularies,
  filteredVocabularies,
}: VocabularySectionProps) {
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});

  return (
    <div>
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
                    item.partOfSpeech,
                    darkMode,
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
  );
}
