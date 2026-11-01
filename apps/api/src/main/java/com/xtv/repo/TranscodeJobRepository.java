package com.xtv.repo;

import com.xtv.domain.TranscodeJob;
import com.xtv.domain.Vocab;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, Long> {
  List<TranscodeJob> findTop5ByStateOrderByCreatedAtAsc(Vocab.JobState state);

  List<TranscodeJob> findByVideoIdOrderByIdDesc(Long videoId);

  void deleteByVideoId(Long videoId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update TranscodeJob j
      set j.state = com.xtv.domain.Vocab.JobState.RUNNING, j.updatedAt = :now
      where j.id = :id and j.state = com.xtv.domain.Vocab.JobState.PENDING
      """)
  int claim(@Param("id") Long id, @Param("now") Instant now);
}
