CREATE TABLE krishi.notifications
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
            REFERENCES krishi.users(id)
            ON DELETE CASCADE
);
