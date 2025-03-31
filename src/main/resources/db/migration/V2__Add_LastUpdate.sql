-- Create the `last_update` table if it does not exist
CREATE TABLE IF NOT EXISTS last_update (
    id VARCHAR(255) PRIMARY KEY UNIQUE,
    updated_time TIMESTAMP NOT NULL
);
