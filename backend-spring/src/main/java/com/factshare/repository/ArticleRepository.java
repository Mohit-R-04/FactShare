package com.factshare.repository;
import com.factshare.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, String> {
    List<Article> findByUserIdOrderBySubmissionDateDesc(String userId);
}
