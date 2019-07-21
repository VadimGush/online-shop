package net.thumbtack.onlineshop.service;

import net.thumbtack.onlineshop.domain.dao.AccountDao;
import net.thumbtack.onlineshop.domain.dao.SessionDao;
import net.thumbtack.onlineshop.domain.models.Account;
import net.thumbtack.onlineshop.domain.models.AccountFactory;
import net.thumbtack.onlineshop.domain.models.Session;
import net.thumbtack.onlineshop.dto.AccountDto;
import net.thumbtack.onlineshop.dto.AdminDto;
import net.thumbtack.onlineshop.dto.ClientDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static net.thumbtack.onlineshop.utils.TestUtils.createAdminEditDto;
import static net.thumbtack.onlineshop.utils.TestUtils.createClientEditDto;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    private AccountService accountService;

    @Mock
    private AccountDao mockAccountDao;

    @Mock
    private SessionDao mockSessionDao;

    @Before
    public void setUpClass() {
        MockitoAnnotations.initMocks(this);
        accountService = new AccountService(mockAccountDao, mockSessionDao);
    }

    @Test
    public void testRegistration() throws ServiceException {

        // Проверяем регистрацию администратора
        Account admin = generateAdmin();

        when(mockAccountDao.exists(admin.getLogin())).thenReturn(false);

        Account result = accountService.register(new AdminDto(admin));

        verify(mockAccountDao).insert(any());

        assertTrue(result.isAdmin());
        assertEquals(admin.getFirstName(), result.getFirstName());
        assertEquals(admin.getLastName(), result.getLastName());
        assertEquals(admin.getPatronymic(), result.getPatronymic());
        assertEquals(admin.getPosition(), result.getPosition());
        assertEquals(admin.getLogin(), result.getLogin());
        assertEquals(admin.getPassword(), result.getPassword());

        // Проверяем регистрацию без отчества

        admin = generateAdmin();
        admin.setPatronymic(null);
        result = accountService.register(new AdminDto(admin));

        verify(mockAccountDao, times(2)).insert(any());

        assertNull(result.getPatronymic());
    }

    @Test(expected = ServiceException.class)
    public void testRegisterWithSameLogin() throws ServiceException {
        // Проверяем что администратора с тем же логином создать нельзя
        Account admin =  generateAdmin();

        try {
            // Login already in use
            when(mockAccountDao.exists("vadim")).thenReturn(true);

            accountService.register(new AdminDto(
                    admin.getFirstName(), admin.getLastName(), admin.getPatronymic(),
                    admin.getPosition(), admin.getLogin(), admin.getPassword()
            ));

        } catch (ServiceException e) {
            // Записиси пользователя не произошло
            verify(mockAccountDao, never()).insert(admin);

            assertEquals(ServiceException.ErrorCode.LOGIN_ALREADY_IN_USE, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testEdit() throws ServiceException {
        Account admin = AccountFactory.createAdmin(
                "werewrwe", "ewrwe", "werew", "erer2", "vadim", "23"
        );
        when(mockSessionDao.get("token")).thenReturn(new Session("token", admin));

        AccountDto result = accountService.edit("token", createAdminEditDto(
                "name", "lastName", "patro", "pos", "23", "33"
        ));

        verify(mockAccountDao).update(any());

        assertEquals("name", result.getFirstName());
        assertEquals("lastName", result.getLastName());
        assertEquals("patro", result.getPatronymic());
        assertEquals("pos", result.getPosition());
    }

    @Test(expected = ServiceException.class)
    public void testEditWithWrongPassword() throws ServiceException {

        Account admin = AccountFactory.createAdmin(
                "werewrwe", "ewrwe", "werew", "erer2", "vadim", "23"
        );
        when(mockSessionDao.get("token")).thenReturn(new Session("token", admin));

        try {
            accountService.edit("token", createAdminEditDto(
                    "name", "lastName", "patro", "pos", "43", "33"
            ));

            verify(mockAccountDao, never()).update(any());
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.WRONG_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testGetAll() throws ServiceException {
        List<Account> clients = new ArrayList<>();
        clients.add(generateClient());
        clients.add(generateClient());

        when(mockAccountDao.getClients()).thenReturn(clients);
        Account admin = generateAdmin();
        when(mockSessionDao.get("token")).thenReturn(new Session("token", admin));

        List<AccountDto> result = accountService.getAll("token");
        assertEquals(clients.size(), result.size());
        // ТЗ пункт 3.7
        assertEquals("client", result.get(0).getUserType());
        assertEquals("client", result.get(1).getUserType());
        assertNull(result.get(0).getDeposit());
        assertNull(result.get(1).getDeposit());

        verify(mockAccountDao).getClients();
    }

    @Test(expected = ServiceException.class)
    public void testNotLoginEdit() throws ServiceException {
        when(mockSessionDao.get("token")).thenReturn(null);

        try {
            accountService.edit("token", new AdminDto());

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).update(any());

            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = ServiceException.class)
    public void testNotAdminEdit() throws ServiceException {
        when(mockSessionDao.get("token")).thenReturn(new Session("token", generateClient()));

        try {
            accountService.edit("token", new AdminDto());

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).update(any());

            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = ServiceException.class)
    public void testNotLoginGetAll() throws ServiceException {
        when(mockSessionDao.get("token")).thenReturn(null);

        try {
            accountService.getAll("token");

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).getClients();

            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
            throw e;
        }
    }

    @Test(expected = ServiceException.class)
    public void testNotAdminGetAll() throws ServiceException {
        when(mockSessionDao.get("token")).thenReturn(new Session("token", generateClient()));

        try {
            accountService.getAll("token");

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).update(any());

            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testClientRegister() throws ServiceException {

        // Проверяем регистрацию как обычно

        Account client = generateClient();

        when(mockAccountDao.exists(client.getLogin())).thenReturn(false);

        Account result = accountService.register(new ClientDto(client));

        verify(mockAccountDao).insert(any());

        assertEquals(client.getFirstName(), result.getFirstName());
        assertEquals(client.getLastName(), result.getLastName());
        assertEquals(client.getPatronymic(), result.getPatronymic());
        assertEquals(client.getDeposit(), result.getDeposit());
        assertEquals(client.getEmail(), result.getEmail());
        assertEquals(client.getAddress(), result.getAddress());
        assertEquals(client.getPhone(), result.getPhone());
        assertFalse(result.isAdmin());

        // Проверяем регистрацию без отчества

        client = generateClient();
        client.setPatronymic(null);

        result = accountService.register(new ClientDto(client));

        verify(mockAccountDao, times(2)).insert(any());

        assertNull(result.getPatronymic());
    }

    @Test(expected = ServiceException.class)
    public void testClientRegisterWithSameLogin() throws ServiceException {
        Account client = generateClient();

        when(mockAccountDao.exists(client.getLogin())).thenReturn(true);

        try {
            accountService.register(new ClientDto(client));

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).insert(client);
            assertEquals(ServiceException.ErrorCode.LOGIN_ALREADY_IN_USE, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testClientEdit() throws ServiceException {

        Account client = generateClient();

        when(mockSessionDao.get("token")).thenReturn(new Session("token", client));

        ClientDto edited = createClientEditDto(
                "new name", "new last name", "new patro",
                "new email", "new address", "new phone", client.getPassword(),
                "new password"
        );

        AccountDto result = accountService.edit("token", edited);

        verify(mockAccountDao).update(client);

        assertEquals(edited.getFirstName(), result.getFirstName());
        assertEquals(edited.getLastName(), result.getLastName());
        assertEquals(edited.getPatronymic(), result.getPatronymic());
        assertEquals(edited.getAddress(), result.getAddress());
        assertEquals(edited.getEmail(), result.getEmail());
        assertEquals(edited.getPhone(), result.getPhone());
    }

    @Test(expected = ServiceException.class)
    public void testClientEditWrongPassword() throws ServiceException {

        Account client = generateClient();

        when(mockSessionDao.get("token")).thenReturn(new Session("token", client));

        ClientDto edited = createClientEditDto(
                "new name", "new last name", "new patro",
                "new email", "new address", "new phone", "wrong",
                "new password"
        );

        try {
            accountService.edit("token", edited);

        } catch (ServiceException e) {
            verify(mockAccountDao, never()).update(client);
            assertEquals(ServiceException.ErrorCode.WRONG_PASSWORD, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testLogin() throws ServiceException {
        Account account = AccountFactory.createAdmin(
                "werew", "werwr", "werwe", "werew", "wer"
        );
        when(mockAccountDao.get("login", "password")).thenReturn(account);

        String token = accountService.login("login", "password");
        verify(mockSessionDao).insert(any());
        assertNotNull(token);
    }

    @Test(expected = ServiceException.class)
    public void testClientLoginWithWrongCred() throws ServiceException {

        when(mockAccountDao.get("login", "password")).thenReturn(null);

        try {
            accountService.login("login", "password");
        } catch (ServiceException e) {
            verify(mockSessionDao, never()).insert(any());
            assertEquals(ServiceException.ErrorCode.USER_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    @Test
    public void testLogout() {
        Session session = new Session();
        when(mockSessionDao.get("token")).thenReturn(session);

        accountService.logout("token");

        verify(mockSessionDao).delete(session);
    }

    @Test
    public void testLogoutWrongSession() {
        when(mockSessionDao.get("token")).thenReturn(null);

        accountService.logout("token");

        verify(mockSessionDao, never()).delete(any());
    }

    @Test
    public void testGetAccount() throws ServiceException {
        Account account = AccountFactory.createAdmin(
                "rwer", "werew", "werew", "werw", "werew"
        );
        when(mockSessionDao.get("token")).thenReturn(new Session("token", account));

        AccountDto result = accountService.get("token");
        assertEquals(account.getFirstName(), result.getFirstName());
        assertEquals(account.getLastName(), result.getLastName());
        assertEquals(account.getPosition(), result.getPosition());
    }

    @Test(expected = ServiceException.class)
    public void testGetAccountWrongSession() throws ServiceException {
        when(mockSessionDao.get("token")).thenReturn(null);

        try {
            accountService.get("token");
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
            throw e;
        }
    }

    private Account generateClient() {
        return AccountFactory.createClient(
                "rewrw", "sder", "werew", "ewrwe", "wrwe", "werwe", "werw"
        );
    }

    private Account generateAdmin() {
        return AccountFactory.createAdmin(
                "vadim", "gush", "vadimovich", "coder", "vadim", "Iddqd225"
        );
    }


}
