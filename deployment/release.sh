#!/bin/bash

# before running the script, change the version, e.g:
# find . -name pom.xml -type f -print0 | xargs -0 sed -i '' 's|0.2.0-SNAPSHOT|0.1.0|g'
# and check manually with git diff 

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR=$DIR/..

# 1: parent
echo "releasing linksmart-gc-parent..."
cd ${ROOT_DIR}/linksmart-gc-parent && mvn clean deploy

# 2: component
echo "releasing linksmart-gc-osgi-component..."
cd ${ROOT_DIR}/linksmart-gc-osgi-component && mvn clean deploy

# 3: apis
echo "releasing gc-apis..."
cd ${ROOT_DIR}/gc-apis && mvn clean deploy

# 4: it-utils
echo "releasing deployment/it-utils..."
cd ${ROOT_DIR}/deployment/it-utils/ && mvn clean deploy

# 5: features
echo "releasing deployment/features..."
cd ${ROOT_DIR}/deployment/features/ && mvn clean deploy

# 6: sc-client-api
echo "releasing gc-lc-bridge/gc-sc-api..."
cd ${ROOT_DIR}/gc-lc-bridge/gc-sc-api/ && mvn clean deploy

# 7: gc-sc-client
echo "releasing gc-lc-bridge/gc-sc-client..."
cd ${ROOT_DIR}/gc-lc-bridge/gc-sc-client/ && mvn clean deploy

## the rest
# 8: backbone-http-protocol
echo "releasing backbone-http-protocol..."
cd ${ROOT_DIR}/backbone-http-protocol && mvn clean deploy

# 8: backbone-zmq
echo "releasing backbone-zmq..."
cd ${ROOT_DIR}/backbone-zmq && mvn clean deploy

# 9: gc-backbone-router
echo "releasing gc-backbone-router..."
cd ${ROOT_DIR}/gc-backbone-router && mvn clean deploy

# 10: gc-identity-manager
echo "releasing gc-identity-manager..."
cd ${ROOT_DIR}/gc-identity-manager && mvn clean deploy

# 11: gc-lc-bridge
echo "releasing gc-lc-bridge..."
cd ${ROOT_DIR}/gc-lc-bridge && mvn clean deploy

# 12: gc-network-manager
echo "releasing gc-network-manager..."
cd ${ROOT_DIR}/gc-network-manager && mvn clean deploy

# 13: http-tunneling
echo "releasing http-tunneling..."
cd ${ROOT_DIR}/http-tunneling && mvn clean deploy

# 14: network-manager-rest
echo "releasing network-manager-rest..."
cd ${ROOT_DIR}/network-manager-rest && mvn clean deploy

# finally, deployment/distribution
echo "releasing network-manager-rest..."
cd ${ROOT_DIR}/deployment/distribution && mvn clean deploy