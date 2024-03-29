package net.thumbtack.onlineshop.controller;

import net.thumbtack.onlineshop.domain.models.Account;
import net.thumbtack.onlineshop.dto.AccountDto;
import net.thumbtack.onlineshop.dto.DepositDto;
import net.thumbtack.onlineshop.dto.ProductDto;
import net.thumbtack.onlineshop.dto.ResultBasketDto;
import net.thumbtack.onlineshop.dto.validation.ValidationException;
import net.thumbtack.onlineshop.service.AccountService;
import net.thumbtack.onlineshop.service.ClientService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ClientControllerTest {

    private ClientController controller;

    @Mock
    private ClientService mockClientService;

    @Mock
    private AccountService mockAccountService;

    @Mock
    private BindingResult mockResult;

    @Before
    public void setUpClass() {
        MockitoAnnotations.initMocks(this);

        controller = new ClientController(mockClientService, mockAccountService);
    }

    @Test
    public void testPutDeposit() throws Exception {

        DepositDto deposit = new DepositDto();
        deposit.setDeposit(1);

        AccountDto expected = new AccountDto();
        when(mockResult.hasErrors()).thenReturn(false);
        when(mockClientService.putDeposit("token", 1)).thenReturn(expected);

        AccountDto result = controller.putDeposit("token", deposit, mockResult);

        verify(mockClientService).putDeposit("token", 1);
        assertEquals(expected, result);
    }

    @Test(expected = ValidationException.class)
    public void testPutDepositValidation() throws Exception {

        DepositDto deposit = new DepositDto();
        when(mockResult.hasErrors()).thenReturn(true);

        try {
            controller.putDeposit("token", deposit, mockResult);
        } catch (ValidationException e) {
            verify(mockClientService, never()).putDeposit(any(), anyInt());
            throw e;
        }
    }

    @Test
    public void testGetDeposit() throws Exception {

        Account account = new Account();
        account.setFirstName("Vadim");
        account.setLastName("Gush");

        when(mockAccountService.getAccount("token")).thenReturn(account);

        AccountDto result = controller.getDeposit("token");

        assertEquals(account.getFirstName(), result.getFirstName());
        assertEquals(account.getLastName(), result.getLastName());

    }

    @Test
    public void testBuyProduct() throws Exception {

        ProductDto product = new ProductDto();
        ProductDto expected = new ProductDto();
        when(mockResult.hasErrors()).thenReturn(false);
        when(mockClientService.buyProduct("token", product))
                .thenReturn(expected);

        ProductDto result = controller.buyProduct("token", product, mockResult);

        verify(mockClientService).buyProduct("token", product);
        assertEquals(1, (int)product.getCount());
        assertEquals(expected, result);

    }

    @Test(expected = ValidationException.class)
    public void testBuyProductValidation() throws Exception {

        ProductDto product = new ProductDto();
        when(mockResult.hasErrors()).thenReturn(true);

        try {
            controller.buyProduct("token", product, mockResult);
        } catch (ValidationException e) {
            verify(mockClientService, never()).buyProduct(any(), any());
            throw e;
        }
    }

    @Test
    public void testAddToBasket() throws Exception {

        ProductDto product = new ProductDto();
        List<ProductDto> expected = new ArrayList<>();

        when(mockResult.hasErrors()).thenReturn(false);
        when(mockClientService.addToBasket("token", product))
                .thenReturn(expected);

        List<ProductDto> result = controller.addToBasket("token", product, mockResult);

        verify(mockClientService).addToBasket("token", product);
        assertEquals(expected, result);
        assertEquals(1, (int)product.getCount());

    }

    @Test(expected = ValidationException.class)
    public void testAddToBasketValidation() throws Exception {

        ProductDto product = new ProductDto();
        when(mockResult.hasErrors()).thenReturn(true);

        try {
            controller.addToBasket("token", product, mockResult);
        } catch (ValidationException e) {
            verify(mockClientService, never()).addToBasket(any(), any());
            throw e;
        }

    }

    @Test
    public void testDeleteFromBasket() throws Exception {

        String result = controller.deleteFromBasket("token", 1);

        assertEquals("{}", result);
        verify(mockClientService).deleteFromBasket("token", 1);
    }

    @Test
    public void testEditProductCount() throws Exception {

        ProductDto product = new ProductDto();
        List<ProductDto> expected = new ArrayList<>();
        when(mockResult.hasErrors()).thenReturn(false);
        when(mockClientService.editProductCount("token", product))
                .thenReturn(expected);

        List<ProductDto> result = controller.editProductCount("token", product, mockResult);

        verify(mockClientService).editProductCount("token", product);
        assertEquals(expected, result);
    }

    @Test(expected = ValidationException.class)
    public void testEditProductCountValidation() throws Exception {

        when(mockResult.hasErrors()).thenReturn(true);

        try {
            controller.editProductCount("token", null, mockResult);
        } catch (ValidationException e) {
            verify(mockClientService, never()).editProductCount(any(), any());
            throw e;
        }
    }

    @Test
    public void testGetBasket() throws Exception {
        List<ProductDto> expected = new ArrayList<>();

        when(mockClientService.getBasket("token")).thenReturn(expected);

        List<ProductDto> result = controller.getBasket("token");

        verify(mockClientService).getBasket("token");
        assertEquals(expected, result);
    }

    @Test
    public void testBuyBasket() throws Exception {
        List<ProductDto> buy = new ArrayList<>();
        ResultBasketDto expected = new ResultBasketDto();
        when(mockResult.hasErrors()).thenReturn(false);
        when(mockClientService.buyBasket("token", buy))
                .thenReturn(expected);

        ResultBasketDto result = controller.buyBasket("token", buy, mockResult);

        verify(mockClientService).buyBasket("token", buy);
        assertEquals(expected, result);
    }

    @Test(expected = ValidationException.class)
    public void testBuyBasketValidation() throws Exception {

        when(mockResult.hasErrors()).thenReturn(true);

        try {
            controller.buyBasket("token", null, mockResult);
        } catch (ValidationException e) {
            verify(mockClientService, never()).buyBasket(any(), any());
            throw e;
        }
    }

}
