package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Account;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.User;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  private UserRepository userRepository;
  private AccountRepository accountRepository;
  private PrincipalRepository principalRepository;

  @Autowired
  public UserService(UserRepository userRepository, AccountRepository accountRepository,
                     PrincipalRepository principalRepository) {
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.principalRepository = principalRepository;
  }
  public UserDTO create(String accountId, String name) {

//    User user = new User(TSID.from(accountId).toLong(), name);
//    this.userRepository.save(user);
//    return UserDTO.from(user);
    return null;
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

  @Transactional
  public UserDTO signUpWithExternal(String externalId, String name) {
    if (name == null || name.isBlank()) name = "Your name here";
    if (externalId == null) throw new RuntimeException("external id must be provided");
    List<Principal> principalList = principalRepository.findByExternalId(externalId);
    if (!principalList.isEmpty()) {
      throw new RuntimeException("User already exists");
    }
    Principal principal = new Principal(externalId);
    Account account = new Account();
    User user = new User(account, principal, name);
    principalRepository.save(principal);
    accountRepository.save(account);
    userRepository.save(user);

    return UserDTO.from(user);
  }
}
