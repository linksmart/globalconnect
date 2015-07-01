#!/bin/bash

# 1: parent
echo "releasing linksmart-gc-parent..."
cd linksmart-gc-parent && mvn clean deploy

# 2: component
cd linksmart-gc-osgi-component && mvn clean deploy
echo "releasing linksmart-gc-osgi-component..."

# 3: apis
cd gc-apis && mvn clean deploy
echo "releasing gc-apis..."

# 4: it-utils
cd deployment/it-utils/ && mvn clean deploy
echo "releasing deployment/it-utils..."

# 5: features
cd deployment/gc-features/ && mvn clean deploy
echo "releasing deployment/gc-features..."

# 6: sc-client-api
cd gc-lc-bridge/gc-sc-api/ && mvn clean deploy
echo "releasing gc-lc-bridge/gc-sc-api..."

# 7: gc-sc-client
cd gc-lc-bridge/gc-sc-client/ && mvn clean deploy
echo "releasing gc-lc-bridge/gc-sc-client..."

## the rest
# 8: backbone-http-protocol
cd backbone-http-protocol && mvn clean deploy
echo "releasing backbone-http-protocol..."

# 8: backbone-zmq
cd backbone-zmq && mvn clean deploy
echo "releasing backbone-zmq..."

# 9: gc-backbone-router
cd gc-backbone-router && mvn clean deploy
echo "releasing gc-backbone-router..."

# 10: gc-identity-manager
cd gc-identity-manager && mvn clean deploy
echo "releasing gc-identity-manager..."

# 11: gc-lc-bridge
cd gc-lc-bridge && mvn clean deploy
echo "releasing gc-lc-bridge..."

# 12: gc-network-manager
cd gc-network-manager && mvn clean deploy
echo "releasing gc-network-manager..."

# 13: http-tunneling
cd http-tunneling && mvn clean deploy
echo "releasing http-tunneling..."

# 14: network-manager-rest
cd network-manager-rest && mvn clean deploy
echo "releasing network-manager-rest..."