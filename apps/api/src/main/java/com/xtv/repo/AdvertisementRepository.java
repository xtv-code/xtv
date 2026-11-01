package com.xtv.repo;

import com.xtv.domain.Advertisement;
import com.xtv.domain.Vocab;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long>, JpaSpecificationExecutor<Advertisement> {
  List<Advertisement> findBySlotAndEnabledTrue(Vocab.AdSlot slot);
}
