package com.example.javabig;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.palette.ColorPalette;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.wordstart.CenterWordStart;
import com.kennycason.kumo.WordFrequency;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.security.MessageDigest;

public class StudentController implements Initializable {
    @FXML private Label studentNameLabel;
    @FXML private TableView<ExamPaper> examTable;
    @FXML private TableColumn<ExamPaper, String> titleColumn;
    @FXML private TableColumn<ExamPaper, String> descColumn;
    @FXML private TableColumn<ExamPaper, String> authorColumn;
    @FXML private TableColumn<ExamPaper, String> createdColumn;
    @FXML private TableColumn<ExamPaper, Void> actionColumn;
    @FXML private TableView<PaperResult> scoreTable;
    @FXML private TableColumn<PaperResult, String> scorePaperColumn;
    @FXML private TableColumn<PaperResult, String> scoreValueColumn;
    @FXML private TableColumn<PaperResult, String> scoreTimeColumn;
    @FXML private TextArea aiConversationArea;
    @FXML private TextField aiInputField;
    @FXML private Label infoUsernameLabel;
    @FXML private Label infoEmailLabel;
    @FXML private Label infoPhoneLabel;
    @FXML private Label infoCreatedLabel;
    @FXML private LineChart<String, Number> scoreLineChart;
    @FXML private ImageView wordCloudView;
    @FXML private PieChart typePieChart;
    @FXML private BarChart<String, Number> scoreBarChart;

    private static final String AI_API_KEY = "sk-dbhV140jGebFvQbT2B2KxfnEzZdrzQRmaYoxzMgVcErTXACh";
    private static final String AI_API_URL = "https://xiaoai.plus/v1/chat/completions";

