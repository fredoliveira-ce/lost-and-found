package nl.fred.lostandfound.data.client;

import java.util.Map;
import nl.fred.lostandfound.domain.client.UserInfo;
import nl.fred.lostandfound.domain.client.UserServiceClient;
import org.springframework.stereotype.Component;

@Component
public class MockUserServiceClient implements UserServiceClient {

  private static final Map<Long, String> KNOWN_USERS = Map.of(
      1001L, "Alice Johnson",
      1002L, "Brian Smith",
      1003L, "Carla Mendes"
  );

  @Override
  public UserInfo findUser(Long userId) {
    return new UserInfo(userId, KNOWN_USERS.getOrDefault(userId, "User " + userId));
  }

}
