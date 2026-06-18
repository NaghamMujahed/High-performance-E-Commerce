import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '5s', target: 100 },
    ],

    // thresholds: {
    //     http_req_failed: ['rate<0.01'],
    //     http_req_duration: ['avg<500', 'p(95)<800', 'p(99)<1500'],
    // },
};

export default function () {
    // const limit = 10;

    const res = http.post(`http://localhost:8080/products/754/buy-async?quantity=1&userId=1305`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has body': (r) => r.body.length > 2,
    });

    sleep(1);
}