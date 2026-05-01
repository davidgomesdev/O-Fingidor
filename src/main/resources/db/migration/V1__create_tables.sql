CREATE TABLE personas
(
    id        SERIAL PRIMARY KEY,
    code_name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO personas (code_name)
VALUES ('o_fingidor'),
       ('fernando_pessoa'),
       ('alberto_caeiro'),
       ('alvaro_de_campos'),
       ('ricardo_reis'),
       ('bernardo_soares');

CREATE TABLE sessions
(
    conversation_id UUID PRIMARY KEY,
    persona_id      INTEGER NOT NULL REFERENCES personas (id)
);

CREATE TABLE chat_memory
(
    id              UUID PRIMARY KEY,
    conversation_id UUID                     NOT NULL REFERENCES sessions (conversation_id) ON DELETE CASCADE,
    message_json    TEXT                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_memory_conversation_id ON chat_memory (conversation_id);

CREATE TABLE chat_history
(
    id              UUID PRIMARY KEY,
    conversation_id UUID                     NOT NULL REFERENCES sessions (conversation_id) ON DELETE CASCADE,
    user_message    TEXT                     NOT NULL,
    ai_response     TEXT                     NOT NULL,
    sources_json    TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_history_conversation_id ON chat_history (conversation_id);
