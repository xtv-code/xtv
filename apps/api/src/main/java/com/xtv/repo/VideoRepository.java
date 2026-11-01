package com.xtv.repo;

import com.xtv.domain.Video;
import com.xtv.domain.Vocab;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
  Page<Video> findByStatusOrderByPublishedAtDesc(Vocab.VideoStatus status, Pageable pageable);

  List<Video> findByStatusAndFeaturedTrueOrderByPublishedAtDesc(Vocab.VideoStatus status, Pageable pageable);

  Page<Video> findByStatusOrderByViewCountDesc(Vocab.VideoStatus status, Pageable pageable);

  List<Video> findByIdIn(Collection<Long> ids);

  Optional<Video> findFirstByTitle(String title);

  Page<Video> findBySeriesIdAndStatus(Long seriesId, Vocab.VideoStatus status, Pageable pageable);

  long countBySeriesId(Long seriesId);

  @Query("select count(v) from Video v join v.categories c where c.id = :id")
  long countUsingCategory(@Param("id") Long id);

  @Query("select count(v) from Video v join v.tags t where t.id = :id")
  long countUsingTag(@Param("id") Long id);

  @Query("select count(v) from Video v join v.people p where p.id = :id")
  long countUsingPerson(@Param("id") Long id);

  long countBySeriesIdAndStatus(Long seriesId, Vocab.VideoStatus status);

  @Query("""
      select v from Video v join v.categories c
      where c.slug = :slug and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
      """)
  Page<Video> findPublishedByCategory(@Param("slug") String slug, Pageable pageable);

  @Query("""
      select v from Video v join v.tags t
      where t.slug = :slug and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
      """)
  Page<Video> findPublishedByTag(@Param("slug") String slug, Pageable pageable);

  @Query("""
      select v from Video v join v.people p
      where p.slug = :slug and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
      """)
  Page<Video> findPublishedByPerson(@Param("slug") String slug, Pageable pageable);

  @Query("""
      select v from Video v join v.categories c
      where c.id = :categoryId and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
      """)
  List<Video> findHomeByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

  @Query("""
      select v from Video v
      where v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
        and (lower(v.title) like lower(concat('%', :q, '%'))
          or lower(v.i18n) like lower(concat('%', :q, '%'))
          or exists (select t from v.tags t where lower(t.name) like lower(concat('%', :q, '%')) or lower(t.i18n) like lower(concat('%', :q, '%')))
          or exists (select p from v.people p where lower(p.name) like lower(concat('%', :q, '%')) or lower(p.i18n) like lower(concat('%', :q, '%'))))
      """)
  Page<Video> search(@Param("q") String q, Pageable pageable);

  @Query("""
      select v from Video v
      where v.id <> :videoId
        and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
        and exists (select t from v.tags t where t.id in :tagIds)
      """)
  Page<Video> relatedByTags(@Param("tagIds") Collection<Long> tagIds, @Param("videoId") Long videoId, Pageable pageable);

  @Query("""
      select v from Video v
      where v.id <> :videoId
        and v.status = com.xtv.domain.Vocab.VideoStatus.PUBLISHED
        and exists (select p from v.people p where p.id in :personIds)
      """)
  Page<Video> moreByPeople(@Param("personIds") Collection<Long> personIds, @Param("videoId") Long videoId, Pageable pageable);
}
