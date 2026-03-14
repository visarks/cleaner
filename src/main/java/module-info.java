module com.cleaner {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires java.desktop;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.feather;
    requires java.net.http;

    opens com.cleaner to javafx.fxml;
    opens com.cleaner.controller to javafx.fxml;
    opens com.cleaner.model to com.fasterxml.jackson.databind;
    opens com.cleaner.config to com.fasterxml.jackson.databind;
    opens com.cleaner.update to javafx.fxml;

    exports com.cleaner;
    exports com.cleaner.controller;
    exports com.cleaner.model;
    exports com.cleaner.config;
    exports com.cleaner.update;
}