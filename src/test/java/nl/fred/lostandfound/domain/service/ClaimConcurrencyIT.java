package nl.fred.lostandfound.domain.service;

import nl.fred.lostandfound.LostAndFoundApplication;
import nl.fred.lostandfound.data.repository.ClaimRepository;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InsufficientQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LostAndFoundApplication.class)
@DisplayName("Proves the claim endpoint's row lock is safe under real concurrent claims")
class ClaimConcurrencyIT {

  private static final int CONCURRENT_CLAIMANTS = 8;

  @Autowired private ClaimService claimService;
  @Autowired private LostItemRepository lostItemRepository;
  @Autowired private ClaimRepository claimRepository;

  @Test
  @DisplayName("only one of many concurrent claims for the last unit succeeds")
  void onlyOneConcurrentClaimSucceedsForTheLastUnit() throws InterruptedException {
    final LostItem contendedItem = lostItemRepository.save(
        LostItem.builder().itemName("Concurrency Test Item").quantity(1).place("LoadTest").build());

    final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CLAIMANTS);
    final CountDownLatch readySignal = new CountDownLatch(CONCURRENT_CLAIMANTS);
    final CountDownLatch startSignal = new CountDownLatch(1);

    try {
      final List<Callable<Boolean>> claimAttempts = new ArrayList<>();
      for (int i = 0; i < CONCURRENT_CLAIMANTS; i++) {
        final long userId = 9000L + i;
        claimAttempts.add(() -> {
          readySignal.countDown();
          startSignal.await();
          try {
            claimService.claim(contendedItem.getId(), userId, 1);
            return true;
          } catch (final InsufficientQuantityException _) {
            return false;
          }
        });
      }

      final List<Future<Boolean>> futures = claimAttempts.stream().map(executor::submit).toList();

      assertThat(readySignal.await(5, TimeUnit.SECONDS)).isTrue();
      startSignal.countDown();

      final long successCount = futures.stream().map(this::get).filter(Boolean::booleanValue).count();

      assertThat(successCount).isEqualTo(1);
      assertThat(claimRepository.sumQuantityByLostItemId(contendedItem.getId())).isEqualTo(1);
    } finally {
      executor.shutdownNow();
      claimRepository.findAll().stream()
          .filter(claim -> claim.getLostItem().getId().equals(contendedItem.getId()))
          .forEach(claimRepository::delete);
      lostItemRepository.deleteById(contendedItem.getId());
    }
  }

  private boolean get(final Future<Boolean> future) {
    try {
      return future.get();
    } catch (final Exception e) {
      throw new IllegalStateException("Concurrent claim attempt failed unexpectedly.", e);
    }
  }

}
