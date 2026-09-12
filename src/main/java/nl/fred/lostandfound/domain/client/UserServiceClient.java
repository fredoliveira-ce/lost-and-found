package nl.fred.lostandfound.domain.client;

@FunctionalInterface
public interface UserServiceClient {

    UserInfo findUser(Long userId);

}
