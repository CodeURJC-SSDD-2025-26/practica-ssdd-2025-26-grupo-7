package es.urjc.code.utilityservice.dto;

import java.util.List;

public record TournamentPdfRequest(
        Long id,
        String name,
        String game,
        String platform,
        String mode,
        Integer maxTeams,
        String startDate,
        String state,
        String description,
        String rules,
        List<MatchInfo> matches
) {
    public record MatchInfo(
            String matchDate,
            String localTeamName,
            String awayTeamName,
            String phase,
            String result,
            String matchState
    ) {}
}
