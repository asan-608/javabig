package com.example.javabig;

import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML private RadioButton optionARadio;
    @FXML private RadioButton optionBRadio;
    @FXML private RadioButton optionCRadio;
    @FXML private RadioButton optionDRadio;
    @FXML private ToggleGroup optionsGroup;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;

    private final List<QuestionData> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    public void startExam(long paperId, String title) {
        paperTitleLabel.setText(title);
        loadQuestions(paperId);
        if (!questionList.isEmpty()) {
            currentIndex = 0;
            score = 0;
            showQuestion(questionList.get(currentIndex));
        } else {
            statusLabel.setText("试卷暂无题目");
            submitButton.setDisable(true);
        }
    }

    private void loadQuestions(long paperId) {
        questionList.clear();
        String sqlQ = "SELECT q.question_id, q.content, q.correct_answer " +
                "FROM paper_questions pq JOIN questions q ON pq.question_id = q.question_id " +
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
                    List<String> opts = new ArrayList<>();
                    try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                        psOpt.setLong(1, qid);
                        try (ResultSet rsOpt = psOpt.executeQuery()) {
                            while (rsOpt.next()) {
                                opts.add(rsOpt.getString(1));
                            }
                        }
                    }
                    if (opts.size() == 4) {
                        questionList.add(new QuestionData(content, opts, correct));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmitAnswer() {
        if (currentIndex >= questionList.size()) {
            return;
        }
        Toggle selected = optionsGroup.getSelectedToggle();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "请选择一个选项");
            return;
        }
        String ans = selected.getUserData().toString();
        QuestionData q = questionList.get(currentIndex);
        if (ans.equalsIgnoreCase(q.correctAnswer)) {
            score++;
        }
        currentIndex++;
        if (currentIndex < questionList.size()) {
            showQuestion(questionList.get(currentIndex));
        } else {
            alert(Alert.AlertType.INFORMATION,
                    "考试完成，得分：" + score + "/" + questionList.size());
            Stage stage = (Stage) paperTitleLabel.getScene().getWindow();
            stage.close();
        }
    }

    private void showQuestion(QuestionData q) {
        questionLabel.setText(q.content);
        optionARadio.setText(q.options.get(0));
        optionBRadio.setText(q.options.get(1));
        optionCRadio.setText(q.options.get(2));
        optionDRadio.setText(q.options.get(3));
        optionsGroup.selectToggle(null);
        statusLabel.setText("题目 " + (currentIndex + 1) + "/" + questionList.size());
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static class QuestionData {
        final String content;
        final List<String> options;
        final String correctAnswer;
        QuestionData(String content, List<String> options, String correct) {
            this.content = content;
            this.options = options;
            this.correctAnswer = correct;
        }
    }
}