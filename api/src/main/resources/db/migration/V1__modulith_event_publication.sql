-- Spring Modulith's event-publication registry (ADR 007). The spring-modulith
-- -starter-jpa maps a JpaEventPublication entity, and Hibernate runs with
-- ddl-auto: validate, so this table must exist. This is Modulith's canonical
-- PostgreSQL schema (schemas/v2/schema-postgresql.sql, Spring Modulith 2.1.1).
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
