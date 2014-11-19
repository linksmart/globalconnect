package main

import (
	"flag"
	"fmt"
	"math/rand"
	"strings"
	"time"

	zmq "github.com/pebbe/zmq4"
)

const (
	DiscoveryTopic = "aloha"
)

var (
	id  = flag.String("id", "A", "Client ID")
	pub = flag.String("pub", "tcp://localhost:6000", "PUB socket endpoint")
	sub = flag.String("sub", "tcp://localhost:6001", "SUB socket endpoint")
)

func publish(id, endpoint string) {
	publisher, _ := zmq.NewSocket(zmq.PUB)
	publisher.Connect(endpoint)

	rand.Seed(time.Now().UnixNano())
	count := 0
	var msg string
	for {
		msg = fmt.Sprintf("%c FROM %s: %d", rand.Intn(3)+'A', id, count)
		if strings.HasPrefix(msg, id) {
			msg = fmt.Sprintf("%s ALOHA %s", DiscoveryTopic, id)
		}
		_, err := publisher.SendMessage(msg)
		if err != nil {
			break
		}
		count++
		fmt.Printf("SENT (%d): %s\n", count, []string{msg})
		time.Sleep(3 * time.Second)
	}
}

func subscribe(id, endpoint string) {
	subscriber, _ := zmq.NewSocket(zmq.SUB)
	subscriber.Connect(endpoint)
	subscriber.SetSubscribe(id)
	subscriber.SetSubscribe(DiscoveryTopic)
	defer subscriber.Close()

	count := 0
	for {
		msg, err := subscriber.RecvMessage(0)
		if err != nil {
			break
		}
		count++
		fmt.Printf("RCVD (%d): %s\n", count, msg)
	}
}

func main() {
	flag.Parse()

	go publish(*id, *pub)
	go subscribe(*id, *sub)

	for _ = range time.Tick(100) {
	}
}
