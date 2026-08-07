package nc.admitionum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.service.InvitationService;

@RestController
@RequestMapping("/api/public/invitations")
public class PublicInvitationController {

    private final InvitationService invitationService;

    public PublicInvitationController(
            InvitationService invitationService) {

        this.invitationService = invitationService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<InvitationPublicResponse>
            getInvitation(
                    @PathVariable("code") String code) {

        InvitationPublicResponse response =
            invitationService
                .getPublicInvitation(code);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{code}/response")
    public ResponseEntity<SaveRsvpResponse>
            saveResponse(
                    @PathVariable("code") String code,
                    @Valid
                    @RequestBody SaveRsvpRequest request) {

        SaveRsvpResponse response =
            invitationService
                .saveResponse(code, request);

        return ResponseEntity.ok(response);
    }
}