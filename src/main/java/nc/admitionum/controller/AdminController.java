package nc.admitionum.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nc.admitionum.dto.admin.AdminDashboardResponse;
import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.service.AdminService;
import nc.admitionum.service.CsvExportService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    private final CsvExportService csvExportService;

    public AdminController(
            AdminService adminService,
            CsvExportService csvExportService) {

        this.adminService = adminService;
        this.csvExportService =
            csvExportService;
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

    @GetMapping(
        value = "/responses.csv",
        produces = "text/csv"
    )
    public ResponseEntity<byte[]>
            exportResponsesCsv() {

        String csv =
            csvExportService.exportResponses();

        byte[] csvBytes =
            csv.getBytes(
                StandardCharsets.UTF_8
            );

        HttpHeaders headers =
            new HttpHeaders();

        headers.setContentType(
            MediaType.parseMediaType(
                "text/csv;charset=UTF-8"
            )
        );

        headers.setContentDisposition(
            ContentDisposition
                .attachment()
                .filename(
                    "wedding-responses.csv",
                    StandardCharsets.UTF_8
                )
                .build()
        );

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(csvBytes);
    }
}