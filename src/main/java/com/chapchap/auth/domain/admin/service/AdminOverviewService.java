package com.chapchap.auth.domain.admin.service;

import com.chapchap.auth.domain.admin.repository.AdminCredentialRepository;
import com.chapchap.auth.domain.admin.response.AdminAccountSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOverviewService {
    private final AdminCredentialRepository credentials;
    private final JdbcTemplate jdbc;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public Page<AdminAccountSummary> accounts(String search, int page, int size) {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        return credentials.searchAccounts(search.trim(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(c -> AdminAccountSummary.from(c, now));
    }

    public record Count(String label, long count) {}
    public record Daily(LocalDate date, long count) {}
    public record Overview(LocalDateTime asOf, LocalDate from, LocalDate to, long totalMembers,
            long newMembers, List<Count> statuses, List<Count> providers, List<Daily> registrations) {}

    public Overview overview(int days) {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDate to = now.toLocalDate(), from = to.minusDays(days - 1L);
        List<Count> statuses = jdbc.query("select status, count(*) from users where role in ('CUSTOMER','RIDER') group by status",
                (rs, n) -> new Count(rs.getString(1), rs.getLong(2)));
        List<Count> providers = jdbc.query("select s.provider, count(*) from social_accounts s join users u on u.user_id=s.user_id where u.role in ('CUSTOMER','RIDER') and u.status='ACTIVE' group by s.provider",
                (rs, n) -> new Count(rs.getString(1), rs.getLong(2)));
        Map<LocalDate, Long> counts = new HashMap<>();
        jdbc.query("select cast(created_at as date), count(*) from users where role in ('CUSTOMER','RIDER') and created_at >= ? and created_at < ? group by cast(created_at as date)",
                (rs, n) -> { counts.put(rs.getDate(1).toLocalDate(), rs.getLong(2)); return 0; },
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        List<Daily> daily = from.datesUntil(to.plusDays(1)).map(d -> new Daily(d, counts.getOrDefault(d, 0L))).toList();
        return new Overview(now, from, to, statuses.stream().mapToLong(Count::count).sum(),
                daily.stream().mapToLong(Daily::count).sum(), statuses, providers, daily);
    }
}
