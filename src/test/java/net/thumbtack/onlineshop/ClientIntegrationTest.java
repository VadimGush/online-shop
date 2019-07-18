package net.thumbtack.onlineshop;

import com.fasterxml.jackson.databind.JsonNode;
import net.thumbtack.onlineshop.dto.*;
import net.thumbtack.onlineshop.service.ServerControlService;
import net.thumbtack.onlineshop.utils.IntegrationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = OnlineShopServer.class
)
@TestPropertySource("classpath:config-test.properties")
@AutoConfigureMockMvc
public class ClientIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServerControlService serverControl;

    private IntegrationUtils utils;

    @Before
    public void cleanDatabase() {
        serverControl.clear();
        utils = new IntegrationUtils(mvc);
    }

    @Test
    public void testRegisterClient() throws Exception {
        // Успешная регистрациия клиента

        ClientDto client = utils.getDefaultClient();
        // Тире должны будут удалится из записи
        client.setPhone("+7-964-995-18-43");

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isOk())
                .andReturn();

        // Проверяем информацию
        JsonNode node = utils.read(result);

        assertNotNull(node.get("id"));
        assertEquals(client.getFirstName(), node.get("firstName").asText());
        assertEquals(client.getLastName(), node.get("lastName").asText());
        assertEquals(client.getPatronymic(), node.get("patronymic").asText());
        assertEquals(client.getEmail(), node.get("email").asText());
        assertEquals("+79649951843", node.get("phone").asText());
        assertEquals(client.getAddress(), node.get("address").asText());
        assertEquals(0, node.get("deposit").asInt());

        assertNull(node.get("login"));
        assertNull(node.get("password"));
    }

    @Test
    public void testRegisterClientWithSameAdminLogin() throws Exception {
        // Нельзя зарегистрировать клиента под тем же логином, что и администратора

        utils.register(utils.getDefaultAdmin());

        ClientDto client = utils.getDefaultClient();
        client.setLogin(utils.getDefaultAdmin().getLogin());

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("LoginInUse", "login"));
    }

    @Test
    public void testRegisterClientWithSameLogin() throws Exception {
        // Нельзя регистрировать клиента с занятым логином

        utils.register(utils.getDefaultClient());

        ClientDto client = utils.getDefaultClient();

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("LoginInUse", "login"));
    }

    @Test
    public void testGetProductWithoutLogin() throws Exception {
        // Пытаемся получить данные о товаре без логина

        MvcResult result = utils.get("/api/products/3", "werew")
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testGetProductNotExist() throws Exception {
        // Пытаемся получить информацию о несуществующей товаре

        String session = utils.register(utils.getDefaultClient());

        MvcResult result = utils.get("/api/products/3", session)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "ProductNotFound");
    }

    @Test
    public void testGetProduct() throws Exception {
        // Получем полную информацию о товаре

        // Создаём товар от имени админа (и пару категорий к этому товару)
        String adminSession = utils.register(utils.getDefaultAdmin());
        long category1 = utils.register(adminSession, new CategoryDto("category1"));
        long category2 = utils.register(adminSession, new CategoryDto("category2"));

        // Количество товара по умолчанию = 0
        long product = utils.register(adminSession,
                utils.getProduct("iphone", 10, Arrays.asList(category1, category2))
        );

        // Теперь получем информацию о товаре от имени клиента
        String session = utils.register(utils.getDefaultClient());
        MvcResult result = utils.get("/api/products/" + product, session)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertEquals(product, node.get("id").asLong());
        assertEquals("iphone", node.get("name").asText());
        assertEquals(10, node.get("price").asInt());
        assertEquals(0, node.get("count").asInt());

        assertEquals(2, node.get("categories").size());
        assertEquals(category1, node.get("categories").get(0).asLong());
        assertEquals(category2, node.get("categories").get(1).asLong());

    }

    @Test
    public void testRegisterWithoutPatronymic() throws Exception {

        // Отчество может отсутствовать, но не может быть пустым
        ClientDto client = utils.getDefaultClient();
        client.setPatronymic("");

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("OptionalRussianName", "patronymic"));
    }

    @Test
    public void testRegisterWithoutPatronymicNull() throws Exception {

        // Отчество может отсутствовать
        ClientDto client = utils.getDefaultClient();
        client.setPatronymic(null);

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertNull(node.get("patronymic"));
    }

    @Test
    public void testRegisterWithRequiredFields() throws Exception {

        ClientDto client = new ClientDto();
        client.setFirstName("");
        client.setLastName("");

        client.setEmail("");
        client.setAddress("");
        client.setPhone("");

        client.setLogin("");
        client.setPassword("");

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrors(result, Arrays.asList(
                Pair.of("RequiredRussianName", "firstName"),
                Pair.of("RequiredRussianName", "lastName"),
                Pair.of("NotBlank", "email"),
                Pair.of("Phone", "phone"),
                Pair.of("NotBlank", "address"),
                Pair.of("Login", "login"),
                Pair.of("Password", "password")
        ));
    }

    @Test
    public void testRegistrationWithWrongFields() throws Exception {

        ClientDto client = new ClientDto();
        // Неверный формат почты
        client.setEmail("vadim.");
        // На адрес не накладывается никаких ограничений
        client.setAddress("dfrer23423423432j2r 2..1121");
        // Пароль не должен быть сликшом коротким
        client.setPassword("ewr");
        // Номер телефона тоже проверяется
        client.setPhone("+79123123");
        // Логин не может содержать знаки препинания
        client.setLogin("vadim!");
        // ФИО не может состоять не из русских букв и пробела
        client.setFirstName("Vadim");
        client.setLastName("Gush");
        client.setPatronymic("Vadimovich");

        MvcResult result = utils.post("/api/clients", null, client)
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode node = utils.read(result);
        assertEquals(7, node.get("errors").size());

        utils.assertErrors(result, Arrays.asList(
                Pair.of("Email", "email"),
                Pair.of("RequiredRussianName", "firstName"),
                Pair.of("RequiredRussianName", "lastName"),
                Pair.of("OptionalRussianName", "patronymic"),
                Pair.of("Phone", "phone"),
                Pair.of("Login", "login"),
                Pair.of("Password", "password")
        ));
    }

    @Test
    public void testLoginAndLogout() throws Exception {

        // Этот самый клиент, который будет зареган через registerClient()
        ClientDto client = utils.getDefaultClient();
        // Региструем пользователя под логином DeNis
        String session = utils.register(client);

        // Logout
        utils.delete("/api/sessions", session)
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));

        // Login
        // Логин не чувствителен к регистру
        MvcResult result = utils.post("/api/sessions", session, new LoginDto(
                "denis", "Denis225"))
                .andExpect(status().isOk())
                .andReturn();

        // Проверяем информацию
        JsonNode node = utils.read(result);

        assertNotNull(node.get("id"));
        assertEquals(client.getFirstName(), node.get("firstName").asText());
        assertEquals(client.getLastName(), node.get("lastName").asText());
        assertEquals(client.getPatronymic(), node.get("patronymic").asText());
        assertEquals(client.getEmail(), node.get("email").asText());
        assertEquals(client.getPhone(), node.get("phone").asText());
        assertEquals(client.getAddress(), node.get("address").asText());
        assertEquals(0, node.get("deposit").asInt());

        assertNull(node.get("login"));
        assertNull(node.get("password"));

    }

    @Test
    public void testLoginWithWrongPassword() throws Exception {

        utils.register(utils.getDefaultClient());

        MvcResult result = utils.post("/api/sessions", null, new LoginDto(
                "denis", "erew2342"))
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "UserNotFound");
    }

    @Test
    public void testLoginWithWrongLogin() throws Exception {

        MvcResult result = utils.post("/api/sessions", null, new LoginDto(
                "ewrw1", "wer2343242"))
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "UserNotFound");
    }

    @Test
    public void testLoginWithEmptyFields() throws Exception {

        MvcResult result = utils.post("/api/sessions", null, new LoginDto())
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrors(result, Arrays.asList(
                Pair.of("Login", "login"),
                Pair.of("Password", "password")
        ));

    }

    @Test
    public void testLogoutWithoutSession() throws Exception {

        // Выход без сессии тоже работает
        utils.delete("/api/sessions", null)
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
    }

    @Test
    public void testGetAccount() throws Exception {

        ClientDto client = utils.getDefaultClient();
        String session = utils.register(client);

        MvcResult result = utils.get("/api/accounts", session)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertNotNull(node.get("id"));
        assertEquals(client.getFirstName(), node.get("firstName").asText());
        assertEquals(client.getLastName(), node.get("lastName").asText());
        assertEquals(client.getPatronymic(), node.get("patronymic").asText());
        assertEquals(client.getEmail(), node.get("email").asText());
        assertEquals(client.getPhone(), node.get("phone").asText());
        assertEquals(client.getAddress(), node.get("address").asText());
        assertEquals(0, node.get("deposit").asInt());

        assertNull(node.get("login"));
        assertNull(node.get("password"));
    }

    @Test
    public void testGetAccountWithoutLogin() throws Exception {
        // Проверяем что с неверной сессией мы данные не получим
        MvcResult result = utils.get("/api/accounts", "erew")
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testGetDeposit() throws Exception {

        ClientDto client = utils.getDefaultClient();
        String session = utils.register(client);

        // Ложим ему деньги на счёт
        utils.put("/api/deposits", session, new DepositDto(12001))
                .andExpect(status().isOk());

        // Запрос get deposit должен вернуть всю информацию
        // о клиенте ровно так же как и запрос на регистрацию
        MvcResult result = utils.get("/api/deposits", session)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertNotNull(node.get("id"));
        assertEquals(client.getFirstName(), node.get("firstName").asText());
        assertEquals(client.getLastName(), node.get("lastName").asText());
        assertEquals(client.getPatronymic(), node.get("patronymic").asText());
        assertEquals(client.getEmail(), node.get("email").asText());
        assertEquals(client.getPhone(), node.get("phone").asText());
        assertEquals(client.getAddress(), node.get("address").asText());
        assertEquals(12001, node.get("deposit").asInt());

        assertNull(node.get("login"));
        assertNull(node.get("password"));
    }

    @Test
    public void testAddDeposit() throws Exception {

        ClientDto client = utils.getDefaultClient();
        String session = utils.register(client);

        MvcResult result = utils.put("/api/deposits", session, new DepositDto(15))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertNotNull(node.get("id"));
        assertEquals(client.getFirstName(), node.get("firstName").asText());
        assertEquals(client.getLastName(), node.get("lastName").asText());
        assertEquals(client.getPatronymic(), node.get("patronymic").asText());
        assertEquals(client.getEmail(), node.get("email").asText());
        assertEquals(client.getPhone(), node.get("phone").asText());
        assertEquals(client.getAddress(), node.get("address").asText());
        assertEquals(15, node.get("deposit").asInt());

        assertNull(node.get("login"));
        assertNull(node.get("password"));
    }

    @Test
    public void testAddDepositNegative() throws Exception {

        String session = utils.register(utils.getDefaultClient());

        MvcResult result = utils.put("/api/deposits", session, new DepositDto(-13))
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("DecimalMin", "deposit"));
    }

    @Test
    public void testGetDepositWithoutLogin() throws Exception {
        MvcResult result = utils.get("/api/deposits", "erew")
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testPutDepositWithoutLogin() throws Exception {
        MvcResult result = utils.put("/api/deposits", "rwer", new DepositDto(34))
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testBuyProductWithoutLogin() throws Exception {

        ProductDto product = utils.getProduct("product", 1, null);

        MvcResult result = utils.post("/api/purchases", "rwe", product)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testBuyProductNotExist() throws Exception {
        // Пытаемся купить товар, которого нет в БД

        String session = utils.register(utils.getDefaultClient());

        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setName("erw");
        product.setPrice(2);

        MvcResult result = utils.post("/api/purchases", session, product)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("ProductNotFound", "id"));
    }

    @Test
    public void testBuyProductWithWrongInfo() throws Exception {
        // Пытаемся купить товар, но неверно указываем данные о нём

        // Создадим товар от имени администратора
        String adminSession = utils.register(utils.getDefaultAdmin());
        ProductDto product = new ProductDto();
        product.setName("iphone");
        product.setPrice(39_999);
        long productId = utils.register(adminSession, product);

        String session = utils.register(utils.getDefaultClient());

        // Неверное имя
        ProductDto toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName("other");
        toBuy.setPrice(product.getPrice());

        MvcResult result = utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("WrongProductInfo", "name"));

        // Неверная цена
        toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName(product.getName());
        toBuy.setPrice(1100);

        result = utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("WrongProductInfo", "price"));
    }

    @Test
    public void testBuyProductNotEnough() throws Exception {
        // Пытаемся купить товара больше чем есть на складе

        // Создадим товар
        String adminSession = utils.register(utils.getDefaultAdmin());
        ProductDto product = new ProductDto();
        product.setName("iphone");
        product.setPrice(39_999);
        product.setCount(3);
        long productId = utils.register(adminSession, product);

        String session = utils.register(utils.getDefaultClient());

        // Купим больше чем есть
        ProductDto toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName(product.getName());
        toBuy.setPrice(product.getPrice());
        toBuy.setCount(4);

        MvcResult result = utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertError(result, Pair.of("NotEnoughProduct", "count"));

    }

    @Test
    public void testBuyProductNotEnoughMoney() throws Exception {
        // Пытаемся купить товара на сумму больше чем наш депозит

        // Создадим товар
        String adminSession = utils.register(utils.getDefaultAdmin());
        ProductDto product = new ProductDto();
        product.setName("iphone");
        product.setPrice(39_999);
        product.setCount(3);
        long productId = utils.register(adminSession, product);

        // Создадим клиента и положим немного денег
        String session = utils.register(utils.getDefaultClient());
        utils.put("/api/deposits", session, new DepositDto(100_000))
                .andExpect(status().isOk());

        // Теперь скупаем весь товар что есть на складе
        ProductDto toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName(product.getName());
        toBuy.setPrice(product.getPrice());
        toBuy.setCount(3);

        MvcResult result = utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotEnoughMoney");
    }

    @Test
    public void testBuyProduct() throws Exception {
        // Покупаем товар

        // Создадим товар
        String adminSession = utils.register(utils.getDefaultAdmin());
        ProductDto product = new ProductDto();
        product.setName("iphone");
        product.setPrice(39_999);
        product.setCount(4);
        long productId = utils.register(adminSession, product);

        // Создадим клиента и положим немного денег
        String session = utils.register(utils.getDefaultClient());
        utils.put("/api/deposits", session, new DepositDto(200_000))
                .andExpect(status().isOk());

        // Теперь скупаем весь товар что есть на складе
        ProductDto toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName(product.getName());
        toBuy.setPrice(product.getPrice());
        toBuy.setCount(3);

        // Проверяем что результат содержит данные о купленном товаре
        MvcResult result = utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);
        assertEquals(productId, node.get("id").asLong());
        assertEquals(product.getName(), node.get("name").asText());
        assertEquals((int)product.getPrice(), node.get("price").asInt());
        assertEquals((int)toBuy.getCount(), node.get("count").asInt());

        // Проверяем что количество денег на счету изменилось
        result = utils.get("/api/deposits", session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);
        assertEquals(200_000 - 3 * 39_999, node.get("deposit").asInt());

        // Проверяем что количество товара на складе изменилось
        result = utils.get("/api/products/" + productId, session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);
        assertEquals(1, node.get("count").asInt());

        // Если мы не указываем сколько хотим купить. то количество равно одному
        toBuy = new ProductDto();
        toBuy.setId(productId);
        toBuy.setName(product.getName());
        toBuy.setPrice(product.getPrice());
        toBuy.setCount(null);

        utils.post("/api/purchases", session, toBuy)
                .andExpect(status().isOk());

        // И товара должно не остаться на складе, так как выкупили последний
        result = utils.get("/api/products/" + productId, session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);
        assertEquals(0, node.get("count").asInt());
    }

    /**
     * Получаем список товаров отсортированных по именам
     * Требования и проверки точно такие же как и в интеграционных администратора
     *
     * Создавать товары мы будем через аккунт админа, а через
     * клиента получать список товаров
     */
    @Test
    public void testGetProductsByProductsNames() throws Exception {

        String adminSession = utils.register(utils.getDefaultAdmin());
        String session = utils.register(utils.getDefaultClient());

        // Подгатавливаем список товаров
        long category = utils.register(adminSession, new CategoryDto("category"));

        long warcraft = utils.register(adminSession,
                utils.getProduct("warcraft", 10_000, null));

        long apple = utils.register(adminSession,
                utils.getProduct("apple", 10_000, Collections.singletonList(category)));

        long berretta = utils.register(adminSession,
                utils.getProduct("berretta", 10_000, null));

        // Создадим удалённый товар и проверим что в список он не попадёт
        long deleted = utils.register(adminSession,
                utils.getProduct("deleted", 10_000, null));

        utils.delete("/api/products/" + deleted, adminSession)
                .andExpect(status().isOk());

        // Теперь получем список всех товаров от имени клиента
        MvcResult result = utils.get("/api/products", session)
                .andExpect(status().isOk())
                .andReturn();

        String firstResult = utils.getContent(result);
        JsonNode node = utils.read(result);

        assertEquals(3, node.size());

        assertEquals(apple, node.get(0).get("id").asLong());
        assertEquals("apple", node.get(0).get("name").asText());
        assertEquals(10_000, node.get(0).get("price").asInt());
        assertEquals(0, node.get(0).get("count").asInt());
        assertEquals(1, node.get(0).get("categories").size());
        assertEquals(category, node.get(0).get("categories").get(0).asLong());

        assertEquals(berretta, node.get(1).get("id").asLong());
        assertEquals("berretta", node.get(1).get("name").asText());
        assertEquals(10_000, node.get(1).get("price").asInt());
        assertEquals(0, node.get(1).get("count").asInt());

        assertEquals(warcraft, node.get(2).get("id").asLong());
        assertEquals("warcraft", node.get(2).get("name").asText());
        assertEquals(10_000, node.get(2).get("price").asInt());
        assertEquals(0, node.get(2).get("count").asInt());

        // Указываем сортировку по товарам явно и проверяем, что ответ будет тот же самый
        // потому что по умолчанию всегда используется сортировка по товарам
        utils.get("/api/products?order=product", session)
                .andExpect(status().isOk())
                .andExpect(content().string(firstResult));

        // Сортировка по товарам, но только товары, которые не принадлежат не одной категории
        result = utils.get("/api/products?order=product&category=", session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);
        assertEquals(2, node.size());
        assertEquals("berretta", node.get(0).get("name").asText());
        assertEquals("warcraft", node.get(1).get("name").asText());

        // Список товаров, которые принадлежат данным категориям
        result = utils.get("/api/products?order=product&category=" + category, session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);
        assertEquals(1, node.size());
        assertEquals("apple", node.get(0).get("name").asText());
    }

    /**
     * Получение списка товаров отсортированных по именам категорий, а внутри
     * категорий по именам товаров
     *
     * Опять же проверки такие же как и в интгеграционных для администратора
     */
    @Test
    public void testGetProductsByCategories() throws Exception {

        String adminSession = utils.register(utils.getDefaultAdmin());
        String session = utils.register(utils.getDefaultClient());

        // Подгатавливаем список
        long pen = utils.register(adminSession,
                utils.getProduct("pen", 10_000, null));

        long array = utils.register(adminSession,
                utils.getProduct("array", 10_000, null));

        // Список категорий для товаров
        long bat = utils.register(adminSession, new CategoryDto("bat"));
        long wat = utils.register(adminSession, new CategoryDto("wat"));
        long at = utils.register(adminSession, new CategoryDto("at"));

        // И пару товаров зависящих от категорий
        utils.register(adminSession,
                utils.getProduct("xen", 10_000, Collections.singletonList(at)));

        utils.register(adminSession,
                utils.getProduct("apple", 10_000, Collections.singletonList(wat)));

        long berretta = utils.register(adminSession,
                utils.getProduct("berretta", 10_000, Arrays.asList(at, wat)));

        utils.register(adminSession,
                utils.getProduct("warcraft", 10_000, Collections.singletonList(bat)));

        // И теперь получем списки от имени клиента
        MvcResult result = utils.get("/api/products?order=category", session)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = utils.read(result);

        /*
        Ожидаем такой порядок:

        Категория   |   Товар
        ---------------------------
                        array
                        pen
        at              berretta
        at              xen
        bat             warcraft
        wat             apple
        wat             berretta

         */

        // Проверяем первые два продукта без категорий

        assertEquals(array, node.get(0).get("id").asLong());
        assertEquals("array", node.get(0).get("name").asText());
        assertEquals(10_000, node.get(0).get("price").asInt());
        assertEquals(0, node.get(0).get("count").asInt());
        assertNull(node.get(0).get("categories"));

        assertEquals(pen, node.get(1).get("id").asLong());
        assertEquals("pen", node.get(1).get("name").asText());
        assertEquals(10_000, node.get(1).get("price").asInt());
        assertEquals(0, node.get(1).get("count").asInt());
        assertNull(node.get(1).get("categories"));

        // Теперь проверяем список с категориями
        // at   -> berretta
        assertEquals(berretta, node.get(2).get("id").asLong());
        assertEquals("berretta", node.get(2).get("name").asText());
        assertEquals(10_000, node.get(2).get("price").asInt());
        assertEquals(0, node.get(2).get("count").asInt());
        assertEquals(1, node.get(2).get("categories").size());
        assertEquals(at, node.get(2).get("categories").get(0).asLong());

        // Теперь будем проверять только именами и категории, так как
        // формат однозначно верный
        // at   -> xen
        assertEquals("xen", node.get(3).get("name").asText());
        assertEquals(1, node.get(3).get("categories").size());
        assertEquals(at, node.get(3).get("categories").get(0).asLong());

        // bat  -> warcraft
        assertEquals("warcraft", node.get(4).get("name").asText());
        assertEquals(1, node.get(4).get("categories").size());
        assertEquals(bat, node.get(4).get("categories").get(0).asLong());

        // wat  -> apple
        assertEquals("apple", node.get(5).get("name").asText());
        assertEquals(1, node.get(5).get("categories").size());
        assertEquals(wat, node.get(5).get("categories").get(0).asLong());

        // wat  -> berretta
        assertEquals("berretta", node.get(6).get("name").asText());
        assertEquals(1, node.get(6).get("categories").size());
        assertEquals(wat, node.get(6).get("categories").get(0).asLong());

        // Сортировка по именам категорий товаров, которые не принадлежат не одной категории
        result = utils.get("/api/products?order=category&category=", session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);

        // Должна быть сортировка по именам первых двух товаров
        assertEquals(2, node.size());
        assertEquals("array", node.get(0).get("name").asText());
        assertEquals("pen", node.get(1).get("name").asText());

        // Сортировка по именам категорий товаров, которые принадлежат категориям
        result = utils.get("/api/products?order=category&category=" + wat + "," + bat, session)
                .andExpect(status().isOk())
                .andReturn();

        node = utils.read(result);

        // Должны получить три товара которые содержат категории
        // wat и bat, отсортированные по категориям, а затем по именам товаров
        assertEquals(3, node.size());
        assertEquals("warcraft", node.get(0).get("name").asText());
        assertEquals("apple", node.get(1).get("name").asText());
        assertEquals("berretta", node.get(2).get("name").asText());

    }


    @Test
    public void testAddProductToBasketWithoutLogin() throws Exception {

        ProductDto product = utils.getProduct("product", 1, null);

        MvcResult result = utils.post("/api/baskets", "erwe", product)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testDeleteFromBasketWithoutLogin() throws Exception {

        MvcResult result = utils.delete("/api/baskets/3", "wrew")
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testEditProductCountWithoutLogin() throws Exception {

        ProductDto product = utils.getProduct("product", null, null);

        MvcResult result = utils.put("/api/baskets", "werwe", product)
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testGetBasketWithoutLogin() throws Exception {

        MvcResult result = utils.get("/api/baskets", "werwe")
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

    @Test
    public void testBuyBasketWithoutLogin() throws Exception {

        MvcResult result = utils.post("/api/purchases/baskets", "wre",
                Collections.singletonList(utils.getProduct("product", null, null)))
                .andExpect(status().isBadRequest())
                .andReturn();

        utils.assertErrorCode(result, "NotLogin");
    }

}
