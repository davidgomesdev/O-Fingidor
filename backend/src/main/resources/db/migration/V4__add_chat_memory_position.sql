ALTER TABLE chat_memory
    ADD COLUMN IF NOT EXISTS position INTEGER;

-- ids are UUIDv7 generated in insertion order, so they recover the original message order
UPDATE chat_memory
SET position = ordered.position FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY conversation_id ORDER BY id) - 1 AS position
      FROM chat_memory) AS ordered
WHERE chat_memory.id = ordered.id;

ALTER TABLE chat_memory
    ALTER COLUMN position SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_memory_conversation_id_position ON chat_memory (conversation_id, position);
