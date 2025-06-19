package com.example.javabig;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    // 题库管理相关控件
    @FXML private TabPane mainTabPane;
    @FXML private Tab addChoiceTab;
    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, Long> idColumn;
    @FXML private TableColumn<Question, String> contentColumn;
    @FXML private javafx.scene.control.Label teacherNameLabel;
    @FXML private javafx.scene.control.Button topRightButton;

    // 学生管理相关控件
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> studentNameColumn;
    @FXML private TableColumn<Student, String> studentEmailColumn;
    @FXML private TableColumn<Student, String> studentPhoneColumn;
    @FXML private TableColumn<Student, String> studentCreatedColumn;
    @FXML private TableColumn<Student, Void> studentActionColumn;

    // 当前教师（管理员）ID，需要在登录时赋值
    private long currentTeacherId = 0L;
    private String currentTeacherName = "";

    // 编辑状态下的题目 ID，0 表示新建
    private long editingQuestionId = 0L;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化难度选项
        difficultyChoiceBox.setItems(
            FXCollections.observableArrayList("简单", "中等", "困难")
        );
        difficultyChoiceBox.setValue("简单");

        // 表格列绑定
        if (idColumn != null) {
            idColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("questionId"));
        }
        if (contentColumn != null) {
            contentColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        }

        if (studentNameColumn != null) {
            studentNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("username"));
        }
        if (studentEmailColumn != null) {
            studentEmailColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("email"));
        }
        if (studentPhoneColumn != null) {
            studentPhoneColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));
        }
        if (studentCreatedColumn != null) {
            studentCreatedColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("createdAt"));
        }
        if (studentActionColumn != null) {
            studentActionColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.Button editBtn = new javafx.scene.control.Button("修改");
                private final javafx.scene.control.Button delBtn = new javafx.scene.control.Button("删除");
                private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, editBtn, delBtn);
                {
                    editBtn.setOnAction(e -> {
                        Student s = getTableView().getItems().get(getIndex());
                        showEditStudentDialog(s);
                    });
                    delBtn.setOnAction(e -> {
                        Student s = getTableView().getItems().get(getIndex());
                        showDeleteStudentDialog(s);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    /** 加载当前教师的题库到表格 */
    private void loadQuestions() {
        if (questionTable == null) {
            return;
        }
        questionTable.getItems().clear();
        String sql = "SELECT question_id, content FROM questions WHERE created_by=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentTeacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questionTable.getItems().add(new Question(rs.getLong(1), rs.getString(2)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 加载学生列表 */
    private void loadStudents() {
        if (studentTable == null) {
            return;
        }
        studentTable.getItems().clear();
        String sql = "SELECT user_id, username, email, phone, created_at FROM users WHERE role='user'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    studentTable.getItems().add(new Student(
                            rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        // SQL 语句
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

        String updateQuestionSql = """
            UPDATE questions
               SET content=?, difficulty_id=?, correct_answer=?
             WHERE question_id=?
            """;
        String deleteOptionsSql = "DELETE FROM question_options WHERE question_id=?";

        // 数据库写入
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            long questionId = editingQuestionId;
            if (editingQuestionId == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, content);
                    ps.setInt(2, SINGLE_CHOICE_TYPE_ID);
                    ps.setInt(3, difficultyId);
                    ps.setString(4, correctAnswer);
                    ps.setString(5, "");
                    ps.setLong(6, currentTeacherId);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    questionId = rs.getLong(1);
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(updateQuestionSql)) {
                    ps.setString(1, content);
                    ps.setInt(2, difficultyId);
                    ps.setString(3, correctAnswer);
                    ps.setLong(4, editingQuestionId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteOptionsSql)) {
                    ps.setLong(1, editingQuestionId);
                    ps.executeUpdate();
                }
            }

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
            if (editingQuestionId == 0) {
                showAlert(Alert.AlertType.INFORMATION, "单选题添加成功！");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "修改成功！");
            }
            editingQuestionId = 0L;
            clearFormFields();
            loadQuestions();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "添加失败：" + e.getMessage());
        }
    }

    /** 点击删除按钮 */
    @FXML
    private void handleDeleteQuestion(ActionEvent event) {
        Question selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "请先选择题目");
            return;
        }
        String sql1 = "DELETE FROM question_options WHERE question_id=?";
        String sql2 = "DELETE FROM questions WHERE question_id=?";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setLong(1, selected.getQuestionId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                ps.setLong(1, selected.getQuestionId());
                ps.executeUpdate();
            }
            conn.commit();
            loadQuestions();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "删除失败：" + e.getMessage());
        }
    }

    /** 点击编辑按钮，将所选题目填入表单 */
    @FXML
    private void handleEditQuestion(ActionEvent event) {
        Question selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "请先选择题目");
            return;
        }
        String sqlQ = "SELECT content, difficulty_id, correct_answer FROM questions WHERE question_id=?";
        String sqlOpt = "SELECT content, is_correct FROM question_options WHERE question_id=? ORDER BY sequence";
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlQ)) {
                ps.setLong(1, selected.getQuestionId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        questionContentArea.setText(rs.getString(1));
                        int diffId = rs.getInt(2);
                        difficultyChoiceBox.setValue(diffId == 2 ? "中等" : (diffId == 3 ? "困难" : "简单"));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlOpt)) {
                ps.setLong(1, selected.getQuestionId());
                try (ResultSet rs = ps.executeQuery()) {
                    List<TextField> fields = Arrays.asList(optionATextField, optionBTextField, optionCTextField, optionDTextField);
                    List<RadioButton> radios = Arrays.asList(optionARadio, optionBRadio, optionCRadio, optionDRadio);
                    int idx = 0;
                    while (rs.next() && idx < 4) {
                        fields.get(idx).setText(rs.getString(1));
                        boolean correct = rs.getBoolean(2);
                        radios.get(idx).setSelected(correct);
                        idx++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "加载题目失败：" + e.getMessage());
            return;
        }
        editingQuestionId = selected.getQuestionId();
        mainTabPane.getSelectionModel().select(addChoiceTab);
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

    /** 登录后由外部设置当前教师信息 */
    public void setTeacherInfo(long teacherId, String username) {
        this.currentTeacherId = teacherId;
        this.currentTeacherName = username;
        if (teacherNameLabel != null) {
            teacherNameLabel.setText("账号：" + username);
        }
        loadQuestions();
        loadStudents();
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectFirst();
        }
    }

    /** 表格行的数据结构 */
    public static class Question {
        private final long questionId;
        private final String content;

        public Question(long id, String content) {
            this.questionId = id;
            this.content = content;
        }

        public long getQuestionId() {
            return questionId;
        }

        public String getContent() {
            return content;
        }
    }

    /** 学生表格数据结构 */
    public static class Student {
        private final long userId;
        private final String username;
        private final String email;
        private final String phone;
        private final String createdAt;

        public Student(long userId, String username, String email, String phone, String createdAt) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.createdAt = createdAt;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getCreatedAt() { return createdAt; }
    }

    /** 顶部按钮点击 */
    @FXML
    private void handleTopRightButton(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "功能待实现");
    }

    /** 显示编辑学生对话框 */
    private void showEditStudentDialog(Student s) {
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField(s.getUsername());
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField(s.getEmail());
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField(s.getPhone());
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("保存");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("编辑学生信息"),
                nameField, emailField, phoneField, btnBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 300, 250);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("编辑学生");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String sql = "UPDATE users SET username=?, email=?, phone=? WHERE user_id=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setLong(4, s.getUserId());
                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "修改成功");
                dialog.close();
                loadStudents();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "修改失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        dialog.showAndWait();
    }

    /** 删除确认 */
    private void showDeleteStudentDialog(Student s) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "确认删除该学生？", javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                String sql = "DELETE FROM users WHERE user_id=?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, s.getUserId());
                    ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "删除成功");
                    loadStudents();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "删除失败：" + ex.getMessage());
                }
            }
        });
    }
}