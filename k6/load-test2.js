import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '30s', target: 400 },
    { duration: '10s', target: 0 },
  ],
};

export default function () {
  const res = http.get('http://localhost:8080/api/article/1');
  
  check(res, {
    'is status 200': (r) => r.status === 200,
    'transaction time OK': (r) => r.timings.duration < 500, 
  });
  
  sleep(0.05); 
}
