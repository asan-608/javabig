package com.example.javabig;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class TeacherController implements Initializable {

    // —— FXML 注入的控件 —— 
    @FXML private TextArea questionContentArea;
    @FXML private ToggleGroup optionsToggleGroup;
    @FXML private RadioButton optionARadio;
    @FXML private RadioButton optionBRadio;
    @FXML private RadioButton optionCRadio;
    @FXML private RadioButton optionDRadio;
    @FXML private TextField optionATextField;
    @FXML private TextField optionBTextField;
    @FXML private TextField optionCTextField;
    @FXML private TextField optionDTextField;
    @FXML private ChoiceBox<String> difficultyChoiceBox;

    // 当前教师（管理员）ID，需要在登录时赋值
    private long currentTeacherId = 0L;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化难度选项
        difficultyChoiceBox.setItems(
            FXCollections.observableArrayList("简单", "中等", "困难")
        );
        difficultyChoiceBox.setValue("简单");
    }

    /**
     * 点击“添加”按钮时调用，负责收集表单、校验并写库
     */
    @FXML
    private void handleAddChoiceQuestion(ActionEvent event) {
        // 1. 题干
        String content = questionContentArea.getText().trim();
        if (content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "请填写题干");
            return;
        }

        // 2. 四个选项文本
        List<String> options = Arrays.asList(
            optionATextField.getText().trim(),
            optionBTextField.getText().trim(),
            optionCTextField.getText().trim(),
            optionDTextField.getText().trim()
        );
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "请填写选项 " + (char)('A' + i));
                return;
            }
        }

        // 3. 正确答案（由 RadioButton userData 决定）
        Toggle selected = optionsToggleGroup.getSelectedToggle();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "请选择正确答案");
            return;
        }
        int correctIndex = Integer.parseInt(selected.getUserData().toString());
        String correctAnswer = String.valueOf((char)('A' + correctIndex));

        // 4. 难度映射到数据库中的 difficulty_id（传统 switch–case）
        int difficultyId;
        String diff = difficultyChoiceBox.getValue();
        switch (diff) {
            case "中等":
                difficultyId = 2;
                break;
            case "困难":
                difficultyId = 3;
                break;
            default:
                difficultyId = 1;
                break;
        }

        // 5. 题型 ID：单选题假设为 1
        final int SINGLE_CHOICE_TYPE_ID = 1;

        // 6. SQL 语句
        String insertQuestionSql = """
            INSERT INTO questions
              (content, type_id, difficulty_id, correct_answer, explanation, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        String insertOptionSql = """
            INSERT INTO question_options
              (question_id, content, is_correct, sequence)
            VALUES (?, ?, ?, ?)
            """;

        // 7. 数据库写入
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            // 插入 questions，获取自增主键
            long questionId;
            try (PreparedStatement ps = conn.prepareStatement(
                    insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, content);
                ps.setInt(2, SINGLE_CHOICE_TYPE_ID);
                ps.setInt(3, difficultyId);
                ps.setString(4, correctAnswer);
                ps.setString(5, "");  // 解析暂留空
                ps.setLong(6, currentTeacherId);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                questionId = rs.getLong(1);
            }

            // 插入四个选项
            try (PreparedStatement psOpt = conn.prepareStatement(insertOptionSql)) {
                for (int i = 0; i < options.size(); i++) {
                    psOpt.setLong(1, questionId);
                    psOpt.setString(2, options.get(i));
                    psOpt.setBoolean(3, i == correctIndex);
                    psOpt.setInt(4, i + 1);
                    psOpt.addBatch();
                }
                psOpt.executeBatch();
            }

            conn.commit();
            showAlert(Alert.AlertType.INFORMATION, "单选题添加成功！");
            clearFormFields();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "添加失败：" + e.getMessage());
        }
    }

    /** 清空表单，便于下一次输入 */
    private void clearFormFields() {
        questionContentArea.clear();
        optionATextField.clear();
        optionBTextField.clear();
        optionCTextField.clear();
        optionDTextField.clear();
        optionsToggleGroup.getToggles().forEach(t -> t.setSelected(false));
        difficultyChoiceBox.setValue("简单");
    }

    /** 弹窗封装 */
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误"
                        : (type == Alert.AlertType.WARNING ? "警告" : "提示"));
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /** 登录后需由外部设置当前教师 ID */
    public void setCurrentTeacherId(long teacherId) {
        this.currentTeacherId = teacherId;
    }
}
