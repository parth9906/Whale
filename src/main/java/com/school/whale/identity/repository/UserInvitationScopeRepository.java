
package com.school.whale.identity.repository;

import com.school.whale.identity.entity.UserInvitationScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserInvitationScopeRepository
        extends JpaRepository<UserInvitationScope, UUID> {

    List<UserInvitationScope> findByInvitation_Id(UUID invitationId);
}
