#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="./"
PASS="changeit"
CN_SERVER="localhost"   # this is the tlsAuthName in tests

mkdir -p "${OUT_DIR}"
cd "${OUT_DIR}"

echo "==> Generating test CA..."
# CA private key (RSA 2048 for simplicity; ECDSA also fine)
openssl genrsa -out ca.key 2048

# Self-signed CA cert (10 years)
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
  -subj "/C=XX/ST=Test/L=Test/O=TestCA/OU=Test/CN=Test CA" \
  -out ca.pem \
  -addext "basicConstraints=CA:true" \
  -addext "keyUsage=keyCertSign,cRLSign" \
  -addext "subjectKeyIdentifier=hash"

# Optional: CA as PKCS#12 (rarely needed)
openssl pkcs12 -export -name testca -inkey ca.key -in ca.pem \
  -passout pass:${PASS} -out ca.p12

echo "==> Generating server key and CSR..."
openssl genrsa -out server.key 2048

# CSR with CN=localhost (SAN will be set via ext file on signing)
openssl req -new -key server.key \
  -subj "/C=XX/ST=Test/L=Test/O=TestServer/OU=Test/CN=${CN_SERVER}" \
  -out server.csr

# Extensions for server cert: SAN, KU/EKU
cat > server-ext.cnf <<EOF
subjectAltName = DNS:${CN_SERVER}
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
EOF

echo "==> Signing server certificate with the CA..."
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out server.crt -days 825 -sha256 -extfile server-ext.cnf

# Full chain (server + CA) for debugging / some servers
cat server.crt ca.pem > server-fullchain.crt

echo "==> Building server PKCS#12 keystore (for the DoT server)..."
# Includes private key + server cert + CA chain; alias 'server'
openssl pkcs12 -export \
  -name server \
  -inkey server.key \
  -in server.crt \
  -certfile ca.pem \
  -out server.p12 \
  -passout pass:${PASS}

echo "==> Building client PKCS#12 truststore (CA only, for the client tests)..."
# Requires keytool (from the JDK)
rm -f truststore.p12 || true
keytool -importcert -noprompt \
  -alias testca \
  -file ca.pem \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass "${PASS}"

echo "==> Verifying..."
openssl x509 -in server.crt -noout -text | grep -E "Subject:|X509v3 Subject Alternative Name|Extended Key Usage|Key Usage" || true
openssl verify -CAfile ca.pem server.crt

echo "==> Done. Files created in ${OUT_DIR}:"
ls -1

