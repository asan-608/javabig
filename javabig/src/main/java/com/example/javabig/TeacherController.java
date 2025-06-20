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
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;

import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.palette.ColorPalette;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.collide.CollisionMode;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.wordstart.CenterWordStart;
import com.kennycason.kumo.WordFrequency;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

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
    @FXML private TableColumn<Question, String> typeColumn;
    @FXML private TableColumn<Question, String> authorColumn;
    @FXML private javafx.scene.control.Label teacherNameLabel;
    @FXML private javafx.scene.control.Button topRightButton;
    @FXML private ImageView wordCloudView;

    // 题型管理相关控件
    @FXML private Tab typeManageTab;
    @FXML private TableView<QuestionType> typeTable;
    @FXML private TableColumn<QuestionType, String> typeNameColumn;
    @FXML private TableColumn<QuestionType, Integer> typeScoreColumn;
    @FXML private TableColumn<QuestionType, Void> typeActionColumn;

    // 学生管理相关控件
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> studentNameColumn;
    @FXML private TableColumn<Student, String> studentEmailColumn;
    @FXML private TableColumn<Student, String> studentPhoneColumn;
    @FXML private TableColumn<Student, String> studentCreatedColumn;
    @FXML private TableColumn<Student, Void> studentActionColumn;

    // 试卷管理相关控件
    @FXML private TableView<ExamPaper> paperTable;
    @FXML private TableColumn<ExamPaper, String> paperTitleColumn;
    @FXML private TableColumn<ExamPaper, String> paperDescColumn;
    @FXML private TableColumn<ExamPaper, String> paperAuthorColumn;
    @FXML private TableColumn<ExamPaper, String> paperCreatedColumn;
    @FXML private TableColumn<ExamPaper, Void> paperActionColumn;

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
        if (typeColumn != null) {
            typeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("typeName"));
        }
        if (authorColumn != null) {
            authorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
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

        if (paperTitleColumn != null) {
            paperTitleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        }
        if (paperDescColumn != null) {
            paperDescColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        }
        if (paperAuthorColumn != null) {
            paperAuthorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
        }
        if (paperCreatedColumn != null) {
            paperCreatedColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("createdAt"));
        }
        if (paperActionColumn != null) {
            paperActionColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.Button delBtn = new javafx.scene.control.Button("删除");
                {
                    delBtn.setOnAction(e -> {
                        ExamPaper p = getTableView().getItems().get(getIndex());
                        handleDeletePaper(p);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : delBtn);
                }
            });
        }

        if (typeNameColumn != null) {
            typeNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        }
        if (typeScoreColumn != null) {
            typeScoreColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("score"));
        }
        if (typeActionColumn != null) {
            typeActionColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.Button editBtn = new javafx.scene.control.Button("修改分数");
                private final javafx.scene.control.Button addBtn = new javafx.scene.control.Button("新增题目");
                private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, editBtn, addBtn);
                {
                    editBtn.setOnAction(e -> {
                        QuestionType qt = getTableView().getItems().get(getIndex());
                        showEditTypeDialog(qt);
                    });
                    addBtn.setOnAction(e -> {
                        QuestionType qt = getTableView().getItems().get(getIndex());
                        showAddQuestionDialog(qt);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        loadQuestionTypes();
        loadExamPapers();
    }

    /** 加载当前教师的题库到表格 */
    private void loadQuestions() {
        if (questionTable == null) {
            return;
        }
        questionTable.getItems().clear();
        String sql = """
            SELECT q.question_id, q.content, u.username, qt.name
              FROM questions q
              JOIN users u ON q.created_by = u.user_id
              JOIN question_types qt ON q.type_id = qt.type_id
             WHERE q.created_by=?
        """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentTeacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questionTable.getItems().add(
                        new Question(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4))
                    );
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

    /** 加载题型列表 */
    private void loadQuestionTypes() {
        if (typeTable == null) {
            return;
        }
        typeTable.getItems().clear();
        String sql = "SELECT type_id, code, name, score FROM question_types";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                typeTable.getItems().add(new QuestionType(
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 加载试卷列表 */
    private void loadExamPapers() {
        if (paperTable == null) {
            return;
        }
        paperTable.getItems().clear();
        String sql = """
            SELECT p.paper_id, p.title, p.description, u.username, p.created_at
              FROM exam_papers p
              JOIN users u ON p.created_by = u.user_id
            """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                paperTable.getItems().add(new ExamPaper(
                        rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 从所有题目内容生成词云 */
    private void loadWordCloud() {
        if (wordCloudView == null) {
            return;
        }
        String sql = "SELECT content FROM questions";
        java.util.List<String> texts = new java.util.ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                texts.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        java.util.List<WordFrequency> wordFrequencies = analyzer.load(texts);
        Dimension dimension = new Dimension(600, 400);
        WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new RectangleBackground(dimension));
        wordCloud.setFontScalar(new SqrtFontScalar(10, 40));
        wordCloud.setBackgroundColor(new Color(255,255,255,0));
        wordCloud.setColorPalette(new ColorPalette(new Color(64,69,241), new Color(64,141,241), new Color(0x408DF1), new Color(0x40A7F1)));
        wordCloud.setWordStartStrategy(new CenterWordStart());
        wordCloud.build(wordFrequencies);
        BufferedImage img = wordCloud.getBufferedImage();
        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(img, null);
        wordCloudView.setImage(fxImage);
    }

    /** 点击新增题型按钮 */
    @FXML
    private void handleAddType(ActionEvent event) {
        javafx.scene.control.TextField codeField = new javafx.scene.control.TextField();
        codeField.setPromptText("题型编码");
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        nameField.setPromptText("题型名称");
        javafx.scene.control.TextField scoreField = new javafx.scene.control.TextField();
        scoreField.setPromptText("分值");
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("保存");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("新增题型"), codeField, nameField, scoreField, btnBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 300, 250);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("新增题型");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String s = scoreField.getText().trim();
            if (code.isEmpty() || name.isEmpty() || s.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "请填写完整信息");
                return;
            }
            int score;
            try { score = Integer.parseInt(s); } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "分值必须为数字");
                return;
            }
            String sql = "INSERT INTO question_types (code, name, score) VALUES (?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code);
                ps.setString(2, name);
                ps.setInt(3, score);
                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "新增成功");
                dialog.close();
                loadQuestionTypes();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "新增失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());
        dialog.showAndWait();
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
            loadWordCloud();
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
            loadWordCloud();
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
        loadExamPapers();
        loadWordCloud();
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectFirst();
        }
    }

    /** 表格行的数据结构 */
    public static class Question {
        private final long questionId;
        private final String content;
        private final String author;
        private final String typeName;

        public Question(long id, String content, String author, String typeName) {
            this.questionId = id;
            this.content = content;
            this.author = author;
            this.typeName = typeName;
        }

        public long getQuestionId() { return questionId; }
        public String getContent() { return content; }
        public String getAuthor() { return author; }
        public String getTypeName() { return typeName; }
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

    /** 试卷表格数据结构 */
    public static class ExamPaper {
        private final long paperId;
        private final String title;
        private final String description;
        private final String author;
        private final String createdAt;

        public ExamPaper(long paperId, String title, String description, String author, String createdAt) {
            this.paperId = paperId;
            this.title = title;
            this.description = description;
            this.author = author;
            this.createdAt = createdAt;
        }

        public long getPaperId() { return paperId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public String getCreatedAt() { return createdAt; }
    }

    /** 题型表格数据结构 */
    public static class QuestionType {
        private final int typeId;
        private final String code;
        private final String name;
        private final int score;

        public QuestionType(int typeId, String code, String name, int score) {
            this.typeId = typeId;
            this.code = code;
            this.name = name;
            this.score = score;
        }

        public int getTypeId() { return typeId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public int getScore() { return score; }
    }

    /** 顶部按钮点击 */
    @FXML
    private void handleTopRightButton(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "功能待实现");
    }

    /** 编辑题型分值对话框 */
    private void showEditTypeDialog(QuestionType qt) {
        javafx.scene.control.TextField scoreField = new javafx.scene.control.TextField(String.valueOf(qt.getScore()));
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("保存");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("修改分值 - " + qt.getName()),
                scoreField, btnBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 250, 180);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("修改分值");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String s = scoreField.getText().trim();
            int score;
            try { score = Integer.parseInt(s); } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "分值必须为数字");
                return;
            }
            String sql = "UPDATE question_types SET score=? WHERE type_id=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, score);
                ps.setInt(2, qt.getTypeId());
                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "修改成功");
                dialog.close();
                loadQuestionTypes();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "修改失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    /** 新增题目对话框，根据题型不同呈现 */
    private void showAddQuestionDialog(QuestionType qt) {
        javafx.scene.control.TextArea contentArea = new javafx.scene.control.TextArea();
        contentArea.setPromptText("题干");
        javafx.scene.control.ChoiceBox<String> diffBox = new javafx.scene.control.ChoiceBox<>(
                FXCollections.observableArrayList("简单", "中等", "困难"));
        diffBox.setValue("简单");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.getChildren().addAll(new javafx.scene.control.Label("新增题目 - " + qt.getName()), contentArea);

        java.util.List<javafx.scene.control.TextField> optionFields = new java.util.ArrayList<>(java.util.Arrays.asList(
                new javafx.scene.control.TextField(),
                new javafx.scene.control.TextField(),
                new javafx.scene.control.TextField(),
                new javafx.scene.control.TextField()));

        // Toggle groups declared here for use inside event handlers
        javafx.scene.control.ToggleGroup tg = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.ToggleGroup tfGroup = new javafx.scene.control.ToggleGroup();

        switch (qt.getCode()) {
            case "single_choice":
                javafx.scene.layout.HBox optBox = new javafx.scene.layout.HBox(20);
                char c1 = 'A';
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(5);
                    javafx.scene.control.RadioButton rb = new javafx.scene.control.RadioButton(String.valueOf((char)(c1 + i)));
                    rb.setToggleGroup(tg);
                    rb.setUserData(i);
                    optionFields.get(i).setPromptText("选项 " + (char)(c1 + i));
                    vb.getChildren().addAll(rb, optionFields.get(i));
                    optBox.getChildren().add(vb);
                }
                root.getChildren().addAll(new javafx.scene.control.Label("选项"), optBox);
                break;
            case "multiple_choice":
                javafx.scene.layout.HBox optBox2 = new javafx.scene.layout.HBox(20);
                char c2 = 'A';
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(5);
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(String.valueOf((char)(c2 + i)));
                    cb.setUserData(i);
                    optionFields.get(i).setPromptText("选项 " + (char)(c2 + i));
                    vb.getChildren().addAll(cb, optionFields.get(i));
                    optBox2.getChildren().add(vb);
                }
                root.getChildren().addAll(new javafx.scene.control.Label("选项"), optBox2);
                break;
                case "fill_blank":
                optionFields.clear();
                javafx.scene.control.TextField answerField = new javafx.scene.control.TextField();
                answerField.setPromptText("答案");
                optionFields.add(answerField);
                root.getChildren().add(new javafx.scene.control.Label("答案"));
                root.getChildren().add(answerField);
                break;
            case "true_false":
                javafx.scene.control.RadioButton trueBtn = new javafx.scene.control.RadioButton("正确");
                trueBtn.setToggleGroup(tfGroup);
                trueBtn.setUserData("T");
                javafx.scene.control.RadioButton falseBtn = new javafx.scene.control.RadioButton("错误");
                falseBtn.setToggleGroup(tfGroup);
                falseBtn.setUserData("F");
                javafx.scene.layout.HBox tfBox = new javafx.scene.layout.HBox(10, trueBtn, falseBtn);
                root.getChildren().add(new javafx.scene.control.Label("正确答案"));
                root.getChildren().add(tfBox);
                break;
        }

        root.getChildren().add(diffBox);
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("保存");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(btnBox);

        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 350);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("新增题目");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String content = contentArea.getText().trim();
            if (content.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "请填写题干");
                return;
            }

            int difficultyId;
            switch (diffBox.getValue()) {
                case "中等": difficultyId = 2; break;
                case "困难": difficultyId = 3; break;
                default: difficultyId = 1; break;
            }

            String insertQ = "INSERT INTO questions (content, type_id, difficulty_id, correct_answer, explanation, created_by) VALUES (?, ?, ?, ?, ?, ?)";
            String insertOpt = "INSERT INTO question_options (question_id, content, is_correct, sequence) VALUES (?, ?, ?, ?)";

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                long qid;
                try (PreparedStatement ps = conn.prepareStatement(insertQ, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, content);
                    ps.setInt(2, qt.getTypeId());
                    ps.setInt(3, difficultyId);
                    String correctAnswer = "";

                    if ("single_choice".equals(qt.getCode())) {
                        Toggle selected = tg.getSelectedToggle();
                        if (selected == null) { showAlert(Alert.AlertType.WARNING, "请选择正确答案"); return; }
                        correctAnswer = String.valueOf((char)('A' + Integer.parseInt(selected.getUserData().toString())));
                    } else if ("multiple_choice".equals(qt.getCode())) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            javafx.scene.control.CheckBox cb = (javafx.scene.control.CheckBox)((javafx.scene.layout.VBox) ((javafx.scene.layout.HBox)root.getChildren().get(2)).getChildren().get(i)).getChildren().get(0);
                            if (cb.isSelected()) sb.append((char)('A' + i));
                        }
                        if (sb.length() == 0) { showAlert(Alert.AlertType.WARNING, "请选择正确答案"); return; }
                        correctAnswer = sb.toString();
                    } else if ("fill_blank".equals(qt.getCode())) {
                        correctAnswer = optionFields.get(0).getText().trim();
                        if (correctAnswer.isEmpty()) { showAlert(Alert.AlertType.WARNING, "请填写答案"); return; }
                    } else if ("true_false".equals(qt.getCode())) {
                        Toggle sel = tfGroup.getSelectedToggle();
                        if (sel == null) { showAlert(Alert.AlertType.WARNING, "请选择答案"); return; }
                        correctAnswer = sel.getUserData().toString();
                    }

                    ps.setString(4, correctAnswer);
                    ps.setString(5, "");
                    ps.setLong(6, currentTeacherId);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    qid = rs.getLong(1);
                }

                if ("single_choice".equals(qt.getCode()) || "multiple_choice".equals(qt.getCode())) {
                    try (PreparedStatement ps = conn.prepareStatement(insertOpt)) {
                        for (int i = 0; i < 4; i++) {
                            ps.setLong(1, qid);
                            ps.setString(2, optionFields.get(i).getText().trim());
                            boolean correct;
                            if ("single_choice".equals(qt.getCode())) {
                                Toggle selected = tg.getSelectedToggle();
                                correct = selected != null && Integer.parseInt(selected.getUserData().toString()) == i;
                            } else {
                                javafx.scene.control.CheckBox cb = (javafx.scene.control.CheckBox)((javafx.scene.layout.VBox)((javafx.scene.layout.HBox)root.getChildren().get(2)).getChildren().get(i)).getChildren().get(0);
                                correct = cb.isSelected();
                            }
                            ps.setBoolean(3, correct);
                            ps.setInt(4, i + 1);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "新增成功");
                dialog.close();
                loadQuestions();
                loadWordCloud();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "新增失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        dialog.showAndWait();
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

    /** 手动新增试卷按钮 */
    @FXML
    private void handleAddPaperManual(ActionEvent event) {
        showAddPaperDialog(false);
    }

    /** 自动组卷按钮 */
    @FXML
    private void handleAddPaperAuto(ActionEvent event) {
        showAddPaperDialog(true);
    }

    /** 删除试卷 */
    private void handleDeletePaper(ExamPaper paper) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "确认删除该试卷？", javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                String sql1 = "DELETE FROM paper_questions WHERE paper_id=?";
                String sql2 = "DELETE FROM exam_papers WHERE paper_id=?";
                try (Connection conn = DBUtil.getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                        ps.setLong(1, paper.getPaperId());
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                        ps.setLong(1, paper.getPaperId());
                        ps.executeUpdate();
                    }
                    conn.commit();
                    loadExamPapers();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "删除失败：" + ex.getMessage());
                }
            }
        });
    }

    /** 新增试卷对话框 */
    private void showAddPaperDialog(boolean auto) {
        javafx.scene.control.TextField titleField = new javafx.scene.control.TextField();
        titleField.setPromptText("试卷名称");
        javafx.scene.control.TextField descField = new javafx.scene.control.TextField();
        descField.setPromptText("试卷描述");
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("保存");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, titleField, descField);

        javafx.scene.control.Spinner<Integer> countSpinner = new javafx.scene.control.Spinner<>(1, 50, 5);
        java.util.List<javafx.scene.control.CheckBox> checkBoxes = new java.util.ArrayList<>();

        if (auto) {
            root.getChildren().add(new javafx.scene.control.Label("题目数量"));
            root.getChildren().add(countSpinner);
        } else {
            javafx.scene.layout.VBox qBox = new javafx.scene.layout.VBox(5);
            String sql = "SELECT question_id, content FROM questions WHERE created_by=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, currentTeacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(rs.getString(2));
                        cb.setUserData(rs.getLong(1));
                        checkBoxes.add(cb);
                        qBox.getChildren().add(cb);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(qBox);
            sp.setPrefHeight(200);
            root.getChildren().add(sp);
        }

        root.getChildren().add(btnBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 350, auto ? 220 : 350);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("新增试卷");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { showAlert(Alert.AlertType.WARNING, "请填写名称"); return; }
            String desc = descField.getText().trim();
            String insertPaper = "INSERT INTO exam_papers (title, description, created_by) VALUES (?, ?, ?)";
            String insertPQ = "INSERT INTO paper_questions (paper_id, question_id, sequence) VALUES (?, ?, ?)";
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                long pid;
                try (PreparedStatement ps = conn.prepareStatement(insertPaper, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, desc);
                    ps.setLong(3, currentTeacherId);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    pid = rs.getLong(1);
                }
                java.util.List<Long> qIds = new java.util.ArrayList<>();
                if (auto) {
                    String sqlRand = "SELECT question_id FROM questions ORDER BY RAND() LIMIT ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlRand)) {
                        ps.setInt(1, countSpinner.getValue());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) qIds.add(rs.getLong(1));
                        }
                    }
                } else {
                    for (javafx.scene.control.CheckBox cb : checkBoxes) {
                        if (cb.isSelected()) qIds.add((Long) cb.getUserData());
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insertPQ)) {
                    int seq = 1;
                    for (Long qid : qIds) {
                        ps.setLong(1, pid);
                        ps.setLong(2, qid);
                        ps.setInt(3, seq++);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "新增成功");
                dialog.close();
                loadExamPapers();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "新增失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        dialog.showAndWait();
    }
}