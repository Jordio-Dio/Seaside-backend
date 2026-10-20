package com.seaside.seaside_api.feature.auth.repository;

import com.seaside.seaside_api.feature.auth.model.VerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationOtpRepository extends JpaRepository<VerificationOtp, UUID> {

    Optional<VerificationOtp> findByEmailAndCodeAndEstVerifieFalse(String email, String code);

    // Vérifier si un email a déjà un OTP vérifié récent
    boolean existsByEmailAndEstVerifieTrue(String email);

    // Supprimer les anciens OTP d'un email avant d'en créer un nouveau
    @Modifying
    @Query("DELETE FROM VerificationOtp o WHERE o.email = :email")
    void deleteByEmail(String email);
}
