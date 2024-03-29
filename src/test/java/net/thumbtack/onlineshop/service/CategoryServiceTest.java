package net.thumbtack.onlineshop.service;

import net.thumbtack.onlineshop.domain.dao.CategoryDao;
import net.thumbtack.onlineshop.domain.dao.SessionDao;
import net.thumbtack.onlineshop.domain.models.Account;
import net.thumbtack.onlineshop.domain.models.AccountFactory;
import net.thumbtack.onlineshop.domain.models.Category;
import net.thumbtack.onlineshop.domain.models.Session;
import net.thumbtack.onlineshop.dto.CategoryDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CategoryServiceTest {

    private CategoriesService categoryService;

    @Mock
    private SessionDao mockSessionDao;

    @Mock
    private CategoryDao mockCategoryDao;

    @Before
    public void setUpClass() {
        MockitoAnnotations.initMocks(this);
        categoryService = new CategoriesService(
                mockSessionDao, mockCategoryDao
        );
    }


    /**
     * Добавление категории
     */
    @Test
    public void testAddCategory() throws ServiceException {
        setAdmin();

        // Такой категории ещё не было
        when(mockCategoryDao.exists("category")).thenReturn(false);

        CategoryDto category = categoryService.addCategory("token", new CategoryDto(
                "category"
        ));

        verify(mockCategoryDao).insert(any());

        assertEquals("category", category.getName());
        assertNull(category.getParentId());
        assertNull(category.getParentName());
    }

    /**
     * Нельзя добавить категорию с тем же именем
     */
    @Test(expected = ServiceException.class)
    public void testAddCategorySameName() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.exists("category")).thenReturn(true);

        try {
            categoryService.addCategory("token", new CategoryDto("category"));
        } catch (ServiceException e) {
            verify(mockCategoryDao, never()).insert(any());
            verify(mockCategoryDao, never()).update(any());
            assertEquals(ServiceException.ErrorCode.SAME_CATEGORY_NAME, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Нельзя добавить категорию с несуществующим родителем
     */
    @Test(expected = ServiceException.class)
    public void testAddCategoryWithWrongParent() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.exists("category")).thenReturn(false);
        when(mockCategoryDao.get(0)).thenReturn(null);

        try {
            categoryService.addCategory("token", new CategoryDto("category", 0));
        } catch (ServiceException e) {
            verify(mockCategoryDao, never()).insert(any());
            verify(mockCategoryDao, never()).update(any());
            assertEquals(ServiceException.ErrorCode.CATEGORY_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Нельзя добавить подкатегорию для подкатегории (подкатегория второго уровня)
     */
    @Test(expected = ServiceException.class)
    public void testAddSubcategoryOfSubcategory() throws ServiceException {
        setAdmin();

        Category category = new Category("category", new Category("parent"));

        when(mockCategoryDao.exists("category")).thenReturn(false);
        when(mockCategoryDao.get(0)).thenReturn(category);

        try {
            categoryService.addCategory("token", new CategoryDto("category", 0));
        } catch (ServiceException e) {
            verify(mockCategoryDao, never()).insert(any());
            verify(mockCategoryDao, never()).update(any());
            assertEquals(ServiceException.ErrorCode.SECOND_SUBCATEGORY, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Получение информации о категории
     */
    @Test
    public void testGetCategory() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.get(0)).thenReturn(
                new Category("category", new Category("parent"))
        );
        CategoryDto result = categoryService.getCategory("token", 0);

        assertEquals("category", result.getName());
        assertEquals("parent", result.getParentName());
    }

    /**
     * Нельзя получить информацию о несуществующей категории
     */
    @Test(expected = ServiceException.class)
    public void testGetCategoryNotExist() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.get(0)).thenReturn(null);

        try {
            categoryService.getCategory("token", 0);
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.CATEGORY_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Редактирование категории с переносом в другую категорию
     */
    @Test
    public void testEditCategory() throws ServiceException {
        setAdmin();

        // Меняем подкатегорию и переносим её в другую категорию

        Category category = new Category("category", new Category("parent"));
        Category parent = new Category("another parent");
        when(mockCategoryDao.get(0)).thenReturn(category);
        when(mockCategoryDao.get(1)).thenReturn(parent);

        CategoryDto result = categoryService.editCategory("token", new CategoryDto(
                "new name", 1L
        ), 0);
        verify(mockCategoryDao).update(any());
        verify(mockCategoryDao, never()).insert(any());
        assertEquals("new name", result.getName());
        assertEquals("another parent", result.getParentName());

    }

    /**
     * Редактирование категории с простым изменением имени
     */
    @Test
    public void testEditCategory2() throws ServiceException {
        setAdmin();

        // Меняем имя категории
        Category category = new Category("category", new Category("parent"));
        when(mockCategoryDao.get(0)).thenReturn(category);

        CategoryDto result = categoryService.editCategory("token", new CategoryDto("new name"), 0);
        verify(mockCategoryDao).update(any());
        verify(mockCategoryDao, never()).insert(any());
        assertEquals("new name", result.getName());
        assertEquals("parent", result.getParentName());
    }

    /**
     * Редактирование родительской категории
     */
    @Test
    public void testEditCategory3() throws ServiceException {
        setAdmin();

        // Просто меняем родителя у категори
        Category category = new Category("category", new Category("parent"));
        Category parent = new Category("another");
        when(mockCategoryDao.get(0)).thenReturn(category);
        when(mockCategoryDao.get(1)).thenReturn(parent);

        CategoryDto result = categoryService.editCategory("token", new CategoryDto(1L), 0);
        verify(mockCategoryDao).update(any());
        verify(mockCategoryDao, never()).insert(any());
        assertEquals("category", result.getName());
        assertEquals("another", result.getParentName());

    }

    /**
     * Нельзя редактировать несуществующую категорию
     */
    @Test(expected = ServiceException.class)
    public void testEditCategoryNotFound() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.get(0)).thenReturn(null);

        try {
            categoryService.editCategory("token", new CategoryDto("some shit"), 0);
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.CATEGORY_NOT_FOUND, e.getErrorCode());
            throw e;
        }

    }

    /**
     * Нельзя категории присвоить несуществующего родителя
     */
    @Test(expected = ServiceException.class)
    public void testEditCategoryParentCategoryNotFound() throws ServiceException {
        setAdmin();

        Category category = new Category("category", new Category("parent"));
        when(mockCategoryDao.get(0)).thenReturn(category);
        when(mockCategoryDao.get(1)).thenReturn(null);

        try {
            categoryService.editCategory("token", new CategoryDto(1L), 0);
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.CATEGORY_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Нельзя редактировать категорию без указания родительской
     */
    @Test(expected = ServiceException.class)
    public void testEditCategoryParentCategoryEmpty() throws ServiceException {
        setAdmin();

        Category category = new Category("category", new Category("parent"));
        when(mockCategoryDao.get(0)).thenReturn(category);
        when(mockCategoryDao.get(1)).thenReturn(null);

        try {
            categoryService.editCategory("token", new CategoryDto(), 0);
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.EDIT_CATEGORY_EMPTY, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Нельзя родительскую категорию сделать подкатегорией
     */
    @Test(expected = ServiceException.class)
    public void testEditCategoryCategoryToSubCategory() throws ServiceException {
        setAdmin();

        Category category = new Category("category");
        Category parent = new Category("parent");
        when(mockCategoryDao.get(0)).thenReturn(category);
        when(mockCategoryDao.get(1)).thenReturn(parent);

        try {
            categoryService.editCategory("token", new CategoryDto(1L), 0);
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.CATEGORY_TO_SUBCATEGORY, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Удаление категории
     */
    @Test
    public void testDeleteCategory() throws ServiceException {
        setAdmin();

        Category category = new Category("category");
        when(mockCategoryDao.get(0)).thenReturn(category);

        categoryService.deleteCategory("token", 0);
        verify(mockCategoryDao).delete(any());
    }

    /**
     * Нельзя удалить несуществующую категорию
     */
    @Test(expected = ServiceException.class)
    public void testDeleteWrongCategory() throws ServiceException {
        setAdmin();

        when(mockCategoryDao.get(0)).thenReturn(null);

        try {
            categoryService.deleteCategory("token", 0);
        } catch (ServiceException e) {
            verify(mockCategoryDao, never()).delete(any());
            assertEquals(ServiceException.ErrorCode.CATEGORY_NOT_FOUND, e.getErrorCode());
            throw e;
        }
    }

    /**
     * Получаем список категорий
     */
    @Test
    public void testGetCategories() throws ServiceException {
        setAdmin();

        List<Category> categories = Arrays.asList(
                new Category("category1"),
                new Category("category2")
        );
        when(mockCategoryDao.getAll()).thenReturn(categories);

        List<CategoryDto> result = categoryService.getCategories("token");

        assertEquals(categories.size(), result.size());
    }


    /**
     * Нельзя вызвать методы без логина, которые требуют авторизации пользователя
     */
    @Test
    public void testNotLogin() {

        when(mockSessionDao.get("token")).thenReturn(null);

        try {
            categoryService.addCategory("token", new CategoryDto());
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
        }

        try {
            categoryService.getCategory("token", 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
        }

        try {
            categoryService.getCategories("token");
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
        }

        try {
            categoryService.editCategory("token", null, 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
        }

        try {
            categoryService.deleteCategory("token", 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_LOGIN, e.getErrorCode());
        }

    }

    /**
     * Нельзя вызвать методы изменения категорий от имени клиента
     */
    @Test
    public void testNotAdmin() {

        when(mockSessionDao.get("token")).thenReturn(new Session("token",
                AccountFactory.createClient(
                        "ewrwe", "werew", "werwe", "werwe", "werwee",
                        "werwer", "wer"
                )
        ));

        try {
            categoryService.addCategory("token", new CategoryDto());
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
        }

        try {
            categoryService.getCategory("token", 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
        }

        try {
            categoryService.getCategories("token");
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
        }

        try {
            categoryService.editCategory("token", null, 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
        }

        try {
            categoryService.deleteCategory("token", 0);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.ErrorCode.NOT_ADMIN, e.getErrorCode());
        }

    }

    private void setAdmin() {
        when(mockSessionDao.get("token")).thenReturn(new Session("token", generateAdmin()));
    }

    private Account generateAdmin() {
        return AccountFactory.createAdmin(
                "vadim", "gush", "vadimovich", "coder", "vadim", "Iddqd225"
        );
    }
}
