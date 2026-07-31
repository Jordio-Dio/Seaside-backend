package com.seaside.seaside_api.repository;

import com.seaside.seaside_api.entity.EntryCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntryCountRepository extends JpaRepository<EntryCount, UUID> {
}