package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.User;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

  private UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }
  public UserDTO create(String accountId, String name) {
    User user = new User(TSID.from(accountId).toLong(), name);
    this.userRepository.save(user);
    return UserDTO.from(user);
  }

  public UserDTO createApiKey(String userId) {
    Optional<User> userOpt = this.userRepository.findById(TSID.from(userId).toLong());
    if (userOpt.isEmpty()) {
      throw new RuntimeException();
    }
    User user = userOpt.get();
    user.createApiKey();
    this.userRepository.save(user);
    return UserDTO.from(user);
  }
}
