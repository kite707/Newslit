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

export type ArticleData = SentenceData[];

export interface ArticleDateRange {
  articleId: number;
  startIndex: number;
  endIndex: number;
  wordCount: number;
  displayDate: string;
}

export type AvailableDatesResponse = ArticleDateRange[];
