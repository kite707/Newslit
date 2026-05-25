export const getShortPos = (pos: string): string => {
  const posMap: Record<string, string> = {
    명사: "n",
    동사: "v",
    형용사: "a",
    부사: "adv",
  };
  return posMap[pos] || pos;
};

export const getPosColor = (_pos: string, darkMode: boolean): string => {
  // Monochrome newspaper tag: uppercase, bordered, no color.
  return darkMode
    ? "border border-edge-dark text-muted-dark uppercase tracking-wider"
    : "border border-newsedge text-newsmuted uppercase tracking-wider";
};