    private long currentUserId = 0L;
    private String currentUsername = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (titleColumn != null) {
            titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        }
        if (descColumn != null) {
            descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        }
        if (authorColumn != null) {
            authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        }
        if (createdColumn != null) {
            createdColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        }
        if (actionColumn != null) {
            actionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("开始考试");
                {
                    btn.setOnAction(e -> {
                        ExamPaper p = getTableView().getItems().get(getIndex());
                        openExamWindow(p);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }
        if (scorePaperColumn != null) {
            scorePaperColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        }
        if (scoreValueColumn != null) {
            scoreValueColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        }
        if (scoreTimeColumn != null) {
            scoreTimeColumn.setCellValueFactory(new PropertyValueFactory<>("takenAt"));
        }
        loadExamPapers();
        loadScores();
        loadWordCloud();
        loadQuestionTypeStats();
        loadScoreTrend();
        loadScoreDistribution();
    }

    private void loadExamPapers() {
        if (examTable == null) return;
        examTable.getItems().clear();
        String sql = """
            SELECT p.paper_id, p.title, p.description, u.username, p.created_at
              FROM exam_papers p
              JOIN users u ON p.created_by = u.user_id
            """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                examTable.getItems().add(new ExamPaper(
                        rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScores() {
        if (scoreTable == null) return;
        scoreTable.getItems().clear();
        String sql = "SELECT p.paper_id, p.title, er.score, er.taken_at " +
                "FROM exam_papers p LEFT JOIN exam_results er " +
                "ON p.paper_id=er.paper_id AND er.user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer s = (Integer) rs.getObject(3);
                    String scoreText = s == null ? "未开始" : String.valueOf(s);
                    String time = rs.getString(4);
                    if (time == null) time = "未开始";
                    scoreTable.getItems().add(new PaperResult(
                            rs.getLong(1), rs.getString(2), scoreText, time));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadProfile() {
        if (infoUsernameLabel == null) return;
        String sql = "SELECT username, email, phone, created_at FROM users WHERE user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    infoUsernameLabel.setText("用户名: " + rs.getString(1));
                    infoEmailLabel.setText("邮箱: " + rs.getString(2));
                    infoPhoneLabel.setText("手机号: " + rs.getString(3));
                    infoCreatedLabel.setText("注册时间: " + rs.getString(4));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScoreTrend() {
        if (scoreLineChart == null) return;
        scoreLineChart.getData().clear();
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        String sql = "SELECT taken_at, score FROM exam_results WHERE user_id=? ORDER BY taken_at";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>(rs.getString(1), rs.getInt(2)));
                            // 查询并填充成绩随时间变化的折线图数据
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        scoreLineChart.getData().add(series);
    }

    private void loadQuestionTypeStats() {
        if (typePieChart == null) return;
        typePieChart.getData().clear();
        String sql = "SELECT qt.name, COUNT(*) FROM questions q JOIN question_types qt ON q.type_id=qt.type_id GROUP BY qt.name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                typePieChart.getData().add(new PieChart.Data(rs.getString(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScoreDistribution() {
        if (scoreBarChart == null) return;
        scoreBarChart.getData().clear();
                // 统计各试卷的平均分并填充柱状图
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        String sql = "SELECT p.title, AVG(er.score) FROM exam_papers p LEFT JOIN exam_results er ON p.paper_id=er.paper_id GROUP BY p.paper_id, p.title";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(rs.getString(1), rs.getDouble(2)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        scoreBarChart.getData().add(series);
    }

    private void loadWordCloud() {
        if (wordCloudView == null) return;
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
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.otf")) {
            if (fontStream != null) {
                wordCloud.setKumoFont(new KumoFont(fontStream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void openExamWindow(ExamPaper paper) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exam-view.fxml"));
            Parent root = loader.load();
            ExamController controller = loader.getController();
            controller.startExam(paper.getPaperId(), paper.getTitle(), currentUserId);
            Stage stage = new Stage();
            stage.setTitle(paper.getTitle());
            stage.setScene(new Scene(root, 600, 450));
            stage.setOnHiding(e -> {
                loadScores();
                loadScoreTrend();
                loadScoreDistribution();
            });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStudentInfo(long id, String username) {
        this.currentUserId = id;
        this.currentUsername = username;
        if (studentNameLabel != null) {
            studentNameLabel.setText("账号：" + username);
        }
        loadScores();
        loadProfile();
        loadScoreTrend();
        loadWordCloud();
        loadQuestionTypeStats();
        loadScoreDistribution();
    }

    @FXML
    private void handleSendMessage() {
        if (aiInputField == null || aiConversationArea == null) return;
        String text = aiInputField.getText().trim();
        if (text.isEmpty()) return;
        aiConversationArea.appendText("我: " + text + "\n");
        aiInputField.clear();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return requestAi(text);
            }
        };
        task.setOnSucceeded(e -> {
            String reply = task.getValue();
            if (reply != null) {
                aiConversationArea.appendText("AI: " + reply + "\n");
            }
        });
        task.setOnFailed(e -> aiConversationArea.appendText("AI 请求失败\n"));
        new Thread(task).start();
    }

    private String requestAi(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String escaped = prompt.replace("\"", "\\\"");
        String json = String.format("{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}", escaped);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + AI_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray choices = obj.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (msg != null && msg.has("content")) {
                return msg.get("content").getAsString().trim();
            }
        }
        return null;
    }

    @FXML
    private void handleLogout() {
        MainApp.showLogin();
    }

    @FXML
    private void handleEditInfo() {
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField(currentUsername);
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField();

        // load current values
        String sql = "SELECT email, phone FROM users WHERE user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    emailField.setText(rs.getString(1));
                    phoneField.setText(rs.getString(2));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
            String updateSql = "UPDATE users SET username=?, email=?, phone=? WHERE user_id=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                ps2.setString(1, name);
                ps2.setString(2, email);
                ps2.setString(3, phone);
                ps2.setLong(4, currentUserId);
                ps2.executeUpdate();
                currentUsername = name;
                if (studentNameLabel != null) {
                    studentNameLabel.setText("账号：" + name);
                }
                loadProfile();
                showAlert(Alert.AlertType.INFORMATION, "修改成功");
                dialog.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "修改失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    @FXML
    private void handleChangePassword() {
        javafx.scene.control.PasswordField oldField = new javafx.scene.control.PasswordField();
        oldField.setPromptText("原密码");
        javafx.scene.control.PasswordField newField = new javafx.scene.control.PasswordField();
        newField.setPromptText("新密码");
        javafx.scene.control.PasswordField confirmField = new javafx.scene.control.PasswordField();
        confirmField.setPromptText("确认新密码");
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("修改");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("取消");
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("修改密码"),
                oldField, newField, confirmField, btnBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("registerForm");
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 300, 250);
        scene.getStylesheets().add(getClass().getResource("auth.css").toExternalForm());
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("修改密码");
        dialog.setScene(scene);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        saveBtn.setOnAction(e -> {
            String oldPass = oldField.getText();
            String newPass = newField.getText();
            String conf = confirmField.getText();
            if (oldPass.isEmpty() || newPass.isEmpty() || conf.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "请填写完整信息");
                return;
            }
            if (!newPass.equals(conf)) {
                showAlert(Alert.AlertType.WARNING, "两次密码不一致");
                return;
            }
            String checkSql = "SELECT password_hash FROM users WHERE user_id=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hash = rs.getString(1);
                        if (!hash.equals(sha256(oldPass))) {
                            showAlert(Alert.AlertType.ERROR, "原密码错误");
                            return;
                        }
                    }
                }

                String update = "UPDATE users SET password_hash=? WHERE user_id=?";
                try (PreparedStatement ps2 = conn.prepareStatement(update)) {
                    ps2.setString(1, sha256(newPass));
                    ps2.setLong(2, currentUserId);
                    ps2.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "修改成功");
                    dialog.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "修改失败：" + ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String msg) {
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

    public static class PaperResult {
        private final long paperId;
        private final String title;
        private final String score;
        private final String takenAt;

        public PaperResult(long paperId, String title, String score, String takenAt) {
            this.paperId = paperId;
            this.title = title;
            this.score = score;
            this.takenAt = takenAt;
        }

        public long getPaperId() { return paperId; }
        public String getTitle() { return title; }
        public String getScore() { return score; }
        public String getTakenAt() { return takenAt; }
    }
}