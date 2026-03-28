-- ============================================================
-- V2__seed_data.sql
-- Seed data for development & testing
--
-- Accounts:
--   admin@todolistapp.com  | Admin@123  | role: ADMIN
--   john@todolistapp.com   | User@123   | role: USER
--   jane@todolistapp.com   | User@123   | role: USER
-- ============================================================

-- ============================================================
-- 1. USERS dulu tanpa pic_profile_id (belum ada file_uploads)
-- ============================================================
INSERT INTO
    users (
        id,
        name,
        email,
        password,
        role,
        is_active,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        'Admin',
        'admin@todolistapp.com',
        '$2a$12$3agZ7wIhd.4R5juY7cfF7OpPoLMrXRN2NamdwhFGPZD0avk9j6r0q',
        'ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'John Doe',
        'john@todolistapp.com',
        '$2a$12$9sZa9XoMBqGSBQDpbxTtheRiCWS1CfNW2Il.nnZXRqCPq6wqM5DEu',
        'USER',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'Jane Smith',
        'jane@todolistapp.com',
        '$2a$12$9sZa9XoMBqGSBQDpbxTtheRiCWS1CfNW2Il.nnZXRqCPq6wqM5DEu',
        'USER',
        TRUE,
        NOW(),
        NOW()
    );

-- ============================================================
-- 2. FILE_UPLOADS — foto profil & attachment todos (LOCAL DEV)
-- ============================================================
INSERT INTO
    file_uploads (
        id,
        user_id,
        storage_key,
        original_filename,
        content_type,
        file_size,
        status,
        public_url,
        created_at,
        updated_at
    )
VALUES
    -- Foto profil John
    (
        '00000000-0000-0000-0010-000000000001',
        '00000000-0000-0000-0000-000000000002',
        'users/00000000-0000-0000-0000-000000000002/profile/avatar.jpg',
        'avatar.jpg',
        'image/jpeg',
        102400,
        'UPLOADED',
        'http://localhost:8080/files/users/00000000-0000-0000-0000-000000000002/profile/avatar.jpg',
        NOW(),
        NOW()
    ),
    -- Foto profil Jane
    (
        '00000000-0000-0000-0010-000000000002',
        '00000000-0000-0000-0000-000000000003',
        'users/00000000-0000-0000-0000-000000000003/profile/avatar.jpg',
        'avatar.jpg',
        'image/jpeg',
        87040,
        'UPLOADED',
        'http://localhost:8080/files/users/00000000-0000-0000-0000-000000000003/profile/avatar.jpg',
        NOW(),
        NOW()
    ),
    -- Image attachment todo John
    (
        '00000000-0000-0000-0010-000000000003',
        '00000000-0000-0000-0000-000000000002',
        'users/00000000-0000-0000-0000-000000000002/todos/s3-diagram.png',
        's3-diagram.png',
        'image/png',
        204800,
        'UPLOADED',
        'http://localhost:8080/files/users/00000000-0000-0000-0000-000000000002/todos/s3-diagram.png',
        NOW(),
        NOW()
    ),
    -- Image attachment todo Jane
    (
        '00000000-0000-0000-0010-000000000004',
        '00000000-0000-0000-0000-000000000003',
        'users/00000000-0000-0000-0000-000000000003/todos/jpa-notes.png',
        'jpa-notes.png',
        'image/png',
        153600,
        'UPLOADED',
        'http://localhost:8080/files/users/00000000-0000-0000-0000-000000000003/todos/jpa-notes.png',
        NOW(),
        NOW()
    ),
    -- Contoh status PENDING
    (
        '00000000-0000-0000-0010-000000000005',
        '00000000-0000-0000-0000-000000000002',
        'users/00000000-0000-0000-0000-000000000002/todos/pending-upload.jpg',
        'pending-upload.jpg',
        'image/jpeg',
        NULL,
        'PENDING',
        NULL,
        NOW(),
        NOW()
    );
-- ============================================================
-- 3. UPDATE users — pasang pic_profile_id setelah file_uploads ada
-- ============================================================
UPDATE users
SET
    pic_profile_id = '00000000-0000-0000-0010-000000000001',
    updated_at = NOW()
WHERE
    id = '00000000-0000-0000-0000-000000000002';
-- John

UPDATE users
SET
    pic_profile_id = '00000000-0000-0000-0010-000000000002',
    updated_at = NOW()
WHERE
    id = '00000000-0000-0000-0000-000000000003';
-- Jane

-- Admin tidak punya foto profil — pic_profile_id tetap NULL

