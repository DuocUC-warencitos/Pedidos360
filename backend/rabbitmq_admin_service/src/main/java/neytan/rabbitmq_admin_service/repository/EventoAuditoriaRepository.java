package neytan.rabbitmq_admin_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import neytan.rabbitmq_admin_service.model.EventoAuditoria;

public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long> {
}
