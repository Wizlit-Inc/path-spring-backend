-- Create the `point` table if it does not exist
CREATE TABLE IF NOT EXISTS point (
    id SERIAL PRIMARY KEY, -- Primary key with string ID
    title VARCHAR(255) NOT NULL UNIQUE, -- Title cannot be null
    description VARCHAR(255), -- Title cannot be null
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Automatically sets the current timestamp on insert
);

-- Create the `edge` table with a surrogate primary key
CREATE TABLE IF NOT EXISTS edge (
    id BIGSERIAL PRIMARY KEY,
    start_point BIGINT NOT NULL,
    end_point BIGINT NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_start FOREIGN KEY (start_point) REFERENCES point(id) ON DELETE CASCADE,
    CONSTRAINT fk_end FOREIGN KEY (end_point) REFERENCES point(id) ON DELETE CASCADE,
    CONSTRAINT unique_edge UNIQUE (start_point, end_point)
);
