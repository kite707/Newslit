import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 0 },
  ],
};

export default function () {
  const res = http.get('http://localhost:8080/api/article/1');
  
  check(res, {
    'is status 200': (r) => r.status === 200,
    'transaction time OK': (r) => r.timings.duration < 500,
  });
  
  sleep(1);
}
