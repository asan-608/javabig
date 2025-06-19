package com.example.javabig;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.example.javabig.TeacherController;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLoginButtonAction() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            alert(Alert.AlertType.WARNING, "请输入用户名和密码");
            return;
        }

        String hash = sha256(pass);
        String sql = "SELECT user_id, role FROM users WHERE username=? AND password_hash=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("user_id");
                    String role = rs.getString("role");
                    alert(Alert.AlertType.INFORMATION, "登录成功，欢迎 " + user + "！");

                    // 根据角色跳转
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    Parent root;
                    if ("admin".equals(role)) {
                        // 教师窗口
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("teacher-view.fxml"));
                        root = loader.load();
                        TeacherController controller = loader.getController();
                        controller.setTeacherInfo(userId, user);
                    } else {
                        // 其他角色主界面（示例为 main-view.fxml）
                        root = FXMLLoader.load(getClass().getResource("main-view.fxml"));
                    }
                    stage.setScene(new Scene(root));
                } else {
                    alert(Alert.AlertType.ERROR, "用户名或密码错误");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "登录异常：" + e.getMessage());
        }
    }

    @FXML
    private void handleShowRegisterAction() {
        MainApp.showRegister();
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bs = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bs) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
