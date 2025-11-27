export interface VocabularyItem {
  id: number;
  articleId: number;
  word: string;
  meaning: string;
  partOfSpeech: string;
  exampleSentence: string | null;
  exampleTranslation: string | null;
  createdAt: string;
}
