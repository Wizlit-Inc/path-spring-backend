-- Create the `point` table if it does not exist
CREATE TABLE IF NOT EXISTS point (
    id SERIAL PRIMARY KEY, -- Primary key with string ID
    title VARCHAR(255) NOT NULL UNIQUE, -- Title cannot be null
    objective VARCHAR(255), -- Title cannot be null
    document VARCHAR(255), -- Title cannot be null
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Automatically sets the current timestamp on insert
);

-- Create the `edge` table with a surrogate primary key
CREATE TABLE IF NOT EXISTS edge (
    id BIGSERIAL PRIMARY KEY,
    origin_point BIGINT NOT NULL,
    destination_point BIGINT NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_start FOREIGN KEY (origin_point) REFERENCES point(id) ON DELETE CASCADE,
    CONSTRAINT fk_end FOREIGN KEY (destination_point) REFERENCES point(id) ON DELETE CASCADE,
    CONSTRAINT unique_edge UNIQUE (origin_point, destination_point)
);
