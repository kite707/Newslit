export const getShortPos = (pos: string): string => {
  const posMap: Record<string, string> = {
    명사: "명사",
    동사: "동사",
    형용사: "형용사",
    부사: "부사",
  };
  return posMap[pos] || pos;
};

export const getPosColor = (darkMode: boolean): string => {
  // Monochrome newspaper tag: uppercase, bordered, no color.
  return darkMode
    ? "border border-edge-dark text-muted-dark uppercase tracking-wider"
    : "border border-newsedge text-newsmuted uppercase tracking-wider";
};
