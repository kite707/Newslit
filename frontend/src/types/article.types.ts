export interface Vocabulary {
  id: number;
  word: string;
  partOfSpeech: string;
  meaning: string;
  exampleSentence?: string;
  exampleTranslation?: string;
}

export interface SentenceData {
  articleId: number;
  orderIndex: number;
  englishText: string;
  koreanText: string;
  status: string;
}

export interface DailyData {
  dailyId: number;
  title: string;
  publishedDate: string;
  source: string;
  sourceUrl: string;
  totalPages: number;
  currentPages:number;
  sentences: SentenceData[];
}
export interface ArticleDateRange {
  articleId: number;
  startIndex: number;
  endIndex: number;
  wordCount: number;
  displayDate: string;
}

export type AvailableDatesResponse = ArticleDateRange[];
