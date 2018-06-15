package com.clstephenson.dataaccess;

import com.clstephenson.DateTimeUtil;
import com.clstephenson.Localization;
import com.clstephenson.LoginSession;
import com.clstephenson.datamodels.Address;
import com.clstephenson.datamodels.City;
import com.clstephenson.datamodels.Country;
import com.clstephenson.datamodels.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository implements Repository<Customer> {

    private Connection dbConnection;

    public CustomerRepository() {
        try {
            dbConnection = DBManager.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
            //todo fix exception
        }
    }

    @Override
    public int add(Customer customer, LoginSession session) throws SQLException {
        AddressRepository addressRepository = new AddressRepository();
        Address address = addressRepository.findSingle(addr -> addr.getId() == customer.getAddress().getId());
        int addressId;
        if(address == null) {
            addressId = addressRepository.add(customer.getAddress(), session);
            customer.getAddress().setId(addressId); //add the newly generated ID to the customer address
        } else {
            addressId = address.getId();
        }
        if(addressId != 0) {
            String currentUserName = session.getLoggedInUser().getUserName();
            String sql = "INSERT INTO customer (customerName, addressid, active, createDate, createdBy, lastUpdateBy) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try(PreparedStatement statement = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, customer.getName());
                statement.setInt(2, addressId);
                statement.setInt(3, customer.isActive() ? 1 : 0);
                statement.setObject(4, DateTimeUtil.getDateTimeForSQL());
                statement.setString(5, currentUserName);
                statement.setString(6, currentUserName);
                statement.executeUpdate();
                ResultSet rs = statement.getGeneratedKeys();
                if(rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException();
                }
            } catch (SQLException e) {
                throw new SQLException(Localization.getString("error.db.addingcustomer"), e);
            }
        } else {
            // error - address could not be added, therefore customer could not be added
            throw new SQLException(Localization.getString("error.db.addingcustomer"));
        }
    }

    @Override
    public boolean update(Customer customer, LoginSession session) throws SQLException {
        new AddressRepository().update(customer.getAddress(), session);
        String sql = "UPDATE customer set customerName=?, addressId=?, active=?, lastUpdateBy=? WHERE customerid=?";
        try(PreparedStatement statement = dbConnection.prepareStatement(sql)) {
            statement.setString(1, customer.getName());
            statement.setInt(2, customer.getAddress().getId());
            statement.setInt(3, customer.isActive() ? 1 : 0);
            statement.setString(4, session.getLoggedInUser().getUserName());
            statement.setInt(5, customer.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SQLException(Localization.getString("error.db.updatingcustomer"), e);
        }
    }

    @Override
    public boolean removeById(int id) throws SQLException {
        try (CallableStatement statement = dbConnection.prepareCall("{CALL remove_customer(?)}")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            String message = Localization.getString("error.db.removingcustomer") + " = " + id;
            throw new SQLException(message, e);
        }
    }

    @Override
    public boolean remove(Customer customer) throws SQLException {
        return removeById(customer.getId());
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        String query = "SELECT * FROM customer_view";
        ArrayList<Customer> customers = new ArrayList<>();
        try (Statement statement = dbConnection.createStatement()) {
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                customers.add(mapResultSetToObject(rs));
            }
            return customers;
        } catch (SQLException e) {
            String message = Localization.getString("error.db.customerquery");
            throw new SQLException(message, e);
        }
    }

    @Override
    public Customer findById(int id) {
        try {
            return findSingle(customer -> customer.getId() == id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
            //todo fix exception
        }
    }

    private Customer mapResultSetToObject(ResultSet rs) throws SQLException {
        int customerId = rs.getInt("customerid");
        String customerName = rs.getString("customerName");
        boolean isActive = rs.getInt("active") > 0;
        Country country = new Country(rs.getInt("countryid"), rs.getString("country"));
        City city = new City(rs.getInt("cityid"), rs.getString("city"), country);
        Address address = new Address(
                rs.getInt("addressid"),
                rs.getString("address"),
                rs.getString("address2"),
                city,
                rs.getString("postalCode"),
                rs.getString("phone"));
        return new Customer(customerId, customerName, address, isActive);
    }

}
