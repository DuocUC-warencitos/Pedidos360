#!/usr/bin/env bash
# Crea los topicos de Pedidos360 de forma idempotente.
# Se ejecuta dentro del contenedor kafka:
#   docker exec kafka bash /opt/kafka/init-topics.sh
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP_SERVERS:-localhost:9092}"
KAFKA_BIN="${KAFKA_BIN:-/opt/kafka/bin}"

create_topic() {
  local name="$1"
  local retention_ms="$2"

  echo "==> Topico $name (3 particiones, RF 3, min.insync.replicas=2)"
  "$KAFKA_BIN/kafka-topics.sh" \
    --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$name" \
    --partitions 3 \
    --replication-factor 3 \
    --config min.insync.replicas=2 \
    --config retention.ms="$retention_ms"

  # Asegura la configuracion si el topico ya existia con otros valores.
  "$KAFKA_BIN/kafka-configs.sh" \
    --bootstrap-server "$BOOTSTRAP" \
    --alter \
    --entity-type topics \
    --entity-name "$name" \
    --add-config "min.insync.replicas=2,retention.ms=$retention_ms"
}

# Eventos de dominio: productos y stock (consumidos por analytics-service). 30 dias.
create_topic "productos360.eventos.productos" 2592000000
create_topic "productos360.eventos.stock" 2592000000
# Auditoria: 7 dias. Metricas y DLT: 30 dias.
create_topic "productos360.eventos.auditoria" 604800000
create_topic "productos360.eventos.metricas" 2592000000
create_topic "productos360.eventos.auditoria.dlt" 2592000000
create_topic "productos360.eventos.metricas.dlt" 2592000000

echo "==> Estado del cluster"
"$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --describe
