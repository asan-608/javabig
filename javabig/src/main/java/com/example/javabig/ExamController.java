package com.example.javabig;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExamController {
    @FXML private Label paperTitleLabel;
    @FXML private Label questionLabel;
    @FXML private VBox answerBox;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;

    private final List<QuestionData> questionList = new ArrayList<>();
    private final List<String> userAnswers = new ArrayList<>();
    private ToggleGroup singleGroup = null; // for single choice & true/false
    private List<CheckBox> multiChecks = null; // for multiple choice
    private TextField blankField = null; // for fill in blank
    private int currentIndex = 0;
    private int score = 0;
    private long currentPaperId = 0L;
    private long currentUserId = 0L;

    public void startExam(long paperId, String title, long userId) {
        this.currentPaperId = paperId;
        this.currentUserId = userId;
        paperTitleLabel.setText(title);
        loadQuestions(paperId);
        if (!questionList.isEmpty()) {
            currentIndex = 0;
            score = 0;
            userAnswers.clear();
            for (int i = 0; i < questionList.size(); i++) {
                userAnswers.add(null);
            }
            showQuestion(questionList.get(currentIndex));
        } else {
            statusLabel.setText("试卷暂无题目");
            submitButton.setDisable(true);
        }
    }

    private void loadQuestions(long paperId) {
        questionList.clear();
        String sqlQ = "SELECT q.question_id, q.content, q.correct_answer, qt.code " +
                "FROM paper_questions pq " +
                "JOIN questions q ON pq.question_id = q.question_id " +
                "JOIN question_types qt ON q.type_id = qt.type_id " +
                "WHERE pq.paper_id=? ORDER BY pq.sequence";
        String sqlOpt = "SELECT content FROM question_options WHERE question_id=? ORDER BY sequence";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement psQ = conn.prepareStatement(sqlQ)) {
            psQ.setLong(1, paperId);
            try (ResultSet rsQ = psQ.executeQuery()) {
                while (rsQ.next()) {
                    long qid = rsQ.getLong(1);
                    String content = rsQ.getString(2);
                    String correct = rsQ.getString(3);
                    String code = rsQ.getString(4);
                    List<String> opts = new ArrayList<>();
                    if ("single_choice".equals(code) || "multiple_choice".equals(code)) {
                        try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                            psOpt.setLong(1, qid);
                            try (ResultSet rsOpt = psOpt.executeQuery()) {
                                while (rsOpt.next()) {
                                    opts.add(rsOpt.getString(1));
                                }
                            }
                        }
                    }
                    questionList.add(new QuestionData(content, code, opts, correct));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrevQuestion() {
        saveCurrentAnswer();
        if (currentIndex > 0) {
            currentIndex--;
            showQuestion(questionList.get(currentIndex));
        }
    }

    @FXML
    private void handleNextQuestion() {
        saveCurrentAnswer();
        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            showQuestion(questionList.get(currentIndex));
        }
    }

    @FXML
    private void handleSubmitPaper() {
        saveCurrentAnswer();
        score = 0;
        for (int i = 0; i < questionList.size(); i++) {
            QuestionData q = questionList.get(i);
            String ans = userAnswers.get(i);
            if (ans == null) continue;
            if ("multiple_choice".equals(q.typeCode)) {
                String correct = q.correctAnswer == null ? "" : q.correctAnswer;
                if (ans.replaceAll("\\s", "").equalsIgnoreCase(correct)) {
                    score++;
                }
            } else {
                if (ans.equalsIgnoreCase(q.correctAnswer)) {
                    score++;
                }
            }
        }
        alert(Alert.AlertType.INFORMATION,
                "考试完成，得分：" + score + "/" + questionList.size());
        saveResult();
        Stage stage = (Stage) paperTitleLabel.getScene().getWindow();
        stage.close();
    }

    private void saveResult() {
        String sql = "INSERT INTO exam_results (user_id, paper_id, score, taken_at) " +
                "VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE score=?, taken_at=NOW()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            ps.setLong(2, currentPaperId);
            ps.setInt(3, score);
            ps.setInt(4, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showQuestion(QuestionData q) {
        questionLabel.setText(q.content);
        answerBox.getChildren().clear();
        singleGroup = null;
        multiChecks = null;
        blankField = null;

        if ("single_choice".equals(q.typeCode)) {
            singleGroup = new ToggleGroup();
            for (int i = 0; i < q.options.size(); i++) {
                RadioButton rb = new RadioButton(q.options.get(i));
                rb.setUserData(String.valueOf((char)('A' + i)));
                rb.setToggleGroup(singleGroup);
                answerBox.getChildren().add(rb);
            }
        } else if ("multiple_choice".equals(q.typeCode)) {
            multiChecks = new ArrayList<>();
            for (int i = 0; i < q.options.size(); i++) {
                CheckBox cb = new CheckBox(q.options.get(i));
                cb.setUserData(String.valueOf((char)('A' + i)));
                multiChecks.add(cb);
                answerBox.getChildren().add(cb);
            }
        } else if ("fill_blank".equals(q.typeCode)) {
            blankField = new TextField();
            // 限制宽度为 200 像素
            blankField.setPrefWidth(200);
            blankField.setMaxWidth(200);
            // 可选：设置最小宽度，防止过窄
            blankField.setMinWidth(100);
            answerBox.getChildren().add(blankField);
        } else if ("true_false".equals(q.typeCode)) {
            singleGroup = new ToggleGroup();
            RadioButton t = new RadioButton("正确");
            t.setUserData("T");
            t.setToggleGroup(singleGroup);
            RadioButton f = new RadioButton("错误");
            f.setUserData("F");
            f.setToggleGroup(singleGroup);
            answerBox.getChildren().add(new HBox(10, t, f));
        }

        // restore answer if exists
        String saved = userAnswers.get(currentIndex);
        if (saved != null) {
            if ("single_choice".equals(q.typeCode) || "true_false".equals(q.typeCode)) {
                for (Toggle t : singleGroup.getToggles()) {
                    if (saved.equalsIgnoreCase(String.valueOf(t.getUserData()))) {
                        singleGroup.selectToggle(t);
                        break;
                    }
                }
            } else if ("multiple_choice".equals(q.typeCode)) {
                for (CheckBox cb : multiChecks) {
                    if (saved.contains(String.valueOf(cb.getUserData()))) {
                        cb.setSelected(true);
                    }
                }
            } else if ("fill_blank".equals(q.typeCode)) {
                blankField.setText(saved);
            }
        }

        prevButton.setDisable(currentIndex == 0);
        nextButton.setDisable(currentIndex == questionList.size() - 1);
        statusLabel.setText("题目 " + (currentIndex + 1) + "/" + questionList.size());
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    /** 保存当前题目的作答 */
    private void saveCurrentAnswer() {
        if (currentIndex >= questionList.size()) return;
        QuestionData q = questionList.get(currentIndex);
        String ans = null;
        if ("single_choice".equals(q.typeCode) || "true_false".equals(q.typeCode)) {
            if (singleGroup != null && singleGroup.getSelectedToggle() != null) {
                ans = String.valueOf(singleGroup.getSelectedToggle().getUserData());
            }
        } else if ("multiple_choice".equals(q.typeCode)) {
            if (multiChecks != null) {
                StringBuilder sb = new StringBuilder();
                for (CheckBox cb : multiChecks) {
                    if (cb.isSelected()) sb.append(cb.getUserData());
                }
                if (sb.length() > 0) ans = sb.toString();
            }
        } else if ("fill_blank".equals(q.typeCode)) {
            if (blankField != null) ans = blankField.getText().trim();
        }
        userAnswers.set(currentIndex, ans);
    }

    private static class QuestionData {
        final String content;
        final String typeCode;
        final List<String> options;
        final String correctAnswer;
        QuestionData(String content, String typeCode, List<String> options, String correct) {
            this.content = content;
            this.typeCode = typeCode;
            this.options = options;
            this.correctAnswer = correct;
        }
    }
}