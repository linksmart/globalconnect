# Building
```
docker build --rm -t lsgc/base base
docker build --rm -t lsgc/zmq-supernode zmq-supernode
```

# Running
```
docker run -d -p 7000:7000 -p 7001:7001 --name="lsgc-zmq-supernode" lsgc/zmq-supernode
```
