package nc.admitionum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.service.InvitationService;

@RestController
@RequestMapping("/api/public/registrations")
public class PublicRegistrationController {

    private final InvitationService invitationService;

    public PublicRegistrationController(
            InvitationService invitationService) {

        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<SaveRsvpResponse>
            register(
                    @Valid
                    @RequestBody
                    SaveRsvpRequest request) {

        SaveRsvpResponse response =
            invitationService
                .registerPublicResponse(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}