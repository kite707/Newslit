import http from "k6/http";
import { check } from "k6";
export const options = { vus: 250, duration: "30s" };

export default function () {
  if (Math.random() < 0.8) {
    http.get("http://localhost:8080/api/sentence?articleId=513");
  } else {
    const healthyRes = http.get("http://localhost:8080/api/article/514");

    check(healthyRes, {
      "정상 API는 번역 장애에 휩쓸리지 않고 200 반환": (r) => r.status === 200,
      "정상 API는 0.5초 이내 응답": (r) => r.timings.duration < 500,
    });
  }
}
