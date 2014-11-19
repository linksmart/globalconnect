package main

import zmq "github.com/pebbe/zmq4"

func main() {
	subscriber, _ := zmq.NewSocket(zmq.XSUB)
	subscriber.Bind("tcp://*:6000")

	publisher, _ := zmq.NewSocket(zmq.XPUB)
	publisher.Bind("tcp://*:6001")
	zmq.Proxy(subscriber, publisher, nil)
}
