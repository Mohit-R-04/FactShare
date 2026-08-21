package com.factshare.repository;
import com.factshare.model.CommunityArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityArticleRepository extends JpaRepository<CommunityArticle, String> {
    List<CommunityArticle> findAllByOrderBySubmissionDateDesc();
}
