
BEGIN;

-- 1. Create Institution Admin role.
INSERT INTO role (
    id,
    code,
    name,
    description,
    active,
    created_at,
    updated_at
)
VALUES (
           gen_random_uuid(),
           'INSTITUTION_ADMIN',
           'Institution Administrator',
           'Full administrative access within assigned institutions, excluding platform-level administration.',
           TRUE,
           NOW(),
           NOW()
       )
    ON CONFLICT (code) DO NOTHING;


-- 2. Assign institution-level permissions.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.code = 'INSTITUTION_ADMIN'
  AND r.active = TRUE
  AND p.active = TRUE
  AND p.code NOT IN (
                     'ENTERPRISE_CREATE',
                     'ENTERPRISE_READ',
                     'REGION_CREATE',
                     'REGION_READ',
                     'CLUSTER_CREATE',
                     'CLUSTER_READ',
                     'INSTITUTION_CREATE',
                     'ROLE_CREATE',
                     'ROLE_PERMISSION_ASSIGN',
                     'PERMISSION_CREATE',
                     'PERMISSION_READ'
    )
    ON CONFLICT DO NOTHING;

COMMIT;
