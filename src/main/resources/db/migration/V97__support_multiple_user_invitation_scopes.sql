CREATE TABLE user_invitation_scope (
                                       id UUID PRIMARY KEY,
                                       invitation_id UUID NOT NULL,
                                       scope_type VARCHAR(30) NOT NULL,
                                       scope_id UUID NOT NULL,

                                       CONSTRAINT fk_invitation_scope_invitation
                                           FOREIGN KEY (invitation_id)
                                               REFERENCES user_invitation(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT uk_invitation_scope
                                           UNIQUE (invitation_id, scope_type, scope_id)
);

CREATE INDEX idx_invitation_scope_invitation
    ON user_invitation_scope(invitation_id);

-- Backfill existing invitations so that they retain their current
-- single-scope assignment under the new representation.
INSERT INTO user_invitation_scope (
    id,
    invitation_id,
    scope_type,
    scope_id
)
SELECT
    gen_random_uuid(),
    id,
    scope_type,
    scope_id
FROM user_invitation;