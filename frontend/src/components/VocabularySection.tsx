import { useState } from "react";
import type { VocabularyItem } from "@/types";
import { getShortPos, getPosColor } from "@/utils/pos";

interface VocabularySectionProps {
  darkMode: boolean;
  vocabularies: VocabularyItem[];
  filteredVocabularies: VocabularyItem[];
}

export default function VocabularySection({
  darkMode,
  vocabularies,
  filteredVocabularies,
}: VocabularySectionProps) {
  const [showMeaning, setShowMeaning] = useState<Record<number, boolean>>({});

  const allShown = filteredVocabularies.every((vocab) => {
    const idx = vocabularies.indexOf(vocab);
    return showMeaning[idx];
  });

  const isEmpty = filteredVocabularies.length === 0;

  return (
    <div>
      <div
        className={`flex justify-between items-end pb-2 mb-4 border-b ${
          darkMode ? "border-edge-dark" : "border-newsrule"
        }`}
      >
        <h2 className="font-masthead text-2xl font-black">Key Words</h2>
        {!isEmpty && (
          <button
            onClick={() => {
              const newState: Record<number, boolean> = {};
              filteredVocabularies.forEach((vocab) => {
                const idx = vocabularies.indexOf(vocab);
                newState[idx] = !allShown;
              });
              setShowMeaning(newState);
            }}
            className={`text-xs font-semibold uppercase tracking-[1.5px] transition-colors ${
              darkMode
                ? "text-muted-dark hover:text-text-dark"
                : "text-newsmuted hover:text-ink"
            }`}
          >
            {allShown ? "뜻 숨기기" : "뜻 보기"}
          </button>
        )}
      </div>

      {isEmpty && (
        <p
          className={`py-6 text-center text-sm italic ${
            darkMode ? "text-muted-dark" : "text-newsmuted"
          }`}
        >
          오늘의 핵심 단어가 없습니다.
        </p>
      )}

      <div className="grid sm:grid-cols-2 gap-px bg-transparent">
        {filteredVocabularies.map((item) => {
          const idx = vocabularies.indexOf(item);
          return (
            <div
              key={item.id}
              onClick={() =>
                setShowMeaning({ ...showMeaning, [idx]: !showMeaning[idx] })
              }
              className={`p-4 border cursor-pointer transition-colors ${
                darkMode
                  ? "border-edge-dark hover:bg-paper-dark"
                  : "border-newsedge hover:bg-paper"
              }`}
            >
              <div className="flex items-center gap-2 mb-1">
                <span className="font-bold text-lg">{item.word}</span>
                <span
                  className={`text-xs px-2 py-0.5 ${getPosColor(
                    item.partOfSpeech,
                    darkMode,
                  )}`}
                >
                  {getShortPos(item.partOfSpeech)}
                </span>
              </div>

              {showMeaning[idx] && (
                <p
                  className={`text-sm mb-2 ${
                    darkMode ? "text-text-dark" : "text-newsbody"
                  }`}
                >
                  {item.meaning}
                </p>
              )}

              {item.exampleSentence && showMeaning[idx] && (
                <div
                  className={`mt-2 pl-3 border-l-2 ${
                    darkMode ? "border-edge-dark" : "border-newsrule"
                  }`}
                >
                  <p className="text-sm italic mb-0.5">
                    "{item.exampleSentence}"
                  </p>
                  {item.exampleTranslation && (
                    <p
                      className={`text-xs ${
                        darkMode ? "text-muted-dark" : "text-newsmuted"
                      }`}
                    >
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
