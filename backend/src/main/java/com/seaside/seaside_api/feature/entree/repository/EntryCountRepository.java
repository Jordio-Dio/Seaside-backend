package com.seaside.seaside_api.feature.entree.repository;

import com.seaside.seaside_api.feature.entree.model.EntryCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntryCountRepository extends JpaRepository<EntryCount, UUID> {
}