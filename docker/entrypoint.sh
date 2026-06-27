#!/bin/sh
set -e

case "${MSGR_SERVICE}" in
  gateway) JAR="gateway.jar" ;;
  auth)    JAR="auth.jar" ;;
  user)    JAR="user.jar" ;;
  chat)    JAR="chat.jar" ;;
  file)    JAR="file.jar" ;;
  *)
    echo "Unknown MSGR_SERVICE: ${MSGR_SERVICE}. Expected: gateway, auth, user, chat, file."
    exit 1
    ;;
esac

exec java ${JAVA_OPTS:-} -jar "/app/${JAR}"