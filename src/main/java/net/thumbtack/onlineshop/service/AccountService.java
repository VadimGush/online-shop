package net.thumbtack.onlineshop.service;

import net.thumbtack.onlineshop.domain.dao.AccountDao;
import net.thumbtack.onlineshop.domain.dao.SessionDao;
import net.thumbtack.onlineshop.domain.models.Account;
import net.thumbtack.onlineshop.domain.models.AccountFactory;
import net.thumbtack.onlineshop.domain.models.Session;
import net.thumbtack.onlineshop.dto.AccountDto;
import net.thumbtack.onlineshop.dto.AdminDto;
import net.thumbtack.onlineshop.dto.ClientDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для работы с аккаунтами
 */
@Service
public class AccountService extends GeneralService {

    private AccountDao accountDao;
    private SessionDao sessionDao;

    @Autowired
    public AccountService(AccountDao accountDao, SessionDao sessionDao) {
        super(sessionDao);
        this.accountDao = accountDao;
        this.sessionDao = sessionDao;
    }

    /**
     * Регистрирует нового клиента
     *
     * @param client информация о клиенте
     * @return созданный аккаунт клиента
     */
    public Pair<Account, String> register(ClientDto client) throws ServiceException {

        // Логин не чувствителен к регистру
        client.setLogin(client.getLogin().toLowerCase());

        // Проверяем занят ли он
        if (accountDao.exists(client.getLogin())) {
            throw new ServiceException(ServiceException.ErrorCode.LOGIN_ALREADY_IN_USE, "login");
        }

        client.setPhone(formatPhone(client.getPhone()));

        // Регистрация клиента
        Account registeredClient = AccountFactory.createClient(
                client.getFirstName(),
                client.getLastName(),
                client.getPatronymic(),
                client.getEmail(),
                client.getAddress(),
                client.getPhone(),
                client.getLogin(),
                client.getPassword()
        );
        accountDao.insert(registeredClient);

        // Создание сессии
        Session session = new Session(UUID.randomUUID().toString(), registeredClient);
        sessionDao.insert(session);

        return Pair.of(registeredClient, session.getUUID());

    }

    /**
     * Регистрирует нового администратора
     *
     * @param admin регистрационная инфа об админе
     * @return аккаунт зарегистрированного администратора
     */
    public Pair<Account, String> register(AdminDto admin) throws ServiceException {

        // Логин не чувствителен
        admin.setLogin(admin.getLogin().toLowerCase());

        if (accountDao.exists(admin.getLogin())) {
            throw new ServiceException(ServiceException.ErrorCode.LOGIN_ALREADY_IN_USE, "login");
        }

        // Регистрация админа
        Account registeredAdmin = AccountFactory.createAdmin(
                admin.getFirstName(),
                admin.getLastName(),
                admin.getPatronymic(),
                admin.getPosition(),
                admin.getLogin(),
                admin.getPassword()
        );
        accountDao.insert(registeredAdmin);

        // Создание сессии
        Session session = new Session(UUID.randomUUID().toString(), registeredAdmin);
        sessionDao.insert(session);

        return Pair.of(registeredAdmin, session.getUUID());
    }

    /**
     * Изменяет информацию о клиенте
     *
     * @param sessionId сессия клиента
     * @param client    новая инфа клиента
     * @return аккаунт изменённого клиента
     */
    public AccountDto edit(String sessionId, ClientDto client) throws ServiceException {

        Account account = getClient(sessionId);

        if (!accountDao.isPasswordMatch(account.getId(), client.getOldPassword())) {
            throw new ServiceException(ServiceException.ErrorCode.WRONG_PASSWORD, "oldPassword");
        }

        client.setPhone(formatPhone(client.getPhone()));

        account.setFirstName(client.getFirstName());
        account.setLastName(client.getLastName());
        account.setPatronymic(client.getPatronymic());
        account.setEmail(client.getEmail());
        account.setAddress(client.getAddress());
        account.setPhone(client.getPhone());
        account.setPassword(client.getNewPassword());

        accountDao.update(account);

        return new AccountDto(account);
    }

    /**
     * Изменяет данные администратора
     *
     * @param sessionId сессия администратора
     * @param admin     запрос с изменёнными данными
     * @return аккаунт изменённого администратора
     */
    public AccountDto edit(String sessionId, AdminDto admin) throws ServiceException {

        Account account = getAdmin(sessionId);

        if (!accountDao.isPasswordMatch(account.getId(), admin.getOldPassword())) {
            throw new ServiceException(ServiceException.ErrorCode.WRONG_PASSWORD, "oldPassword");
        }

        account.setFirstName(admin.getFirstName());
        account.setLastName(admin.getLastName());
        account.setPatronymic(admin.getPatronymic());
        account.setPassword(admin.getNewPassword());
        account.setPosition(admin.getPosition());

        accountDao.update(account);
        return new AccountDto(account);
    }

    /**
     * Получает список всех клиентов
     *
     * @param sessionId сессия администратора
     * @return список всех клиентов
     */
    public List<AccountDto> getAll(String sessionId) throws ServiceException {

        getAdmin(sessionId);

        List<Account> clients = accountDao.getClients();
        List<AccountDto> result = new ArrayList<>();

        clients.forEach((client) -> result.add(new AccountDto(client, true)));

        return result;
    }

    /**
     * Проводит авторизацию пользователя по логину и паролю
     *
     * @param login    пароль пользователя
     * @param password логин пользователя
     * @return идентификатор новой сессии
     */
    public String login(String login, String password) throws ServiceException {
        // не чувствителен к регистру
        login = login.toLowerCase();

        Account account = accountDao.get(login, password);

        if (account == null) {
            throw new ServiceException(ServiceException.ErrorCode.USER_NOT_FOUND);
        }

        Session session = new Session(UUID.randomUUID().toString(), account);
        sessionDao.insert(session);
        return session.getUUID();
    }

    /**
     * Получает информацию об аккаунте по его сессии
     *
     * @param sessionId сессия пользователя
     * @return аккаунт пользователя из БД
     */
    public AccountDto get(String sessionId) throws ServiceException {
        Session session = sessionDao.get(sessionId);

        if (session == null) {
            throw new ServiceException(ServiceException.ErrorCode.NOT_LOGIN);
        }

        return new AccountDto(session.getAccount());
    }

    /**
     * Удаляет сессию из БД
     *
     * @param sessionId сессия пользователя
     */
    public void logout(String sessionId) {
        Session session = sessionDao.get(sessionId);

        if (session != null) {
            sessionDao.delete(session);
        }
    }

    private String formatPhone(String phone) {
        return phone
                .replaceAll("-", "")
                .replace("+7", "8");
    }

}
