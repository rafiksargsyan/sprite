package com.rsargsyan.sprite.main_ctx;

import com.rsargsyan.sprite.main_ctx.core.app.JobSpecService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.JobSpecCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Account;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.JobSpecRepository;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

public class UnitTest {
  @Test
  @DisplayName("test empty name")
  void test() {

    AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
    JobSpecRepository jobSpecRepository = Mockito.mock(JobSpecRepository.class);
    final String dummyAccountId = TSID.fast().toString();
    final long dummyAccountLongId = TSID.from(dummyAccountId).toLong();
    Account account = Mockito.mock(Account.class);
    Mockito.when(account.getId()).thenReturn(dummyAccountLongId);
    Mockito.when(jobSpecRepository.countByAccountId(dummyAccountLongId)).thenReturn(100L);
    Mockito.when(accountRepository.findById(dummyAccountLongId)).thenReturn(Optional.of(account));
    JobSpecService jobSpecService = new JobSpecService(jobSpecRepository, accountRepository);
    JobSpecCreationDTO dto = new JobSpecCreationDTO("name", "description", Collections.emptyList());

    var ex = Assertions.assertThrows(InvalidThumbnailConfigException.class,
        () -> jobSpecService.create(dummyAccountId, dto));
    Assertions.assertEquals("Job spec limit of 100 reached", ex.getMessage());

//    Mockito.when(account.getId()).thenReturn(dummyAccountLongId);
//    Mockito.when(accountRepository.findById(TSID.from(dummyAccountId).toLong())).thenReturn(Optional.of(new Account()));
//    JobSpecService jobSpecService = new JobSpecService(null, accountRepository);
//    JobSpecCreationDTO dto = new JobSpecCreationDTO("", "description", Collections.emptyList());
//    jobSpecService.create(TSID.fast().toString(), dto);
//    Assertions.assertThrows()
  }
}
