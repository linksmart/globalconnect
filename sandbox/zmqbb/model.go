package zmqbb

import (
	"bytes"
	"encoding/binary"
	"fmt"
)

const (
	HeartbeatTopic            = "HEARTBEAT"
	BroadcastTopic            = "BROADCAST"
	HeartbeatInterval         = 1000 // msec
	HeartbeatTimeout          = 2100 // msec
	MessageTypeUnicast   byte = 0x01
	MessageTypeDiscovery byte = 0x02
	MessageTypeHeartbeat byte = 0x03
	MessageTypePeerDown  byte = 0x04
)

// Message exchanged on ZMQ sockets
type Message struct {
	Topic     string
	Type      byte
	Timestamp int64
	Sender    string
	Payload   []byte
}

// Reads Message from a [][]byte ZMQ multipart
func (m *Message) FromBytes(pkt [][]byte) error {
	if len(pkt) < 4 {
		return fmt.Errorf("Invalid packet")
	}

	var timestamp int64
	buf := bytes.NewBuffer(pkt[2])
	binary.Read(buf, binary.BigEndian, &timestamp)

	m.Topic = string(pkt[0])
	m.Type = pkt[1][0]
	m.Timestamp = timestamp
	m.Sender = string(pkt[3])

	if len(pkt) == 5 {
		m.Payload = pkt[4]
	}
	return nil
}

// Converts Message into a [][]byte ZMQ multipart
func (m *Message) ToBytes() *[][]byte {
	tsBuf := new(bytes.Buffer)
	binary.Write(tsBuf, binary.BigEndian, m.Timestamp)

	return &[][]byte{
		[]byte(m.Topic),
		[]byte{m.Type},
		tsBuf.Bytes(),
		[]byte(m.Sender),
		m.Payload,
	}
}
