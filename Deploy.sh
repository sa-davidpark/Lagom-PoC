#!/bin/sh
clear
eval $(minikube docker-env)
kubectl delete deployment hello
kubectl delete deployment hello-stream
sbt clean docker:publishLocal
kubectl apply -k deploy/base
kubectl get pods