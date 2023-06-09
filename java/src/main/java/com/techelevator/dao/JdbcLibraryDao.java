package com.techelevator.dao;

import com.techelevator.model.BookDto;
import com.techelevator.model.DuplicateBookException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JdbcLibraryDao implements LibraryDao {

    private JdbcTemplate jdbcTemplate;

    public JdbcLibraryDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public BookDto addBook(BookDto book) throws DuplicateBookException {
        Integer publisherId = null;
        Integer bookId = null;
        String getPublisherIdSql = "SELECT publisher_id FROM publishers WHERE name = ?";
        SqlRowSet row = jdbcTemplate.queryForRowSet(getPublisherIdSql, book.getPublisher());
        if (row.next()) {
            publisherId = row.getInt("publisher_id");
        }
        else {
            String publisherSql = "insert into publishers(name) values(?) RETURNING publisher_id";
            publisherId = jdbcTemplate.queryForObject(publisherSql, Integer.class, book.getPublisher());
        }
        String getBookSql = "SELECT * FROM books WHERE isbn = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(getBookSql, book.getIsbn());
        if (results.next()) {
            throw new DuplicateBookException();
        }
        else {
            String bookSql =
                    "insert into books(title, publisher_id ,date_added, isbn, page_count, description, publish_date, image_link) " +
                            "values(?, ? , current_date, ?, ?, ?, ?, ? ) RETURNING book_id;";
            bookId = jdbcTemplate.queryForObject(bookSql, Integer.class, book.getTitle(),
                    publisherId, book.getIsbn(), book.getPageCount(), book.getDescription(), book.getPublishedDate(), book.getImageLink());
        }
        String[] categories = book.getCategories();
        for (String category : categories) {
            String categorySql = " SELECT category_id FROM categories WHERE name= ?";
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(categorySql, category);
            if (rowSet.next()) {
                Integer categoryId = rowSet.getInt("category_id");
                String bookCategorySql = " insert into books_categories(category_id, book_id) " +
                        "values ( ?, ? );";
                jdbcTemplate.update(bookCategorySql, categoryId, bookId);

            } else{
                String addCategorySql = " insert into categories(name) "  +
                        "values (?) RETURNING category_id; ";
                Integer categoryId = jdbcTemplate.queryForObject(addCategorySql, Integer.class, category);
                String bookCategorySql = " insert into books_categories(category_id, book_id) " +
                        "values ( ?, ? );";
                jdbcTemplate.update(bookCategorySql, categoryId, bookId);
            }

        }
        String[] authors = book.getAuthors();
        for (String author : authors) {
            String authorSql = " SELECT author_id FROM authors WHERE name = ?";
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(authorSql, author);

            if (rowSet.next()) {
                Integer authorId = rowSet.getInt("author_id");
                String bookAuthorSql = " insert into books_authors(author_id, book_id) " +
                        "values ( ?, ? );";
                jdbcTemplate.update(bookAuthorSql, authorId, bookId);

            } else{
                String addCategorySql = " insert into authors(name) "  +
                        "values (?) RETURNING author_id; ";
                Integer authorId = jdbcTemplate.queryForObject(addCategorySql, Integer.class, author);
                String bookCategorySql = " insert into books_authors(author_id, book_id) " +
                        "values ( ?, ? );";
                jdbcTemplate.update(bookCategorySql, authorId, bookId);
            }

        }
        book.setBookId(bookId);

        return book;
    }

    @Override
    public List<BookDto> getBooks() {
        List<BookDto> books = new ArrayList<>();
        String Sql = "SELECT books.book_id, books.title ,  books.date_added ,  books.isbn ,  books.page_count ,  books.description ,  books.publish_date ,  books.image_link, publishers.name " +
                "FROM books " +
                "JOIN publishers ON books.publisher_id = publishers.publisher_id";
        SqlRowSet results = jdbcTemplate.queryForRowSet(Sql);
        while ( results.next()) {
            BookDto book = mapRowToBook(results);
            book.setCategories(getBookCategories(book.getBookId()).toArray(new String[0]));
            book.setAuthors(getBookAuthors(book.getBookId()).toArray(new String[0]));
            books.add(book);
        }
        return books;
    }

    @Override
    public LocalDate addSearchDate(String user) {
        LocalDate searchDate = null;
        String searchSql = "select last_search from users where username = ?";
        searchDate =  jdbcTemplate.queryForObject(searchSql, LocalDate.class, user);
        String updateSearch = "update users set last_search = current_date where username = ?";
        jdbcTemplate.update(updateSearch, user);
        return searchDate;
    }

    @Override
    public LocalDate getUserSearchDate(String username) {
        LocalDate searchDate = null;
        String searchSql = "select last_search from users where username = ?";
        searchDate =  jdbcTemplate.queryForObject(searchSql, LocalDate.class, username);
        return searchDate;
    }

    @Override
    public void addToReadingList(int bookId, int userId) {
        List<BookDto> books = null;
        String updateSql = " INSERT INTO reading_list (user_id, book_id, read) " +
                "VALUES (?, ?, false); ";
        jdbcTemplate.update(updateSql, userId, bookId);
    }
    @Override
    public void deleteFromReadingList(int bookId, int userId){
        String deleteSql = " DELETE FROM reading_list " +
                "WHERE user_id = ? AND book_id = ?;";
        jdbcTemplate.update(deleteSql, userId, bookId);
    }
    @Override
    public List<BookDto> getReadingList(int userId) {
        List<BookDto> getTheReadingList = new ArrayList<>();
        String getReadingListSql = " SELECT books.title ,  books.date_added ,  books.isbn ,  books.page_count ,  " +
                "books.description ,  books.publish_date ,  books.image_link, publishers.name, reading_list.read, books.book_id " +
                "FROM reading_list " +
                "JOIN books ON books.book_id = reading_list.book_id " +
                "JOIN publishers ON books.publisher_id = publishers.publisher_id " +
                "WHERE user_id = ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(getReadingListSql, userId);
        while (rowSet.next()) {
            BookDto book = mapRowToBook(rowSet);
            book.setRead(rowSet.getBoolean("read"));
            book.setCategories(getBookCategories(book.getBookId()).toArray(new String[0]));
            book.setAuthors(getBookAuthors(book.getBookId()).toArray(new String[0]));
            getTheReadingList.add(book);

        }

        return getTheReadingList;
    }

    @Override
    public List<String> getGenres() {
        List<String> genres = new ArrayList<>();
        String genreSql = "SELECT name FROM categories";
        SqlRowSet results = jdbcTemplate.queryForRowSet(genreSql);
        while (results.next()) {
            genres.add(results.getString("name"));
        }
        return genres;
    }


    private BookDto mapRowToBook(SqlRowSet results) {
        BookDto book = new BookDto();
        book.setTitle(results.getString("title"));
        book.setIsbn(results.getString("isbn"));
        book.setPublisher(results.getString("name"));
        book.setDescription(results.getString("description"));
        book.setDateAdded((results.getDate("date_added")));
        book.setPageCount(results.getInt("page_count"));
        book.setPublishedDate(results.getDate("publish_date"));
        book.setImageLink(results.getString("image_link"));
        book.setBookId(results.getInt("book_id"));
        return book;
    }

    private List<String> getBookCategories(int bookId) {

         String categoriesSql = "SELECT name " +
                 "FROM categories " +
                 "JOIN books_categories ON categories.category_id = books_categories.category_id " + "WHERE book_id = ?;";
         SqlRowSet rowResults = jdbcTemplate.queryForRowSet(categoriesSql, bookId);
         List<String> categories = new ArrayList<>();
         while (rowResults.next()) {
             categories.add(rowResults.getString("name"));
         }
         return categories;
     }

     private List<String> getBookAuthors(int bookId) {
         String authorSql = " SELECT name " +
                 "FROM authors " +
                 "JOIN books_authors ON authors.author_id = books_authors.author_id " +
                 "WHERE book_id = ?;";
         SqlRowSet authorRow = jdbcTemplate.queryForRowSet(authorSql, bookId);
         List <String> authors = new ArrayList<>();
         while (authorRow.next()){
             authors.add(authorRow.getString("name"));
         }
         return authors;
     }

}