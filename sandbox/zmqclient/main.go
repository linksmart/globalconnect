package main

import (
	"bytes"
	"encoding/binary"
	"flag"
	"fmt"
	"math/rand"
	"time"

	"log"

	zmq "github.com/pebbe/zmq4"
	"linksmart.eu/globalconnect/sandbox/zmqbb"
)

var (
	id  = flag.String("id", "A", "Client ID")
	pub = flag.String("pub", "tcp://localhost:6000", "PUB socket endpoint")
	sub = flag.String("sub", "tcp://localhost:6001", "SUB socket endpoint")
)

func publish(id, endpoint string) {
	p, _ := zmq.NewSocket(zmq.PUB)
	p.Connect(endpoint)

	rand.Seed(time.Now().UnixNano())
	var msg zmqbb.Message
	count := 0
	for {
		msg.Topic = string(rand.Intn(3) + 'A')
		msg.Type = zmqbb.MessageTypeUnicast

		buf := new(bytes.Buffer)
		binary.Write(buf, binary.BigEndian, count)
		msg.Payload = buf.Bytes()

		if msg.Topic == id {
			msg.Topic = zmqbb.BroadcastTopic
			msg.Type = zmqbb.MessageTypeDiscovery
			msg.Payload = []byte{}
		}
		msg.Sender = id
		msg.Timestamp = time.Now().UnixNano()

		_, err := p.SendMessage(*msg.ToBytes())
		if err != nil {
			fmt.Println("Error writing message to publish socket: ", err.Error())
		}
		count++
		fmt.Printf("SENT: %v\n", msg)
		time.Sleep(3 * time.Second)
	}
}

func subscribe(id, endpoint string) {
	s, _ := zmq.NewSocket(zmq.SUB)
	s.Connect(endpoint)
	s.SetSubscribe(id)
	s.SetSubscribe(zmqbb.BroadcastTopic)
	defer s.Close()

	for {
		var msg zmqbb.Message

		pkt, err := s.RecvMessageBytes(0)
		err = msg.FromBytes(pkt)

		if err != nil {
			log.Println("Error reading from subscribe socket: ", err.Error())
		}
		switch msg.Type {
		case zmqbb.MessageTypeUnicast:
			fmt.Printf("RCVD UNICAST from %s: %v\n", msg.Sender, msg)
		case zmqbb.MessageTypeDiscovery:
			fmt.Printf("RCVD DISCOVERY from %s: %v\n", msg.Sender, msg)
		case zmqbb.MessageTypePeerDown:
			fmt.Printf("RCVD PEERDOWN for %s: %v\n", msg.Sender, msg)
		}
	}
}

func heartbeat(id, endpoint string) {
	p, _ := zmq.NewSocket(zmq.PUB)
	p.Connect(endpoint)

	ticker := time.NewTicker(zmqbb.HeartbeatInterval * time.Millisecond)

	for _ = range ticker.C {
		msg := zmqbb.Message{
			zmqbb.HeartbeatTopic,
			zmqbb.MessageTypeHeartbeat,
			time.Now().UnixNano(),
			id,
			[]byte{},
		}
		p.SendMessage(*msg.ToBytes())
	}
}

func main() {
	flag.Parse()

	go publish(*id, *pub)
	go subscribe(*id, *sub)
	go heartbeat(*id, *pub)

	for _ = range time.Tick(100) {
	}
}
