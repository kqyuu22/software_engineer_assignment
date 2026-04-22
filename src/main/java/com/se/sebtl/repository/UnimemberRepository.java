package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.se.sebtl.model.UniMember;
import com.se.sebtl.model.Role;

public interface UnimemberRepository extends JpaRepository<UniMember, Integer> {
    @Query("SELECT u.role FROM UniMember u WHERE u.userId = :userId")
    Optional<Role> findRoleByUserId(@Param("userId") int userId);
}