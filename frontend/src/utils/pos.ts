export const getShortPos = (pos: string): string => {
  const posMap: Record<string, string> = {
    명사: "n",
    동사: "v",
    형용사: "a",
    부사: "adv",
  };
  return posMap[pos] || pos;
};

export const getPosColor = (pos: string, darkMode: boolean): string => {
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
