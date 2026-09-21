import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

export const options = {
    scenarios: {
        payments: {
            executor: 'constant-arrival-rate',
            rate: 250,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 100,
            maxVUs: 500,
        },
    },
};

export default function () {
    const n = exec.scenario.iterationInTest;

    let senderId, receiverId;

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
        transactionId: `validation-${n}`,
        senderId,
        receiverId,
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
            timeout: '10s',
        }
    );

    check(response, {
        'HTTP status is 202': (r) => r.status === 202,
        'payment initiated': (r) =>
            r.body != null &&
            r.body.includes('Payment initiated successfully'),
    });
}
