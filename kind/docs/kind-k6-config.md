## 💡 Бонус: k6 как Job внутри kind — **лучше**, чем port-forward

В kind у вас есть **огромное преимущество** перед локальным экспериментом: вы
можете запустить **k6 как Job внутри кластера**. Это **убирает** `port-forward`
из уравнения (ту самую проблему, с которой вы столкнулись).

**`k6-job.yaml`:**

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: k6-loadtest
spec:
  template:
    spec:
      containers:
        - name: k6
          image: grafana/k6:latest
          command: [ "k6", "run", "/scripts/script.js" ]
          volumeMounts:
            - name: script
              mountPath: /scripts
      restartPolicy: Never
      volumes:
        - name: script
          configMap:
            name: k6-script
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: k6-script
data:
  script.js: |
    import http from 'k6/http';
    import { check, sleep } from 'k6';

    export const options = {
      stages: [
        { duration: '30s', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 0 },
      ],
    };

    export default function () {
      const res = http.post('http://tasks-service/tasks/create',
        JSON.stringify({ title: `task-${__VU}-${__ITER}` }),
        { headers: { 'Content-Type': 'application/json' } });
      check(res, { 'status is 201': (r) => r.status === 201 });
      sleep(1);
    }
```

```bash
kubectl apply -f k6-job.yaml
kubectl logs -f job/k6-loadtest
```
