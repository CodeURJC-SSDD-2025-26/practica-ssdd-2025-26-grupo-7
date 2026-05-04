package es.urjc.code.utilityservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import es.urjc.code.utilityservice.dto.TournamentPdfRequest;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfService {

    public byte[] generateTournamentPdf(TournamentPdfRequest tournament) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

            Paragraph title = new Paragraph("Resumen de Torneo: " + tournament.name(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);
            document.add(new Paragraph("Información General", headerFont));
            document.add(new Paragraph("----------------------------------------------------------------", headerFont));

            document.add(createKeyValueParagraph("Juego: ", tournament.game(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Plataforma: ", tournament.platform(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Modo: ", tournament.mode(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Equipos Máximos: ", String.valueOf(tournament.maxTeams()),
                    boldFont, normalFont));
            document.add(createKeyValueParagraph("Fecha de Inicio: ", tournament.startDate(), boldFont, normalFont));
            document.add(createKeyValueParagraph("Estado: ", tournament.state(), boldFont, normalFont));
            document.add(new Paragraph(" "));

            if (tournament.description() != null && !tournament.description().isEmpty()) {
                document.add(new Paragraph("Descripción:", boldFont));
                document.add(new Paragraph(tournament.description(), normalFont));
                document.add(new Paragraph(" "));
            }

            if (tournament.rules() != null && !tournament.rules().isEmpty()) {
                document.add(new Paragraph("Reglas:", boldFont));
                document.add(new Paragraph(tournament.rules(), normalFont));
                document.add(new Paragraph(" "));
            }

            document.add(new Paragraph("Lista de Partidos", headerFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addHeaderCell(table, "Fecha");
            addHeaderCell(table, "Partido");
            addHeaderCell(table, "Fase");
            addHeaderCell(table, "Resultado");

            List<TournamentPdfRequest.MatchInfo> matches = tournament.matches();
            if (matches != null && !matches.isEmpty()) {
                for (TournamentPdfRequest.MatchInfo m : matches) {
                    table.addCell(new Phrase(m.matchDate(), normalFont));
                    String local = (m.localTeamName() != null) ? m.localTeamName() : "TBD";
                    String away = (m.awayTeamName() != null) ? m.awayTeamName() : "TBD";
                    table.addCell(new Phrase(local + " vs " + away, normalFont));
                    table.addCell(new Phrase(m.phase(), normalFont));
                    table.addCell(new Phrase(m.result() != null ? m.result()
                            : (m.matchState() != null ? m.matchState() : "Programado"), normalFont));
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
