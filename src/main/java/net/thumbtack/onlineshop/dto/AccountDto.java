package net.thumbtack.onlineshop.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.thumbtack.onlineshop.domain.models.Account;

/**
 * AccountDto - DTO только для сериализации на отправку клиенту
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private String address;
    private String phone;
    private String position;
    private String userType;
    private Integer deposit;

    public AccountDto() {

    }

    public AccountDto(Account account) {
        this.id = account.getId();
        this.firstName = account.getFirstName();
        this.lastName = account.getLastName();
        this.patronymic = account.getPatronymic();
        this.email = account.getEmail();
        this.address = account.getAddress();
        this.phone = account.getPhone();
        this.position = account.getPosition();
        this.deposit = account.getDeposit();
    }

    public AccountDto(Account account, boolean hide) {
        this.id = account.getId();
        this.firstName = account.getFirstName();
        this.lastName = account.getLastName();
        this.patronymic = account.getPatronymic();
        this.email = account.getEmail();
        this.address = account.getAddress();
        this.phone = account.getPhone();
        this.position = account.getPosition();

        // ТЗ пункт 3.7
        if (!hide)
            this.deposit = account.getDeposit();
        else
            this.userType = "client";
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDeposit() {
        return deposit;
    }

    public void setDeposit(Integer deposit) {
        this.deposit = deposit;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @JsonIgnore
    public String getFullName() {
        String fullName = lastName + " " + firstName;

        if (patronymic != null) {
            fullName = fullName + " " + patronymic;
        }

        return fullName;
    }
}
