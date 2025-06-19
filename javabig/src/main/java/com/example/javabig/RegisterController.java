package com.example.javabig;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;

    @FXML
    private void handleRegisterButtonAction() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        String conf = confirmField.getText();

        if (user.isEmpty() || pass.isEmpty() || conf.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "请输入完整信息");
            return;
        }
        if (!pass.equals(conf)) {
            showAlert(Alert.AlertType.WARNING, "两次密码不一致");
            return;
        }

        // 简单 SHA-256 哈希（生产环境建议使用 BCrypt）
        String hash = sha256(pass);

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, hash);
            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "注册成功，请登录");
            // 跳回登录
            MainApp.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "注册失败：" + e.getMessage());
        }
    }

    @FXML
    private void handleShowLoginAction() {
        MainApp.showLogin();
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String sha256(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bs = md.digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bs) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