-- ============================================================
-- 4. CATEGORIES
-- ============================================================
INSERT INTO
    categories (
        id,
        user_id,
        name,
        description,
        color,
        is_default,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0001-000000000001',
        '00000000-0000-0000-0000-000000000002',
        'Personal',
        'Kegiatan pribadi sehari-hari',
        '#4A90E2',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0001-000000000002',
        '00000000-0000-0000-0000-000000000002',
        'Work',
        'Pekerjaan dan proyek kantor',
        '#E25C4A',
        FALSE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0001-000000000003',
        '00000000-0000-0000-0000-000000000002',
        'Shopping',
        'Daftar belanjaan',
        '#50C878',
        FALSE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0000-000000000003',
        'Personal',
        'Kegiatan pribadi',
        '#9B59B6',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0002-000000000002',
        '00000000-0000-0000-0000-000000000003',
        'Study',
        'Belajar dan pengembangan diri',
        '#F39C12',
        FALSE,
        NOW(),
        NOW()
    );

-- ============================================================
-- 5. TODOS (dengan image_id)
-- ============================================================
INSERT INTO
    todos (
        id,
        user_id,
        title,
        description,
        completed,
        due_date,
        image_id,
        priority,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0000-000000000002',
        'Setup project Spring Boot',
        'Inisialisasi project, konfigurasi Maven, dan dependency dasar',
        TRUE,
        NULL,
        NULL, -- tidak ada image
        'HIGH',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0003-000000000002',
        '00000000-0000-0000-0000-000000000002',
        'Implementasi JWT Authentication',
        'Buat login, register, refresh token endpoint',
        TRUE,
        NULL,
        NULL,
        'HIGH',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0003-000000000003',
        '00000000-0000-0000-0000-000000000002',
        'Implementasi file upload S3',
        'Signed URL strategy untuk upload foto profil dan attachment todo',
        FALSE,
        DATE_ADD(NOW(), INTERVAL 3 DAY),
        '00000000-0000-0000-0010-000000000003', -- ada image
        'MEDIUM',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0003-000000000004',
        '00000000-0000-0000-0000-000000000002',
        'Beli bahan masak minggu ini',
        'Ayam, sayuran, bumbu dapur, minyak goreng',
        FALSE,
        DATE_ADD(NOW(), INTERVAL 1 DAY),
        NULL,
        'LOW',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0003-000000000005',
        '00000000-0000-0000-0000-000000000002',
        'Review pull request tim',
        NULL,
        FALSE,
        DATE_ADD(NOW(), INTERVAL 2 DAY),
        NULL,
        'HIGH',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0003-000000000006',
        '00000000-0000-0000-0000-000000000002',
        'Todo yang sudah dihapus (soft delete)',
        'Ini tidak akan muncul di list normal',
        FALSE,
        NULL,
        NULL,
        'LOW',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0004-000000000001',
        '00000000-0000-0000-0000-000000000003',
        'Belajar Spring Data JPA',
        'Entity, Repository, JPQL, dan EntityGraph',
        TRUE,
        NULL,
        '00000000-0000-0000-0010-000000000004', -- ada image
        'HIGH',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0004-000000000002',
        '00000000-0000-0000-0000-000000000003',
        'Baca buku Clean Code',
        'Minimal 2 chapter per minggu',
        FALSE,
        DATE_ADD(NOW(), INTERVAL 7 DAY),
        NULL,
        'MEDIUM',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0004-000000000003',
        '00000000-0000-0000-0000-000000000003',
        'Olahraga pagi',
        'Jogging 30 menit setiap hari',
        FALSE,
        DATE_ADD(NOW(), INTERVAL 1 DAY),
        NULL,
        'MEDIUM',
        NOW(),
        NOW()
    );

-- ============================================================
-- 6. SOFT DELETE simulasi
-- ============================================================
UPDATE todos
SET
    deleted_at = NOW(),
    updated_at = NOW()
WHERE
    id = '00000000-0000-0000-0003-000000000006';

-- ============================================================
-- 7. TODO_CATEGORIES pivot
-- ============================================================
INSERT INTO
    todo_categories (todo_id, category_id)
VALUES (
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0001-000000000002'
    ), -- John: Setup → Work
    (
        '00000000-0000-0000-0003-000000000002',
        '00000000-0000-0000-0001-000000000002'
    ), -- John: JWT → Work
    (
        '00000000-0000-0000-0003-000000000003',
        '00000000-0000-0000-0001-000000000002'
    ), -- John: S3 → Work
    (
        '00000000-0000-0000-0003-000000000003',
        '00000000-0000-0000-0001-000000000001'
    ), -- John: S3 → Personal (multi-category)
    (
        '00000000-0000-0000-0003-000000000004',
        '00000000-0000-0000-0001-000000000003'
    ), -- John: Belanja → Shopping
    (
        '00000000-0000-0000-0003-000000000005',
        '00000000-0000-0000-0001-000000000002'
    ), -- John: PR Review → Work
    (
        '00000000-0000-0000-0004-000000000001',
        '00000000-0000-0000-0002-000000000002'
    ), -- Jane: JPA → Study
    (
        '00000000-0000-0000-0004-000000000002',
        '00000000-0000-0000-0002-000000000002'
    ), -- Jane: Clean Code → Study
    (
        '00000000-0000-0000-0004-000000000003',
        '00000000-0000-0000-0002-000000000001'
    );
-- Jane: Olahraga → Personal