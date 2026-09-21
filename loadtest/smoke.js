import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

export const options = {
    scenarios: {
        payments: {
            executor: 'constant-arrival-rate',
            rate: 250,
            timeUnit: '1s',
            duration: '4s',
            preAllocatedVUs: 50,
            maxVUs: 200,
        },
    },
};

export default function () {
    const n = exec.scenario.iterationInTest;

    let senderId;
    let receiverId;

    if (n % 3 === 0) {
        senderId = 1;
        receiverId = 2;
    } else if (n % 3 === 1) {
        senderId = 2;
        receiverId = 3;
    } else {
        senderId = 3;
        receiverId = 1;
    }

    const payload = JSON.stringify({
        transactionId: `smoke-${n}`,
        senderId: senderId,
        receiverId: receiverId,
        amount: 0.01,
        currency: 'INR',
    });

    const response = http.post(
        'http://host.docker.internal:8080/v1/payments',
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    check(response, {
        'HTTP status is 202': (r) => r.status === 202,
        'payment initiated': (r) =>
            r.body.includes('Payment initiated successfully'),
    });
}