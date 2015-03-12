# Environment
  
  Scripts tested with Docker 1.3.2.
  Docker 1.0.1 wont work.

# Building
```
docker build --rm -t lsgc/base lsgc-base

docker build --rm -t lsgc/zmq-supernode zmq-supernode
```

# Running
```
docker run -d -p 7000:7000 -p 7001:7001 --name="lsgc-zmq-supernode" lsgc/zmq-supernode
```
