package nc.admitionum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nc.admitionum.dto.admin.AdminDashboardResponse;
import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(
            AdminService adminService) {

        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse>
            getDashboard() {

        AdminDashboardResponse response =
            adminService.getDashboard();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/responses")
    public ResponseEntity<List<AdminRsvpResponse>>
            getResponses() {

        List<AdminRsvpResponse> response =
            adminService.getResponses();

        return ResponseEntity.ok(response);
    }
}