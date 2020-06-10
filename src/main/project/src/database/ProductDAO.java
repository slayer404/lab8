package src.database;

import com.sun.istack.internal.NotNull;
import src.elements.Coordinates;
import src.elements.Location;
import src.elements.Person;
import src.elements.Product;

import javax.xml.bind.ValidationException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ProductDAO implements DAO<Product, String> {

    private Connection connection;

    ProductDAO(final Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(@NotNull final Product product) {
        try(PreparedStatement statement = connection.prepareStatement(sqlQueries.INSERT.QUERY)) {
            statement.setString(1, product.getName());
            statement.setFloat(2, product.getCoordinates().getX());
            statement.setDouble(3, product.getCoordinates().getY());
            statement.setDate(4, Date.valueOf(product.getCreationDate().toString()));
            statement.setLong(5, product.getPrice());
            statement.setString(6, product.getPartNumber());
            statement.setString(7, product.getUnitOfMeasure().toString());
            statement.setString(8, product.getOwner().getName());
            statement.setInt(9, product.getOwner().getHeight());
            statement.setString(10, product.getOwner().getEyeColor().toString());
            statement.setString(11, product.getOwner().getLocation().getName());
            statement.setLong(12, product.getOwner().getLocation().getX());
            statement.setLong(13, product.getOwner().getLocation().getY());
            statement.setInt(14, product.getOwner().getLocation().getZ());
            statement.executeQuery();
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public Product read(@NotNull final String name) {
        Product product = new Product();
        try(PreparedStatement statement = connection.prepareStatement(sqlQueries.GET.QUERY)) {
            statement.setString(1, name);
        } catch(SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return product;
    }

    public ArrayList<Product> readAll() {
        ArrayList<Product> products = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sqlQueries.GET_ALL.QUERY)) {
            final ResultSet rs = statement.executeQuery();
            while(rs.next()) {
                try {
                Product product = new Product(rs.getString("name"),
                        new Coordinates(rs.getFloat("coordinate_x"), rs.getDouble("coordinate_y")),
                        LocalDate.parse(rs.getString("creation_date")), rs.getLong("price"),
                        rs.getString("part_number"),  rs.getString("unit_of_measure"),
                        new Person(rs.getString("person_name"), rs.getInt("person_height"), rs.getString("person_eyeColor"),
                        new Location(rs.getLong("location_x"), rs.getLong("location_y"), rs.getInt("location_z"), rs.getString("location_name"))));
                product.setId(rs.getInt("id"));
                products.add(product);
                } catch(ValidationException ex) {
                    System.out.println(ex.getMessage());
                }
            }
            rs.close();
        } catch (SQLException ex) {
          //  System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        return products;
    }

    @Override
    public void delete(@NotNull final Product product) {
        try (PreparedStatement statement = connection.prepareStatement(sqlQueries.DELETE.QUERY)) {
            statement.setInt(1, product.getId());
            statement.setString(2, product.getName());
            statement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void update(@NotNull final Product product) {
        try (PreparedStatement statement = connection.prepareStatement(sqlQueries.UPDATE.QUERY)) {
            statement.setString(1, product.getName());
            statement.executeQuery().next();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void clear() {
        try (PreparedStatement statement = connection.prepareStatement(sqlQueries.CLEAR.QUERY)) {
            statement.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    enum sqlQueries {
        INSERT("INSERT INTO products (id, name, coordinate_x, coordinate_y, creation_date, price, part_number, unit_of_measure, person_name, person_height, person_eyeColor, location_name, location_x, location_y, location_z) VALUES (DEFAULT, (?), (?), (?), (?), (?), (?), (?), (?), (?), (?), (?), (?), (?)) RETURNING id"),
        GET("SELECT p.id, p.name, p.coordinate_x, p.coordinate_y, p.creation_date, p.price, p.part_number, p.unit_of_measure, p.person_name, p.person_height, p.person_eyeColor, p.location_name, p.location_x, p.location_y, p.location_z FROM products"),
        UPDATE("UPDATE products SET id = (DEFAULT) WHERE name = (?) RETURNING id"),
        DELETE("DELETE FROM products WHERE id = (?) AND name = (?) RETURNING id"),
        GET_ALL("SELECT * FROM products"),
        CLEAR("TRUNCATE TABLE products");

        String QUERY;

        sqlQueries(String query) {
            this.QUERY = query;
        }
    }
}
