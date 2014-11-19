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
	id = flag.String("id", "A", "Client ID")
)

func publish(id string) {
	publisher, _ := zmq.NewSocket(zmq.PUB)
	publisher.Connect("tcp://localhost:6000")

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

func subscribe(id string) {
	subscriber, _ := zmq.NewSocket(zmq.SUB)
	subscriber.Connect("tcp://localhost:6001")
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

	go publish(*id)
	go subscribe(*id)

	for _ = range time.Tick(100) {
	}
}
