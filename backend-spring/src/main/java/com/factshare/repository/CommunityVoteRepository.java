package com.factshare.repository;
import com.factshare.model.CommunityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityVoteRepository extends JpaRepository<CommunityVote, String> {
    Optional<CommunityVote> findByArticleIdAndUserId(String articleId, String userId);
    List<CommunityVote> findByArticleIdOrderByCreatedAtAsc(String articleId);
    List<CommunityVote> findByArticleIdInOrderByCreatedAtAsc(Collection<String> articleIds);
}
