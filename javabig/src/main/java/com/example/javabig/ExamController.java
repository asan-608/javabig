package com.example.javabig;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ExamController {
    @FXML private Label paperTitleLabel;

    public void setPaperTitle(String title) {
        paperTitleLabel.setText(title);
    }
}