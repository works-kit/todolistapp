-- ============================================================
-- V1__init_schema.sql
-- Initial schema for TodolistApp
-- ============================================================

-- 1. file_uploads dibuat duluan (direferensikan users & todos)
--    FK user_id ditambahkan belakangan via ALTER karena circular reference
CREATE TABLE file_uploads (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL COMMENT 'Pemilik file',
    storage_key VARCHAR(512) NOT NULL COMMENT 'Path di dalam bucket S3/GCS',
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NULL COMMENT 'Ukuran file dalam bytes',
    status ENUM(
        'PENDING',
        'UPLOADED',
        'REPLACED',
        'FAILED'
    ) NOT NULL DEFAULT 'PENDING',
    public_url VARCHAR(768) NULL COMMENT 'URL publik setelah UPLOADED',
    deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_file_storage_key (storage_key (255)),
    INDEX idx_file_user_id (user_id),
    INDEX idx_file_status (status),
    INDEX idx_file_deleted (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================

-- 2. users (FK → file_uploads untuk pic_profile_id)
CREATE TABLE users (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    pic_profile_id CHAR(36) NULL DEFAULT NULL COMMENT 'FK ke file_uploads.id',
    refresh_token VARCHAR(512) NULL DEFAULT NULL,
    refresh_token_expired_at BIGINT NULL DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user_email (email),
    INDEX idx_user_deleted (deleted_at),
    INDEX idx_user_pic_profile (pic_profile_id),
    FULLTEXT INDEX idx_user_search (name, email),
    CONSTRAINT fk_users_pic_profile FOREIGN KEY (pic_profile_id) REFERENCES file_uploads (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================

-- 3. categories
CREATE TABLE categories (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(20) NULL DEFAULT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user_category_name (user_id, name),
    INDEX idx_user_category_created (user_id, created_at),
    INDEX idx_category_deleted (deleted_at),
    FULLTEXT INDEX idx_category_search (name, description),
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================

-- 4. todos
CREATE TABLE todos (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    due_date DATETIME NULL,
    image_id CHAR(36) NULL DEFAULT NULL COMMENT 'FK ke file_uploads.id',
    priority ENUM('LOW', 'MEDIUM', 'HIGH') NULL DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_todo_user_status (user_id, completed),
    INDEX idx_todo_user_due (user_id, due_date),
    INDEX idx_todo_user_priority (user_id, priority),
    INDEX idx_todo_user_created (user_id, created_at),
    INDEX idx_todo_image (image_id),
    INDEX idx_todo_deleted (deleted_at),
    FULLTEXT INDEX idx_todo_search (title, description),
    CONSTRAINT fk_todo_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_todo_image FOREIGN KEY (image_id) REFERENCES file_uploads (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================

-- 5. todo_categories (pivot, hard delete)
CREATE TABLE todo_categories (
    todo_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    PRIMARY KEY (todo_id, category_id),
    CONSTRAINT fk_pivot_todo FOREIGN KEY (todo_id) REFERENCES todos (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pivot_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ============================================================

-- 6. Tutup circular reference: tambahkan FK user_id di file_uploads
--    (users sudah ada di langkah 2, baru bisa direferensikan sekarang)
ALTER TABLE file_uploads
ADD CONSTRAINT fk_file_uploads_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE;