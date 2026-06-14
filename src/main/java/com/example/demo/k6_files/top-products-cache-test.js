import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 20 },
        { duration: '30s', target: 100 },
        { duration: '60s', target: 100 },
        { duration: '20s', target: 0 },
    ],

    // thresholds: {
    //     http_req_failed: ['rate<0.01'],
    //     http_req_duration: ['avg<500', 'p(95)<800', 'p(99)<1500'],
    // },
};

export default function () {
    const limit = 10;

    const res = http.get(`http://localhost:8080/products/top-selling/by-cache?limit=${limit}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has body': (r) => r.body.length > 2,
    });

    sleep(1);
}