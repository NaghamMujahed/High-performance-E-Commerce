import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const PRODUCT_ID = '1';
const USER_ID = '1';
const QUANTITY = '1';
const REQUEST_TIMEOUT = '180s';

export const options = {
    stages: [
        { duration: '2s', target: 50 },
    ],

    // thresholds: {
    //     http_req_failed: ['rate<0.01'],
    //     http_req_duration: ['avg<500', 'p(95)<800', 'p(99)<1500'],
    // },
};

export default function () {
    // const limit = 10;

    const res = http.post(
        `${BASE_URL}/products/with-Virtual/${PRODUCT_ID}?quantity=${QUANTITY}`,
        null,
        { timeout: REQUEST_TIMEOUT },
    );

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has body': (r) => r.body.length > 2,
    });

    sleep(1);
}
