package com.factshare.repository;
import com.factshare.model.Article;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ArticleRepository extends MongoRepository<Article, String> {
    List<Article> findByUserIdOrderBySubmissionDateDesc(String userId);
}
