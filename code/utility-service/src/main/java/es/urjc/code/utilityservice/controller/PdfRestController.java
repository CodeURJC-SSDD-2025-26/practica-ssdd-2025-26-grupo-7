package es.urjc.code.utilityservice.controller;

import es.urjc.code.utilityservice.dto.TournamentPdfRequest;
import es.urjc.code.utilityservice.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf")
public class PdfRestController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/tournament")
    public ResponseEntity<byte[]> generateTournamentPdf(@RequestBody TournamentPdfRequest request) {
        try {
            byte[] pdf = pdfService.generateTournamentPdf(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Tournament_" + request.id() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
