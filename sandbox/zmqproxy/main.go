package main

import (
	"flag"
	"log"
	"sync"
	"time"

	zmq "github.com/pebbe/zmq4"
	"linksmart.eu/globalconnect/sandbox/zmqbb"
)

var (
	xpub = flag.String("xpub", "tcp://*:6001", "XPUB socket endpoint")
	xsub = flag.String("xsub", "tcp://*:6000", "XSUB socket endpoint")
)

type Heartbeats struct {
	sync.RWMutex
	beats map[string]*time.Timer
}

func main() {
	flag.Parse()

	subSocket, _ := zmq.NewSocket(zmq.XSUB)
	subSocket.Bind(*xsub)

	pubSocket, _ := zmq.NewSocket(zmq.XPUB)
	pubSocket.Bind(*xpub)

	// publisher to announce peerdowns
	downCh := make(chan string)
	subEndpoint, _ := subSocket.GetLastEndpoint()
	go publisher(subEndpoint, downCh)

	// subscriber to listen for heartbeats
	pubEndpoint, _ := pubSocket.GetLastEndpoint()
	go subscriber(pubEndpoint, downCh)

	// start proxy
	zmq.Proxy(subSocket, pubSocket, nil)
}

func subscriber(endpoint string, downCh chan<- string) {
	heartbeats := Heartbeats{
		beats: make(map[string]*time.Timer),
	}

	s, _ := zmq.NewSocket(zmq.SUB)
	s.Connect(endpoint)
	s.SetSubscribe(zmqbb.HeartbeatTopic)
	defer s.Close()

	announceDown := func(id string) func() {
		return func() {
			log.Println("TIMEOUT heartbeat for peer ", id)
			downCh <- id

			heartbeats.Lock()
			delete(heartbeats.beats, id)
			log.Println("Current # of peers: ", len(heartbeats.beats))
			heartbeats.Unlock()
		}
	}

	for {
		var msg zmqbb.Message
		pkt, err := s.RecvMessageBytes(0)
		err = msg.FromBytes(pkt)
		if err != nil {
			log.Println("Error reading from the hearbeat monitor socket: ", err.Error())
		}
		heartbeats.RLock()
		timer, ok := heartbeats.beats[msg.Sender]

		if !ok {
			log.Println("FIRST heartbeat from peer ", msg.Sender)
			heartbeats.beats[msg.Sender] = time.AfterFunc(zmqbb.HeartbeatTimeout*time.Millisecond, announceDown(msg.Sender))
			log.Println("Current # of peers: ", len(heartbeats.beats))
		} else {
			// log.Println("UPDATE heartbeat from ", msg.Sender)
			timer.Reset(zmqbb.HeartbeatTimeout * time.Millisecond)
		}
		heartbeats.RUnlock()
	}
}

func publisher(endpoint string, downCh <-chan string) {
	p, _ := zmq.NewSocket(zmq.PUB)
	p.Connect(endpoint)

	for peerID := range downCh {
		log.Println("Announcing PEERDOWN for peer ", peerID)
		msg := zmqbb.Message{
			zmqbb.BroadcastTopic,
			zmqbb.MessageTypePeerDown,
			time.Now().UnixNano(),
			peerID,
			[]byte{},
		}
		p.SendMessage(*msg.ToBytes())
	}
}
