# Запуск теста с k6 из кластера

```shell
kubectl apply -f k6-job.yaml
kubectl logs -f job/k6-loadtest
kubectl delete job k6-loadtest
```