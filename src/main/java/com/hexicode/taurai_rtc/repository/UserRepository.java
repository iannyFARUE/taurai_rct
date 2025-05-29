package com.hexicode.taurai_rtc.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hexicode.taurai_rtc.entity.User;
public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByEmail(String email);

}