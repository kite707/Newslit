import { useState } from "react";
import { ExternalLink, Play, Pause } from "lucide-react";
import type { DailyData, VocabularyItem } from "@/types";
import { getShortPos, getPosColor } from "@/utils/pos";

interface ArticleSectionProps {
  darkMode: boolean;
  articleData: DailyData;
  vocabularies: VocabularyItem[];
  currentAudioUrl: string;
  playingSentenceIndex: number | null;
  playSentenceAudio: (index: number) => void;
}

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

    const words = vocabularies
      .filter((v) => v.word)
      .map((v) => v.word.toLowerCase())
      .sort((a, b) => b.length - a.length)
      .map((word) => word.replace(/[.*+?^${}()|[\\\\]/g, "\\\\$&"));
    if (words.length === 0) return text;
    const regex = new RegExp(`\\b(${words.join("|")})\\b`, "gi");

    const parts = text.split(regex);
    let wordCounter = 0;

    return parts.map((part, index) => {
      const vocab = vocabularies.find(
        (v) => v.word && v.word.toLowerCase() === part.toLowerCase(),
      );

      if (vocab) {
        const uniqueId = `${sentenceIndex}-${wordCounter}`;
        wordCounter++;

        return (
          <span
            key={index}
            className={`relative inline-block cursor-pointer font-bold border-b-2 ${
              darkMode
                ? "text-text-dark border-muted-dark hover:border-text-dark"
                : "text-ink border-newsmuted hover:border-ink"
            }`}
            onMouseEnter={() => setHoveredWordId(uniqueId)}
            onMouseLeave={() => setHoveredWordId(null)}
          >
            {part}
            {hoveredWordId === uniqueId && (
              <span
                className={`absolute z-10 bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 shadow-lg whitespace-nowrap border ${
                  darkMode
                    ? "bg-card-dark text-text-dark border-edge-dark"
                    : "bg-white text-ink border-newsedge"
                }`}
              >
                <span className="block font-bold">{vocab.word}</span>
                <span
                  className={`text-xs px-2 py-0.5 ${getPosColor(
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
    <article
      className={`p-6 sm:p-8 mb-6 border ${
        darkMode
          ? "bg-card-dark border-edge-dark"
          : "bg-white border-newsedge"
      }`}
    >
      {/* Section eyebrow + source line */}
      <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
        <span className="news-eyebrow">
          {articleData.source} · Today's Edition
        </span>
        <span
          className={`text-xs font-semibold ${
            darkMode ? "text-muted-dark" : "text-newsmuted"
          }`}
        >
          Page {articleData.currentPages} of {articleData.totalPages}
        </span>
      </div>

      {/* Headline */}
      <h2 className="font-masthead text-3xl sm:text-4xl font-black leading-tight mb-3">
        {articleData.title}
      </h2>

      {/* Byline / dateline */}
      <div
        className={`flex items-center gap-4 flex-wrap text-sm italic pb-4 mb-6 ${
          darkMode
            ? "text-muted-dark border-b border-edge-dark"
            : "text-newsmuted border-b border-newsrule"
        }`}
      >
        <span>{articleData.publishedDate}</span>
        {articleData.sourceUrl && (
          <a
            href={articleData.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={`not-italic flex items-center gap-1 underline underline-offset-2 ${
              darkMode ? "hover:text-text-dark" : "hover:text-ink"
            }`}
          >
            Read original <ExternalLink className="w-3 h-3" />
          </a>
        )}
      </div>

      {/* Body */}
      <div className="space-y-5 mb-6">
        {articleData.sentences.map((sentence, index) => (
          <div key={index} className="flex items-start gap-3">
            {/* Audio button */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                playSentenceAudio(index);
              }}
              className={`flex-shrink-0 mt-1 p-1.5 border transition-all ${
                playingSentenceIndex === index
                  ? darkMode
                    ? "bg-text-dark text-ink border-text-dark"
                    : "bg-ink text-paper border-ink"
                  : darkMode
                    ? "border-edge-dark text-muted-dark hover:border-text-dark hover:text-text-dark"
                    : "border-newsedge text-newsmuted hover:border-ink hover:text-ink"
              } disabled:opacity-40`}
              disabled={!currentAudioUrl}
            >
              {playingSentenceIndex === index ? (
                <Pause className="w-3.5 h-3.5" />
              ) : (
                <Play className="w-3.5 h-3.5" />
              )}
            </button>

            {/* Sentence */}
            <div
              className="flex-1 cursor-pointer"
              onClick={() =>
                setExpandedSentences((prev) => ({
                  ...prev,
                  [index]: !prev[index],
                }))
              }
            >
              <p className="text-lg leading-[1.9]">
                {highlightVocabulary(sentence.englishText, index)}
              </p>
              <div
                className={`overflow-hidden transition-all duration-300 ${
                  expandedSentences[index]
                    ? "max-h-40 opacity-100 mt-2"
                    : "max-h-0 opacity-0"
                }`}
              >
                <p
                  className={`text-base italic pl-4 border-l-2 ${
                    darkMode
                      ? "text-muted-dark border-edge-dark"
                      : "text-newsbody border-newsrule"
                  }`}
                >
                  {sentence.koreanText}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div
        className={`flex items-center justify-center pt-4 border-t ${
          darkMode ? "border-edge-dark" : "border-newsrule"
        }`}
      >
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
          className={`text-xs font-semibold uppercase tracking-[1.5px] transition-colors ${
            darkMode
              ? "text-muted-dark hover:text-text-dark"
              : "text-newsmuted hover:text-ink"
          }`}
        >
          {articleData.sentences.every((_, i) => expandedSentences[i])
            ? "Hide all translations ▲"
            : "Show all translations ▼"}
        </button>
      </div>
    </article>
  );
}
