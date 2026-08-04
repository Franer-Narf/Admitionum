package nc.admitionum.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.model.Invitation;
import nc.admitionum.repository.InvitationRepository;

@Service
@Transactional(readOnly = true)
public class InvitationService {

    private final InvitationRepository invitationRepository;

    public InvitationService(
            InvitationRepository invitationRepository) {

        this.invitationRepository = invitationRepository;
    }

    public InvitationPublicResponse getPublicInvitation(
            String accessCode) {

        String normalizedAccessCode =
            normalizeAccessCode(accessCode);

        Invitation invitation =
            invitationRepository
                .findByAccessCode(normalizedAccessCode)
                .orElseThrow(
                    InvitationNotFoundException::new
                );

        verifyInvitationIsActive(invitation);
        verifyInvitationHasNotExpired(invitation);

        return new InvitationPublicResponse(
            invitation.getDisplayName(),
            invitation.getMaxGuests(),
            invitation.getExpiresAt(),
            null
        );
    }

    private String normalizeAccessCode(
            String accessCode) {

        if (accessCode == null || accessCode.isBlank()) {
            throw new InvitationNotFoundException();
        }

        return accessCode.trim();
    }

    private void verifyInvitationIsActive(
            Invitation invitation) {

        if (!Boolean.TRUE.equals(invitation.getIsActive())) {
            throw new InvitationDisabledException();
        }
    }

    private void verifyInvitationHasNotExpired(
            Invitation invitation) {

        LocalDateTime expiresAt =
            invitation.getExpiresAt();

        if (expiresAt == null) {
            return;
        }

        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        if (!expiresAt.isAfter(nowUtc)) {
            throw new InvitationExpiredException();
        }
    }
}