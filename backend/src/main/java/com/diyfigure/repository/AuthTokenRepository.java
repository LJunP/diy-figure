package com.diyfigure.repository;

import com.diyfigure.common.enums.AuthTokenPurpose;
import com.diyfigure.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, AuthTokenPurpose purpose);

    List<AuthToken> findByUserIdAndPurposeAndConsumedAtIsNull(Long userId, AuthTokenPurpose purpose);
}
