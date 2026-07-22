package com.factshare.repository;
import com.factshare.model.CommunityArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CommunityArticleRepository extends MongoRepository<CommunityArticle, String> {
    List<CommunityArticle> findAllByOrderBySubmissionDateDesc();
}
