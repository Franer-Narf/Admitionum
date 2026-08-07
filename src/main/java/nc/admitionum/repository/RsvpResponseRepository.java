package nc.admitionum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nc.admitionum.model.RsvpResponse;

public interface RsvpResponseRepository
        extends JpaRepository<RsvpResponse, Integer> {

    Optional<RsvpResponse> findByInvitationId(
        Integer invitationId
    );
}