package nc.admitionum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nc.admitionum.model.Invitation;

public interface InvitationRepository
        extends JpaRepository<Invitation, Integer> {

    Optional<Invitation> findByAccessCode(String accessCode);
}