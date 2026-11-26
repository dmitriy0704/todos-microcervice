# Запуск теста с k6 из кластера

```shell
kubectl apply -f k6-job.yaml
kubectl logs -f job/k6-loadtest
kubectl delete job k6-loadtest
```


# Очистка postgres после теста

```shell
kubectl exec -it tasks-postgres-deployment-7b878bf5-zbfwp -- psql -U admin -d tasksdb -c "SELECT count(*) FROM tasks;"
kubectl exec -it tasks-postgres-deployment-7b878bf5-zbfwp -- psql -U admin -d tasksdb -c "TRUNCATE TABLE tasks RESTART IDENTITY CASCADE;"
```