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

export interface AvailableDatesResponse {
  dates: number[];
}
