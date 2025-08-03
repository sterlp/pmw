#!/bin/bash
pip install --upgrade -r requirements.txt
mkdocs build
# Configuration
HOST="w0125542.kasserver.com"
USER="f0179a87"
# export LFTP_PASSWORD
PASSWORD=$(security find-generic-password -a "$USER" -s "$HOST" -w)
LOCAL_DIR="site"
REMOTE_DIR="."

# Upload with mirror (reverse mirror from local to remote)
lftp -u "$USER","$PASSWORD" "$HOST" <<EOF
mirror --reverse \
       --delete \
       --verbose \
       --parallel=8 \
       "$LOCAL_DIR" "$REMOTE_DIR"
quit
EOF