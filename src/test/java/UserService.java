import java.util.List;

public interface UserService {
    User queryUser(String userName,String password);
    int addUser(User user);
    void insertUsers(List<User> users);
}
