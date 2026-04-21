// package com.se.sebtl.repository;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import java.util.Optional;
// import com.se.sebtl.model.User;
// import com.se.sebtl.model.Role;

// public interface UserRepository extends JpaRepository<User, Integer> {
//     // Replaces getUserRole() manual SQL
//     @Query("SELECT u.role FROM User u WHERE u.userId = :userId")
//     Optional<Role> findRoleByUserId(@Param("userId") int userId);
// }
