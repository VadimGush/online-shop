package net.thumbtack.onlineshop.controller;

import net.thumbtack.onlineshop.domain.models.Account;
import net.thumbtack.onlineshop.dto.AccountDto;
import net.thumbtack.onlineshop.dto.AdminDto;
import net.thumbtack.onlineshop.dto.ClientDto;
import net.thumbtack.onlineshop.dto.LoginDto;
import net.thumbtack.onlineshop.dto.actions.Edit;
import net.thumbtack.onlineshop.dto.actions.Register;
import net.thumbtack.onlineshop.dto.validation.ValidationException;
import net.thumbtack.onlineshop.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * Контроллер запросов для работы с аккаунтами
 */
@RestController
@RequestMapping("api")
public class AccountController {

    private AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("clients")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto registerClient(
            @RequestBody @Validated(Register.class) ClientDto client,
            BindingResult result,
            HttpServletResponse response) throws Exception {

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        return setSessionAndReturn(accountService.register(client), response);
    }

    @PostMapping("admins")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto registerAdmin(
            @RequestBody @Validated(Register.class) AdminDto admin,
            BindingResult result,
            HttpServletResponse response) throws Exception {

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        return setSessionAndReturn(accountService.register(admin), response);
    }


    @PutMapping("admins")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto editAdmin(
            @CookieValue("JAVASESSIONID") String session,
            @RequestBody @Validated(Edit.class) AdminDto admin,
            BindingResult result) throws Exception {

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        return accountService.edit(session, admin);
    }

    @PutMapping("clients")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto editClient(
            @CookieValue("JAVASESSIONID") String session,
            @RequestBody @Valid ClientDto client,
            BindingResult result) throws Exception {

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        return accountService.edit(session, client);
    }

    @GetMapping("clients")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountDto> getClients(
            @CookieValue("JAVASESSIONID") String session) throws Exception {

        return accountService.getAll(session);
    }

    @GetMapping("accounts")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto getAccount(
            @CookieValue("JAVASESSIONID") String session) throws Exception {

        return accountService.get(session);
    }

    @PostMapping("sessions")
    @ResponseStatus(HttpStatus.OK)
    public AccountDto login(
            @RequestBody @Validated(Register.class) LoginDto account,
            BindingResult result,
            HttpServletResponse response) throws Exception {

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        String session = accountService.login(account.getLogin(), account.getPassword());
        response.addCookie(new Cookie("JAVASESSIONID", session));

        return accountService.get(session);
    }

    @DeleteMapping("sessions")
    @ResponseStatus(HttpStatus.OK)
    public String logout(
            @CookieValue(value = "JAVASESSIONID", required = false) String session) {

        accountService.logout(session);
        return "{}";
    }

    private AccountDto setSessionAndReturn(Pair<Account, String> accountAndSession, HttpServletResponse response) {
        response.addCookie(
                new Cookie(
                        "JAVASESSIONID",
                        accountAndSession.getSecond()
                )
        );
        return new AccountDto(accountAndSession.getFirst());
    }


}
