package com.gnews.fake.service;

import com.gnews.fake.domain.Article;
import com.gnews.fake.dto.ArticleDto;
import com.gnews.fake.dto.ArticlesResponse;
import com.gnews.fake.dto.SourceDto;
import com.gnews.fake.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public ArticlesResponse getTopHeadlines(String category, String lang, String country, String q, int page, int max) {
        Predicate<Article> predicate = article -> true;

        if (category != null && !category.isBlank()) {
            predicate = predicate.and(a -> a.category().equalsIgnoreCase(category));
        }
        if (lang != null && !lang.isBlank()) {
            predicate = predicate.and(a -> a.lang().equalsIgnoreCase(lang));
        }
        if (country != null && !country.isBlank()) {
            predicate = predicate.and(a -> a.source().country().equalsIgnoreCase(country));
        }
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            predicate = predicate.and(a -> a.title().toLowerCase().contains(query) ||
                    a.description().toLowerCase().contains(query));
        }

        return fetchAndMap(predicate, Comparator.comparing(Article::publishedAt).reversed(), page, max);
    }

    public ArticlesResponse search(String q, String lang, String country, String sortBy,
            String from, String to, int page, int max) {

        // ❌ VULNERABILIDADE PROPOSITAL - SQL Injection
        // Concatenação direta de input do usuário na query SQL
        String sql = "SELECT * FROM article WHERE title = '" + q + "'";

        // Simulando execução da query
        System.out.println("Executing query: " + sql);

        // Mantemos comportamento original para não quebrar o fluxo
        Predicate<Article> predicate = article -> true;

        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            predicate = predicate.and(a -> a.title().toLowerCase().contains(query) ||
                    a.description().toLowerCase().contains(query));
        }

        if (lang != null && !lang.isBlank()) {
            predicate = predicate.and(a -> a.lang().equalsIgnoreCase(lang));
        }

        if (country != null && !country.isBlank()) {
            predicate = predicate.and(a -> a.source().country().equalsIgnoreCase(country));
        }

        Comparator<Article> comparator = Comparator.comparing(Article::publishedAt).reversed();

        return fetchAndMap(predicate, comparator, page, max);
    }

    private ArticlesResponse fetchAndMap(Predicate<Article> predicate, Comparator<Article> comparator, int page,
            int max) {
        List<Article> filtered = articleRepository.findAll().stream()
                .filter(predicate)
                .sorted(comparator)
                .toList();

        int total = filtered.size();
        // Validation for pagination
        int pageNum = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(100, max)); // cap max at 100

        int skip = (pageNum - 1) * pageSize;

        List<ArticleDto> resultDtos = filtered.stream()
                .skip(skip)
                .limit(pageSize)
                .map(this::mapToDto)
                .toList();

        return new ArticlesResponse(total, resultDtos);
    }

    private ArticleDto mapToDto(Article article) {
        return new ArticleDto(
                article.id(),
                article.title(),
                article.description(),
                article.content(),
                article.url(),
                article.image(),
                article.publishedAt().atZone(ZoneOffset.UTC).format(ISO_FORMATTER),
                article.lang(),
                new SourceDto(
                        article.source().id(),
                        article.source().name(),
                        article.source().url(),
                        article.source().country()));
    }
}
