package es.urjc.code.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.Tournament;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfService {

    public byte[] generateTournamentPdf(Tournament tournament) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("Resumen de Torneo: " + tournament.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Details section
            document.add(new Paragraph("Información General", headerFont));
            document.add(new Paragraph("----------------------------------------------------------------", headerFont));

            document.add(createKeyValueParagraph("Juego: ", tournament.getGame(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Plataforma: ", tournament.getPlatform(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Modo: ", tournament.getMode(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Equipos Máximos: ", String.valueOf(tournament.getMaxTeams()),
                    boldFont, normalFont));
            document.add(createKeyValueParagraph("Fecha de Inicio: ", tournament.getStartDate(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Estado: ", tournament.getState(), boldFont, normalFont));
            document.add(new Paragraph(" "));

            if (tournament.getDescription() != null && !tournament.getDescription().isEmpty()) {
                document.add(new Paragraph("Descripción:", boldFont));
                document.add(new Paragraph(tournament.getDescription(), normalFont));
                document.add(new Paragraph(" "));
            }

            if (tournament.getRules() != null && !tournament.getRules().isEmpty()) {
                document.add(new Paragraph("Reglas:", boldFont));
                document.add(new Paragraph(tournament.getRules(), normalFont));
                document.add(new Paragraph(" "));
            }

            // Matches table
            document.add(new Paragraph("Lista de Partidos", headerFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Header
            addHeaderCell(table, "Fecha");
            addHeaderCell(table, "Partido");
            addHeaderCell(table, "Fase");
            addHeaderCell(table, "Resultado");

            List<Match> matches = tournament.getMatches();
            if (matches != null && !matches.isEmpty()) {
                for (Match m : matches) {
                    table.addCell(new Phrase(m.getMatchDate(), normalFont));
                    String local = (m.getLocalTeam() != null) ? m.getLocalTeam().getName() : "TBD";
                    String away = (m.getAwayTeam() != null) ? m.getAwayTeam().getName() : "TBD";
                    table.addCell(new Phrase(local + " vs " + away, normalFont));
                    table.addCell(new Phrase(m.getPhase(), normalFont));
                    table.addCell(new Phrase(m.getResult() != null ? m.getResult()
                            : (m.getState() != null ? m.getState() : "Programado"), normalFont));
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No hay partidos registrados aún."));
                emptyCell.setColspan(4);
                emptyCell.setPadding(10);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            }
            document.add(table);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private Paragraph createKeyValueParagraph(String key, String value, Font keyFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(key, keyFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        return p;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
        cell.setBackgroundColor(Color.GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
