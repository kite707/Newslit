export interface Vocabulary {
  id: number;
  word: string;
  partOfSpeech: string;
  meaning: string;
  exampleSentence?: string;
  exampleTranslation?: string;
}

export interface ArticleData {
  id?: number;
  displayDate: string;
  source: string;
  title: string;
  originalText: string;
  translatedText: string;
  vocabularies: Vocabulary[];
}

export interface ArticleDateRange {
  articleId: number;
  startIndex: number;
  endIndex: number;
  wordCount: number;
  displayDate: string;
}

export type AvailableDatesResponse = ArticleDateRange[];
