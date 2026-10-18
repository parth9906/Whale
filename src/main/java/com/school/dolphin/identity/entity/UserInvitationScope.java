
package com.school.dolphin.identity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "user_invitation_scope",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invitation_scope",
                columnNames = {
                        "invitation_id",
                        "scope_type",
                        "scope_id"
                }
        ),
        indexes = @Index(
                name = "idx_invitation_scope_invitation",
                columnList = "invitation_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInvitationScope {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private UserInvitation invitation;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private OrganizationScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    public static UserInvitationScope create(
            UserInvitation invitation,
            OrganizationScopeType scopeType,
            UUID scopeId
    ) {
        UserInvitationScope scope = new UserInvitationScope();
        scope.id = UUID.randomUUID();
        scope.invitation = invitation;
        scope.scopeType = scopeType;
        scope.scopeId = scopeId;
        return scope;
    }
}
