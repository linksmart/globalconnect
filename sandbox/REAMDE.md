## zmqproxy & zmqclient
Example/prototype of a ZeroMQ-based "backbone":

* zmqproxy is the "proxy" binding on XPUB & XSUB sockets
* zmqclient are the clients communicating through the proxy

Usage:

```
$ zmqproxy --help
Usage of zmqproxy:
  -xpub="tcp://*:6001": XPUB socket endpoint
  -xsub="tcp://*:6000": XSUB socket endpoint

$ zmqclient --help
Usage of zmqclient:
  -id="A": Client ID
  -pub="tcp://localhost:6000": PUB socket endpoint
  -sub="tcp://localhost:6001": SUB socket endpoint
```

Example:

```
$ zmqproxy
$ zmqclient --id A
$ zmqclient --id B
$ zmqclient --id C
```