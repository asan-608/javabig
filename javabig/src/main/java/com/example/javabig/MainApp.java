package com.example.javabig;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLogin();
        primaryStage.show();
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login-view.fxml"));
            primaryStage.setTitle("登录");
            primaryStage.setScene(new Scene(loader.load(), 315, 385));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("register-view.fxml"));
            primaryStage.setTitle("注册");
            primaryStage.setScene(new Scene(loader.load(), 315, 385));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}