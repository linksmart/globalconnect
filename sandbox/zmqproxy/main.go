package main

import (
	"flag"

	zmq "github.com/pebbe/zmq4"
)

var (
	pub = flag.String("xpub", "tcp://*:6001", "XPUB socket endpoint")
	sub = flag.String("xsub", "tcp://*:6000", "XSUB socket endpoint")
)

func main() {
	flag.Parse()

	subscriber, _ := zmq.NewSocket(zmq.XSUB)
	subscriber.Bind(*sub)

	publisher, _ := zmq.NewSocket(zmq.XPUB)
	publisher.Bind(*pub)
	zmq.Proxy(subscriber, publisher, nil)
}
