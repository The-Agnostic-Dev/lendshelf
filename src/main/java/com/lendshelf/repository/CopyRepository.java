package com.lendshelf.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lendshelf.domain.Copy;
import com.lendshelf.domain.CopyStatus;

public interface CopyRepository extends JpaRepository<Copy, Long> {

    List<Copy> findByBookIdOrderByBarcode(Long bookId);

    Optional<Copy> findFirstByBookIdAndStatusOrderByBarcode(Long bookId, CopyStatus status);

    Optional<Copy> findByBarcode(String barcode);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByStatus(CopyStatus status);
}
