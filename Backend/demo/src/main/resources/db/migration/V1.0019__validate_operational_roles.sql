-- Remove legacy role rows that advertise a domain profile which does not exist.
DELETE FROM user_role ur
WHERE ur.role = 'TRAINER'
  AND NOT EXISTS (SELECT 1 FROM trainer t WHERE t.user_id = ur.user_id);

DELETE FROM user_role ur
WHERE ur.role = 'CLIENT'
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.user_id = ur.user_id);

-- Refuse startup if ambiguous/role-less accounts remain and require an explicit repair.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM "user" u
        LEFT JOIN user_role ur
          ON ur.user_id = u.id
         AND ur.role IN ('MANAGER', 'TRAINER', 'CLIENT')
        GROUP BY u.id
        HAVING COUNT(ur.id) <> 1
    ) THEN
        RAISE EXCEPTION 'Every user must have exactly one operational role';
    END IF;
END $$;
