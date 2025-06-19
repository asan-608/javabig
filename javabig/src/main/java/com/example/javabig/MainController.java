package com.example.javabig;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    @FXML private Label questionLabel;
    @FXML private RadioButton optionARadio;
    @FXML private RadioButton optionBRadio;
    @FXML private RadioButton optionCRadio;
    @FXML private RadioButton optionDRadio;
    @FXML private Button startButton;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;
    @FXML private ToggleGroup optionsGroup;

    private final List<QuestionData> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    @FXML
    private void handleStartTest() {
        loadRandomQuestions();
        if (questionList.isEmpty()) {
            alert(AlertType.INFORMATION, "题库为空，请联系教师添加题目");
            return;
        }
        currentIndex = 0;
        score = 0;
        startButton.setDisable(true);
        submitButton.setDisable(false);
        showQuestion(questionList.get(currentIndex));
    }

    @FXML
    private void handleSubmitAnswer() {
        Toggle selected = optionsGroup.getSelectedToggle();
        if (selected == null) {
            alert(AlertType.WARNING, "请选择一个选项");
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
            alert(AlertType.INFORMATION, "测试完成，得分：" + score + "/" + questionList.size());
            resetView();
        }
    }

    private void resetView() {
        startButton.setDisable(false);
        submitButton.setDisable(true);
        questionLabel.setText("");
        optionsGroup.getToggles().forEach(t -> {
            RadioButton rb = (RadioButton) t;
            rb.setText("");
            rb.setSelected(false);
        });
        statusLabel.setText("");
        questionList.clear();
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

    private void loadRandomQuestions() {
        questionList.clear();
        String sqlQ = "SELECT question_id, content, correct_answer FROM questions ORDER BY RAND() LIMIT 5";
        String sqlOpt = "SELECT content FROM question_options WHERE question_id=? ORDER BY sequence";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement psQ = conn.prepareStatement(sqlQ)) {
            try (ResultSet rsQ = psQ.executeQuery()) {
                while (rsQ.next()) {
                    long id = rsQ.getLong(1);
                    String content = rsQ.getString(2);
                    String correct = rsQ.getString(3);
                    List<String> opts = new ArrayList<>();
                    try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                        psOpt.setLong(1, id);
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

    private void alert(AlertType type, String msg) {
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