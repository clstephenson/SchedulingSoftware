package com.clstephenson;

import com.clstephenson.dataaccess.CustomerRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.Locale;

public class Main extends Application{

    public static LoginSession session;

    public static void main(String[] args) throws SQLException {
        //TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        //Locale.setDefault(Locale.ITALY);

        System.out.println("Current Date/Time: " + ZonedDateTime.now());

        List<Appointment> appointmentsNextFifteenMinutes =
                ScheduleManager.getUserAppointmentsStartingSoon("test", 15);
        System.out.println("**Appointments in next 15 minutes**");
        appointmentsNextFifteenMinutes.stream().forEach(System.out::println);
        System.out.println("**End of upcoming appointments**");

        launch(args);
        //testSchedulingNewAppointment();
    }

    @Override
    public void start(Stage primaryStage) {
        Parent root = null;
        try {
            String fxmlPath = AppConfiguration.getConfigurationProperty("fxml.path") + "Main.fxml";
            root = FXMLLoader.load(Paths.get(fxmlPath).toUri().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Could not load Main.fxml", e);
            //todo fix this exception handling
        }
        final int APP_WIDTH = 1000;
        final int APP_HEIGHT = 600;
        Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
        primaryStage.setTitle("Scheduling Application"); //todo localize the title
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    //todo remove this method
    public static void testSchedulingNewAppointment() throws SQLException {
        Customer customer = new CustomerRepository().findSingle(customer1 -> customer1.getId() == 6);
        AppointmentLocation location = AppointmentLocation.PHOENIX;
        LocalDate appointmentDate = LocalDate.now(location.getTimeZoneId());
        LocalTime appointmentStartTime = LocalTime.now(location.getTimeZoneId()).plusMinutes(10);
        LocalTime appointmentEndTime = appointmentStartTime.plusMinutes(30);
        Appointment newAppointment = ScheduleManager.scheduleAppointment(
                customer,
                AppointmentType.INITIAL_CONSULT,
                "this is a description",
                AppointmentLocation.PHOENIX,
                "test",
                "URL test",
                LocalDateTime.of(appointmentDate, appointmentStartTime),
                LocalDateTime.of(appointmentDate, appointmentEndTime),
                session
        );
        System.out.println("Created Appointment: " + newAppointment);
    }

    //todo remove this method
    private static void testAddingCustomerToDatabase() throws SQLException {
        Country country = new Country("My Country");
        City city = new City("My City", country);
        Address address = new Address("123 My Address", "", city, "85255", "112-556-5285");
        Customer customer = new Customer("Chris Test", address, true);
        CustomerRepository customerRepository = new CustomerRepository();
        int custId = customerRepository.add(customer, session);
        System.out.println(customerRepository.findSingle(c -> c.getId() == custId));
    }
}
