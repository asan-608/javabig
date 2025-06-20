package com.example.javabig;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

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
        loadExamPapers();
        loadScores();
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
        String sql = "SELECT p.paper_id, p.title, er.score " +
                "FROM exam_papers p LEFT JOIN exam_results er " +
                "ON p.paper_id=er.paper_id AND er.user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer s = (Integer) rs.getObject(3);
                    String scoreText = s == null ? "未开始" : String.valueOf(s);
                    scoreTable.getItems().add(new PaperResult(
                            rs.getLong(1), rs.getString(2), scoreText));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            stage.setOnHiding(e -> loadScores());
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
    }

    @FXML
    private void handleLogout() {
        MainApp.showLogin();
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

        public PaperResult(long paperId, String title, String score) {
            this.paperId = paperId;
            this.title = title;
            this.score = score;
        }

        public long getPaperId() { return paperId; }
        public String getTitle() { return title; }
        public String getScore() { return score; }
    }
}