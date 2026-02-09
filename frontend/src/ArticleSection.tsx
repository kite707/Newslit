import { useState } from "react";
import { ExternalLink, Play, Pause } from "lucide-react";
import type { DailyData, VocabularyItem } from "@/types";

interface ArticleSectionProps {
  darkMode: boolean;
  articleData: DailyData;
  vocabularies: VocabularyItem[];
  currentAudioUrl: string;
  playingSentenceIndex: number | null;
  playSentenceAudio: (index: number) => void;
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

export default function ArticleSection({
  darkMode,
  articleData,
  vocabularies,
  currentAudioUrl,
  playingSentenceIndex,
  playSentenceAudio,
}: ArticleSectionProps) {
  const [hoveredWordId, setHoveredWordId] = useState<string | null>(null);
  const [expandedSentences, setExpandedSentences] = useState<
    Record<number, boolean>
  >({});

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
                    darkMode,
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

  return (
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
  );
}
